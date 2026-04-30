package com.citytrip.service.skill;

import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatSkillPayloadVO;
import com.citytrip.service.application.itinerary.ChatItineraryEditWorkflowService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Order(12)
public class ItineraryEditSkill implements ChatSkillHandler {

    private final ChatItineraryEditWorkflowService workflowService;

    public ItineraryEditSkill(ChatItineraryEditWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public String skillName() {
        return "itinerary_edit";
    }

    @Override
    public boolean supports(ChatReqDTO req) {
        if (req == null
                || req.getContext() == null
                || req.getContext().getItinerary() == null
                || req.getContext().getItinerary().getNodes() == null
                || req.getContext().getItinerary().getNodes().isEmpty()) {
            return false;
        }
        String question = req.getQuestion();
        if (!StringUtils.hasText(question)) {
            return false;
        }
        String normalized = question.trim();
        if (normalized.contains("换成") || normalized.contains("替换")) {
            return false;
        }
        boolean hasTimeEditIntent = (normalized.contains("开始") || normalized.contains("结束") || normalized.contains("出发"))
                && (normalized.contains("改") || normalized.contains("调整") || normalized.matches(".*\\d{1,2}:\\d{2}.*"));
        return normalized.contains("少玩")
                || normalized.contains("多玩")
                || normalized.contains("少待")
                || normalized.contains("多待")
                || normalized.contains("延长")
                || normalized.contains("缩短")
                || normalized.contains("删除")
                || normalized.contains("去掉")
                || normalized.contains("移除")
                || hasTimeEditIntent
                || normalized.contains("加一个")
                || normalized.contains("新增")
                || normalized.contains("插入");
    }

    @Override
    public ChatSkillPayloadVO execute(ChatReqDTO req) {
        return workflowService.handle(req);
    }
}
