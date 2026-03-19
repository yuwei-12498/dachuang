package com.citytrip.service.impl;

import com.citytrip.config.LlmProperties;
import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatVO;
import com.citytrip.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class RoutingChatServiceImpl implements ChatService {
    private static final Logger log = LoggerFactory.getLogger(RoutingChatServiceImpl.class);

    private final OpenAiChatServiceImpl openAiChatService;
    private final MockChatServiceImpl mockChatService;
    private final LlmProperties llmProperties;

    public RoutingChatServiceImpl(OpenAiChatServiceImpl openAiChatService,
                                  MockChatServiceImpl mockChatService,
                                  LlmProperties llmProperties) {
        this.openAiChatService = openAiChatService;
        this.mockChatService = mockChatService;
        this.llmProperties = llmProperties;
    }

    @Override
    public ChatVO answerQuestion(ChatReqDTO req) {
        if (llmProperties.isMockOnly()) {
            log.info("聊天服务当前使用 Mock 模型，provider=mock");
            return mockChatService.answerQuestion(req);
        }

        boolean canTryReal = llmProperties.canTryReal();
        if (!canTryReal) {
            if (llmProperties.isRealOnly()) {
                return handleFailureOrFallback(req, "API Key 未配置或真实模型未启用", null);
            }
            log.info("聊天服务当前使用 Mock 模型，provider={}，原因=未检测到可用真实模型配置", llmProperties.getProvider());
            return mockChatService.answerQuestion(req);
        }

        try {
            log.info("聊天服务当前优先使用真实模型，provider={}，model={}",
                    llmProperties.getProvider(), llmProperties.getOpenai().getModel());
            ChatVO result = openAiChatService.answerQuestion(req);
            if (result == null || result.getAnswer() == null || result.getAnswer().trim().isEmpty()) {
                throw new IllegalStateException("真实模型返回空结果");
            }
            return result;
        } catch (Exception e) {
            return handleFailureOrFallback(req, e.getMessage(), e);
        }
    }

    private ChatVO handleFailureOrFallback(ChatReqDTO req, String reason, Exception e) {
        if (llmProperties.isFallbackToMock()) {
            log.warn("【RoutingChatServiceImpl】聊天服务发生降级，改用 Mock。降级原因={}", reason, e);
            return mockChatService.answerQuestion(req);
        }
        log.error("【RoutingChatServiceImpl】聊天服务真实模型调用失败，且已禁用降级。失败原因={}", reason, e);
        throw new RuntimeException("真实大模型调用失败，且未启用 Mock 降级: " + reason, e);
    }
}
