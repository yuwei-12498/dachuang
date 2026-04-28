package com.citytrip.service.domain.ai;

import com.citytrip.assembler.ItineraryComparisonAssembler;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.vo.DepartureLegEstimateVO;
import com.citytrip.model.vo.ItineraryRouteDecorationVO;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryOptionVO;
import com.citytrip.model.vo.RouteNodeDecorationVO;
import com.citytrip.model.vo.SegmentTransportAnalysisVO;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.LlmService;
import com.citytrip.service.domain.planning.NodeNearbyEnrichmentService;
import com.citytrip.service.geo.GeoPoint;
import com.citytrip.service.geo.GeoRouteEstimate;
import com.citytrip.service.geo.GeoSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
public class ItineraryAiDecorationService {
    private static final Logger log = LoggerFactory.getLogger(ItineraryAiDecorationService.class);
    private static final long DEPARTURE_LEG_TIMEOUT_CAP_MS = 500L;
    private static final long DECORATION_BUDGET_RESERVE_MS = 160L;

    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final ItineraryComparisonAssembler itineraryComparisonAssembler;
    private final NodeNearbyEnrichmentService nodeNearbyEnrichmentService;
    private final AsyncTaskExecutor itineraryAiCallExecutor;
    private final long aiTimeoutMs;
    @Autowired(required = false)
    private GeoSearchService geoSearchService;

