package com.citytrip.service.impl;

import com.citytrip.config.LlmProperties;
import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
public class RealChatGatewayService {
    private final SpringAiGatewaySupport springAiGatewaySupport;
    private final LlmProperties llmProperties;
    private final SafePromptBuilder safePromptBuilder;

    public RealChatGatewayService(SpringAiGatewaySupport springAiGatewaySupport,
                                  LlmProperties llmProperties,
                                  SafePromptBuilder safePromptBuilder) {
        this.springAiGatewaySupport = springAiGatewaySupport;
        this.llmProperties = llmProperties;
        this.safePromptBuilder = safePromptBuilder;
    }

    public ChatVO answerQuestion(ChatReqDTO req) {
        String answer = callChatCompletion(req, null);
        ChatVO vo = new ChatVO();
        vo.setAnswer(answer);
        vo.setRelatedTips(buildRelatedTips(req));
        return vo;
    }

    public ChatVO streamAnswer(ChatReqDTO req, Consumer<String> tokenConsumer) {
        String answer = callChatCompletion(req, tokenConsumer);
        ChatVO vo = new ChatVO();
        vo.setAnswer(answer);
        vo.setRelatedTips(buildRelatedTips(req));
        return vo;
    }

    private String callChatCompletion(ChatReqDTO req, Consumer<String> tokenConsumer) {
        if (!llmProperties.canTryRealChat()) {
            throw new IllegalStateException("OpenAI real chat model is not configured");
        }

        LlmProperties.ResolvedOpenAiOptions chatOptions = llmProperties.getOpenai().resolveChatOptions();
        String systemPrompt = safePromptBuilder.buildChatSystemPrompt();
        String userPrompt = safePromptBuilder.buildChatUserPrompt(req);
        String answer = tokenConsumer == null
                ? springAiGatewaySupport.call(chatOptions, systemPrompt, userPrompt)
                : springAiGatewaySupport.stream(chatOptions, systemPrompt, userPrompt, tokenConsumer);
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI returned empty chat answer");
        }
        return answer.trim();
    }

    private List<String> buildRelatedTips(ChatReqDTO req) {
        String question = req == null ? "" : safe(req.getQuestion());
        List<String> tips = new ArrayList<>();
        if (question.contains("拍照") || question.contains("机位")) {
            tips.add("成都有哪些适合拍照的点位？");
            tips.add("这些点位怎么安排半日路线？");
        } else if (question.contains("雨")) {
            tips.add("雨天成都有哪些室内可逛点？");
            tips.add("雨天路线怎么减少步行？");
        } else if (question.contains("亲子") || question.contains("孩子")) {
            tips.add("亲子行程适合哪些博物馆？");
            tips.add("带孩子出行怎样控制步行强度？");
        } else {
            tips.add("可以按我的偏好生成一条路线吗？");
            tips.add("这条路线里哪个点最值得久留？");
        }
        return tips;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "" : value.trim();
    }
}
