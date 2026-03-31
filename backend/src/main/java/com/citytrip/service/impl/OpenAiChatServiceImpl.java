package com.citytrip.service.impl;

import com.citytrip.config.LlmProperties;
import com.citytrip.model.dto.ChatReqDTO;
import com.citytrip.model.vo.ChatStatusVO;
import com.citytrip.model.vo.ChatVO;
import com.citytrip.service.ChatService;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class OpenAiChatServiceImpl implements ChatService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiChatServiceImpl.class);

    private final RestTemplate llmRestTemplate;
    private final LlmProperties llmProperties;

    public OpenAiChatServiceImpl(RestTemplate llmRestTemplate, LlmProperties llmProperties) {
        this.llmRestTemplate = llmRestTemplate;
        this.llmProperties = llmProperties;
    }

    @Override
    public ChatVO answerQuestion(ChatReqDTO req) {
        if (!llmProperties.canTryReal()) {
            throw new IllegalStateException("OpenAI real model is not configured");
        }

        String answer = callChatCompletion(buildSystemPrompt(), buildUserPrompt(req));
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI returned empty chat answer");
        }

        ChatVO vo = new ChatVO();
        vo.setAnswer(answer.trim());
        vo.setRelatedTips(buildRelatedTips(req));
        return vo;
    }

    @Override
    public ChatStatusVO getStatus() {
        ChatStatusVO vo = new ChatStatusVO();
        vo.setProvider("real");
        vo.setConfigured(llmProperties.canTryReal());
        vo.setRealModelAvailable(llmProperties.canTryReal());
        vo.setFallbackToMock(llmProperties.isFallbackToMock());
        vo.setTimeoutSeconds(llmProperties.getTimeoutSeconds());
        vo.setModel(llmProperties.getOpenai().getModel());
        vo.setBaseUrl(llmProperties.getOpenai().getBaseUrl());
        vo.setMessage(llmProperties.canTryReal()
                ? "真实大模型配置已就绪。"
                : "真实大模型配置不完整，请检查 API Key、Base URL 和模型开关。");
        return vo;
    }

    private String buildSystemPrompt() {
        return "你是“行城有数”的成都旅游智能助手。回答要自然、简洁、具体，优先结合用户上下文给出建议。"
                + "如果信息不确定，不要编造门票价格和营业时间。"
                + "最后尽量给出适合继续追问的方向。";
    }

    private String buildUserPrompt(ChatReqDTO req) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户问题：").append(safe(req == null ? null : req.getQuestion())).append("\n");
        if (req != null && req.getContext() != null) {
            ChatReqDTO.ChatContext ctx = req.getContext();
            sb.append("页面类型：").append(safe(ctx.getPageType())).append("\n");
            sb.append("偏好标签：").append(ctx.getPreferences() == null ? "[]" : ctx.getPreferences()).append("\n");
            sb.append("是否雨天：").append(Boolean.TRUE.equals(ctx.getRainy()) ? "是" : "否").append("\n");
            sb.append("是否夜游：").append(Boolean.TRUE.equals(ctx.getNightMode()) ? "是" : "否").append("\n");
            sb.append("同行类型：").append(safe(ctx.getCompanionType())).append("\n");
        }
        sb.append("请直接回答问题，并尽量给出 2 条适合继续追问的简短建议。");
        return sb.toString();
    }

    private List<String> buildRelatedTips(ChatReqDTO req) {
        String question = req == null ? "" : safe(req.getQuestion());
        List<String> tips = new ArrayList<>();
        if (question.contains("拍照") || question.contains("机位")) {
            tips.add("成都哪些地方适合傍晚拍照？");
            tips.add("这些地点怎么安排成半日路线？");
        } else if (question.contains("雨")) {
            tips.add("雨天成都有哪些室内景点？");
            tips.add("雨天路线怎么减少步行？");
        } else if (question.contains("亲子") || question.contains("孩子")) {
            tips.add("亲子行程适合安排哪些博物馆？");
            tips.add("带孩子出行怎么控制步行强度？");
        } else {
            tips.add("可以帮我按偏好生成一条路线吗？");
            tips.add("这条路线里哪个点最值得停留？");
        }
        return tips;
    }

    private String callChatCompletion(String systemPrompt, String userPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(llmProperties.getOpenai().getApiKey().trim());

            OpenAiChatRequest request = new OpenAiChatRequest();
            request.setModel(llmProperties.getOpenai().getModel());
            request.setTemperature(llmProperties.getOpenai().getTemperature());
            request.setMessages(Arrays.asList(
                    new OpenAiMessage("system", systemPrompt),
                    new OpenAiMessage("user", userPrompt)
            ));

            HttpEntity<OpenAiChatRequest> entity = new HttpEntity<>(request, headers);
            String url = normalizeBaseUrl(llmProperties.getOpenai().getBaseUrl()) + "/chat/completions";
            ResponseEntity<OpenAiChatResponse> response = llmRestTemplate.exchange(
                    url, HttpMethod.POST, entity, OpenAiChatResponse.class
            );

            OpenAiChatResponse body = response.getBody();
            if (body == null || body.getChoices() == null || body.getChoices().isEmpty()) {
                throw new IllegalStateException("OpenAI response choices is empty");
            }

            OpenAiChatResponse.Choice choice = body.getChoices().get(0);
            if (choice.getMessage() == null || choice.getMessage().getContent() == null
                    || choice.getMessage().getContent().trim().isEmpty()) {
                throw new IllegalStateException("OpenAI message content is empty");
            }
            return choice.getMessage().getContent();
        } catch (HttpStatusCodeException e) {
            log.warn("Real model request failed with status={}, body={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new IllegalStateException(describeHttpFailure(e), e);
        } catch (ResourceAccessException e) {
            log.warn("Real model request timeout or network failure: {}", e.getMessage());
            throw new IllegalStateException("OpenAI request timeout or network error", e);
        } catch (Exception e) {
            log.warn("Real model request failed: {}", e.getMessage(), e);
            throw new IllegalStateException("OpenAI request failed: " + e.getMessage(), e);
        }
    }

    private String describeHttpFailure(HttpStatusCodeException e) {
        int code = e.getStatusCode().value();
        String body = e.getResponseBodyAsString();
        if (code == 429) {
            return "OpenAI request limited or quota exhausted";
        }
        if ((code == 400 || code == 404) && body != null) {
            String lower = body.toLowerCase();
            if (lower.contains("model")) {
                return "OpenAI model is unavailable or unsupported";
            }
            if (lower.contains("api key") || lower.contains("unauthorized") || lower.contains("invalid")) {
                return "OpenAI credentials are invalid";
            }
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

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "未提供" : value.trim();
    }

    public static class OpenAiChatRequest {
        private String model;
        private List<OpenAiMessage> messages;
        private Double temperature;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public List<OpenAiMessage> getMessages() {
            return messages;
        }

        public void setMessages(List<OpenAiMessage> messages) {
            this.messages = messages;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }
    }

    public static class OpenAiMessage {
        private String role;
        private String content;

        public OpenAiMessage() {
        }

        public OpenAiMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class OpenAiChatResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() {
            return choices;
        }

        public void setChoices(List<Choice> choices) {
            this.choices = choices;
        }

        public static class Choice {
            private OpenAiMessage message;

            public OpenAiMessage getMessage() {
                return message;
            }

            public void setMessage(OpenAiMessage message) {
                this.message = message;
            }
        }
    }
}
