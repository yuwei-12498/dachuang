package com.citytrip.service.impl;

import com.citytrip.mapper.PoiMapper;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.domain.policy.MaxStopsPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
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
    private final PoiMapper poiMapper;
    private final ItineraryRouteOptimizer routeOptimizer;
    private final HybridPoiRecallService hybridPoiRecallService;
    private final Executor planningExecutor;
    private final Executor aiExecutor;
    private final long aiTimeoutMs;
    private final MaxStopsPolicy maxStopsPolicy;

    public PlanningOrchestrator(PoiMapper poiMapper,
                                ItineraryRouteOptimizer routeOptimizer,
                                HybridPoiRecallService hybridPoiRecallService,
                                MaxStopsPolicy maxStopsPolicy,
                                @Qualifier("itineraryPlanningExecutor") Executor planningExecutor,
                                @Qualifier("itineraryAiExecutor") Executor aiExecutor,
                                @Value("${app.planning.ai-timeout-ms:800}") long aiTimeoutMs) {
        this.poiMapper = poiMapper;
        this.routeOptimizer = routeOptimizer;
        this.hybridPoiRecallService = hybridPoiRecallService;
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
                PLANNING_DB_FETCH_LIMIT
        );
        HybridPoiRecallService.RecallResult recallResult = hybridPoiRecallService.recall(
                userId,
                normalized,
                rawCandidates,
                RECALL_LIMIT
        );
        return new RecallStage(
                rawCandidates == null ? Collections.emptyList() : rawCandidates,
                recallResult.filteredCandidates(),
                recallResult.recalledCandidates(),
                normalizeRecallStrategy(recallResult.recallStrategy())
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

    private RoutePlanningSnapshot routePlanningSnapshot(RecallStage recallStage, GenerateReqDTO normalized) {
        List<Poi> candidates = recallStage.recalledCandidates() == null ? Collections.emptyList() : recallStage.recalledCandidates();
        int maxStops = maxStopsPolicy.resolve(normalized, candidates.size());
        List<ItineraryRouteOptimizer.RouteOption> rankedRoutes = routeOptimizer.rankRoutes(candidates, normalized, maxStops);
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

    private String normalizeRecallStrategy(String recallStrategy) {
        return StringUtils.hasText(recallStrategy) ? recallStrategy.trim() : RECALL_STRATEGY;
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
