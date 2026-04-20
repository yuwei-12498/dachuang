package com.citytrip.service.impl;

import com.citytrip.config.LlmProperties;
import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.vo.ChatStatusVO;
import com.citytrip.model.vo.ChatVO;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryOptionVO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiServiceDelegationTest {

    @Test
    void shouldDelegateChatCallsToGatewayService() {
        StubRealChatGatewayService realChatGatewayService = new StubRealChatGatewayService();
        LlmProperties llmProperties = buildValidChatProperties();
        OpenAiChatServiceImpl service = new OpenAiChatServiceImpl(realChatGatewayService, llmProperties);
        Consumer<String> tokenConsumer = token -> {
        };

        ChatReqDTO req = new ChatReqDTO();
        req.setQuestion("Recommend a rainy-day museum route in Chengdu.");

        ChatVO answer = new ChatVO();
        answer.setAnswer("Visit Chengdu Museum first.");
        realChatGatewayService.answerToReturn = answer;
        realChatGatewayService.streamAnswerToReturn = answer;

        ChatVO actualAnswer = service.answerQuestion(req);
        ChatVO actualStreamAnswer = service.streamAnswer(req, tokenConsumer);

        assertThat(actualAnswer).isSameAs(answer);
        assertThat(actualStreamAnswer).isSameAs(answer);
        assertThat(realChatGatewayService.lastAnswerReq).isSameAs(req);
        assertThat(realChatGatewayService.lastStreamReq).isSameAs(req);
        assertThat(realChatGatewayService.lastTokenConsumer).isSameAs(tokenConsumer);
    }

    @Test
    void shouldExposeSceneSpecificChatStatus() {
        StubRealChatGatewayService realChatGatewayService = new StubRealChatGatewayService();
        LlmProperties llmProperties = buildValidChatProperties();
        llmProperties.setTimeoutSeconds(18);
        llmProperties.getOpenai().setBaseUrl("https://global.example/v1");
        llmProperties.getOpenai().setModel("gpt-global");
        llmProperties.getOpenai().getChat().setBaseUrl("https://chat.example/v1");
        llmProperties.getOpenai().getChat().setModel("gpt-chat");

        OpenAiChatServiceImpl service = new OpenAiChatServiceImpl(realChatGatewayService, llmProperties);

        ChatStatusVO status = service.getStatus();

        assertThat(status.getProvider()).isEqualTo("real");
        assertThat(status.getBaseUrl()).isEqualTo("https://chat.example/v1");
        assertThat(status.getModel()).isEqualTo("gpt-chat");
        assertThat(status.getTimeoutSeconds()).isEqualTo(18);
        assertThat(status.isConfigured()).isTrue();
        assertThat(status.isRealModelAvailable()).isTrue();
    }

    @Test
    void shouldDelegateTextGenerationToGatewayService() {
        StubRealLlmGatewayService realLlmGatewayService = new StubRealLlmGatewayService();
        OpenAiLlmServiceImpl service = new OpenAiLlmServiceImpl(realLlmGatewayService);

        GenerateReqDTO req = new GenerateReqDTO();
        req.setTripDays(1D);
        ItineraryNodeVO node = new ItineraryNodeVO();
        node.setPoiName("Chengdu Museum");
        List<ItineraryNodeVO> nodes = List.of(node);

        realLlmGatewayService.explainItineraryToReturn = "This route is compact and weather-friendly.";
        realLlmGatewayService.generateTipsToReturn = "Bring an umbrella and reserve popular venues.";
        realLlmGatewayService.explainOptionRecommendationToReturn = "Balanced route with strong theme match.";
        realLlmGatewayService.explainPoiChoiceToReturn = "This stop fits the morning indoor preference.";
        ItineraryOptionVO option = new ItineraryOptionVO();
        option.setSummary("Overall score is balanced.");

        assertThat(service.explainItinerary(req, nodes)).isEqualTo("This route is compact and weather-friendly.");
        assertThat(service.generateTips(req)).isEqualTo("Bring an umbrella and reserve popular venues.");
        assertThat(service.explainOptionRecommendation(req, option)).isEqualTo("Balanced route with strong theme match.");
        assertThat(service.explainPoiChoice(req, node)).isEqualTo("This stop fits the morning indoor preference.");

        assertThat(realLlmGatewayService.lastExplainReq).isSameAs(req);
        assertThat(realLlmGatewayService.lastExplainNodes).isSameAs(nodes);
        assertThat(realLlmGatewayService.lastTipsReq).isSameAs(req);
        assertThat(realLlmGatewayService.lastOptionReq).isSameAs(req);
        assertThat(realLlmGatewayService.lastOption).isSameAs(option);
        assertThat(realLlmGatewayService.lastPoiReq).isSameAs(req);
        assertThat(realLlmGatewayService.lastPoiNode).isSameAs(node);
    }

    private LlmProperties buildValidChatProperties() {
        LlmProperties llmProperties = new LlmProperties();
        llmProperties.getOpenai().setEnabled(true);
        llmProperties.getOpenai().setApiKey("integration-test-api-key");
        llmProperties.getOpenai().setBaseUrl("https://global.example/v1");
        llmProperties.getOpenai().setModel("gpt-global");
        llmProperties.getOpenai().getChat().setBaseUrl("https://chat.example/v1");
        llmProperties.getOpenai().getChat().setModel("gpt-chat");
        return llmProperties;
    }

    private static final class StubRealChatGatewayService extends RealChatGatewayService {
        private ChatReqDTO lastAnswerReq;
        private ChatReqDTO lastStreamReq;
        private Consumer<String> lastTokenConsumer;
        private ChatVO answerToReturn;
        private ChatVO streamAnswerToReturn;

        private StubRealChatGatewayService() {
            super(null, new LlmProperties(), new SafePromptBuilder());
        }

        @Override
        public ChatVO answerQuestion(ChatReqDTO req) {
            this.lastAnswerReq = req;
            return answerToReturn;
        }

        @Override
        public ChatVO streamAnswer(ChatReqDTO req, Consumer<String> tokenConsumer) {
            this.lastStreamReq = req;
            this.lastTokenConsumer = tokenConsumer;
            return streamAnswerToReturn;
        }
    }

    private static final class StubRealLlmGatewayService extends RealLlmGatewayService {
        private GenerateReqDTO lastExplainReq;
        private List<ItineraryNodeVO> lastExplainNodes;
        private GenerateReqDTO lastTipsReq;
        private GenerateReqDTO lastOptionReq;
        private ItineraryOptionVO lastOption;
        private GenerateReqDTO lastPoiReq;
        private ItineraryNodeVO lastPoiNode;
        private String explainItineraryToReturn;
        private String generateTipsToReturn;
        private String explainOptionRecommendationToReturn;
        private String explainPoiChoiceToReturn;

        private StubRealLlmGatewayService() {
            super(null, new LlmProperties(), new SafePromptBuilder());
        }

        @Override
        public String explainItinerary(GenerateReqDTO userReq, List<ItineraryNodeVO> nodes) {
            this.lastExplainReq = userReq;
            this.lastExplainNodes = nodes;
            return explainItineraryToReturn;
        }

        @Override
        public String generateTips(GenerateReqDTO userReq) {
            this.lastTipsReq = userReq;
            return generateTipsToReturn;
        }

        @Override
        public String explainOptionRecommendation(GenerateReqDTO userReq, ItineraryOptionVO option) {
            this.lastOptionReq = userReq;
            this.lastOption = option;
            return explainOptionRecommendationToReturn;
        }

        @Override
        public String explainPoiChoice(GenerateReqDTO userReq, ItineraryNodeVO node) {
            this.lastPoiReq = userReq;
            this.lastPoiNode = node;
            return explainPoiChoiceToReturn;
        }
    }
}
