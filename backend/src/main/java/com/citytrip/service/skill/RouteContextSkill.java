package com.citytrip.service.skill;

import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatSkillPayloadVO;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Order(10)
public class RouteContextSkill extends AbstractGeoSkill {

    @Override
    public String skillName() {
        return "route_context";
    }

    @Override
    public boolean supports(ChatReqDTO req) {
        return req != null
                && req.getContext() != null
                && req.getContext().getItinerary() != null
                && req.getContext().getItinerary().getNodes() != null
                && !req.getContext().getItinerary().getNodes().isEmpty()
                && containsAny(questionOf(req), "这条路线", "当前行程", "这趟安排");
    }

    @Override
    public ChatSkillPayloadVO execute(ChatReqDTO req) {
        String city = cityOf(req);
        List<ChatSkillPayloadVO.ResultItem> items = new ArrayList<>();
        for (ChatReqDTO.ChatRouteNode node : req.getContext().getItinerary().getNodes()) {
            if (node == null || !StringUtils.hasText(node.getPoiName())) {
                continue;
            }
            ChatSkillPayloadVO.ResultItem item = new ChatSkillPayloadVO.ResultItem();
            item.setName(node.getPoiName().trim());
            item.setCategory(node.getCategory());
            item.setCityName(city);
            item.setSource("itinerary-route");
            item.setLatitude(node.getLatitude());
            item.setLongitude(node.getLongitude());
            items.add(item);
        }
        String summary = req.getContext().getItinerary().getSummary();
        ChatSkillPayloadVO payload = buildPayload(skillName(), "route_context", city,
                summary, "route", items.size(), 0, items,
                "itinerary-route", "我先把当前行程里的站点列给你。");
        payload.getQuery().setKeyword(summary);
        return payload;
    }
}