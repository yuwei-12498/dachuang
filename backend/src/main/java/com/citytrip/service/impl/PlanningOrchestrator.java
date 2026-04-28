package com.citytrip.service.impl;

import com.citytrip.mapper.PoiMapper;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.domain.planning.ExternalPoiCandidateService;
import com.citytrip.service.domain.policy.MaxStopsPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Component
public class PlanningOrchestrator {

    public static final String ALGORITHM_VERSION = "hybrid-dp-beam-v2";
    public static final String RECALL_STRATEGY = "hybrid-usercf-content-v1";

    private static final Logger log = LoggerFactory.getLogger(PlanningOrchestrator.class);
    private static final int PLANNING_DB_FETCH_LIMIT = 200;
    private static final int RECALL_LIMIT = 18;
    private static final int EXTERNAL_RECALL_LIMIT = 8;
    private static final int MAX_RECALL_POOL_SIZE = RECALL_LIMIT + EXTERNAL_RECALL_LIMIT;
    private final PoiMapper poiMapper;
    private final ItineraryRouteOptimizer routeOptimizer;
    private final HybridPoiRecallService hybridPoiRecallService;
    private final ExternalPoiCandidateService externalPoiCandidateService;
    private final Executor planningExecutor;
    private final Executor aiExecutor;
    private final long aiTimeoutMs;
    private final MaxStopsPolicy maxStopsPolicy;

    public PlanningOrchestrator(PoiMapper poiMapper,
                                ItineraryRouteOptimizer routeOptimizer,
                                HybridPoiRecallService hybridPoiRecallService,
                                ExternalPoiCandidateService externalPoiCandidateService,
                                MaxStopsPolicy maxStopsPolicy,
                                @Qualifier("itineraryPlanningExecutor") Executor planningExecutor,
                                @Qualifier("itineraryAiExecutor") Executor aiExecutor,
                                @Value("${app.planning.ai-timeout-ms:800}") long aiTimeoutMs) {
        this.poiMapper = poiMapper;
        this.routeOptimizer = routeOptimizer;
        this.hybridPoiRecallService = hybridPoiRecallService;
        this.externalPoiCandidateService = externalPoiCandidateService;
        this.maxStopsPolicy = maxStopsPolicy;
        this.planningExecutor = planningExecutor;
        this.aiExecutor = aiExecutor;
        this.aiTimeoutMs = aiTimeoutMs;
    }

    public PlanningResult generate(Long userId,
                                   GenerateReqDTO request,
                                   RouteItineraryBuilder itineraryBuilder,
                                   AiItineraryDecorator aiDecorator) {
        LocalDateTime planningStartedAt = LocalDateTime.now();
        GenerateReqDTO normalized = routeOptimizer.normalizeRequest(request);

        CompletableFuture<RecallStage> recallFuture = CompletableFuture.supplyAsync(
                () -> safeRecallStage(userId, normalized),
                planningExecutor
        );
        CompletableFuture<RoutePlanningSnapshot> routeFuture = recallFuture.thenApplyAsync(
                stage -> routePlanningSnapshot(stage, normalized),
                planningExecutor
        );
        CompletableFuture<ItineraryVO> baseFuture = routeFuture.thenApplyAsync(
                itineraryBuilder::build,
                planningExecutor
        );
        CompletableFuture<ItineraryVO> decoratedFuture = baseFuture.thenCompose(baseItinerary -> CompletableFuture
                .supplyAsync(() -> aiDecorator.decorate(normalized, baseItinerary), aiExecutor)
                .completeOnTimeout(baseItinerary, aiTimeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    log.warn("AI explain stage fallback to rule-based itinerary, reason={}", ex.getMessage());
                    return baseItinerary;
                }));

