package com.citytrip.service.impl;

import com.citytrip.config.LlmProperties;
import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatSkillPayloadVO;
import com.citytrip.model.vo.ChatVO;
import com.citytrip.service.domain.ai.ChatPoiSkillService;
import com.citytrip.service.skill.SkillRouterService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RealChatGatewayServiceTest {

    @Test
    void streamAnswer_shouldReturnLocalWorkflowProposalWithoutCallingModel() {
        OpenAiGatewayClient gatewayClient = mock(OpenAiGatewayClient.class);
        SafePromptBuilder safePromptBuilder = mock(SafePromptBuilder.class);
        ChatPoiSkillService chatPoiSkillService = mock(ChatPoiSkillService.class);
        SkillRouterService skillRouterService = mock(SkillRouterService.class);

        LlmProperties properties = new LlmProperties();
        properties.setProvider("real");
        properties.getOpenai().setApiKey("sk-test");
        properties.getOpenai().getChat().setBaseUrl("https://api-ai.vivo.com.cn/v1");
        properties.getOpenai().getChat().setModel("Doubao-Seed-2.0-mini");

        RealChatGatewayService service = new RealChatGatewayService(
                gatewayClient,
                properties,
                safePromptBuilder,
                chatPoiSkillService,
                null,
                null,
                null,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(service, "skillRouterService", skillRouterService);

        ChatSkillPayloadVO payload = new ChatSkillPayloadVO();
        payload.setSkillName("itinerary_edit");
        payload.setMessageType("workflow");
        payload.setWorkflowType("itinerary_edit");
        payload.setWorkflowState("proposal_ready");
        payload.setFallbackMessage("本次将这样修改：\n1. 把宽窄巷子停留调整为 60 分钟\n如你同意，我就应用到当前行程。");

        when(skillRouterService.route(any(ChatReqDTO.class))).thenReturn(Optional.of(payload));

        List<String> streamedTokens = new ArrayList<>();
        ChatVO result = service.streamAnswer(editRequest(), streamedTokens::add);

        assertThat(result.getAnswer()).isEqualTo(payload.getFallbackMessage());
        assertThat(result.getSkillPayload()).isSameAs(payload);
        assertThat(streamedTokens).containsExactly(payload.getFallbackMessage());
        verifyNoInteractions(gatewayClient);
        verifyNoInteractions(safePromptBuilder);
        verifyNoInteractions(chatPoiSkillService);
    }

    private ChatReqDTO editRequest() {
        ChatReqDTO.ChatRouteNode node = new ChatReqDTO.ChatRouteNode();
        node.setNodeKey("node-1");
        node.setPoiName("宽窄巷子");
        node.setDayNo(1);
        node.setStepOrder(1);
        node.setStayDuration(90);

        ChatReqDTO.ChatItineraryContext itinerary = new ChatReqDTO.ChatItineraryContext();
        itinerary.setItineraryId(99L);
        itinerary.setNodes(List.of(node));

        ChatReqDTO.ChatContext context = new ChatReqDTO.ChatContext();
        context.setCityName("成都");
        context.setItinerary(itinerary);

        ChatReqDTO req = new ChatReqDTO();
        req.setQuestion("把宽窄巷子少玩半小时");
        req.setContext(context);
        return req;
    }
}
