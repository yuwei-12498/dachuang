package com.citytrip.service.impl;

import com.citytrip.config.LlmProperties;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OpenAiLlmServiceImpl implements LlmService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmServiceImpl.class);

    private final RestTemplate llmRestTemplate;
    private final LlmProperties llmProperties;

    public OpenAiLlmServiceImpl(RestTemplate llmRestTemplate, LlmProperties llmProperties) {
        this.llmRestTemplate = llmRestTemplate;
        this.llmProperties = llmProperties;
    }

    @Override
    public String explainItinerary(GenerateReqDTO userReq, List<ItineraryNodeVO> nodes) {
        String prompt = "请基于用户需求和行程节点，生成一段80字以内的路线推荐理由。\n"
                + "用户需求：" + buildReqSummary(userReq) + "\n"
                + "行程节点：" + buildNodeSummary(nodes);
        return callText(prompt);
    }

    @Override
    public String generateTips(GenerateReqDTO userReq) {
        String prompt = "请根据用户需求，生成2到3条简短的出行提示，合并成一段100字以内中文。\n"
                + "用户需求：" + buildReqSummary(userReq);
        return callText(prompt);
    }

    @Override
    public String explainPoiChoice(GenerateReqDTO userReq, ItineraryNodeVO node) {
        String prompt = "请解释为什么把这个点位放进行程中，限制40字以内。\n"
                + "用户需求：" + buildReqSummary(userReq) + "\n"
                + "点位：" + (node == null ? "未提供" : safe(node.getPoiName()) + "/" + safe(node.getCategory()) + "/" + safe(node.getDistrict()));
        return callText(prompt);
    }

    private String buildReqSummary(GenerateReqDTO req) {
        if (req == null) {
            return "未提供";
        }
        return "天数=" + req.getTripDays()
                + ", 预算=" + safe(req.getBudgetLevel())
                + ", 主题=" + (req.getThemes() == null ? "[]" : req.getThemes())
                + ", 雨天=" + yesNo(req.getIsRainy())
                + ", 夜游=" + yesNo(req.getIsNight())
                + ", 步行强度=" + safe(req.getWalkingLevel())
                + ", 同行类型=" + safe(req.getCompanionType())
                + ", 时间=" + safe(req.getStartTime()) + "-" + safe(req.getEndTime());
    }

    private String buildNodeSummary(List<ItineraryNodeVO> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return "无节点";
        }
        return nodes.stream()
                .map(node -> safe(node.getPoiName()) + "/" + safe(node.getCategory()) + "/" + safe(node.getDistrict()))
                .collect(Collectors.joining(" -> "));
    }

    private String callText(String userPrompt) {
        if (!llmProperties.canTryReal()) {
            throw new IllegalStateException("OpenAI real model is not configured");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(llmProperties.getOpenai().getApiKey().trim());

            OpenAiChatServiceImpl.OpenAiChatRequest request = new OpenAiChatServiceImpl.OpenAiChatRequest();
            request.setModel(llmProperties.getOpenai().getModel());
            request.setTemperature(llmProperties.getOpenai().getTemperature());
            request.setMessages(Arrays.asList(
                    new OpenAiChatServiceImpl.OpenAiMessage("system", "你是旅游路线文案助手，请严格输出中文简洁结果，不要加多余前缀。"),
                    new OpenAiChatServiceImpl.OpenAiMessage("user", userPrompt)
            ));

            HttpEntity<OpenAiChatServiceImpl.OpenAiChatRequest> entity = new HttpEntity<>(request, headers);
            String url = normalizeBaseUrl(llmProperties.getOpenai().getBaseUrl()) + "/chat/completions";
            ResponseEntity<OpenAiChatServiceImpl.OpenAiChatResponse> response = llmRestTemplate.exchange(
                    url, HttpMethod.POST, entity, OpenAiChatServiceImpl.OpenAiChatResponse.class
            );

            OpenAiChatServiceImpl.OpenAiChatResponse body = response.getBody();
            if (body == null || body.getChoices() == null || body.getChoices().isEmpty()) {
                throw new IllegalStateException("OpenAI response choices is empty");
            }

            OpenAiChatServiceImpl.OpenAiChatResponse.Choice choice = body.getChoices().get(0);
            String content = choice.getMessage() == null ? null : choice.getMessage().getContent();
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalStateException("OpenAI message content is empty");
            }
            return content.trim();
        } catch (HttpStatusCodeException e) {
            log.warn("真实大模型文案调用失败，HTTP状态码={}，响应体={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new IllegalStateException(describeHttpFailure(e), e);
        } catch (ResourceAccessException e) {
            log.warn("真实大模型文案调用超时或网络异常: {}", e.getMessage());
            throw new IllegalStateException("OpenAI request timeout or network error", e);
        } catch (Exception e) {
            log.warn("真实大模型文案调用异常: {}", e.getMessage(), e);
            throw new IllegalStateException("OpenAI request failed: " + e.getMessage(), e);
        }
    }

    private String describeHttpFailure(HttpStatusCodeException e) {
        int code = e.getStatusCode().value();
        String body = e.getResponseBodyAsString();
        if (code == 429) {
            return "OpenAI request limited or quota exhausted";
        }
        if (code >= 500) {
            return "OpenAI service unavailable";
        }
        if (body != null) {
            String lower = body.toLowerCase();
            if (lower.contains("insufficient_quota") || lower.contains("quota") || lower.contains("余额")) {
                return "OpenAI quota exhausted";
            }
        }
        return "OpenAI http error: " + code;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "https://api.openai.com";
        }
        String value = baseUrl.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String yesNo(Boolean value) {
        return Boolean.TRUE.equals(value) ? "是" : "否";
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "未提供" : value.trim();
    }
}
