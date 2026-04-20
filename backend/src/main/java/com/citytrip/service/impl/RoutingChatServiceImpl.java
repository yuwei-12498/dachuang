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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Primary
@Service
public class RoutingChatServiceImpl implements ChatService {
    private static final Logger log = LoggerFactory.getLogger(RoutingChatServiceImpl.class);

    private final RealChatGatewayService realChatService;
    private final MockChatServiceImpl mockChatService;
    private final LlmProperties llmProperties;

    public RoutingChatServiceImpl(RealChatGatewayService realChatService,
                                  MockChatServiceImpl mockChatService,
                                  LlmProperties llmProperties) {
        this.realChatService = realChatService;
        this.mockChatService = mockChatService;
        this.llmProperties = llmProperties;
    }

    @Override
    public ChatVO answerQuestion(ChatReqDTO req) {
        if (llmProperties.isMockOnly()) {
            log.info("Chat service is forced to use mock provider");
            return mockChatService.answerQuestion(req);
        }

        if (!llmProperties.canTryRealChat()) {
            String reason = String.join("; ", llmProperties.getRealChatConfigIssues());
            if (llmProperties.isRealOnly()) {
                return buildErrorResponse("Real model config is invalid: " + reason);
            }
            log.info("Real chat model is not available, falling back to mock. provider={}, reason={}",
                    llmProperties.getProvider(), reason);
            return mockChatService.answerQuestion(req);
        }

        try {
            LlmProperties.ResolvedOpenAiOptions chatOptions = llmProperties.getOpenai().resolveChatOptions();
            log.info("Chat service is using real model first. provider={}, model={}",
                    llmProperties.getProvider(), chatOptions.getModel());
            ChatVO result = realChatService.answerQuestion(req);
            if (result == null || result.getAnswer() == null || result.getAnswer().trim().isEmpty()) {
                throw new IllegalStateException("Real model returned empty answer");
            }
            return result;
        } catch (Exception e) {
            if (llmProperties.isFallbackToMock()) {
                log.warn("Real chat model failed, falling back to mock. reason={}", e.getMessage(), e);
                return mockChatService.answerQuestion(req);
            }
            log.error("Real chat model failed and fallback is disabled. reason={}", e.getMessage(), e);
            return buildErrorResponse("Real model request failed: " + safeMessage(e));
        }
    }

    @Override
    public ChatVO streamAnswer(ChatReqDTO req, Consumer<String> tokenConsumer) {
        if (llmProperties.isMockOnly()) {
            return mockChatService.streamAnswer(req, tokenConsumer);
        }

        if (!llmProperties.canTryRealChat()) {
            String reason = String.join("; ", llmProperties.getRealChatConfigIssues());
            if (llmProperties.isRealOnly()) {
                return buildErrorResponse("Real model config is invalid: " + reason);
            }
            log.info("Real chat model is not available, falling back to mock stream. provider={}, reason={}",
                    llmProperties.getProvider(), reason);
            return mockChatService.streamAnswer(req, tokenConsumer);
        }

        AtomicBoolean emittedAnyToken = new AtomicBoolean(false);
        Consumer<String> guardedConsumer = token -> {
            if (token != null && !token.isEmpty()) {
                emittedAnyToken.set(true);
                if (tokenConsumer != null) {
                    tokenConsumer.accept(token);
                }
            }
        };

        try {
            return realChatService.streamAnswer(req, guardedConsumer);
        } catch (Exception e) {
            if (llmProperties.isFallbackToMock() && !emittedAnyToken.get()) {
                log.warn("Real chat stream failed before any token, falling back to mock. reason={}", e.getMessage(), e);
                return mockChatService.streamAnswer(req, tokenConsumer);
            }
            if (llmProperties.isFallbackToMock()) {
                log.warn("Real chat stream failed after partial output; mock fallback skipped. reason={}", e.getMessage(), e);
            } else {
                log.error("Real chat stream failed and fallback is disabled. reason={}", e.getMessage(), e);
            }
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    @Override
    public ChatStatusVO getStatus() {
        LlmProperties.ResolvedOpenAiOptions chatOptions = llmProperties.getOpenai().resolveChatOptions();
        ChatStatusVO vo = new ChatStatusVO();
        vo.setProvider(llmProperties.getProvider());
        vo.setConfigured(llmProperties.canTryRealChat());
        vo.setRealModelAvailable(llmProperties.canTryRealChat());
        vo.setFallbackToMock(llmProperties.isFallbackToMock());
        vo.setTimeoutSeconds(llmProperties.resolveReadTimeoutSeconds());
        vo.setModel(chatOptions.getModel());
        vo.setBaseUrl(chatOptions.getBaseUrl());

        if (llmProperties.isMockOnly()) {
            vo.setMessage("Current chat provider is mock (provider=mock).");
            return vo;
        }

        List<String> issues = llmProperties.getRealChatConfigIssues();
        if (!issues.isEmpty()) {
            vo.setMessage("Real model config is invalid: " + String.join("; ", issues));
            return vo;
        }

        List<String> warnings = llmProperties.getRealModelConfigWarnings();
        if (!warnings.isEmpty()) {
            vo.setMessage("Real model config is available, but with warnings: " + String.join("; ", warnings));
            return vo;
        }

        if (llmProperties.isFallbackToMock()) {
            vo.setMessage("Real model is preferred; fallback to mock is enabled.");
            return vo;
        }

        vo.setMessage("Real model is preferred; fallback to mock is disabled.");
        return vo;
    }

    private ChatVO buildErrorResponse(String message) {
        ChatVO vo = new ChatVO();
        vo.setAnswer(message);
        vo.setRelatedTips(List.of("Check OPENAI_API_KEY / OPENAI_BASE_URL / OPENAI_MODEL", "Retry later"));
        return vo;
    }

    private String safeMessage(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().trim().isEmpty()) {
            return "unknown error";
        }
        return e.getMessage();
    }
}
