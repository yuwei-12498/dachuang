package com.citytrip.service.impl;

import com.citytrip.config.LlmProperties;
import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatStatusVO;
import com.citytrip.model.vo.ChatVO;
import com.citytrip.service.ChatService;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class OpenAiChatServiceImpl implements ChatService {

    private final RealChatGatewayService realChatGatewayService;
    private final LlmProperties llmProperties;

    public OpenAiChatServiceImpl(RealChatGatewayService realChatGatewayService,
                                 LlmProperties llmProperties) {
        this.realChatGatewayService = realChatGatewayService;
        this.llmProperties = llmProperties;
    }

    @Override
    public ChatVO answerQuestion(ChatReqDTO req) {
        if (!llmProperties.canTryRealChat()) {
            throw new IllegalStateException("OpenAI real model is not configured");
        }
        return realChatGatewayService.answerQuestion(req);
    }

    @Override
    public ChatVO streamAnswer(ChatReqDTO req, Consumer<String> tokenConsumer) {
        if (!llmProperties.canTryRealChat()) {
            throw new IllegalStateException("OpenAI real model is not configured");
        }
        return realChatGatewayService.streamAnswer(req, tokenConsumer);
    }

    @Override
    public ChatStatusVO getStatus() {
        LlmProperties.ResolvedOpenAiOptions chatOptions = llmProperties.getOpenai().resolveChatOptions();
        ChatStatusVO vo = new ChatStatusVO();
        vo.setProvider("real");
        vo.setConfigured(llmProperties.canTryRealChat());
        vo.setRealModelAvailable(llmProperties.canTryRealChat());
        vo.setFallbackToMock(llmProperties.isFallbackToMock());
        vo.setTimeoutSeconds(llmProperties.getTimeoutSeconds());
        vo.setModel(chatOptions.getModel());
        vo.setBaseUrl(chatOptions.getBaseUrl());
        vo.setMessage(llmProperties.canTryRealChat()
                ? "Real model config looks valid."
                : "Real model config is incomplete.");
        return vo;
    }
}
