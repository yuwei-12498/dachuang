package com.citytrip.service.impl;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryOptionVO;
import com.citytrip.service.LlmService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MockLlmServiceImpl implements LlmService {

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
}
