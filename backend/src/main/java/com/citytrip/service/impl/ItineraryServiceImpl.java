package com.citytrip.service.impl;

import com.citytrip.common.BadRequestException;
import com.citytrip.model.dto.FavoriteReqDTO;
import com.citytrip.mapper.PoiMapper;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.dto.ReplaceReqDTO;
import com.citytrip.model.dto.ReplanReqDTO;
import com.citytrip.model.dto.ReplanRespDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryOptionVO;
import com.citytrip.model.vo.ItinerarySummaryVO;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.ItineraryService;
import com.citytrip.service.TravelTimeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ItineraryServiceImpl implements ItineraryService {

    private static final int MAX_STOPS = 6;
    private static final int MAX_OPTIONS = 3;

    private final PoiMapper poiMapper;
    private final TravelTimeService travelTimeService;
    private final ItineraryRouteOptimizer routeOptimizer;
    private final SavedItineraryStore savedItineraryStore;

    public ItineraryServiceImpl(PoiMapper poiMapper,
                                TravelTimeService travelTimeService,
                                ItineraryRouteOptimizer routeOptimizer,
                                SavedItineraryStore savedItineraryStore) {
        this.poiMapper = poiMapper;
        this.travelTimeService = travelTimeService;
        this.routeOptimizer = routeOptimizer;
        this.savedItineraryStore = savedItineraryStore;
    }

    @Override
    public ItineraryVO generateUserItinerary(Long userId, GenerateReqDTO req) {
        GenerateReqDTO normalized = routeOptimizer.normalizeRequest(req);
        List<Poi> candidates = routeOptimizer.prepareCandidates(poiMapper.selectList(null), normalized, true);
        List<ItineraryRouteOptimizer.RouteOption> ranked = routeOptimizer.rankRoutes(
                candidates,
                normalized,
                Math.min(MAX_STOPS, candidates.size())
        );
        ItineraryVO itinerary = buildComparedItinerary(ranked, normalized, Collections.emptyMap(), null, Collections.emptySet());
        return savedItineraryStore.save(userId, null, normalized, itinerary);
    }

    @Override
    public ItineraryVO replaceNode(Long userId, Long itineraryId, Long targetPoiId, ReplaceReqDTO req) {
        if (req == null || targetPoiId == null) {
            throw new BadRequestException("缺少需要替换的景点。");
        }

        GenerateReqDTO normalized = routeOptimizer.normalizeRequest(req.getOriginalReq());
        List<ItineraryNodeVO> currentNodes = req.getCurrentNodes() == null ? Collections.emptyList() : req.getCurrentNodes();
        if (currentNodes.isEmpty()) {
            throw new BadRequestException("当前没有可替换的行程节点。");
        }

        List<Poi> currentPois = orderedPois(currentNodes);
        Poi target = currentPois.stream()
                .filter(poi -> Objects.equals(poi.getId(), targetPoiId))
                .findFirst()
                .orElse(null);
        if (target == null) {
            throw new BadRequestException("未找到需要替换的目标景点。");
        }

        Set<Long> currentIds = currentPois.stream().map(Poi::getId).collect(Collectors.toSet());
        List<Poi> pool = routeOptimizer.prepareCandidates(poiMapper.selectList(null), normalized, true).stream()
                .filter(poi -> !currentIds.contains(poi.getId()))
                .sorted((left, right) -> Double.compare(
                        routeOptimizer.replacementScore(target, right),
                        routeOptimizer.replacementScore(target, left)
                ))
                .limit(6)
                .toList();

        ItineraryRouteOptimizer.RouteOption best = null;
        Poi chosen = null;
        for (Poi replacement : pool) {
            List<Poi> replaced = currentPois.stream()
                    .map(poi -> Objects.equals(poi.getId(), targetPoiId) ? replacement : poi)
                    .collect(Collectors.toList());
            List<Poi> prepared = routeOptimizer.prepareCandidates(replaced, normalized, false);
            ItineraryRouteOptimizer.RouteOption candidate = routeOptimizer.bestRoute(prepared, normalized, Math.min(MAX_STOPS, prepared.size()));
            if (isBetter(candidate, best)) {
                best = candidate;
                chosen = replacement;
            }
        }

        if (best == null || best.path().isEmpty()) {
            throw new BadRequestException("未找到更合适的替换方案。");
        }

        Map<Long, String> reasons = reasonMap(currentNodes);
        reasons.remove(targetPoiId);

        ItineraryVO itinerary = buildComparedItinerary(List.of(best), normalized, reasons, null, Collections.emptySet());
        applyReplacementReason(itinerary, target, chosen);
        itinerary.setRecommendReason("已围绕新的点位重算路线，并优先保留原有偏好匹配与顺路程度。");
        itinerary.setTips(buildComparisonTips(normalized, itinerary.getOptions(), itinerary.getSelectedOptionKey()));
        return savedItineraryStore.save(userId, itineraryId, normalized, itinerary);
    }

    @Override
    public ReplanRespDTO replan(Long userId, Long itineraryId, ReplanReqDTO req) {
        ReplanRespDTO resp = new ReplanRespDTO();
        List<ItineraryNodeVO> currentNodes = req == null || req.getCurrentNodes() == null ? Collections.emptyList() : req.getCurrentNodes();
        if (currentNodes.isEmpty()) {
            resp.setSuccess(false);
            resp.setChanged(false);
            resp.setMessage("当前没有可重排的行程。");
            return resp;
        }

        GenerateReqDTO normalized = routeOptimizer.normalizeRequest(req == null ? null : req.getOriginalReq());
        List<Poi> currentPois = orderedPois(currentNodes);
        Set<Long> currentPoiIds = currentPois.stream().map(Poi::getId).collect(Collectors.toSet());
        List<Poi> candidates = routeOptimizer.prepareCandidates(poiMapper.selectList(null), normalized, true);
        List<ItineraryRouteOptimizer.RouteOption> ranked = routeOptimizer.rankRoutes(
                candidates,
                normalized,
                Math.min(MAX_STOPS, candidates.size())
        );
        if (ranked.isEmpty()) {
            resp.setSuccess(false);
            resp.setChanged(false);
            resp.setMessage("在当前时间窗口内没有找到可执行的重排方案。");
            return resp;
        }

        String oldSignature = currentNodes.stream()
                .map(node -> String.valueOf(node.getPoiId()))
                .collect(Collectors.joining("-"));
        Set<String> excludedSignatures = new LinkedHashSet<>();
        excludedSignatures.add(oldSignature);
        if (req != null && req.getExcludedSignatures() != null) {
            req.getExcludedSignatures().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(excludedSignatures::add);
        }

        ItineraryRouteOptimizer.RouteOption chosen = selectAlternativeRoute(
                ranked,
                currentPoiIds,
                currentNodes.size(),
                excludedSignatures
        );

        if (chosen == null) {
            resp.setSuccess(true);
            resp.setChanged(false);
            resp.setMessage("目前没有更好的新方案了。");
            resp.setReason("在当前时间、偏好和营业状态下，新的可行路线已经尝试完了，建议保留当前路线。");
            resp.setItinerary(savedItineraryStore.loadLatest(userId));
            return resp;
        }

        ItineraryVO itinerary = buildComparedItinerary(
                ranked,
                normalized,
                reasonMap(currentNodes),
                chosen.signature(),
                excludedSignatures
        );
        itinerary.setRecommendReason("已为你切换到一组新的候选路线，这次优先避开已经看过的旧方案。");
        itinerary.setTips(buildComparisonTips(normalized, itinerary.getOptions(), itinerary.getSelectedOptionKey()));
        itinerary = savedItineraryStore.save(userId, itineraryId, normalized, itinerary);

        resp.setSuccess(true);
        resp.setChanged(true);
        resp.setMessage("已为你换了一组新的路线方案。");
        resp.setReason("新路线会优先替换掉已经浏览过的组合，而不只是打乱原有顺序。");
        resp.setItinerary(itinerary);
        return resp;
    }

    @Override
    public ItineraryVO getLatestItinerary(Long userId) {
        return savedItineraryStore.loadLatest(userId);
    }

    @Override
    public ItineraryVO getItinerary(Long userId, Long itineraryId) {
        return savedItineraryStore.load(userId, itineraryId);
    }

    @Override
    public List<ItinerarySummaryVO> listItineraries(Long userId, boolean favoriteOnly, Integer limit) {
        return savedItineraryStore.listSummaries(userId, favoriteOnly, limit);
    }

    @Override
    public ItineraryVO favoriteItinerary(Long userId, Long itineraryId, FavoriteReqDTO req) {
        return savedItineraryStore.favorite(userId, itineraryId, req);
    }

    @Override
    public void unfavoriteItinerary(Long userId, Long itineraryId) {
        savedItineraryStore.markFavorite(userId, itineraryId, false);
    }

    private ItineraryVO buildComparedItinerary(List<ItineraryRouteOptimizer.RouteOption> ranked,
                                               GenerateReqDTO req,
                                               Map<Long, String> existingReasons,
                                               String preferredSignature,
                                               Set<String> excludedOptionSignatures) {
        List<ItineraryRouteOptimizer.RouteOption> selectedRoutes = selectOptionRoutes(ranked, preferredSignature, excludedOptionSignatures);
        if (selectedRoutes.isEmpty()) {
            return buildEmptyItinerary(req);
        }

        List<RouteAnalysis> analyses = selectedRoutes.stream()
                .map(route -> analyzeRoute(route, req, existingReasons))
                .toList();
        List<OptionStyle> styles = assignOptionStyles(analyses);

        List<ItineraryOptionVO> options = new ArrayList<>(analyses.size());
        for (int i = 0; i < analyses.size(); i++) {
            options.add(toOption(analyses.get(i), analyses, styles.get(i)));
        }

        String selectedOptionKey = options.get(0).getOptionKey();
        ItineraryVO itinerary = new ItineraryVO();
        itinerary.setOriginalReq(req);
        itinerary.setOptions(options);
        itinerary.setSelectedOptionKey(selectedOptionKey);
        applySelectedOption(itinerary, options.get(0));
        itinerary.setTips(buildComparisonTips(req, options, selectedOptionKey));
        return itinerary;
    }

    private ItineraryVO buildEmptyItinerary(GenerateReqDTO req) {
        ItineraryVO itinerary = new ItineraryVO();
        itinerary.setOriginalReq(req);
        itinerary.setOptions(Collections.emptyList());
        itinerary.setSelectedOptionKey(null);
        itinerary.setNodes(Collections.emptyList());
        itinerary.setTotalCost(BigDecimal.ZERO);
        itinerary.setTotalDuration(0);
        itinerary.setRecommendReason("没有找到可执行的路线。");
        itinerary.setAlerts(List.of("请尝试放宽时间窗口或更换出行日期。"));
        itinerary.setTips(buildComparisonTips(req, Collections.emptyList(), null));
        return itinerary;
    }

    private void applySelectedOption(ItineraryVO itinerary, ItineraryOptionVO option) {
        itinerary.setNodes(option.getNodes());
        itinerary.setTotalCost(option.getTotalCost());
        itinerary.setTotalDuration(option.getTotalDuration());
        itinerary.setRecommendReason(option.getRecommendReason());
        itinerary.setAlerts(option.getAlerts());
    }

    private List<ItineraryRouteOptimizer.RouteOption> selectOptionRoutes(List<ItineraryRouteOptimizer.RouteOption> ranked,
                                                                         String preferredSignature,
                                                                         Set<String> excludedOptionSignatures) {
        if (ranked == null || ranked.isEmpty()) {
            return Collections.emptyList();
        }

        List<ItineraryRouteOptimizer.RouteOption> filtered = ranked.stream()
                .filter(route -> route != null && route.path() != null && !route.path().isEmpty())
                .filter(route -> excludedOptionSignatures == null
                        || !excludedOptionSignatures.contains(route.signature())
                        || Objects.equals(route.signature(), preferredSignature))
                .toList();
        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        List<ItineraryRouteOptimizer.RouteOption> selected = new ArrayList<>();
        if (StringUtils.hasText(preferredSignature)) {
            filtered.stream()
                    .filter(route -> Objects.equals(route.signature(), preferredSignature))
                    .findFirst()
                    .ifPresent(selected::add);
        }

        for (ItineraryRouteOptimizer.RouteOption route : filtered) {
            if (selected.size() >= MAX_OPTIONS) {
                break;
            }
            if (selected.stream().anyMatch(item -> Objects.equals(item.signature(), route.signature()))) {
                continue;
            }
            if (selected.isEmpty() || isDistinctEnough(route, selected)) {
                selected.add(route);
            }
        }

        for (ItineraryRouteOptimizer.RouteOption route : filtered) {
            if (selected.size() >= MAX_OPTIONS) {
                break;
            }
            if (selected.stream().noneMatch(item -> Objects.equals(item.signature(), route.signature()))) {
                selected.add(route);
            }
        }
        return selected;
    }

    private boolean isDistinctEnough(ItineraryRouteOptimizer.RouteOption candidate,
                                     List<ItineraryRouteOptimizer.RouteOption> selected) {
        Set<Long> candidateIds = candidate.path().stream().map(Poi::getId).collect(Collectors.toSet());
        for (ItineraryRouteOptimizer.RouteOption existing : selected) {
            Set<Long> existingIds = existing.path().stream().map(Poi::getId).collect(Collectors.toSet());
            long overlapCount = candidateIds.stream().filter(existingIds::contains).count();
            int divisor = Math.max(1, Math.min(candidateIds.size(), existingIds.size()));
            double overlap = overlapCount * 1.0 / divisor;
            if (overlap > 0.75D) {
                return false;
            }
        }
        return true;
    }

    private RouteAnalysis analyzeRoute(ItineraryRouteOptimizer.RouteOption route,
                                       GenerateReqDTO req,
                                       Map<Long, String> existingReasons) {
        int startMinute = routeOptimizer.parseTimeMinutes(req.getStartTime(), ItineraryRouteOptimizer.DEFAULT_START_MINUTE);
        int currentMinute = startMinute;
        int totalTravelTime = 0;
        int totalWaitTime = 0;
        int themeMatchCount = 0;
        int companionMatchCount = 0;
        int nightFriendlyCount = 0;
        int indoorFriendlyCount = 0;
        int businessRiskScore = 0;
        int statusStaleCount = 0;
        int missingHoursCount = 0;
        Set<String> districts = new LinkedHashSet<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        List<ItineraryNodeVO> nodes = new ArrayList<>();
        Poi prev = null;

        for (int index = 0; index < route.path().size(); index++) {
            Poi poi = route.path().get(index);
            int stay = poi.getStayDuration() == null ? 90 : poi.getStayDuration();
            int travel = prev == null ? 0 : travelTimeService.estimateTravelTimeMinutes(prev, poi);
            int arrival = currentMinute + travel;
            int visitStart = Math.max(arrival, routeOptimizer.resolveOpenMinute(poi, currentMinute));
            int wait = Math.max(0, visitStart - arrival);

            ItineraryNodeVO node = new ItineraryNodeVO();
            node.setStepOrder(index + 1);
            node.setPoiId(poi.getId());
            node.setPoiName(poi.getName());
            node.setCategory(poi.getCategory());
            node.setDistrict(poi.getDistrict());
            node.setAddress(poi.getAddress());
            node.setLatitude(poi.getLatitude());
            node.setLongitude(poi.getLongitude());
            node.setTravelTime(travel);
            node.setStayDuration(stay);
            node.setCost(poi.getAvgCost() == null ? BigDecimal.ZERO : poi.getAvgCost());
            node.setStartTime(routeOptimizer.formatTime(visitStart));
            node.setEndTime(routeOptimizer.formatTime(visitStart + stay));
            node.setOperatingStatus(poi.getOperatingStatus());
            node.setStatusUpdatedAt(poi.getStatusUpdatedAt());
            node.setStatusNote(buildStatusNote(poi, wait));
            node.setSysReason(existingReasons.getOrDefault(
                    node.getPoiId(),
                    buildNodeReason(req, poi, prev, travel, wait, index == 0, index == route.path().size() - 1)
            ));

            totalCost = totalCost.add(node.getCost());
            totalTravelTime += travel;
            totalWaitTime += wait;
            themeMatchCount += matchThemes(req, poi).size();
            if (matchesCompanion(req, poi)) {
                companionMatchCount++;
            }
            if (Boolean.TRUE.equals(req.getIsNight()) && Integer.valueOf(1).equals(poi.getNightAvailable())) {
                nightFriendlyCount++;
            }
            if (Boolean.TRUE.equals(req.getIsRainy())
                    && (Integer.valueOf(1).equals(poi.getIndoor()) || Integer.valueOf(1).equals(poi.getRainFriendly()))) {
                indoorFriendlyCount++;
            }
            if (Boolean.TRUE.equals(poi.getStatusStale())) {
                businessRiskScore += 2;
                statusStaleCount++;
            }
            if (poi.getOpenTime() == null || poi.getCloseTime() == null) {
                businessRiskScore += 2;
                missingHoursCount++;
            }
            if ("UNKNOWN".equalsIgnoreCase(poi.getOperatingStatus())) {
                businessRiskScore += 1;
            }
            if (StringUtils.hasText(poi.getDistrict())) {
                districts.add(poi.getDistrict());
            }

            nodes.add(node);
            currentMinute = visitStart + stay;
            prev = poi;
        }

        List<String> alerts = buildRouteAlerts(req, totalWaitTime, statusStaleCount, missingHoursCount, nightFriendlyCount, indoorFriendlyCount, nodes.size());
        return new RouteAnalysis(
                route,
                nodes,
                currentMinute - startMinute,
                totalCost,
                totalTravelTime,
                totalWaitTime,
                themeMatchCount,
                companionMatchCount,
                nightFriendlyCount,
                indoorFriendlyCount,
                businessRiskScore,
                districts.size(),
                alerts
        );
    }

    private List<String> buildRouteAlerts(GenerateReqDTO req,
                                          int totalWaitTime,
                                          int statusStaleCount,
                                          int missingHoursCount,
                                          int nightFriendlyCount,
                                          int indoorFriendlyCount,
                                          int totalStops) {
        List<String> alerts = new ArrayList<>();
        if (totalWaitTime >= 25) {
            alerts.add("本方案存在等待开门时段，适合稍晚一些出发。");
        }
        if (statusStaleCount > 0) {
            alerts.add("部分场馆状态较久未更新，出发前建议再次确认。");
        }
        if (missingHoursCount > 0) {
            alerts.add("部分点位营业时间信息不完整，建议临行前核对。");
        }
        if (Boolean.TRUE.equals(req.getIsRainy()) && indoorFriendlyCount < Math.max(1, totalStops / 2)) {
            alerts.add("当前为雨天偏好，但这条路线仍包含一定比例的室外点位。");
        }
        if (Boolean.TRUE.equals(req.getIsNight()) && nightFriendlyCount == 0) {
            alerts.add("你开启了夜游偏好，但这条路线的夜间亮点相对有限。");
        }
        return alerts;
    }

    private String buildStatusNote(Poi poi, int waitMinutes) {
        List<String> notes = new ArrayList<>();
        if (waitMinutes > 0) {
            notes.add("到达后需等待约 " + waitMinutes + " 分钟开门。");
        }
        if (Boolean.TRUE.equals(poi.getStatusStale())) {
            notes.add("状态更新时间较久，建议出发前复核。");
        }
        if (poi.getOpenTime() == null || poi.getCloseTime() == null) {
            notes.add("营业时间信息不完整。");
        }
        if (StringUtils.hasText(poi.getAvailabilityNote())) {
            notes.add(poi.getAvailabilityNote().trim());
        }
        return notes.isEmpty() ? null : String.join(" ", notes);
    }

    private String buildNodeReason(GenerateReqDTO req,
                                   Poi poi,
                                   Poi prev,
                                   int travelMinutes,
                                   int waitMinutes,
                                   boolean firstStop,
                                   boolean lastStop) {
        List<String> reasons = new ArrayList<>();
        List<String> matchedThemes = matchThemes(req, poi);
        if (!matchedThemes.isEmpty()) {
            reasons.add("匹配你的主题偏好：" + String.join("、", matchedThemes));
        }
        if (matchesCompanion(req, poi)) {
            reasons.add("更适合同伴类型和游玩节奏");
        }
        if (Boolean.TRUE.equals(req.getIsRainy())
                && (Integer.valueOf(1).equals(poi.getIndoor()) || Integer.valueOf(1).equals(poi.getRainFriendly()))) {
            reasons.add("雨天可执行性更稳");
        }
        if (Boolean.TRUE.equals(req.getIsNight()) && Integer.valueOf(1).equals(poi.getNightAvailable())) {
            reasons.add("夜间体验更完整");
        }
        if (firstStop) {
            reasons.add("适合作为路线起点，开场成本更低");
        } else if (travelMinutes <= 15) {
            reasons.add("与上一站衔接顺路，折返更少");
        } else if (travelMinutes <= 30) {
            reasons.add("仍可在当前时间窗内顺路串联");
        }
        if (waitMinutes > 0 && waitMinutes <= 20) {
            reasons.add("等待时间可控，不会明显压缩后续节奏");
        }
        if (lastStop) {
            reasons.add("适合作为收尾点，方便控制返程节奏");
        }
        if (highPriority(poi)) {
            reasons.add("综合热度和优先级都较高");
        }
        if (reasons.isEmpty()) {
            reasons.add("综合得分更高，且在当前时间窗内更容易执行");
        }
        return reasons.stream().limit(3).collect(Collectors.joining("；"));
    }

    private List<String> matchThemes(GenerateReqDTO req, Poi poi) {
        if (req == null || req.getThemes() == null || !StringUtils.hasText(poi.getTags())) {
            return Collections.emptyList();
        }
        return req.getThemes().stream()
                .filter(StringUtils::hasText)
                .filter(theme -> poi.getTags().contains(theme))
                .toList();
    }

    private boolean matchesCompanion(GenerateReqDTO req, Poi poi) {
        return StringUtils.hasText(req.getCompanionType())
                && StringUtils.hasText(poi.getSuitableFor())
                && poi.getSuitableFor().contains(req.getCompanionType());
    }

    private boolean highPriority(Poi poi) {
        return poi.getPriorityScore() != null && poi.getPriorityScore().doubleValue() >= 4.0D;
    }

    private List<OptionStyle> assignOptionStyles(List<RouteAnalysis> analyses) {
        if (analyses.isEmpty()) {
            return Collections.emptyList();
        }

        int budgetIndex = findIndex(analyses, Comparator.comparing(RouteAnalysis::totalCost));
        int efficientIndex = findIndex(analyses, Comparator
                .comparingInt(RouteAnalysis::totalTravelTime)
                .thenComparingInt(RouteAnalysis::totalDuration));
        int exploreIndex = findIndex(analyses, Comparator
                .comparingInt(RouteAnalysis::themeMatchCount).reversed()
                .thenComparingInt(RouteAnalysis::stopCount).reversed()
                .thenComparingDouble(RouteAnalysis::utility).reversed());
        int stableIndex = findIndex(analyses, Comparator
                .comparingInt(RouteAnalysis::businessRiskScore)
                .thenComparingInt(RouteAnalysis::totalTravelTime));

        List<OptionStyle> styles = new ArrayList<>(analyses.size());
        styles.add(new OptionStyle("balanced", "经典平衡型", "偏好匹配、顺路程度和整体体验更均衡"));
        for (int i = 1; i < analyses.size(); i++) {
            if (i == budgetIndex) {
                styles.add(new OptionStyle("budget", "省钱轻松型", "更适合控制预算和轻量出行"));
            } else if (i == efficientIndex) {
                styles.add(new OptionStyle("efficient", "顺路省时型", "更强调少折返和时间利用率"));
            } else if (i == stableIndex) {
                styles.add(new OptionStyle("stable", "稳妥执行型", "营业状态更稳，临场风险更低"));
            } else if (i == exploreIndex) {
                styles.add(new OptionStyle("explore", "高密度打卡型", "覆盖更多高分点位与主题亮点"));
            } else {
                styles.add(new OptionStyle("alternative-" + (i + 1), "备选方案 " + (i + 1), "提供不同取舍，方便横向比较"));
            }
        }
        return styles;
    }

    private int findIndex(List<RouteAnalysis> analyses, Comparator<RouteAnalysis> comparator) {
        RouteAnalysis best = analyses.stream().min(comparator).orElse(analyses.get(0));
        return analyses.indexOf(best);
    }

    private ItineraryOptionVO toOption(RouteAnalysis analysis,
                                       List<RouteAnalysis> analyses,
                                       OptionStyle style) {
        ItineraryOptionVO option = new ItineraryOptionVO();
        option.setOptionKey(style.key());
        option.setTitle(style.title());
        option.setSubtitle(style.subtitle());
        option.setSignature(analysis.route().signature());
        option.setTotalDuration(analysis.totalDuration());
        option.setTotalCost(analysis.totalCost());
        option.setStopCount(analysis.stopCount());
        option.setTotalTravelTime(analysis.totalTravelTime());
        option.setBusinessRiskScore(analysis.businessRiskScore());
        option.setThemeMatchCount(analysis.themeMatchCount());
        option.setRouteUtility(analysis.utility());
        option.setNodes(analysis.nodes());
        option.setAlerts(analysis.alerts());
        option.setSummary(buildOptionSummary(analysis, analyses));
        option.setHighlights(buildHighlights(analysis, analyses));
        option.setTradeoffs(buildTradeoffs(analysis, analyses));
        option.setRecommendReason(buildRecommendReason(analysis, analyses, style));
        option.setNotRecommendReason(buildNotRecommendReason(analysis, analyses));
        return option;
    }

    private String buildOptionSummary(RouteAnalysis analysis, List<RouteAnalysis> analyses) {
        List<String> parts = new ArrayList<>();
        if (isBestUtility(analysis, analyses)) {
            parts.add("整体排序得分更均衡");
        }
        if (isMinCost(analysis, analyses)) {
            parts.add("预算压力最低");
        }
        if (isMinTravel(analysis, analyses)) {
            parts.add("路上时间最短");
        }
        if (isMaxThemeMatch(analysis, analyses)) {
            parts.add("主题覆盖更集中");
        }
        if (isMinRisk(analysis, analyses)) {
            parts.add("营业风险更低");
        }
        if (parts.isEmpty()) {
            parts.add("提供另一种不同取舍的可执行路线");
        }
        return "从 " + analysis.nodes().get(0).getPoiName()
                + " 出发，到 " + analysis.nodes().get(analysis.nodes().size() - 1).getPoiName()
                + " 收尾，" + String.join("、", parts) + "。";
    }

    private List<String> buildHighlights(RouteAnalysis analysis, List<RouteAnalysis> analyses) {
        List<String> tags = new ArrayList<>();
        if (isBestUtility(analysis, analyses)) {
            tags.add("综合更均衡");
        }
        if (isMinCost(analysis, analyses)) {
            tags.add("预算最低");
        }
        if (isMinTravel(analysis, analyses)) {
            tags.add("路上最省时");
        }
        if (isMaxThemeMatch(analysis, analyses)) {
            tags.add("主题更集中");
        }
        if (isMinRisk(analysis, analyses)) {
            tags.add("营业风险更低");
        }
        if (analysis.uniqueDistrictCount() > 1) {
            tags.add("区域覆盖更丰富");
        }
        return tags.stream().limit(4).toList();
    }

    private List<String> buildTradeoffs(RouteAnalysis analysis, List<RouteAnalysis> analyses) {
        List<String> tags = new ArrayList<>();
        BigDecimal minCost = analyses.stream().map(RouteAnalysis::totalCost).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        int minTravel = analyses.stream().mapToInt(RouteAnalysis::totalTravelTime).min().orElse(0);
        int minRisk = analyses.stream().mapToInt(RouteAnalysis::businessRiskScore).min().orElse(0);
        int maxStops = analyses.stream().mapToInt(RouteAnalysis::stopCount).max().orElse(analysis.stopCount());

        if (analysis.totalCost().compareTo(minCost.add(new BigDecimal("30"))) > 0) {
            tags.add("费用更高");
        }
        if (analysis.totalTravelTime() > minTravel + 20) {
            tags.add("路上时间更长");
        }
        if (analysis.businessRiskScore() > minRisk) {
            tags.add("需二次确认营业状态");
        }
        if (analysis.stopCount() < maxStops) {
            tags.add("打卡密度略低");
        }
        return tags.stream().limit(3).toList();
    }

    private String buildRecommendReason(RouteAnalysis analysis,
                                        List<RouteAnalysis> analyses,
                                        OptionStyle style) {
        List<String> reasons = new ArrayList<>();
        if ("balanced".equals(style.key())) {
            reasons.add("这版路线在偏好匹配、顺路程度和可执行性之间更均衡");
        }
        if (isBestUtility(analysis, analyses)) {
            reasons.add("综合评分在候选方案里更靠前");
        }
        if (isMinCost(analysis, analyses)) {
            reasons.add("总花费在候选方案里更低");
        }
        if (isMinTravel(analysis, analyses)) {
            reasons.add("路上耗时更短，折返更少");
        }
        if (isMaxThemeMatch(analysis, analyses)) {
            reasons.add("主题匹配点位更多");
        }
        if (isMinRisk(analysis, analyses)) {
            reasons.add("营业状态和时间窗风险更低");
        }
        if (reasons.isEmpty()) {
            reasons.add("这版方案在当前条件下依然具备较好的可执行性");
        }
        return String.join("，", reasons) + "。";
    }

    private String buildNotRecommendReason(RouteAnalysis analysis, List<RouteAnalysis> analyses) {
        List<String> reasons = new ArrayList<>();
        BigDecimal minCost = analyses.stream().map(RouteAnalysis::totalCost).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        int minTravel = analyses.stream().mapToInt(RouteAnalysis::totalTravelTime).min().orElse(0);
        int minRisk = analyses.stream().mapToInt(RouteAnalysis::businessRiskScore).min().orElse(0);
        int maxTheme = analyses.stream().mapToInt(RouteAnalysis::themeMatchCount).max().orElse(analysis.themeMatchCount());

        if (analysis.totalCost().compareTo(minCost.add(new BigDecimal("30"))) > 0) {
            reasons.add("如果你更在意预算，这版路线的整体花费会更高");
        }
        if (analysis.totalTravelTime() > minTravel + 20) {
            reasons.add("如果你更想少走少折返，这版路线的路上时间会更长");
        }
        if (analysis.businessRiskScore() > minRisk) {
            reasons.add("如果你更追求稳妥执行，其中部分点位仍建议再次确认营业状态");
        }
        if (analysis.themeMatchCount() < maxTheme) {
            reasons.add("如果你更强调主题纯度，其他方案的主题聚焦度会更高");
        }
        if (reasons.isEmpty()) {
            return "不推荐给只追求单一指标的用户，例如极限省钱或极限打卡需求。";
        }
        return String.join("；", reasons.stream().limit(2).toList()) + "。";
    }

    private boolean isBestUtility(RouteAnalysis analysis, List<RouteAnalysis> analyses) {
        double max = analyses.stream().mapToDouble(RouteAnalysis::utility).max().orElse(analysis.utility());
        return Double.compare(analysis.utility(), max) == 0;
    }

    private boolean isMinCost(RouteAnalysis analysis, List<RouteAnalysis> analyses) {
        BigDecimal min = analyses.stream().map(RouteAnalysis::totalCost).min(BigDecimal::compareTo).orElse(analysis.totalCost());
        return analysis.totalCost().compareTo(min) == 0;
    }

    private boolean isMinTravel(RouteAnalysis analysis, List<RouteAnalysis> analyses) {
        int min = analyses.stream().mapToInt(RouteAnalysis::totalTravelTime).min().orElse(analysis.totalTravelTime());
        return analysis.totalTravelTime() == min;
    }

    private boolean isMaxThemeMatch(RouteAnalysis analysis, List<RouteAnalysis> analyses) {
        int max = analyses.stream().mapToInt(RouteAnalysis::themeMatchCount).max().orElse(analysis.themeMatchCount());
        return analysis.themeMatchCount() == max;
    }

    private boolean isMinRisk(RouteAnalysis analysis, List<RouteAnalysis> analyses) {
        int min = analyses.stream().mapToInt(RouteAnalysis::businessRiskScore).min().orElse(analysis.businessRiskScore());
        return analysis.businessRiskScore() == min;
    }

    private String buildComparisonTips(GenerateReqDTO req,
                                       List<ItineraryOptionVO> options,
                                       String selectedOptionKey) {
        List<String> parts = new ArrayList<>();
        if (options == null || options.isEmpty()) {
            parts.add("请尝试放宽时间窗口或更换出行日期后再生成。");
        } else {
            parts.add("系统已按当前时间窗提供 " + options.size() + " 套可执行方案。");
            ItineraryOptionVO selected = options.stream()
                    .filter(option -> Objects.equals(option.getOptionKey(), selectedOptionKey))
                    .findFirst()
                    .orElse(options.get(0));
            if (selected.getAlerts() != null && !selected.getAlerts().isEmpty()) {
                parts.add("当前选中的方案提示：" + selected.getAlerts().get(0));
            } else {
                parts.add("当前选中的方案更适合作为默认出发版本，若你更看重预算或省时，可以切换其它方案。");
            }
        }
        if (req != null && StringUtils.hasText(req.getTripDate())) {
            parts.add("出行日期：" + req.getTripDate() + "。");
        }
        return String.join("", parts);
    }

    private Map<Long, String> reasonMap(List<ItineraryNodeVO> nodes) {
        Map<Long, String> map = new HashMap<>();
        for (ItineraryNodeVO node : nodes) {
            if (node.getPoiId() != null && StringUtils.hasText(node.getSysReason())) {
                map.put(node.getPoiId(), node.getSysReason());
            }
        }
        return map;
    }

    private List<Poi> orderedPois(List<ItineraryNodeVO> nodes) {
        List<Long> ids = nodes.stream().map(ItineraryNodeVO::getPoiId).filter(Objects::nonNull).toList();
        Map<Long, Poi> byId = poiMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Poi::getId, poi -> poi));
        List<Poi> ordered = new ArrayList<>();
        for (Long id : ids) {
            Poi poi = byId.get(id);
            if (poi != null) {
                ordered.add(poi);
            }
        }
        return ordered;
    }

    private boolean isBetter(ItineraryRouteOptimizer.RouteOption candidate, ItineraryRouteOptimizer.RouteOption current) {
        if (candidate == null || candidate.path().isEmpty()) {
            return false;
        }
        if (current == null || current.path().isEmpty()) {
            return true;
        }
        int byScore = Double.compare(candidate.utility(), current.utility());
        return byScore > 0 || (byScore == 0 && candidate.path().size() > current.path().size());
    }

    private ItineraryRouteOptimizer.RouteOption selectAlternativeRoute(List<ItineraryRouteOptimizer.RouteOption> ranked,
                                                                       Set<Long> currentPoiIds,
                                                                       int currentSize,
                                                                       Set<String> excludedSignatures) {
        List<ItineraryRouteOptimizer.RouteOption> alternatives = ranked.stream()
                .filter(option -> excludedSignatures == null || !excludedSignatures.contains(option.signature()))
                .filter(option -> containsNewPoi(option, currentPoiIds))
                .collect(Collectors.toList());

        if (alternatives.isEmpty()) {
            return null;
        }

        List<ItineraryRouteOptimizer.RouteOption> sameSize = alternatives.stream()
                .filter(option -> option.path().size() == currentSize)
                .collect(Collectors.toList());

        List<ItineraryRouteOptimizer.RouteOption> pool = sameSize.isEmpty() ? alternatives : sameSize;
        pool.sort((left, right) -> {
            int byNewPoiCount = Integer.compare(countNewPois(right, currentPoiIds), countNewPois(left, currentPoiIds));
            if (byNewPoiCount != 0) {
                return byNewPoiCount;
            }
            return Double.compare(right.utility(), left.utility());
        });
        return pool.get(0);
    }

    private boolean containsNewPoi(ItineraryRouteOptimizer.RouteOption option, Set<Long> currentPoiIds) {
        return option.path().stream().map(Poi::getId).anyMatch(id -> !currentPoiIds.contains(id));
    }

    private int countNewPois(ItineraryRouteOptimizer.RouteOption option, Set<Long> currentPoiIds) {
        return (int) option.path().stream().map(Poi::getId).filter(id -> !currentPoiIds.contains(id)).count();
    }

    private void applyReplacementReason(ItineraryVO itinerary, Poi oldPoi, Poi newPoi) {
        if (itinerary == null || itinerary.getNodes() == null || oldPoi == null || newPoi == null) {
            return;
        }
        for (ItineraryNodeVO node : itinerary.getNodes()) {
            if (Objects.equals(node.getPoiId(), newPoi.getId())) {
                node.setSysReason("已将 " + oldPoi.getName() + " 替换为更适合当前路线的相近点位。");
            }
        }
        if (itinerary.getOptions() == null) {
            return;
        }
        itinerary.getOptions().forEach(option -> {
            if (option.getNodes() == null) {
                return;
            }
            option.getNodes().forEach(node -> {
                if (Objects.equals(node.getPoiId(), newPoi.getId())) {
                    node.setSysReason("已将 " + oldPoi.getName() + " 替换为更适合当前路线的相近点位。");
                }
            });
        });
    }

    private record RouteAnalysis(ItineraryRouteOptimizer.RouteOption route,
                                 List<ItineraryNodeVO> nodes,
                                 int totalDuration,
                                 BigDecimal totalCost,
                                 int totalTravelTime,
                                 int totalWaitTime,
                                 int themeMatchCount,
                                 int companionMatchCount,
                                 int nightFriendlyCount,
                                 int indoorFriendlyCount,
                                 int businessRiskScore,
                                 int uniqueDistrictCount,
                                 List<String> alerts) {

        private int stopCount() {
            return nodes == null ? 0 : nodes.size();
        }

        private double utility() {
            return route == null ? 0D : route.utility();
        }
    }

    private record OptionStyle(String key, String title, String subtitle) {
    }
}
