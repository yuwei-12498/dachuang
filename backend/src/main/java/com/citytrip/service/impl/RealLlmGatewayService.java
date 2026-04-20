package com.citytrip.service.impl;

import com.citytrip.config.LlmProperties;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryOptionVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RealLlmGatewayService {
    private final SpringAiGatewaySupport springAiGatewaySupport;
    private final LlmProperties llmProperties;
    private final SafePromptBuilder safePromptBuilder;

    public RealLlmGatewayService(SpringAiGatewaySupport springAiGatewaySupport,
                                 LlmProperties llmProperties,
                                 SafePromptBuilder safePromptBuilder) {
        this.springAiGatewaySupport = springAiGatewaySupport;
        this.llmProperties = llmProperties;
        this.safePromptBuilder = safePromptBuilder;
    }

    public String explainItinerary(GenerateReqDTO userReq, List<ItineraryNodeVO> nodes) {
        return callText(safePromptBuilder.buildExplainItineraryPrompt(userReq, nodes));
    }

    public String generateTips(GenerateReqDTO userReq) {
        return callText(safePromptBuilder.buildGenerateTipsPrompt(userReq));
    }

    public String explainOptionRecommendation(GenerateReqDTO userReq, ItineraryOptionVO option) {
        return callText(safePromptBuilder.buildExplainOptionRecommendationPrompt(userReq, option));
    }

    public String explainPoiChoice(GenerateReqDTO userReq, ItineraryNodeVO node) {
        return callText(safePromptBuilder.buildExplainPoiChoicePrompt(userReq, node));
    }

    private String callText(String userPrompt) {
        if (!llmProperties.canTryRealText()) {
            throw new IllegalStateException("OpenAI real text model is not configured");
        }

        LlmProperties.ResolvedOpenAiOptions textOptions = llmProperties.getOpenai().resolveTextOptions();
        return springAiGatewaySupport.call(
                textOptions,
                safePromptBuilder.buildItinerarySystemPrompt(),
                userPrompt
        );
    }
}
