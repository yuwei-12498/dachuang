package com.citytrip.service.impl;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.vo.DepartureLegEstimateVO;
import com.citytrip.model.vo.ItineraryRouteDecorationVO;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryOptionVO;
import com.citytrip.model.vo.RouteNodeDecorationVO;
import com.citytrip.model.vo.SegmentTransportAnalysisVO;
import com.citytrip.model.vo.SmartFillVO;
import com.citytrip.service.LlmService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MockLlmServiceImpl implements LlmService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public String explainItinerary(GenerateReqDTO userReq, List<ItineraryNodeVO> nodes) {
        int stopCount = nodes == null ? 0 : nodes.size();
        StringBuilder builder = new StringBuilder("【系统生成】已为你组合出 ")
                .append(stopCount)
                .append(" 个顺路景点，整体优先兼顾主题匹配、路程顺滑和时间窗可执行性。");
        if (userReq != null && userReq.getThemes() != null && userReq.getThemes().contains("文化")) {
            builder.append(" 本次路线会更偏向历史文化与城市记忆类点位。");
        }
        builder.append(" 如需更省时或更省钱的方案，可继续切换候选路线。");
        return builder.toString();
    }

    @Override
    public String generateTips(GenerateReqDTO userReq) {
        List<String> tips = new ArrayList<>();
        if (userReq == null) {
            tips.add("出发前记得带好饮水、充电设备和舒适的步行装备。");
            return String.join(" ", tips);
        }

        boolean outdoorTrip = userReq.getThemes() != null
                && userReq.getThemes().stream()
                .anyMatch(theme -> theme != null && (theme.contains("山") || theme.contains("徒步") || theme.contains("户外") || theme.contains("自然")));
        if (outdoorTrip || "高".equals(userReq.getWalkingLevel()) || "high".equalsIgnoreCase(userReq.getWalkingLevel())) {
            tips.add("这条路线可能包含较多步行或爬坡，建议穿防滑鞋，并预留补水时间。");
        }
        if (Boolean.TRUE.equals(userReq.getIsRainy())) {
            tips.add("雨天出行请备好雨具，并注意台阶、石板路和临水区域的防滑。");
        }
        if (Boolean.TRUE.equals(userReq.getIsNight())) {
            tips.add("夜间返程前建议提前确认交通方式，并适当增加保暖衣物。");
        }
        if ("亲子".equals(userReq.getCompanionType())) {
            tips.add("亲子出行建议适当增加休息频次，并提前准备零食和备用衣物。");
        }
        if ("长者".equals(userReq.getCompanionType()) || "老人".equals(userReq.getCompanionType())) {
            tips.add("若有长者同行，建议优先控制步行强度，并尽量选择可随时休息的点位。");
        }
        if (tips.isEmpty()) {
            tips.add("出发前记得带好饮水、充电设备和舒适的步行装备。");
        }
        return String.join(" ", tips);
    }

    @Override
    public String generatePoiWarmTips(GenerateReqDTO userReq, ItineraryNodeVO node) {
        return String.join("\n", buildPoiTipPool(userReq, node));
    }

    @Override
    public String generateRouteWarmTip(GenerateReqDTO userReq, List<ItineraryNodeVO> nodes) {
        int stopCount = nodes == null ? 0 : nodes.size();
        if (stopCount == 0) {
            return "先确认出发时间和入口位置，临走前再看一眼导航会更稳妥。";
        }
        if (Boolean.TRUE.equals(userReq == null ? null : userReq.getIsRainy())) {
            return "雨天路面偏滑，今天按主线慢慢走，拍照和休息都别压太满。";
        }
        if (Boolean.TRUE.equals(userReq == null ? null : userReq.getIsNight())) {
            return "夜游结束别拖太晚，给返程和等车都预留一点机动时间。";
        }
        if (stopCount >= 4) {
            return "今天站点不少，先按主线顺着走，中途记得给休息留出空档。";
        }
        return "今天这条线更适合顺路慢逛，边走边拍会比来回折返更舒服。";
    }

    @Override
    public String explainOptionRecommendation(GenerateReqDTO userReq, ItineraryOptionVO option) {
        if (option == null) {
            return explainItinerary(userReq, Collections.emptyList());
        }

        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(option.getSummary())) {
            parts.add(option.getSummary().trim());
        }
        if (option.getHighlights() != null && !option.getHighlights().isEmpty()) {
            parts.add("核心亮点包括：" + String.join("、", option.getHighlights().stream().limit(2).toList()));
        }
        if (parts.isEmpty()) {
            return explainItinerary(userReq, option.getNodes() == null ? Collections.emptyList() : option.getNodes());
        }
        return String.join("；", parts.stream().limit(2).toList()) + "。";
    }

    @Override
    public String explainPoiChoice(GenerateReqDTO userReq, ItineraryNodeVO node) {
        if (node == null) {
            return "该点位与当前路线的主题和顺路性较为匹配。";
        }
        return "“" + node.getPoiName() + "”被纳入路线，是因为它与偏好主题更匹配，且能和前后点位形成更顺滑的行程衔接。";
    }

    @Override
    public SmartFillVO parseSmartFill(String text, List<String> poiNameHints) {
        SmartFillVO result = new SmartFillVO();
        if (!StringUtils.hasText(text)) {
            return result;
        }

        String raw = text.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        Set<String> summary = new LinkedHashSet<>();

        if (raw.contains("半天")) {
            result.setTripDays(0.5D);
            summary.add("半天");
        } else if (raw.contains("两天") || raw.contains("2天")) {
            result.setTripDays(2.0D);
            summary.add("两天");
        } else if (raw.contains("一天") || raw.contains("1天") || raw.contains("全天")) {
            result.setTripDays(1.0D);
            summary.add("一天");
        }

        if (raw.contains("雨")) {
            result.setIsRainy(true);
            summary.add("雨天优先");
        }
        if (raw.contains("夜") || raw.contains("晚上")) {
            result.setIsNight(true);
            summary.add("夜游");
        }

        if (raw.contains("情侣") || raw.contains("约会") || raw.contains("女朋友") || raw.contains("男朋友")) {
            result.setCompanionType("情侣");
            summary.add("情侣");
        } else if (raw.contains("亲子") || raw.contains("带娃") || raw.contains("孩子")) {
            result.setCompanionType("亲子");
            summary.add("亲子");
        } else if (raw.contains("朋友") || raw.contains("同学")) {
            result.setCompanionType("朋友");
            summary.add("朋友");
        } else if (raw.contains("一个人") || raw.contains("独自")) {
            result.setCompanionType("独自");
            summary.add("独自");
        }

        Set<String> themes = new LinkedHashSet<>();
        if (raw.contains("购物") || raw.contains("商场") || raw.contains("逛街") || raw.contains("太古里") || lower.contains("ifs")) {
            themes.add("购物");
        }
        if (raw.contains("美食") || raw.contains("小吃") || raw.contains("火锅")) {
            themes.add("美食");
        }
        if (raw.contains("博物馆") || raw.contains("历史") || raw.contains("文化")) {
            themes.add("文化");
        }
        if (!themes.isEmpty()) {
            result.setThemes(new ArrayList<>(themes));
            summary.addAll(themes);
        }

        Set<String> mustVisit = new LinkedHashSet<>();
        if (lower.contains("ifs") || raw.contains("国金") || raw.contains("金融中心")) {
            mustVisit.add("IFS国际金融中心");
        }
        if (raw.contains("太古里")) {
            mustVisit.add("太古里");
        }
        if (raw.contains("春熙路")) {
            mustVisit.add("春熙路");
        }
        if (poiNameHints != null) {
            for (String poiName : poiNameHints) {
                if (!StringUtils.hasText(poiName)) {
                    continue;
                }
                if (raw.contains(poiName)) {
                    mustVisit.add(poiName.trim());
                }
            }
        }
        if (!mustVisit.isEmpty()) {
            result.setMustVisitPoiNames(new ArrayList<>(mustVisit));
            summary.add("必去：" + String.join("、", mustVisit));
        }

        result.setSummary(new ArrayList<>(summary));
        return result;
    }

    @Override
    public DepartureLegEstimateVO estimateDepartureLeg(GenerateReqDTO userReq, ItineraryNodeVO firstNode) {
        DepartureLegEstimateVO estimate = new DepartureLegEstimateVO();
        if (firstNode == null) {
            return estimate;
        }

        Integer fallbackMinutes = firstNode.getTravelTime();
        double roadDistanceKm = estimateRoadDistanceKm(userReq, firstNode);
        int minutes = fallbackMinutes == null ? 0 : Math.max(fallbackMinutes, 0);

        if (roadDistanceKm > 0) {
            if (minutes <= 0) {
                if (roadDistanceKm <= 1.2D) {
                    minutes = Math.max(6, (int) Math.ceil((roadDistanceKm / 4.8D) * 60D));
                } else if (roadDistanceKm <= 5D) {
                    minutes = Math.max(14, (int) Math.ceil((roadDistanceKm / 18D) * 60D) + 5);
                } else {
                    minutes = Math.max(24, (int) Math.ceil((roadDistanceKm / 26D) * 60D) + 8);
                }
            }
            estimate.setEstimatedDistanceKm(BigDecimal.valueOf(roadDistanceKm).setScale(1, RoundingMode.HALF_UP));
        }

        if (minutes > 0) {
            estimate.setEstimatedMinutes(minutes);
        }
        estimate.setTransportMode(resolveTransportMode(roadDistanceKm, minutes));
        return estimate;
    }

    @Override
    public SegmentTransportAnalysisVO analyzeSegmentTransport(GenerateReqDTO userReq, ItineraryNodeVO fromNode, ItineraryNodeVO toNode) {
        SegmentTransportAnalysisVO analysis = new SegmentTransportAnalysisVO();
        String transportMode = resolveSegmentTransportMode(fromNode, toNode);
        analysis.setTransportMode(transportMode);
        analysis.setNarrative(buildSegmentNarrative(userReq, fromNode, toNode, transportMode));
        return analysis;
    }

    @Override
    public ItineraryRouteDecorationVO decorateRouteExperience(GenerateReqDTO userReq, List<ItineraryNodeVO> nodes) {
        ItineraryRouteDecorationVO decoration = new ItineraryRouteDecorationVO();
        decoration.setRouteWarmTip(generateRouteWarmTip(userReq, nodes));
        if (nodes == null || nodes.isEmpty()) {
            decoration.setNodes(List.of());
            return decoration;
        }

        List<RouteNodeDecorationVO> items = new ArrayList<>();
        for (int index = 0; index < nodes.size(); index++) {
            ItineraryNodeVO node = nodes.get(index);
            ItineraryNodeVO previousNode = index > 0 ? nodes.get(index - 1) : null;
            SegmentTransportAnalysisVO segment = analyzeSegmentTransport(userReq, previousNode, node);

            RouteNodeDecorationVO item = new RouteNodeDecorationVO();
            item.setIndex(index);
            item.setWarmTips(buildPoiTipPool(userReq, node));
            item.setTransportMode(segment.getTransportMode());
            item.setNarrative(segment.getNarrative());
            items.add(item);
        }
        decoration.setNodes(items);
        return decoration;
    }

    private List<String> buildPoiTipPool(GenerateReqDTO userReq, ItineraryNodeVO node) {
        Set<String> tips = new LinkedHashSet<>();
        String merged = (node == null ? "" : String.join(" ",
                defaultString(node.getPoiName()),
                defaultString(node.getCategory()),
                defaultString(node.getDistrict())
        )).toLowerCase();

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
            tips.add("热门小吃排队快，想吃就先路过先买。");
        } else if (containsAny(merged, "寺", "祠", "宫", "观")) {
            tips.add("参观前先看礼仪提示，走动会更从容。");
            tips.add("台阶区域较多，上下时别急着赶路。");
            tips.add("高峰时段游客集中，想拍空景建议早一点。");
        }

        if (Boolean.TRUE.equals(userReq == null ? null : userReq.getIsRainy())) {
            tips.add("雨天路面偏滑，鞋底抓地力更重要。");
        }
        if (Boolean.TRUE.equals(userReq == null ? null : userReq.getIsNight())) {
            tips.add("返程别拖太晚，出门前先看好回程方式。");
        }
        if ("亲子".equals(userReq == null ? null : userReq.getCompanionType())) {
            tips.add("带小朋友时别把节奏拉太满，中途多休息。");
        }
        if (tips.isEmpty()) {
            tips.add("先看导航和入口位置，到场会更省时间。");
            tips.add("热门时段人会多一点，预留些机动时间。");
            tips.add("边走边拍时注意脚下，节奏放稳更舒服。");
            tips.add("如果要临时换点位，先确认下一站路线。");
        }
        return tips.stream().limit(5).toList();
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

    private double estimateRoadDistanceKm(GenerateReqDTO req, ItineraryNodeVO firstNode) {
        if (req == null || firstNode == null
                || req.getDepartureLatitude() == null || req.getDepartureLongitude() == null
                || firstNode.getLatitude() == null || firstNode.getLongitude() == null) {
            return 0D;
        }

        double fromLat = req.getDepartureLatitude();
        double fromLng = req.getDepartureLongitude();
        double toLat = firstNode.getLatitude().doubleValue();
        double toLng = firstNode.getLongitude().doubleValue();
        if (!isValidLatLng(fromLat, fromLng) || !isValidLatLng(toLat, toLng)) {
            return 0D;
        }

        double straightDistance = haversineDistanceKm(fromLat, fromLng, toLat, toLng);
        if (straightDistance <= 0D) {
            return 0D;
        }

        double roadFactor;
        if (straightDistance <= 1D) {
            roadFactor = 1.2D;
        } else if (straightDistance <= 4D) {
            roadFactor = 1.3D;
        } else if (straightDistance <= 10D) {
            roadFactor = 1.4D;
        } else {
            roadFactor = 1.5D;
        }
        return straightDistance * roadFactor;
    }

    private boolean isValidLatLng(double lat, double lng) {
        return Math.abs(lat) <= 90D && Math.abs(lng) <= 180D;
    }

    private double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = lat1Rad - lat2Rad;
        double deltaLon = Math.toRadians(lon1 - lon2);
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.pow(Math.sin(deltaLon / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    private String resolveTransportMode(double roadDistanceKm, int minutes) {
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

        if (minutes <= 12) {
            return "步行";
        }
        if (minutes <= 22) {
            return "骑行";
        }
        if (minutes <= 45) {
            return "地铁+步行";
        }
        return "打车";
    }


    private String resolveSegmentTransportMode(ItineraryNodeVO fromNode, ItineraryNodeVO toNode) {
        if (toNode != null) {
            if (StringUtils.hasText(toNode.getTravelTransportMode())) {
                return toNode.getTravelTransportMode().trim();
            }
            if (StringUtils.hasText(toNode.getDepartureTransportMode())) {
                return toNode.getDepartureTransportMode().trim();
            }
            BigDecimal distance = toNode.getTravelDistanceKm() != null ? toNode.getTravelDistanceKm() : toNode.getDepartureDistanceKm();
            Integer minutes = toNode.getTravelTime() != null ? toNode.getTravelTime() : toNode.getDepartureTravelTime();
            if (distance != null || minutes != null) {
                return resolveTransportMode(distance == null ? 0D : distance.doubleValue(), minutes == null ? 0 : minutes);
            }
        }
        if (fromNode != null && StringUtils.hasText(fromNode.getTravelTransportMode())) {
            return fromNode.getTravelTransportMode().trim();
        }
        return "??+??";
    }

    private String buildSegmentNarrative(GenerateReqDTO userReq,
                                         ItineraryNodeVO fromNode,
                                         ItineraryNodeVO toNode,
                                         String transportMode) {
        String origin = fromNode == null
                ? defaultString(userReq == null ? null : userReq.getDeparturePlaceName(), "????")
                : defaultString(fromNode.getPoiName(), "???");
        String destination = toNode == null ? "???" : defaultString(toNode.getPoiName(), "???");
        Integer minutes = toNode == null
                ? null
                : (fromNode == null ? toNode.getDepartureTravelTime() : toNode.getTravelTime());
        BigDecimal distanceKm = toNode == null
                ? null
                : (fromNode == null ? toNode.getDepartureDistanceKm() : toNode.getTravelDistanceKm());

        if (minutes != null && minutes > 0 && distanceKm != null && distanceKm.compareTo(BigDecimal.ZERO) > 0) {
            return origin + "?" + destination + "???" + minutes + "????" + distanceKm.setScale(1, RoundingMode.HALF_UP).toPlainString()
                    + "????" + transportMode + "????";
        }
        if (minutes != null && minutes > 0) {
            return origin + "?" + destination + "???" + minutes + "????" + transportMode + "?????";
        }
        return origin + "?" + destination + "?????" + transportMode + "????????";
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

}
