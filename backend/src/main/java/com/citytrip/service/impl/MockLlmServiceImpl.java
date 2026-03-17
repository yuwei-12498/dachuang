package com.citytrip.service.impl;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.service.LlmService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MockLlmServiceImpl implements LlmService {

    @Override
    public String explainItinerary(GenerateReqDTO userReq, List<ItineraryNodeVO> nodes) {
        StringBuilder sb = new StringBuilder("【系统自动生成】");
        sb.append("本市游为您精选了 ").append(nodes.size()).append(" 个打卡地。");
        if (userReq != null && userReq.getThemes() != null && userReq.getThemes().contains("文化")) {
            sb.append("路线深度串联了历史底蕴。");
        }
        sb.append("希望您在成都有一段美妙的旅程！");
        return sb.toString();
    }

    @Override
    public String generateTips(GenerateReqDTO userReq) {
        if (userReq == null) return "💡 建议穿戴舒适的运动鞋，多喝水，享受旅途！";
        
        List<String> tips = new ArrayList<>();
        if (Boolean.TRUE.equals(userReq.getIsRainy())) {
            tips.add("🌧️ 天气多变，建议携带雨伞，注意防滑！");
        }
        if (Boolean.TRUE.equals(userReq.getIsNight())) {
            tips.add("🌃 安排了夜游环节，结束较晚请注意返程的安全。");
        }
        if ("亲子".equals(userReq.getCompanionType())) {
            tips.add("👨‍👩‍👧 亲子出行请注意控制步行时长，随时留意小朋友的休息需求。");
        }
        if (tips.isEmpty()) {
            tips.add("💡 建议穿戴舒适的运动鞋，多喝水，享受旅途！");
        }
        return String.join(" ", tips);
    }

    @Override
    public String explainPoiChoice(GenerateReqDTO userReq, ItineraryNodeVO node) {
        return "基于您的偏好标签与行程顺畅度智能匹配";
    }
}
