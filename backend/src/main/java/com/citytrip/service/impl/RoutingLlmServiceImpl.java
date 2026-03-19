package com.citytrip.service.impl;

import com.citytrip.config.LlmProperties;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Primary
@Service
public class RoutingLlmServiceImpl implements LlmService {
    private static final Logger log = LoggerFactory.getLogger(RoutingLlmServiceImpl.class);

    private final OpenAiLlmServiceImpl openAiLlmService;
    private final MockLlmServiceImpl mockLlmService;
    private final LlmProperties llmProperties;

    public RoutingLlmServiceImpl(OpenAiLlmServiceImpl openAiLlmService,
                                 MockLlmServiceImpl mockLlmService,
                                 LlmProperties llmProperties) {
        this.openAiLlmService = openAiLlmService;
        this.mockLlmService = mockLlmService;
        this.llmProperties = llmProperties;
    }

    @Override
    public String explainItinerary(GenerateReqDTO userReq, List<ItineraryNodeVO> nodes) {
        return routeCall(
                () -> openAiLlmService.explainItinerary(userReq, nodes),
                () -> mockLlmService.explainItinerary(userReq, nodes),
                "explainItinerary"
        );
    }

    @Override
    public String generateTips(GenerateReqDTO userReq) {
        return routeCall(
                () -> openAiLlmService.generateTips(userReq),
                () -> mockLlmService.generateTips(userReq),
                "generateTips"
        );
    }

    @Override
    public String explainPoiChoice(GenerateReqDTO userReq, ItineraryNodeVO node) {
        return routeCall(
                () -> openAiLlmService.explainPoiChoice(userReq, node),
                () -> mockLlmService.explainPoiChoice(userReq, node),
                "explainPoiChoice"
        );
    }

    private String routeCall(TextSupplier realSupplier, TextSupplier mockSupplier, String scene) {
        if (llmProperties.isMockOnly()) {
            log.info("行程文案服务当前使用 Mock 模型，provider=mock，scene={}", scene);
            return mockSupplier.get();
        }

        boolean canTryReal = llmProperties.canTryReal();
        if (!canTryReal) {
            if (llmProperties.isRealOnly()) {
                return handleFailureOrFallback(mockSupplier, scene, "API Key 未配置或真实模型未启用", null);
            }
            log.info("行程文案服务当前使用 Mock 模型，provider={}，scene={}，原因=未检测到可用真实模型配置",
                    llmProperties.getProvider(), scene);
            return mockSupplier.get();
        }

        try {
            log.info("行程文案服务当前优先使用真实模型，provider={}，model={}，scene={}",
                    llmProperties.getProvider(), llmProperties.getOpenai().getModel(), scene);
            String result = realSupplier.get();
            if (result == null || result.trim().isEmpty()) {
                throw new IllegalStateException("真实模型返回空结果");
            }
            return result;
        } catch (Exception e) {
            return handleFailureOrFallback(mockSupplier, scene, e.getMessage(), e);
        }
    }

    private String handleFailureOrFallback(TextSupplier mockSupplier, String scene, String reason, Exception e) {
        if (llmProperties.isFallbackToMock()) {
            log.warn("行程文案服务发生降级，改用 Mock。scene={}，降级原因={}", scene, reason, e);
            return mockSupplier.get();
        }
        log.error("行程文案服务真实模型调用失败，且已禁用降级。scene={}，失败原因={}", scene, reason, e);
        throw new RuntimeException("真实大模型调用失败，且未启用 Mock 降级: " + reason, e);
    }

    @FunctionalInterface
    private interface TextSupplier {
        String get();
    }
}