    @Autowired
    public ItineraryAiDecorationService(LlmService llmService,
                                        ObjectMapper objectMapper,
                                        ItineraryComparisonAssembler itineraryComparisonAssembler,
                                        NodeNearbyEnrichmentService nodeNearbyEnrichmentService,
                                        @Qualifier("itineraryAiCallExecutor") AsyncTaskExecutor itineraryAiCallExecutor,
                                        @Value("${app.planning.ai-timeout-ms:25000}") long aiTimeoutMs) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.itineraryComparisonAssembler = itineraryComparisonAssembler;
        this.nodeNearbyEnrichmentService = nodeNearbyEnrichmentService;
        this.itineraryAiCallExecutor = itineraryAiCallExecutor;
        this.aiTimeoutMs = Math.max(aiTimeoutMs, 1L);
    }

    public ItineraryAiDecorationService(LlmService llmService,
                                        ObjectMapper objectMapper,
                                        ItineraryComparisonAssembler itineraryComparisonAssembler,
                                        @Qualifier("itineraryAiCallExecutor") AsyncTaskExecutor itineraryAiCallExecutor,
                                        long aiTimeoutMs) {
        this(
                llmService,
                objectMapper,
                itineraryComparisonAssembler,
                null,
                itineraryAiCallExecutor,
                aiTimeoutMs
        );
    }

    public ItineraryVO decorateWithLlm(ItineraryVO baseItinerary, GenerateReqDTO req) {
        ItineraryVO itinerary = copyItinerary(baseItinerary);
        if (itinerary == null) {
            return null;
        }
        AiBudget budget = new AiBudget(resolveEffectiveAiBudgetMs());
        Map<String, DepartureLegEstimateVO> departureLegCache = new HashMap<>();
        try {
            decorateDepartureLeg(req, itinerary.getNodes(), departureLegCache, budget);
            ItineraryRouteDecorationVO routeDecoration = resolveRouteExperienceDecoration(req, itinerary.getNodes(), budget);
            if (hasUsefulRouteDecoration(routeDecoration)) {
                applyRouteExperienceDecoration(itinerary, req, routeDecoration);
            } else {
                Map<String, String> nodeReasonCache = new HashMap<>();
                Map<String, NodeWarmTipBundle> nodeWarmTipCache = new HashMap<>();
                Map<String, SegmentTransportAnalysisVO> segmentTransportCache = new HashMap<>();
                decorateNodes(req, itinerary.getNodes(), nodeReasonCache, nodeWarmTipCache, segmentTransportCache, budget);
                if (!StringUtils.hasText(itinerary.getTips())) {
                    applyWarmTips(itinerary, req, budget);
                }
            }
        } catch (RuntimeException ex) {
            log.warn("AI decoration fallback to partially decorated itinerary, reason={}", summarizeExecutionFailure(ex));
        }
        if (!StringUtils.hasText(itinerary.getTips())) {
            applyWarmTips(itinerary, req, budget);
        }
        applyNearbyEnrichment(itinerary, req);
        return itinerary;
    }

    public void applyWarmTips(ItineraryVO itinerary, GenerateReqDTO req) {
        applyWarmTips(itinerary, req, new AiBudget(aiTimeoutMs));
        applyNearbyEnrichment(itinerary, req);
    }

    private void applyWarmTips(ItineraryVO itinerary, GenerateReqDTO req, AiBudget budget) {
        if (itinerary == null) {
            return;
        }

        String routeWarmTip = resolveTextWithFallback(
                () -> llmService.generateRouteWarmTip(req, itinerary.getNodes()),
                "generateRouteWarmTip",
                budget
        );
        String fallback = buildFallbackRouteWarmTip(req, itinerary.getNodes());
        itinerary.setTips(normalizeSingleTip(keepChineseOrFallback(routeWarmTip, fallback), fallback));
    }

    private ItineraryRouteDecorationVO resolveRouteExperienceDecoration(GenerateReqDTO req,
                                                                        List<ItineraryNodeVO> nodes,
                                                                        AiBudget budget) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        ItineraryRouteDecorationVO generated = resolveValueWithFallback(
                () -> llmService.decorateRouteExperience(req, nodes),
                "decorateRouteExperience",
                budget
        );
        return hasUsefulRouteDecoration(generated) ? generated : null;
    }

    private boolean hasUsefulRouteDecoration(ItineraryRouteDecorationVO decoration) {
        if (decoration == null) {
            return false;
        }
        if (StringUtils.hasText(decoration.getRouteWarmTip())) {
            return true;
        }
        if (decoration.getNodes() == null || decoration.getNodes().isEmpty()) {
            return false;
        }
        return decoration.getNodes().stream().anyMatch(item ->
                item != null
                        && ((item.getWarmTips() != null && !item.getWarmTips().isEmpty())
                        || StringUtils.hasText(item.getTransportMode())
                        || StringUtils.hasText(item.getNarrative())));
    }

    private void applyRouteExperienceDecoration(ItineraryVO itinerary,
                                                GenerateReqDTO req,
                                                ItineraryRouteDecorationVO decoration) {
        if (itinerary == null || decoration == null || itinerary.getNodes() == null || itinerary.getNodes().isEmpty()) {
            return;
        }
        if (StringUtils.hasText(decoration.getRouteWarmTip())) {
            itinerary.setTips(decoration.getRouteWarmTip().replaceAll("[\\r\\n]+", " ").trim());
        }
        if (decoration.getNodes() == null || decoration.getNodes().isEmpty()) {
            return;
        }

        for (RouteNodeDecorationVO item : decoration.getNodes()) {
            if (item == null || item.getIndex() == null) {
                continue;
            }
            int index = item.getIndex();
            if (index < 0 || index >= itinerary.getNodes().size()) {
                continue;
            }
            ItineraryNodeVO node = itinerary.getNodes().get(index);
            if (node == null) {
                continue;
            }

            List<String> normalizedTips = normalizeWarmTips(item.getWarmTips());
            if (!normalizedTips.isEmpty()) {
                String selected = normalizedTips.get(ThreadLocalRandom.current().nextInt(normalizedTips.size()));
                applyWarmTipBundle(node, new NodeWarmTipBundle(normalizedTips, selected));
            }

            ItineraryNodeVO fromNode = index == 0 ? null : itinerary.getNodes().get(index - 1);
            String factualMode = resolveExistingSegmentTransportMode(fromNode, node);
            String normalizedMode = normalizeTransportMode(item.getTransportMode(), factualMode, fromNode, node);
            String fallbackNarrative = buildFallbackSegmentNarrative(req, fromNode, node, normalizedMode);
            SegmentTransportAnalysisVO analysis = new SegmentTransportAnalysisVO();
            analysis.setTransportMode(normalizedMode);
            analysis.setNarrative(normalizeNarrative(item.getNarrative(), fallbackNarrative));
            applySegmentTransportAnalysis(node, analysis, index == 0);
        }
    }

    private List<String> normalizeWarmTips(List<String> warmTips) {
        if (warmTips == null || warmTips.isEmpty()) {
            return List.of();
        }
        Set<String> deduplicated = new LinkedHashSet<>();
        for (String tip : warmTips) {
            String normalized = normalizeSingleTip(keepChineseOrFallback(tip, null), null);
            if (StringUtils.hasText(normalized)) {
                deduplicated.add(normalized);
            }
            if (deduplicated.size() >= 5) {
                break;
            }
        }
        return new ArrayList<>(deduplicated);
    }

    private void decorateNodes(GenerateReqDTO req,
                               List<ItineraryNodeVO> nodes,
                               Map<String, String> nodeReasonCache,
                               Map<String, NodeWarmTipBundle> nodeWarmTipCache,
                               Map<String, SegmentTransportAnalysisVO> segmentTransportCache,
                               AiBudget budget) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        ItineraryNodeVO prevNode = null;
        for (ItineraryNodeVO node : nodes) {
            if (node == null) {
                continue;
            }
            String nodeKey = buildNodeKey(node);
            String nodeReason = nodeReasonCache.computeIfAbsent(nodeKey, key -> {
                String reason = resolveTextWithFallback(
                        () -> llmService.explainPoiChoice(req, node),
                        "explainPoiChoice",
                        budget
                );
                reason = keepChineseOrFallback(reason, node.getSysReason());
                return StringUtils.hasText(reason) ? reason.trim() : null;
            });
            if (StringUtils.hasText(nodeReason)) {
                node.setSysReason(nodeReason);
            }

            NodeWarmTipBundle warmTipBundle = nodeWarmTipCache.computeIfAbsent(nodeKey, key -> resolveWarmTipBundleForNode(req, node, budget));
            applyWarmTipBundle(node, warmTipBundle);

            ItineraryNodeVO fromNode = prevNode;
            String segmentKey = buildSegmentKey(req, fromNode, node);
            SegmentTransportAnalysisVO segmentAnalysis = segmentTransportCache.computeIfAbsent(
                    segmentKey,
                    key -> resolveSegmentTransportAnalysis(req, fromNode, node, budget)
            );
            applySegmentTransportAnalysis(node, segmentAnalysis, fromNode == null);
            prevNode = node;
        }
    }

    private void decorateDepartureLeg(GenerateReqDTO req,
                                      List<ItineraryNodeVO> nodes,
                                      Map<String, DepartureLegEstimateVO> departureLegCache,
                                      AiBudget budget) {
        if (nodes == null || nodes.isEmpty() || !hasDepartureCoordinate(req)) {
            return;
        }
        ItineraryNodeVO firstNode = nodes.get(0);
        if (firstNode == null) {
            return;
        }

        String cacheKey = buildDepartureLegKey(req, firstNode);
        DepartureLegEstimateVO estimate;
        if (departureLegCache.containsKey(cacheKey)) {
            estimate = departureLegCache.get(cacheKey);
        } else {
            estimate = resolveDepartureLegEstimate(req, firstNode, budget);
            departureLegCache.put(cacheKey, estimate);
        }
        applyDepartureEstimate(firstNode, estimate);
    }

    private DepartureLegEstimateVO resolveDepartureLegEstimate(GenerateReqDTO req,
                                                               ItineraryNodeVO firstNode,
                                                               AiBudget budget) {
        DepartureLegEstimateVO geoEstimate = resolveDepartureLegEstimateByGeo(req, firstNode);
        if (hasAnyDepartureEstimate(geoEstimate)) {
            return geoEstimate;
        }

        DepartureLegEstimateVO llmEstimate = resolveValueWithFallback(
                () -> llmService.estimateDepartureLeg(req, firstNode),
                "estimateDepartureLeg",
                budget,
                DEPARTURE_LEG_TIMEOUT_CAP_MS
        );
        if (hasAnyDepartureEstimate(llmEstimate)) {
            return llmEstimate;
        }
        return buildDepartureFallback(req, firstNode);
    }

    private void applyDepartureEstimate(ItineraryNodeVO firstNode, DepartureLegEstimateVO estimate) {
        if (firstNode == null || !hasAnyDepartureEstimate(estimate)) {
            return;
        }
        if (estimate.getEstimatedMinutes() != null && estimate.getEstimatedMinutes() > 0) {
            firstNode.setDepartureTravelTime(estimate.getEstimatedMinutes());
        }
        if (estimate.getEstimatedDistanceKm() != null) {
            firstNode.setDepartureDistanceKm(estimate.getEstimatedDistanceKm().setScale(1, RoundingMode.HALF_UP));
        }
        if (StringUtils.hasText(estimate.getTransportMode())) {
            firstNode.setDepartureTransportMode(estimate.getTransportMode().trim());
        }
    }

    private boolean hasAnyDepartureEstimate(DepartureLegEstimateVO estimate) {
        return estimate != null
                && (estimate.getEstimatedMinutes() != null
                || estimate.getEstimatedDistanceKm() != null
                || StringUtils.hasText(estimate.getTransportMode()));
    }

    private DepartureLegEstimateVO buildDepartureFallback(GenerateReqDTO req, ItineraryNodeVO firstNode) {
        if (firstNode == null) {
            return null;
        }
        DepartureLegEstimateVO fallback = new DepartureLegEstimateVO();

        if (firstNode.getTravelTime() != null && firstNode.getTravelTime() > 0) {
            fallback.setEstimatedMinutes(firstNode.getTravelTime());
        }

        double roadDistanceKm = estimateRoadDistanceKm(req, firstNode);
        if (roadDistanceKm > 0D) {
            fallback.setEstimatedDistanceKm(BigDecimal.valueOf(roadDistanceKm).setScale(1, RoundingMode.HALF_UP));
        }

        String mode = resolveFallbackTransportMode(roadDistanceKm, fallback.getEstimatedMinutes());
        if (StringUtils.hasText(mode)) {
            fallback.setTransportMode(mode);
        }

        return hasAnyDepartureEstimate(fallback) ? fallback : null;
    }

    private String buildDepartureLegKey(GenerateReqDTO req, ItineraryNodeVO firstNode) {
        return String.join("#",
                defaultString(req != null && req.getDepartureLatitude() != null ? String.valueOf(req.getDepartureLatitude()) : null),
                defaultString(req != null && req.getDepartureLongitude() != null ? String.valueOf(req.getDepartureLongitude()) : null),
                defaultString(firstNode != null && firstNode.getPoiId() != null ? String.valueOf(firstNode.getPoiId()) : null),
                defaultString(firstNode == null ? null : firstNode.getPoiName())
        );
    }

    private boolean hasDepartureCoordinate(GenerateReqDTO req) {
        if (req == null || req.getDepartureLatitude() == null || req.getDepartureLongitude() == null) {
            return false;
        }
        double lat = req.getDepartureLatitude();
        double lng = req.getDepartureLongitude();
        return Math.abs(lat) <= 90D && Math.abs(lng) <= 180D;
    }

    private DepartureLegEstimateVO resolveDepartureLegEstimateByGeo(GenerateReqDTO req,
                                                                    ItineraryNodeVO firstNode) {
        if (geoSearchService == null
                || !hasDepartureCoordinate(req)
                || firstNode == null
                || firstNode.getLatitude() == null
                || firstNode.getLongitude() == null) {
            return null;
        }
        GeoPoint from = new GeoPoint(
                BigDecimal.valueOf(req.getDepartureLatitude()),
                BigDecimal.valueOf(req.getDepartureLongitude())
        );
        GeoPoint to = new GeoPoint(firstNode.getLatitude(), firstNode.getLongitude());
        if (!from.valid() || !to.valid()) {
            return null;
        }

        GeoRouteEstimate geoEstimate = geoSearchService
                .estimateTravel(from, to, req.getCityName(), null)
                .orElse(null);
        if (geoEstimate == null) {
            return null;
        }

        DepartureLegEstimateVO estimate = new DepartureLegEstimateVO();
        Integer minutes = geoEstimate.durationMinutes();
        if (minutes != null && minutes > 0) {
            estimate.setEstimatedMinutes(minutes);
        }
        BigDecimal distanceKm = geoEstimate.distanceKm();
        if (distanceKm != null && distanceKm.compareTo(BigDecimal.ZERO) > 0) {
            estimate.setEstimatedDistanceKm(distanceKm.setScale(1, RoundingMode.HALF_UP));
        }
        String mode = geoEstimate.transportMode();
        if (!StringUtils.hasText(mode)) {
            mode = resolveFallbackTransportMode(
                    estimate.getEstimatedDistanceKm() == null ? 0D : estimate.getEstimatedDistanceKm().doubleValue(),
                    estimate.getEstimatedMinutes()
            );
        }
        if (StringUtils.hasText(mode)) {
            estimate.setTransportMode(mode);
        }
        return hasAnyDepartureEstimate(estimate) ? estimate : null;
    }

    private NodeWarmTipBundle resolveWarmTipBundleForNode(GenerateReqDTO req, ItineraryNodeVO node, AiBudget budget) {
        String generated = resolveTextWithFallback(
                () -> llmService.generatePoiWarmTips(req, node),
                "generatePoiWarmTips",
                budget
        );
        generated = keepChineseOrFallback(generated, null);

        List<String> candidates = parseTipCandidates(generated);
        if (candidates.size() < 3) {
            buildFallbackPoiWarmTips(req, node).stream()
                    .filter(StringUtils::hasText)
                    .filter(tip -> !candidates.contains(tip))
                    .limit(5 - candidates.size())
                    .forEach(candidates::add);
        }
        String selected = candidates.isEmpty()
                ? null
                : candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        return new NodeWarmTipBundle(candidates, selected);
    }

    private void applyWarmTipBundle(ItineraryNodeVO node, NodeWarmTipBundle bundle) {
        if (node == null || bundle == null) {
            return;
        }
        if (!bundle.candidates().isEmpty()) {
            node.setWarmTipCandidates(new ArrayList<>(bundle.candidates()));
        }
        String selected = normalizeSingleTip(bundle.selectedTip(), node.getStatusNote());
        if (!StringUtils.hasText(selected) && !bundle.candidates().isEmpty()) {
            selected = normalizeSingleTip(bundle.candidates().get(0), node.getStatusNote());
        }
        if (StringUtils.hasText(selected)) {
            node.setSelectedWarmTip(selected);
            node.setStatusNote(selected);
        }
    }

    private SegmentTransportAnalysisVO resolveSegmentTransportAnalysis(GenerateReqDTO req,
                                                                      ItineraryNodeVO fromNode,
                                                                      ItineraryNodeVO toNode,
                                                                      AiBudget budget) {
        SegmentTransportAnalysisVO generated = resolveValueWithFallback(
                () -> llmService.analyzeSegmentTransport(req, fromNode, toNode),
                "analyzeSegmentTransport",
                budget
        );
        String factualMode = resolveExistingSegmentTransportMode(fromNode, toNode);
        String normalizedMode = normalizeTransportMode(generated == null ? null : generated.getTransportMode(), factualMode, fromNode, toNode);
        String fallbackNarrative = buildFallbackSegmentNarrative(req, fromNode, toNode, normalizedMode);
        String narrative = normalizeNarrative(generated == null ? null : generated.getNarrative(), fallbackNarrative);

        SegmentTransportAnalysisVO resolved = new SegmentTransportAnalysisVO();
        resolved.setTransportMode(normalizedMode);
        resolved.setNarrative(narrative);
        return resolved;
    }

    private void applySegmentTransportAnalysis(ItineraryNodeVO node,
                                               SegmentTransportAnalysisVO analysis,
                                               boolean firstStop) {
        if (node == null || analysis == null) {
            return;
        }
        if (StringUtils.hasText(analysis.getTransportMode())) {
            if (firstStop) {
                node.setDepartureTransportMode(analysis.getTransportMode().trim());
            }
            node.setTravelTransportMode(analysis.getTransportMode().trim());
        }
        if (StringUtils.hasText(analysis.getNarrative())) {
            node.setTravelNarrative(analysis.getNarrative().trim());
        }
    }

    private String resolveExistingSegmentTransportMode(ItineraryNodeVO fromNode, ItineraryNodeVO toNode) {
        if (toNode == null) {
            return null;
        }
        if (fromNode == null && StringUtils.hasText(toNode.getDepartureTransportMode())) {
            return toNode.getDepartureTransportMode().trim();
        }
        if (StringUtils.hasText(toNode.getTravelTransportMode())) {
            return toNode.getTravelTransportMode().trim();
        }
        if (StringUtils.hasText(toNode.getDepartureTransportMode())) {
            return toNode.getDepartureTransportMode().trim();
        }
        double distanceKm = resolveSegmentDistanceKm(fromNode, toNode);
        Integer minutes = resolveSegmentMinutes(fromNode, toNode);
        return resolveFallbackTransportMode(distanceKm, minutes);
    }

    private String buildSegmentKey(GenerateReqDTO req, ItineraryNodeVO fromNode, ItineraryNodeVO toNode) {
        String fromKey = fromNode == null
                ? "departure@" + defaultString(req != null && req.getDepartureLatitude() != null ? String.valueOf(req.getDepartureLatitude()) : null)
                + "," + defaultString(req != null && req.getDepartureLongitude() != null ? String.valueOf(req.getDepartureLongitude()) : null)
                : buildNodeKey(fromNode);
        return fromKey + "->" + buildNodeKey(toNode);
    }

    private String normalizeTransportMode(String candidate,
                                          String fallback,
                                          ItineraryNodeVO fromNode,
                                          ItineraryNodeVO toNode) {
        String raw = StringUtils.hasText(candidate) ? candidate.trim() : fallback;
        if (!StringUtils.hasText(raw)) {
            return resolveFallbackTransportMode(resolveSegmentDistanceKm(fromNode, toNode), resolveSegmentMinutes(fromNode, toNode));
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (normalized.contains("subway") || normalized.contains("metro")) {
            return "??+??";
        }
        if (normalized.contains("bus") || normalized.contains("transit")) {
            return "??+??";
        }
        if (normalized.contains("taxi") || normalized.contains("cab") || normalized.contains("drive") || normalized.contains("ride")) {
            return "??";
        }
        if (normalized.contains("bike") || normalized.contains("cycle")) {
            return "??";
        }
        if (normalized.contains("walk")) {
            return "??";
        }
        if (isEnglishDominant(raw) && StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        return raw;
    }

    private String normalizeNarrative(String candidate, String fallback) {
        String preferred = keepChineseOrFallback(candidate, fallback);
        String narrative = StringUtils.hasText(preferred) ? preferred.trim() : fallback;
        if (!StringUtils.hasText(narrative)) {
            return null;
        }
        narrative = narrative.replaceAll("[\r\n]+", " ").trim();
        if (narrative.length() > 72) {
            narrative = narrative.substring(0, 72);
        }
        return narrative;
    }

    private String buildFallbackSegmentNarrative(GenerateReqDTO req,
                                                 ItineraryNodeVO fromNode,
                                                 ItineraryNodeVO toNode,
                                                 String transportMode) {
        String origin = fromNode == null
                ? defaultString(req == null ? null : req.getDeparturePlaceName())
                : defaultString(fromNode.getPoiName());
        if (!StringUtils.hasText(origin)) {
            origin = "当前位置";
        }
        String destination = toNode == null ? "下一站" : defaultString(toNode.getPoiName());
        if (!StringUtils.hasText(destination)) {
            destination = "下一站";
        }
        Integer minutes = resolveSegmentMinutes(fromNode, toNode);
        double distanceKm = resolveSegmentDistanceKm(fromNode, toNode);
        String mode = StringUtils.hasText(transportMode)
                ? transportMode.trim()
                : resolveFallbackTransportMode(distanceKm, minutes);
        if (minutes != null && minutes > 0 && distanceKm > 0D) {
            return origin + "到" + destination + "这段约" + minutes + "分钟，约"
                    + BigDecimal.valueOf(distanceKm).setScale(1, RoundingMode.HALF_UP).toPlainString()
                    + "公里，用" + mode + "更稳妅。";
        }
        if (minutes != null && minutes > 0) {
            return origin + "到" + destination + "这段约" + minutes + "分钟，用" + mode + "衔接更顺。";
        }
        return origin + "到" + destination + "这段建议优先用" + mode + "，整体节奏更稳。";
    }

    private Integer resolveSegmentMinutes(ItineraryNodeVO fromNode, ItineraryNodeVO toNode) {
        if (toNode == null) {
            return null;
        }
        return fromNode == null ? toNode.getDepartureTravelTime() : toNode.getTravelTime();
    }

    private double resolveSegmentDistanceKm(ItineraryNodeVO fromNode, ItineraryNodeVO toNode) {
        if (toNode == null) {
            return 0D;
        }
        BigDecimal distance = fromNode == null ? toNode.getDepartureDistanceKm() : toNode.getTravelDistanceKm();
        return distance == null ? 0D : Math.max(0D, distance.doubleValue());
    }

    private String resolveTextWithFallback(Supplier<String> supplier, String scene, AiBudget budget) {
        String result = resolveValueWithFallback(supplier, scene, budget);
        return StringUtils.hasText(result) ? result.trim() : null;
    }

    private <T> T resolveValueWithFallback(Supplier<T> supplier, String scene, AiBudget budget) {
        return resolveValueWithFallback(supplier, scene, budget, null);
    }

    private <T> T resolveValueWithFallback(Supplier<T> supplier,
                                           String scene,
                                           AiBudget budget,
                                           Long timeoutCapMs) {
        long timeoutMs = budget == null ? aiTimeoutMs : budget.nextTimeoutMs();
        if (timeoutCapMs != null && timeoutCapMs > 0L) {
            timeoutMs = Math.min(timeoutMs, timeoutCapMs);
        }
        if (timeoutMs <= 0) {
            log.debug("Skip AI decoration because budget is exhausted, scene={}", scene);
            return null;
        }

        Future<T> future;
        try {
            future = itineraryAiCallExecutor.submit(supplier::get);
        } catch (RejectedExecutionException ex) {
            log.warn("AI decoration executor is saturated, scene={}, timeoutMs={}, reason={}", scene, timeoutMs, summarizeExecutionFailure(ex));
            return null;
        }

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            log.warn("AI decoration timed out, scene={}, timeoutMs={}", scene, timeoutMs);
            return null;
        } catch (InterruptedException ex) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            log.warn("AI decoration interrupted, scene={}", scene);
            return null;
        } catch (ExecutionException ex) {
            future.cancel(true);
            log.warn("AI decoration fallback, scene={}, reason={}", scene, summarizeExecutionFailure(ex));
            return null;
        } catch (RuntimeException ex) {
            future.cancel(true);
            log.warn("AI decoration fallback, scene={}, reason={}", scene, summarizeExecutionFailure(ex));
            return null;
        }
    }

    private String summarizeExecutionFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (StringUtils.hasText(current.getMessage())) {
                return current.getMessage().trim();
            }
            current = current.getCause();
        }
        return throwable == null ? "unknown" : throwable.getClass().getSimpleName();
    }

    private long resolveEffectiveAiBudgetMs() {
        if (aiTimeoutMs <= 1L) {
            return 1L;
        }
        long reserved = Math.min(DECORATION_BUDGET_RESERVE_MS, Math.max(40L, aiTimeoutMs / 5));
        long effective = aiTimeoutMs - reserved;
        return Math.max(120L, effective);
    }

    private record NodeWarmTipBundle(List<String> candidates, String selectedTip) {
        private NodeWarmTipBundle {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
        }
    }

    private static final class AiBudget {
        private final long deadlineNanos;

        private AiBudget(long totalTimeoutMs) {
            this.deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(totalTimeoutMs, 1L));
        }

        private long nextTimeoutMs() {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                return 0L;
            }
            return Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
        }
    }

    private List<String> parseTipCandidates(String rawTips) {
        if (!StringUtils.hasText(rawTips)) {
            return new ArrayList<>();
        }

        String normalized = rawTips.replace("\r", "\n").trim();
        String[] chunks = normalized.contains("\n")
                ? normalized.split("\n+")
                : normalized.split("[；;]");

        Set<String> deduplicated = new LinkedHashSet<>();
        for (String chunk : chunks) {
            if (!StringUtils.hasText(chunk)) {
                continue;
            }
            String cleaned = chunk.trim()
                    .replaceFirst("^[-•·●]\\s*", "")
                    .replaceFirst("^\\d+[.、]\\s*", "");
            if (!StringUtils.hasText(cleaned)) {
                continue;
            }
            if (isEnglishDominant(cleaned)) {
                continue;
            }
            if (cleaned.length() > 32) {
                cleaned = cleaned.substring(0, 32);
            }
            deduplicated.add(cleaned);
            if (deduplicated.size() >= 5) {
                break;
            }
        }
        return new ArrayList<>(deduplicated);
    }

    private List<String> buildFallbackPoiWarmTips(GenerateReqDTO req, ItineraryNodeVO node) {
        Set<String> tips = new LinkedHashSet<>();
        String merged = ((node == null ? "" : defaultString(node.getPoiName())) + " "
                + (node == null ? "" : defaultString(node.getCategory())) + " "
                + (node == null ? "" : defaultString(node.getDistrict()))).toLowerCase();

        if (containsAny(merged, "青城山", "mount", "mountain", "山", "徒步", "trail", "hiking")) {
            tips.add("上山前先补水，体力更稳。");
            tips.add("台阶较多，穿防滑鞋更安心。");
            tips.add("返程别赶路，给膝盖留余量。");
            tips.add("山里温差偏大，备件薄外套更稳妥。");
            tips.add("拍照别站边缘，转身时多留一步。");
        } else if (containsAny(merged, "ifs", "太古里", "春熙路", "商圈", "国金中心", "skp")) {
            tips.add("商圈入口多，先认准主入口。");
            tips.add("高峰人流密，约好集合点。");
            tips.add("先看楼层导航，少走回头路。");
            tips.add("热门时段电梯慢，预留一点机动时间。");
            tips.add("街区岔路多，拐弯前先看导航箭头。");
        } else if (containsAny(merged, "博物馆", "美术馆", "展览", "纪念馆")) {
            tips.add("热门馆排队快，早点到会更舒服。");
            tips.add("进馆前先看预约信息，少走回头路。");
            tips.add("展厅安静区域多，拍照先留意提示牌。");
            tips.add("重点展区容易停久，别把后面时间挤太满。");
        } else if (containsAny(merged, "古镇", "老街", "宽窄巷子", "锦里", "步行街")) {
            tips.add("街巷岔口多，先定碰头点更省心。");
            tips.add("高峰时人流密，贵重物品尽量贴身放。");
            tips.add("石板路偶尔打滑，转弯时放慢一点。");
            tips.add("热门小吃排队快，想吃就先买。");
        } else if (containsAny(merged, "寺", "祠", "宫", "观")) {
            tips.add("参观前先看礼仪提示，走动会更从容。");
            tips.add("台阶区域较多，上下时别急着赶路。");
            tips.add("高峰时段游客集中，想拍空景建议早一点。");
        }

        if (Boolean.TRUE.equals(req == null ? null : req.getIsRainy())) {
            tips.add("雨天路面偏滑，鞋底抓地力更重要。");
        }
        if (Boolean.TRUE.equals(req == null ? null : req.getIsNight())) {
            tips.add("返程别拖太晚，出门前先看好回程方式。");
        }
        if ("亲子".equals(req == null ? null : req.getCompanionType())) {
            tips.add("带小朋友时别把节奏拉太满，中途多休息。");
        }
        if ("长者".equals(req == null ? null : req.getCompanionType())
                || "老人".equals(req == null ? null : req.getCompanionType())) {
            tips.add("有长者同行时，优先留好坐下休息的时间。");
        }
        if (tips.isEmpty()) {
            tips.add("先看导航和入口位置，到场会更省时间。");
            tips.add("热门时段人会多一点，预留些机动时间。");
            tips.add("边走边拍时注意脚下，节奏放稳更舒服。");
        }
        return tips.stream().limit(5).toList();
    }

    private String buildFallbackRouteWarmTip(GenerateReqDTO req, List<ItineraryNodeVO> nodes) {
        int stopCount = nodes == null ? 0 : nodes.size();
        if (Boolean.TRUE.equals(req == null ? null : req.getIsRainy())) {
            return "雨天路面偏滑，今天按主线慢慢走，拍照和休息都别压太满。";
        }
        if (Boolean.TRUE.equals(req == null ? null : req.getIsNight())) {
            return "夜游结束别拖太晚，给返程和等车都预留一点机动时间。";
        }
        if (stopCount >= 4) {
            return "今天站点不少，先按主线顺着走，中途记得给休息留出空档。";
        }
        return "今天这条线更适合顺路慢逛，边走边拍会比来回折返更舒服。";
    }

    private String keepChineseOrFallback(String generated, String fallback) {
        if (!StringUtils.hasText(generated)) {
            return fallback;
        }
        if (isEnglishDominant(generated)) {
            return fallback;
        }
        return generated;
    }

    private boolean isEnglishDominant(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        int englishLetters = 0;
        int cjkChars = 0;
        for (char ch : text.toCharArray()) {
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                englishLetters++;
                continue;
            }
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                cjkChars++;
            }
        }
        return englishLetters >= 8 && (cjkChars == 0 || englishLetters > cjkChars * 2);
    }

    private String normalizeSingleTip(String preferred, String fallback) {
        String candidate = StringUtils.hasText(preferred) ? preferred.trim() : fallback;
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        if (isEnglishDominant(candidate) && StringUtils.hasText(fallback)) {
            candidate = fallback.trim();
        }
        candidate = candidate.replaceAll("[\\r\\n]+", " ").trim();
        if (candidate.length() > 36) {
            candidate = candidate.substring(0, 36);
        }
        return candidate;
    }

    private String buildNodeKey(ItineraryNodeVO node) {
        if (node == null) {
            return "unknown";
        }
        return defaultString(node.getPoiId() == null ? null : String.valueOf(node.getPoiId()))
                + "#"
                + defaultString(node.getPoiName())
                + "#"
                + defaultString(node.getStepOrder() == null ? null : String.valueOf(node.getStepOrder()));
    }

    private double estimateRoadDistanceKm(GenerateReqDTO req, ItineraryNodeVO firstNode) {
        if (!hasDepartureCoordinate(req) || firstNode == null
                || firstNode.getLatitude() == null
                || firstNode.getLongitude() == null) {
            return 0D;
        }

        double fromLat = req.getDepartureLatitude();
        double fromLng = req.getDepartureLongitude();
        double toLat = firstNode.getLatitude().doubleValue();
        double toLng = firstNode.getLongitude().doubleValue();
        if (!isValidCoordinate(toLat, toLng)) {
            return 0D;
        }

        double straightKm = haversineDistanceKm(fromLat, fromLng, toLat, toLng);
        if (straightKm <= 0D) {
            return 0D;
        }

        double roadFactor;
        if (straightKm <= 1D) {
            roadFactor = 1.2D;
        } else if (straightKm <= 4D) {
            roadFactor = 1.3D;
        } else if (straightKm <= 10D) {
            roadFactor = 1.4D;
        } else {
            roadFactor = 1.5D;
        }
        return straightKm * roadFactor;
    }

    private double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0D;
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = lat1Rad - lat2Rad;
        double deltaLon = Math.toRadians(lon1 - lon2);
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.pow(Math.sin(deltaLon / 2), 2);
        return 2 * earthRadiusKm * Math.asin(Math.sqrt(a));
    }

    private boolean isValidCoordinate(double lat, double lng) {
        return Math.abs(lat) <= 90D && Math.abs(lng) <= 180D;
    }

    private String resolveFallbackTransportMode(double roadDistanceKm, Integer minutes) {
        if (roadDistanceKm > 0D) {
            if (roadDistanceKm <= 1.2D) {
                return "步行";
            }
            if (roadDistanceKm <= 3.5D) {
                return "骑行";
            }
            if (roadDistanceKm <= 10D) {
                return "地铁+步行";
            }
            return "打车";
        }

        int duration = minutes == null ? 0 : minutes;
        if (duration <= 12) {
            return "步行";
        }
        if (duration <= 22) {
            return "骑行";
        }
        if (duration <= 45) {
            return "地铁+步行";
        }
        return "打车";
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text) || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    @SuppressWarnings("unused")
    private String buildComparisonHint(GenerateReqDTO req, ItineraryVO itinerary) {
        if (itineraryComparisonAssembler == null) {
            return null;
        }
        return itineraryComparisonAssembler.buildComparisonTips(
                req,
                itinerary.getOptions(),
                itinerary.getSelectedOptionKey()
        );
    }

    private void applyNearbyEnrichment(ItineraryVO itinerary, GenerateReqDTO req) {
        if (itinerary == null || nodeNearbyEnrichmentService == null) {
            return;
        }
        String cityName = req == null ? null : req.getCityName();
        nodeNearbyEnrichmentService.enrich(itinerary.getNodes(), cityName);
        if (itinerary.getOptions() != null) {
            for (ItineraryOptionVO option : itinerary.getOptions()) {
                if (option == null) {
                    continue;
                }
                nodeNearbyEnrichmentService.enrich(option.getNodes(), cityName);
            }
        }
    }

    private ItineraryVO copyItinerary(ItineraryVO source) {
        if (source == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(source, ItineraryVO.class);
        } catch (IllegalArgumentException ex) {
            return source;
        }
    }
}
