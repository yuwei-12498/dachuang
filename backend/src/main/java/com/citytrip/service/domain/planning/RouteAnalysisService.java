package com.citytrip.service.domain.planning;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.service.TravelTimeService;
import com.citytrip.service.impl.ItineraryRouteOptimizer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RouteAnalysisService {

    private final TravelTimeService travelTimeService;
    private final ItineraryRouteOptimizer routeOptimizer;

    public RouteAnalysisService(TravelTimeService travelTimeService, ItineraryRouteOptimizer routeOptimizer) {
        this.travelTimeService = travelTimeService;
        this.routeOptimizer = routeOptimizer;
    }

    public RouteAnalysis analyzeRoute(ItineraryRouteOptimizer.RouteOption route,
                                      GenerateReqDTO req,
                                      Map<Long, String> existingReasons) {
        GenerateReqDTO normalized = routeOptimizer.normalizeRequest(req);
        Map<Long, String> reasonOverrides = existingReasons == null ? Collections.emptyMap() : existingReasons;

        int startMinute = routeOptimizer.parseTimeMinutes(normalized.getStartTime(), ItineraryRouteOptimizer.DEFAULT_START_MINUTE);
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
            node.setSysReason(reasonOverrides.getOrDefault(
                    node.getPoiId(),
                    buildNodeReason(normalized, poi, prev, travel, wait, index == 0, index == route.path().size() - 1)
            ));

            totalCost = totalCost.add(node.getCost());
            totalTravelTime += travel;
            totalWaitTime += wait;
            themeMatchCount += matchThemes(normalized, poi).size();
            if (matchesCompanion(normalized, poi)) {
                companionMatchCount++;
            }
            if (Boolean.TRUE.equals(normalized.getIsNight()) && Integer.valueOf(1).equals(poi.getNightAvailable())) {
                nightFriendlyCount++;
            }
            if (Boolean.TRUE.equals(normalized.getIsRainy())
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

        List<String> alerts = buildRouteAlerts(
                normalized,
                totalWaitTime,
                statusStaleCount,
                missingHoursCount,
                nightFriendlyCount,
                indoorFriendlyCount,
                nodes.size()
        );
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
            alerts.add("该方案包含一定等待时间，建议稍晚出发或提前预约。");
        }
        if (statusStaleCount > 0) {
            alerts.add("部分点位营业状态更新时间较久，出发前建议再次确认。");
        }
        if (missingHoursCount > 0) {
            alerts.add("部分点位缺少完整营业时间信息，建议临行前核验。");
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
            notes.add("到达后需等待 " + waitMinutes + " 分钟才能开始游览。");
        }
        if (Boolean.TRUE.equals(poi.getStatusStale())) {
            notes.add("营业状态更新时间较久，建议出发前再次确认。");
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
            reasons.add("主题匹配：" + String.join("、", matchedThemes));
        }
        if (matchesCompanion(req, poi)) {
            reasons.add("更适合同伴类型和游玩节奏");
        }
        if (Boolean.TRUE.equals(req.getIsRainy())
                && (Integer.valueOf(1).equals(poi.getIndoor()) || Integer.valueOf(1).equals(poi.getRainFriendly()))) {
            reasons.add("雨天可执行性更强");
        }
        if (Boolean.TRUE.equals(req.getIsNight()) && Integer.valueOf(1).equals(poi.getNightAvailable())) {
            reasons.add("夜间体验更完整");
        }
        if (firstStop) {
            reasons.add("适合作为路线起点，开场切换成本更低");
        } else if (travelMinutes <= 15) {
            reasons.add("与前一站距离近，顺路衔接更好");
        } else if (travelMinutes <= 30) {
            reasons.add("与前一站过渡仍在可接受范围");
        }
        if (waitMinutes > 0 && waitMinutes <= 20) {
            reasons.add("等待时间可控，不会明显压缩后续行程");
        }
        if (lastStop) {
            reasons.add("适合作为收尾点位，便于返程或继续夜游");
        }
        if (highPriority(poi)) {
            reasons.add("综合热度和优先级都较高");
        }
        if (reasons.isEmpty()) {
            reasons.add("在当前时间窗和路线顺序下具有较好的可执行性");
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
        return req != null
                && StringUtils.hasText(req.getCompanionType())
                && StringUtils.hasText(poi.getSuitableFor())
                && poi.getSuitableFor().contains(req.getCompanionType());
    }

    private boolean highPriority(Poi poi) {
        return poi.getPriorityScore() != null && poi.getPriorityScore().doubleValue() >= 4.0D;
    }

    public record RouteAnalysis(ItineraryRouteOptimizer.RouteOption route,
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

        public int stopCount() {
            return nodes == null ? 0 : nodes.size();
        }

        public double utility() {
            return route == null ? 0D : route.utility();
        }
    }
}
