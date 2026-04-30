package com.citytrip.service.domain.planning;

import com.citytrip.model.vo.RoutePathPointVO;
import com.citytrip.model.vo.SegmentRouteGuideVO;
import com.citytrip.model.vo.SegmentRouteStepVO;
import com.citytrip.service.TravelTimeService;
import com.citytrip.service.geo.GeoPoint;
import com.citytrip.service.geo.GeoRouteEstimate;
import com.citytrip.service.geo.GeoRouteStep;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class SegmentRouteGuideService {

    private static final String SOURCE_GEO_PROVIDER = "geo-provider";
    private static final String INCOMPLETE_DETAIL_REASON = "该段暂未获取完整导航详情";
    private static final String MISSING_ROUTE_REASON = "该段导航数据获取失败，当前仅能提供概略通行信息";
    private static final String MISSING_ROUTE_SUMMARY = "该段导航数据暂不可用";

    public SegmentRouteGuideVO buildGuide(TravelTimeService.TravelLegEstimate legEstimate) {
        SegmentRouteGuideVO guide = new SegmentRouteGuideVO();
        if (legEstimate == null) {
            guide.setSummary(MISSING_ROUTE_SUMMARY);
            guide.setDetailAvailable(false);
            guide.setIncompleteReason(MISSING_ROUTE_REASON);
            guide.setSteps(List.of());
            guide.setPathPoints(List.of());
            return guide;
        }

        GeoRouteEstimate detailedRoute = legEstimate.detailedRoute();
        List<SegmentRouteStepVO> steps = toStepVos(detailedRoute == null ? List.of() : detailedRoute.steps());
        List<RoutePathPointVO> pathPoints = resolvePathPoints(legEstimate, detailedRoute);

        guide.setTransportMode(normalizeText(legEstimate.transportMode()));
        guide.setDurationMinutes(legEstimate.estimatedMinutes() < 0 ? 0 : legEstimate.estimatedMinutes());
        guide.setDistanceKm(scaleDistance(legEstimate.estimatedDistanceKm()));
        guide.setSteps(steps);
        guide.setPathPoints(pathPoints);
        guide.setSource(detailedRoute == null ? null : SOURCE_GEO_PROVIDER);

        if (!steps.isEmpty()) {
            guide.setSummary(buildStepSummary(steps, guide.getTransportMode(), guide.getDurationMinutes(), guide.getDistanceKm()));
            guide.setDetailAvailable(true);
            guide.setIncompleteReason(null);
            return guide;
        }

        if (pathPoints.isEmpty()
                && guide.getDurationMinutes() <= 0
                && guide.getDistanceKm() == null) {
            guide.setSummary(MISSING_ROUTE_SUMMARY);
        } else {
            guide.setSummary(buildFallbackSummary(guide.getTransportMode(), guide.getDurationMinutes(), guide.getDistanceKm()));
        }
        guide.setDetailAvailable(false);
        guide.setIncompleteReason(pathPoints.isEmpty() ? MISSING_ROUTE_REASON : INCOMPLETE_DETAIL_REASON);
        return guide;
    }

    private List<SegmentRouteStepVO> toStepVos(List<GeoRouteStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return List.of();
        }
        List<SegmentRouteStepVO> mapped = new ArrayList<>();
        for (int index = 0; index < steps.size(); index++) {
            GeoRouteStep step = steps.get(index);
            if (step == null) {
                continue;
            }
            SegmentRouteStepVO stepVO = new SegmentRouteStepVO();
            stepVO.setStepOrder(index + 1);
            stepVO.setType(normalizeText(step.type()));
            stepVO.setInstruction(normalizeText(step.instruction()));
            stepVO.setDistanceMeters(step.distanceMeters());
            stepVO.setDurationMinutes(step.durationMinutes());
            stepVO.setLineName(normalizeText(step.lineName()));
            stepVO.setFromStation(normalizeText(step.fromStation()));
            stepVO.setToStation(normalizeText(step.toStation()));
            stepVO.setEntranceName(normalizeText(step.entranceName()));
            stepVO.setExitName(normalizeText(step.exitName()));
            stepVO.setStopCount(step.stopCount());
            stepVO.setPathPoints(toRoutePathPoints(step.pathPoints()));
            mapped.add(stepVO);
        }
        return List.copyOf(mapped);
    }

    private List<RoutePathPointVO> resolvePathPoints(TravelTimeService.TravelLegEstimate legEstimate,
                                                     GeoRouteEstimate detailedRoute) {
        if (detailedRoute != null) {
            List<RoutePathPointVO> providerPathPoints = toRoutePathPoints(detailedRoute.pathPoints());
            if (!providerPathPoints.isEmpty()) {
                return providerPathPoints;
            }
        }
        return toRoutePathPoints(legEstimate.pathPoints());
    }

    private String buildStepSummary(List<SegmentRouteStepVO> steps,
                                    String fallbackMode,
                                    Integer fallbackMinutes,
                                    BigDecimal fallbackDistanceKm) {
        List<String> parts = new ArrayList<>();
        for (SegmentRouteStepVO step : steps) {
            String part = buildStepSummaryPart(step);
            if (StringUtils.hasText(part)) {
                if (parts.isEmpty() || !Objects.equals(parts.get(parts.size() - 1), part)) {
                    parts.add(part);
                }
            }
        }
        if (!parts.isEmpty()) {
            return String.join(" → ", parts);
        }
        return buildFallbackSummary(fallbackMode, fallbackMinutes, fallbackDistanceKm);
    }

    private String buildStepSummaryPart(SegmentRouteStepVO step) {
        if (step == null) {
            return null;
        }
        String type = normalizeText(step.getType());
        if (!StringUtils.hasText(type)) {
            return null;
        }
        String normalizedType = type.toLowerCase(Locale.ROOT);
        if ("walk".equals(normalizedType)) {
            if (step.getDistanceMeters() != null && step.getDistanceMeters() > 0) {
                return "步行 " + step.getDistanceMeters() + " 米";
            }
            return "步行";
        }
        if ("metro".equals(normalizedType)) {
            if (step.getStopCount() != null && step.getStopCount() > 0) {
                return "地铁 " + step.getStopCount() + " 站";
            }
            if (StringUtils.hasText(step.getLineName())) {
                return step.getLineName();
            }
            return "地铁";
        }
        if ("bus".equals(normalizedType)) {
            if (step.getStopCount() != null && step.getStopCount() > 0) {
                return "公交 " + step.getStopCount() + " 站";
            }
            if (StringUtils.hasText(step.getLineName())) {
                return step.getLineName();
            }
            return "公交";
        }
        if ("taxi".equals(normalizedType)) {
            if (step.getDurationMinutes() != null && step.getDurationMinutes() > 0) {
                return "打车约 " + step.getDurationMinutes() + " 分钟";
            }
            return "打车";
        }
        if ("transfer".equals(normalizedType)) {
            return "换乘";
        }
        if ("enter".equals(normalizedType) || "exit".equals(normalizedType)) {
            return null;
        }
        return normalizeText(step.getInstruction());
    }

    private String buildFallbackSummary(String transportMode,
                                        Integer durationMinutes,
                                        BigDecimal distanceKm) {
        String mode = StringUtils.hasText(transportMode) ? transportMode.trim() : null;
        Integer safeMinutes = durationMinutes == null || durationMinutes < 0 ? null : durationMinutes;
        BigDecimal safeDistance = scaleDistance(distanceKm);
        if (StringUtils.hasText(mode) && safeMinutes != null && safeDistance != null) {
            return mode + "约 " + safeMinutes + " 分钟，约 " + formatDistanceKm(safeDistance) + " 公里";
        }
        if (StringUtils.hasText(mode) && safeMinutes != null) {
            return mode + "约 " + safeMinutes + " 分钟";
        }
        if (StringUtils.hasText(mode) && safeDistance != null) {
            return mode + "约 " + formatDistanceKm(safeDistance) + " 公里";
        }
        if (safeMinutes != null && safeDistance != null) {
            return "约 " + safeMinutes + " 分钟，约 " + formatDistanceKm(safeDistance) + " 公里";
        }
        if (safeMinutes != null) {
            return "约 " + safeMinutes + " 分钟";
        }
        if (safeDistance != null) {
            return "约 " + formatDistanceKm(safeDistance) + " 公里";
        }
        return MISSING_ROUTE_SUMMARY;
    }

    private BigDecimal scaleDistance(BigDecimal distanceKm) {
        if (distanceKm == null) {
            return null;
        }
        return distanceKm.setScale(1, RoundingMode.HALF_UP);
    }

    private String formatDistanceKm(BigDecimal distanceKm) {
        if (distanceKm == null) {
            return null;
        }
        return distanceKm.stripTrailingZeros().toPlainString();
    }

    private List<RoutePathPointVO> toRoutePathPoints(List<GeoPoint> points) {
        if (points == null || points.isEmpty()) {
            return List.of();
        }
        List<RoutePathPointVO> converted = new ArrayList<>();
        for (GeoPoint point : points) {
            if (point == null || !point.valid()) {
                continue;
            }
            RoutePathPointVO routePathPoint = new RoutePathPointVO();
            routePathPoint.setLatitude(point.latitude());
            routePathPoint.setLongitude(point.longitude());
            converted.add(routePathPoint);
        }
        return List.copyOf(converted);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