        return routeFuture.thenCombine(decoratedFuture, (snapshot, itinerary) -> {
            boolean success = itinerary != null && itinerary.getOptions() != null && !itinerary.getOptions().isEmpty();
            String failReason = success ? null : "当前时间窗与约束条件下未找到可执行路线。";
            return new PlanningResult(
                    snapshot.normalizedRequest(),
                    itinerary,
                    snapshot.rawCandidateCount(),
                    snapshot.filteredCandidateCount(),
                    snapshot.finalCandidateCount(),
                    snapshot.maxStops(),
                    snapshot.generatedRouteCount(),
                    itinerary == null || itinerary.getOptions() == null ? 0 : itinerary.getOptions().size(),
                    ALGORITHM_VERSION,
                    snapshot.recallStrategy(),
                    success,
                    failReason,
                    planningStartedAt
            );
        }).join();
    }

    private RecallStage recallStage(Long userId, GenerateReqDTO normalized) {
        List<Poi> rawCandidates = poiMapper.selectPlanningCandidates(
                normalized != null && Boolean.TRUE.equals(normalized.getIsRainy()),
                normalized == null ? null : normalized.getWalkingLevel(),
                normalized == null ? null : normalized.getCityCode(),
                normalized == null ? null : normalized.getCityName(),
                PLANNING_DB_FETCH_LIMIT
        );
        HybridPoiRecallService.RecallResult recallResult = hybridPoiRecallService.recall(
                userId,
                normalized,
                rawCandidates,
                RECALL_LIMIT
        );
        List<Poi> recalledCandidates = recallResult.recalledCandidates() == null
                ? Collections.emptyList()
                : recallResult.recalledCandidates();
        List<Poi> externalCandidates = recallExternalCandidates(recalledCandidates, normalized);
        List<Poi> mergedCandidates = mergeRecalledCandidates(recalledCandidates, externalCandidates);
        String recallStrategy = normalizeRecallStrategy(recallResult.recallStrategy());
        if (!externalCandidates.isEmpty()) {
            log.info("Planning recall merged {} external POIs from GEO API.", externalCandidates.size());
            recallStrategy = recallStrategy + "+geo-poi";
        }
        return new RecallStage(
                rawCandidates == null ? Collections.emptyList() : rawCandidates,
                recallResult.filteredCandidates(),
                mergedCandidates,
                recallStrategy
        );
    }

    private RecallStage safeRecallStage(Long userId, GenerateReqDTO normalized) {
        try {
            return recallStage(userId, normalized);
        } catch (Exception ex) {
            log.warn("Planning recall stage fallback to empty candidates, reason={}", ex.getMessage(), ex);
            return new RecallStage(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    RECALL_STRATEGY + "-degraded"
            );
        }
    }

    private List<Poi> recallExternalCandidates(List<Poi> baseCandidates, GenerateReqDTO normalized) {
        if (externalPoiCandidateService == null || baseCandidates == null || baseCandidates.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Poi> external = externalPoiCandidateService.recallForReplan(baseCandidates, normalized, EXTERNAL_RECALL_LIMIT);
            if (external == null || external.isEmpty()) {
                return Collections.emptyList();
            }
            List<Poi> prepared = routeOptimizer.prepareCandidates(external, normalized, false);
            return prepared == null ? Collections.emptyList() : prepared;
        } catch (Exception ex) {
            log.warn("External GEO recall failed during generate stage, fallback to local pool only. reason={}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Poi> mergeRecalledCandidates(List<Poi> recalledCandidates, List<Poi> externalCandidates) {
        if ((recalledCandidates == null || recalledCandidates.isEmpty())
                && (externalCandidates == null || externalCandidates.isEmpty())) {
            return Collections.emptyList();
        }
        List<Poi> merged = new ArrayList<>();
        Set<String> dedupeKeys = new LinkedHashSet<>();

        appendCandidates(merged, dedupeKeys, recalledCandidates);
        appendCandidates(merged, dedupeKeys, externalCandidates);

        if (merged.size() > MAX_RECALL_POOL_SIZE) {
            return new ArrayList<>(merged.subList(0, MAX_RECALL_POOL_SIZE));
        }
        return merged;
    }

    private void appendCandidates(List<Poi> merged, Set<String> dedupeKeys, List<Poi> candidates) {
        if (merged == null || dedupeKeys == null || candidates == null || candidates.isEmpty()) {
            return;
        }
        for (Poi candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            String key = buildCandidateDedupeKey(candidate);
            if (dedupeKeys.add(key)) {
                merged.add(candidate);
            }
        }
    }

    private String buildCandidateDedupeKey(Poi poi) {
        if (poi == null) {
            return "null";
        }
        if (poi.getId() != null) {
            return "id:" + poi.getId();
        }
        String name = StringUtils.hasText(poi.getName()) ? poi.getName().trim().toLowerCase(Locale.ROOT) : "";
        String lat = poi.getLatitude() == null ? "" : poi.getLatitude().toPlainString();
        String lon = poi.getLongitude() == null ? "" : poi.getLongitude().toPlainString();
        return name + "|" + lat + "|" + lon;
    }

    private RoutePlanningSnapshot routePlanningSnapshot(RecallStage recallStage, GenerateReqDTO normalized) {
        List<Poi> candidates = recallStage.recalledCandidates() == null ? Collections.emptyList() : recallStage.recalledCandidates();
        candidates = mergeWithMustVisitCandidates(candidates, recallStage.filteredCandidates(), normalized);
        int maxStops = maxStopsPolicy.resolve(normalized, candidates.size());
        int tripDayCount = resolveTripDayCount(normalized);
        List<ItineraryRouteOptimizer.RouteOption> rankedRoutes = tripDayCount > 1
                ? rankMultiDayRoutes(candidates, normalized, maxStops, tripDayCount)
                : routeOptimizer.rankRoutes(candidates, normalized, maxStops);
        return new RoutePlanningSnapshot(
                normalized,
                recallStage.rawCandidates().size(),
                recallStage.filteredCandidates().size(),
                candidates.size(),
                maxStops,
                rankedRoutes == null ? Collections.emptyList() : rankedRoutes,
                recallStage.recallStrategy()
        );
    }

    private List<ItineraryRouteOptimizer.RouteOption> rankMultiDayRoutes(List<Poi> candidates,
                                                                          GenerateReqDTO normalized,
                                                                          int maxStops,
                                                                          int tripDayCount) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        List<Poi> remaining = new ArrayList<>(candidates);
        List<Poi> mergedPath = new ArrayList<>();
        List<String> daySignatures = new ArrayList<>();
        double totalUtility = 0D;

        for (int day = 0; day < tripDayCount; day++) {
            if (remaining.isEmpty()) {
                break;
            }
            int dailyMaxStops = resolveDailyMaxStops(maxStops, tripDayCount, remaining.size());
            if (dailyMaxStops <= 0) {
                break;
            }
            List<ItineraryRouteOptimizer.RouteOption> dailyRanked = routeOptimizer.rankRoutes(remaining, normalized, dailyMaxStops);
            if (dailyRanked == null || dailyRanked.isEmpty() || dailyRanked.get(0) == null || dailyRanked.get(0).path().isEmpty()) {
                break;
            }

            ItineraryRouteOptimizer.RouteOption bestForDay = dailyRanked.get(0);
            mergedPath.addAll(bestForDay.path());
            daySignatures.add(bestForDay.signature());
            totalUtility += bestForDay.utility();

            Set<Long> usedPoiIds = bestForDay.path().stream()
                    .map(Poi::getId)
                    .collect(java.util.stream.Collectors.toSet());
            remaining.removeIf(poi -> poi != null && poi.getId() != null && usedPoiIds.contains(poi.getId()));
        }

        if (mergedPath.isEmpty()) {
            return routeOptimizer.rankRoutes(candidates, normalized, maxStops);
        }
        String combinedSignature = String.join("|", daySignatures);
        return List.of(new ItineraryRouteOptimizer.RouteOption(mergedPath, combinedSignature, totalUtility));
    }

    private int resolveTripDayCount(GenerateReqDTO normalized) {
        if (normalized == null || normalized.getTripDays() == null) {
            return 1;
        }
        return Math.max(1, (int) Math.round(normalized.getTripDays()));
    }

    private int resolveDailyMaxStops(int maxStops, int tripDayCount, int remainingSize) {
        if (remainingSize <= 0 || maxStops <= 0) {
            return 0;
        }
        int average = (int) Math.ceil(maxStops * 1.0D / Math.max(tripDayCount, 1));
        int bounded = Math.max(1, average);
        return Math.min(bounded, remainingSize);
    }

    private String normalizeRecallStrategy(String recallStrategy) {
        return StringUtils.hasText(recallStrategy) ? recallStrategy.trim() : RECALL_STRATEGY;
    }

    private List<Poi> mergeWithMustVisitCandidates(List<Poi> recalledCandidates,
                                                   List<Poi> filteredCandidates,
                                                   GenerateReqDTO normalizedRequest) {
        List<String> mustVisitKeywords = normalizeMustVisitKeywords(normalizedRequest);
        if (mustVisitKeywords.isEmpty()) {
            return recalledCandidates == null ? Collections.emptyList() : recalledCandidates;
        }

        List<Poi> merged = recalledCandidates == null ? new ArrayList<>() : new ArrayList<>(recalledCandidates);
        Set<Long> existingIds = new LinkedHashSet<>();
        for (Poi poi : merged) {
            if (poi != null && poi.getId() != null) {
                existingIds.add(poi.getId());
            }
        }

        if (filteredCandidates != null) {
            for (Poi poi : filteredCandidates) {
                if (!matchesMustVisit(poi, mustVisitKeywords)) {
                    continue;
                }
                Long poiId = poi.getId();
                if (poiId == null || existingIds.add(poiId)) {
                    merged.add(poi);
                }
            }
        }

        return merged;
    }

    private List<String> normalizeMustVisitKeywords(GenerateReqDTO request) {
        if (request == null || request.getMustVisitPoiNames() == null || request.getMustVisitPoiNames().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> normalized = new ArrayList<>();
        for (String keyword : request.getMustVisitPoiNames()) {
            if (StringUtils.hasText(keyword)) {
                String lower = keyword.trim().toLowerCase(Locale.ROOT);
                normalized.add(lower);
                if (lower.contains("ifs") && !normalized.contains("ifs")) {
                    normalized.add("ifs");
                }
            }
        }
        return normalized;
    }

    private boolean matchesMustVisit(Poi poi, List<String> mustVisitKeywords) {
        if (poi == null || !StringUtils.hasText(poi.getName()) || mustVisitKeywords == null || mustVisitKeywords.isEmpty()) {
            return false;
        }
        String poiNameLower = poi.getName().toLowerCase();
        return mustVisitKeywords.stream()
                .anyMatch(keyword -> poiNameLower.contains(keyword) || keyword.contains(poiNameLower));
    }

    @FunctionalInterface
    public interface RouteItineraryBuilder {
        ItineraryVO build(RoutePlanningSnapshot snapshot);
    }

    @FunctionalInterface
    public interface AiItineraryDecorator {
        ItineraryVO decorate(GenerateReqDTO normalizedRequest, ItineraryVO baseItinerary);
    }

    public record RoutePlanningSnapshot(GenerateReqDTO normalizedRequest,
                                        int rawCandidateCount,
                                        int filteredCandidateCount,
                                        int finalCandidateCount,
                                        int maxStops,
                                        List<ItineraryRouteOptimizer.RouteOption> rankedRoutes,
                                        String recallStrategy) {

        public int generatedRouteCount() {
            return rankedRoutes == null ? 0 : rankedRoutes.size();
        }
    }

    public record PlanningResult(GenerateReqDTO normalizedRequest,
                                 ItineraryVO itinerary,
                                 int rawCandidateCount,
                                 int filteredCandidateCount,
                                 int finalCandidateCount,
                                 int maxStops,
                                 int generatedRouteCount,
                                 int displayedOptionCount,
                                 String algorithmVersion,
                                 String recallStrategy,
                                 boolean success,
                                 String failReason,
                                 LocalDateTime planningStartedAt) {
    }

    private record RecallStage(List<Poi> rawCandidates,
                               List<Poi> filteredCandidates,
                               List<Poi> recalledCandidates,
                               String recallStrategy) {
    }
}
