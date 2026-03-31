package com.citytrip.service.impl;

import com.citytrip.config.LlmProperties;
import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatStatusVO;
import com.citytrip.model.vo.ChatVO;
import com.citytrip.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

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
            log.info("Chat service is forced to use mock provider");
            return mockChatService.answerQuestion(req);
        }

        if (!llmProperties.canTryReal()) {
            if (llmProperties.isRealOnly()) {
                return buildErrorResponse("大模型配置不可用，请检查 API Key、Base URL 和模型开关。");
            }
            log.info("Real chat model is not available, falling back to mock. provider={}", llmProperties.getProvider());
            return mockChatService.answerQuestion(req);
        }

        try {
            log.info("Chat service is using real model first. provider={}, model={}",
                    llmProperties.getProvider(), llmProperties.getOpenai().getModel());
            ChatVO result = openAiChatService.answerQuestion(req);
            if (result == null || result.getAnswer() == null || result.getAnswer().trim().isEmpty()) {
                throw new IllegalStateException("真实模型返回了空内容");
            }
            return result;
        } catch (Exception e) {
            if (llmProperties.isFallbackToMock()) {
                log.warn("Real chat model failed, falling back to mock. reason={}", e.getMessage(), e);
                return mockChatService.answerQuestion(req);
            }
            log.error("Real chat model failed and fallback is disabled. reason={}", e.getMessage(), e);
            return buildErrorResponse(
                    "大模型调用失败，请检查模型名、API Key、Base URL、额度或网络连通性。原因：" + safeMessage(e));
        }
    }

    @Override
    public ChatStatusVO getStatus() {
        ChatStatusVO vo = new ChatStatusVO();
        vo.setProvider(llmProperties.getProvider());
        vo.setConfigured(llmProperties.canTryReal());
        vo.setRealModelAvailable(llmProperties.canTryReal());
        vo.setFallbackToMock(llmProperties.isFallbackToMock());
        vo.setTimeoutSeconds(llmProperties.getTimeoutSeconds());
        vo.setModel(llmProperties.getOpenai().getModel());
        vo.setBaseUrl(llmProperties.getOpenai().getBaseUrl());

        if (llmProperties.isMockOnly()) {
            vo.setMessage("当前聊天服务固定使用本地 Mock 响应。");
            return vo;
        }

        if (!llmProperties.canTryReal()) {
            vo.setMessage("真实大模型配置不完整，请检查 API Key、Base URL 和模型开关。");
            return vo;
        }

        if (llmProperties.isFallbackToMock()) {
            vo.setMessage("当前优先调用真实大模型，失败后会降级到 Mock。");
            return vo;
        }

        vo.setMessage("当前优先调用真实大模型，未启用自动降级。");
        return vo;
    }

    private ChatVO buildErrorResponse(String message) {
        ChatVO vo = new ChatVO();
        vo.setAnswer(message);
        vo.setRelatedTips(List.of("检查模型配置", "稍后重试"));
        return vo;
    }

    private String safeMessage(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().trim().isEmpty()) {
            return "未知错误";
        }
        return e.getMessage();
    }
}
