package com.citytrip.service.application.itinerary;

import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.dto.ItineraryEditOperationDTO;
import com.citytrip.model.vo.ChatSkillPayloadVO;
import com.citytrip.model.vo.PoiSearchResultVO;
import com.citytrip.service.PoiService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatItineraryEditWorkflowServiceTest {

    @Test
    void handle_shouldBuildDraftForStayAndDayWindowAdjustments() {
        PoiService poiService = mock(PoiService.class);
        ChatItineraryEditWorkflowService service = new ChatItineraryEditWorkflowService(poiService);

        ChatSkillPayloadVO payload = service.handle(chatReq(
                "把圣灯公园少玩30分钟，第2天10:00开始",
                routeNode("node-a", "圣灯公园", 1, 1, 90),
                routeNode("node-b", "建设路小吃街", 2, 1, 120)
        ));

        assertThat(payload.getMessageType()).isEqualTo("workflow");
        assertThat(payload.getWorkflowType()).isEqualTo("itinerary_edit");
        assertThat(payload.getWorkflowState()).isEqualTo("proposal_ready");
        assertThat(payload.getItineraryEditDraft()).isNotNull();
        assertThat(payload.getItineraryEditDraft().getOperations()).extracting(ItineraryEditOperationDTO::getType)
                .containsExactly("update_stay", "update_day_window");
        assertThat(payload.getItineraryEditDraft().getOperations().get(0).getNodeKey()).isEqualTo("node-a");
        assertThat(payload.getItineraryEditDraft().getOperations().get(0).getStayDuration()).isEqualTo(60);
        assertThat(payload.getItineraryEditDraft().getOperations().get(1).getDayNo()).isEqualTo(2);
        assertThat(payload.getItineraryEditDraft().getOperations().get(1).getStartTime()).isEqualTo("10:00");
        assertThat(payload.getActions()).extracting(ChatSkillPayloadVO.ActionItem::getKey)
                .containsExactly("confirm_itinerary_edit", "decline_itinerary_edit");
    }

    @Test
    void handle_shouldResolveInsertedPoiBeforeGeneratingDraft() {
        PoiService poiService = mock(PoiService.class);
        ChatItineraryEditWorkflowService service = new ChatItineraryEditWorkflowService(poiService);

        when(poiService.searchLive("成都大学", "成都", 5))
                .thenReturn(List.of(poi("成都大学", "高校", "vivo-geo")));

        ChatSkillPayloadVO payload = service.handle(chatReq(
                "在建设路小吃街后面加一个成都大学",
                routeNode("node-b", "建设路小吃街", 2, 1, 120)
        ));

        assertThat(payload.getWorkflowState()).isEqualTo("proposal_ready");
        assertThat(payload.getResults()).hasSize(1);
        assertThat(payload.getItineraryEditDraft()).isNotNull();
        assertThat(payload.getItineraryEditDraft().getOperations()).hasSize(1);
        ItineraryEditOperationDTO operation = payload.getItineraryEditDraft().getOperations().get(0);
        assertThat(operation.getType()).isEqualTo("insert_inline_custom_poi");
        assertThat(operation.getDayNo()).isEqualTo(2);
        assertThat(operation.getTargetIndex()).isEqualTo(2);
        assertThat(operation.getCustomPoiDraft().getName()).isEqualTo("成都大学");
        assertThat(operation.getCustomPoiDraft().getRoughLocation()).isEqualTo("成都大学地址");
    }

    private ChatReqDTO chatReq(String question, ChatReqDTO.ChatRouteNode... nodes) {
        ChatReqDTO req = new ChatReqDTO();
        req.setQuestion(question);

        ChatReqDTO.ChatItineraryContext itinerary = new ChatReqDTO.ChatItineraryContext();
        itinerary.setItineraryId(88L);
        itinerary.setNodes(List.of(nodes));

        ChatReqDTO.ChatContext context = new ChatReqDTO.ChatContext();
        context.setCityName("成都");
        context.setItinerary(itinerary);
        req.setContext(context);
        return req;
    }

    private ChatReqDTO.ChatRouteNode routeNode(String nodeKey,
                                               String name,
                                               int dayNo,
                                               int stepOrder,
                                               int stayDuration) {
        ChatReqDTO.ChatRouteNode node = new ChatReqDTO.ChatRouteNode();
        node.setNodeKey(nodeKey);
        node.setDayNo(dayNo);
        node.setStepOrder(stepOrder);
        node.setPoiId((long) name.hashCode());
        node.setPoiName(name);
        node.setStayDuration(stayDuration);
        node.setCategory("景点");
        node.setDistrict("成都");
        node.setLatitude(new BigDecimal("30.670000"));
        node.setLongitude(new BigDecimal("104.120000"));
        return node;
    }

    private PoiSearchResultVO poi(String name, String category, String source) {
        return new PoiSearchResultVO(
                name,
                name + "地址",
                category,
                new BigDecimal("30.670000"),
                new BigDecimal("104.120000"),
                "成都",
                "510100",
                source
        );
    }
}
