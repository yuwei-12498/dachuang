package com.citytrip.service.impl;

import com.citytrip.config.LlmProperties;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.vo.DepartureLegEstimateVO;
import com.citytrip.model.vo.ItineraryRouteDecorationVO;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryOptionVO;
import com.citytrip.model.vo.RouteNodeDecorationVO;
import com.citytrip.model.vo.SegmentTransportAnalysisVO;
import com.citytrip.model.vo.SmartFillVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class RealLlmGatewayService {
    private final OpenAiGatewayClient openAiGatewayClient;
    private final LlmProperties llmProperties;
    private final SafePromptBuilder safePromptBuilder;
    private final ObjectMapper objectMapper;

    public RealLlmGatewayService(OpenAiGatewayClient openAiGatewayClient,
                                 LlmProperties llmProperties,
                                 SafePromptBuilder safePromptBuilder,
                                 ObjectMapper objectMapper) {
        this.openAiGatewayClient = openAiGatewayClient;
        this.llmProperties = llmProperties;
        this.safePromptBuilder = safePromptBuilder;
        this.objectMapper = objectMapper;
    }

    public String explainItinerary(GenerateReqDTO userReq, List<ItineraryNodeVO> nodes) {
        return callText(safePromptBuilder.buildExplainItineraryPrompt(userReq, nodes));
    }

    public String generateTips(GenerateReqDTO userReq) {
        return callText(safePromptBuilder.buildGenerateTipsPrompt(userReq));
    }

    public String generatePoiWarmTips(GenerateReqDTO userReq, ItineraryNodeVO node) {
        return callText(safePromptBuilder.buildGeneratePoiWarmTipsPrompt(userReq, node));
    }

    public String generateRouteWarmTip(GenerateReqDTO userReq, List<ItineraryNodeVO> nodes) {
        return callText(safePromptBuilder.buildGenerateRouteWarmTipPrompt(userReq, nodes));
    }

    public String explainOptionRecommendation(GenerateReqDTO userReq, ItineraryOptionVO option) {
        return callText(safePromptBuilder.buildExplainOptionRecommendationPrompt(userReq, option));
    }

    public String explainPoiChoice(GenerateReqDTO userReq, ItineraryNodeVO node) {
        return callText(safePromptBuilder.buildExplainPoiChoicePrompt(userReq, node));
    }

    public SmartFillVO parseSmartFill(String text, List<String> poiNameHints) {
        String raw = callText(
                safePromptBuilder.buildSmartFillPrompt(text, poiNameHints),
                safePromptBuilder.buildSmartFillSystemPrompt()
        );
        return parseSmartFillResponse(raw);
    }

    public DepartureLegEstimateVO estimateDepartureLeg(GenerateReqDTO userReq, ItineraryNodeVO firstNode) {
        String raw = callText(
                safePromptBuilder.buildDepartureLegEstimatePrompt(userReq, firstNode),
                safePromptBuilder.buildSmartFillSystemPrompt()
        );
        return parseDepartureLegEstimateResponse(raw);
    }

    public SegmentTransportAnalysisVO analyzeSegmentTransport(GenerateReqDTO userReq, ItineraryNodeVO fromNode, ItineraryNodeVO toNode) {
        String raw = callText(
                safePromptBuilder.buildSegmentTransportAnalysisPrompt(userReq, fromNode, toNode),
                safePromptBuilder.buildSmartFillSystemPrompt()
        );
        return parseSegmentTransportAnalysisResponse(raw);
    }

    public ItineraryRouteDecorationVO decorateRouteExperience(GenerateReqDTO userReq, List<ItineraryNodeVO> nodes) {
        String raw = callText(
                safePromptBuilder.buildRouteExperienceDecorationPrompt(userReq, nodes),
                safePromptBuilder.buildSmartFillSystemPrompt()
        );
        return parseRouteExperienceDecorationResponse(raw);
    }

    private String callText(String userPrompt) {
        return callText(userPrompt, safePromptBuilder.buildItinerarySystemPrompt());
    }

    private String callText(String userPrompt, String systemPrompt) {
        if (!llmProperties.canTryRealText()) {
            throw new IllegalStateException("OpenAI real text model is not configured");
        }
        if (openAiGatewayClient == null) {
            throw new IllegalStateException("OpenAI gateway is not configured");
        }

        LlmProperties.ResolvedOpenAiOptions textOptions = llmProperties.getOpenai().resolveTextOptions();
        return openAiGatewayClient.request(
                textOptions,
                llmProperties.getOpenai().getApiKey(),
                List.of(
                        new OpenAiGatewayClient.OpenAiMessage("system", systemPrompt),
                        new OpenAiGatewayClient.OpenAiMessage("user", userPrompt)
                )
        );
    }

    private SmartFillVO parseSmartFillResponse(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalStateException("智能填写解析失败：模型返回为空");
        }
        String json = extractJsonObject(raw);
        if (!StringUtils.hasText(json)) {
            throw new IllegalStateException("智能填写解析失败：未找到 JSON 结构");
        }
        try {
            SmartFillVO parsed = objectMapper.readValue(json, SmartFillVO.class);
            if (parsed.getThemes() == null) {
                parsed.setThemes(List.of());
            }
            if (parsed.getMustVisitPoiNames() == null) {
                parsed.setMustVisitPoiNames(List.of());
            }
            if (parsed.getSummary() == null) {
                parsed.setSummary(List.of());
            }
            return parsed;
        } catch (Exception ex) {
            throw new IllegalStateException("智能填写解析失败：模型输出不是可解析 JSON", ex);
        }
    }

    private DepartureLegEstimateVO parseDepartureLegEstimateResponse(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalStateException("首段通勤估算失败：模型返回为空");
        }
        String json = extractJsonObject(raw);
        if (!StringUtils.hasText(json)) {
            throw new IllegalStateException("首段通勤估算失败：未提取到 JSON 结构");
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            DepartureLegEstimateVO estimate = new DepartureLegEstimateVO();

            String transportMode = root.path("transportMode").asText(null);
            if (StringUtils.hasText(transportMode)) {
                estimate.setTransportMode(transportMode.trim());
            }

            if (root.hasNonNull("estimatedMinutes")) {
                int minutes = root.path("estimatedMinutes").asInt();
                if (minutes > 0 && minutes <= 240) {
                    estimate.setEstimatedMinutes(minutes);
                }
            }

            if (root.hasNonNull("estimatedDistanceKm")) {
                BigDecimal distance = root.path("estimatedDistanceKm").decimalValue();
                if (distance != null
                        && distance.compareTo(BigDecimal.valueOf(0.1D)) >= 0
                        && distance.compareTo(BigDecimal.valueOf(80D)) <= 0) {
                    estimate.setEstimatedDistanceKm(distance.setScale(1, RoundingMode.HALF_UP));
                }
            }
            return estimate;
        } catch (Exception ex) {
            throw new IllegalStateException("首段通勤估算失败：模型输出不是可解析 JSON", ex);
        }
    }

    SegmentTransportAnalysisVO parseSegmentTransportAnalysisResponse(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalStateException("鍑鸿娈典氦閫氬垎鏋愬け璐ワ細妯″瀷杩斿洖涓虹┖");
        }
        String json = extractJsonObject(raw);
        if (!StringUtils.hasText(json)) {
            throw new IllegalStateException("鍑鸿娈典氦閫氬垎鏋愬け璐ワ細鏈彁鍙栧埌 JSON 缁撴瀯");
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            SegmentTransportAnalysisVO analysis = new SegmentTransportAnalysisVO();
            String transportMode = root.path("transportMode").asText(null);
            if (StringUtils.hasText(transportMode)) {
                analysis.setTransportMode(transportMode.trim());
            }
            String narrative = root.path("narrative").asText(null);
            if (StringUtils.hasText(narrative)) {
                analysis.setNarrative(narrative.replaceAll("[\\r\\n]+", " ").trim());
            }
            return analysis;
        } catch (Exception ex) {
            throw new IllegalStateException("鍑鸿娈典氦閫氬垎鏋愬け璐ワ細妯″瀷杈撳嚭涓嶆槸鍙В鏋?JSON", ex);
        }
    }

    ItineraryRouteDecorationVO parseRouteExperienceDecorationResponse(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalStateException("route experience decoration failed: empty model response");
        }
        String json = extractJsonObject(raw);
        if (!StringUtils.hasText(json)) {
            throw new IllegalStateException("route experience decoration failed: missing json payload");
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            ItineraryRouteDecorationVO decoration = new ItineraryRouteDecorationVO();
            String routeWarmTip = root.path("routeWarmTip").asText(null);
            if (StringUtils.hasText(routeWarmTip)) {
                decoration.setRouteWarmTip(routeWarmTip.replaceAll("[\\r\\n]+", " ").trim());
            }

            List<RouteNodeDecorationVO> decoratedNodes = new ArrayList<>();
            JsonNode nodeArray = root.path("nodes");
            if (nodeArray.isArray()) {
                for (JsonNode node : nodeArray) {
                    if (node == null || node.isNull() || node.isMissingNode()) {
                        continue;
                    }
                    RouteNodeDecorationVO item = new RouteNodeDecorationVO();
                    if (node.hasNonNull("index")) {
                        item.setIndex(node.path("index").asInt());
                    }
                    String transportMode = node.path("transportMode").asText(null);
                    if (StringUtils.hasText(transportMode)) {
                        item.setTransportMode(transportMode.trim());
                    }
                    String narrative = node.path("narrative").asText(null);
                    if (StringUtils.hasText(narrative)) {
                        item.setNarrative(narrative.replaceAll("[\\r\\n]+", " ").trim());
                    }
                    JsonNode warmTips = node.path("warmTips");
                    if (warmTips.isArray()) {
                        List<String> tips = new ArrayList<>();
                        for (JsonNode tip : warmTips) {
                            String text = tip == null ? null : tip.asText(null);
                            if (StringUtils.hasText(text)) {
                                tips.add(text.replaceAll("[\\r\\n]+", " ").trim());
                            }
                        }
                        item.setWarmTips(tips);
                    }
                    decoratedNodes.add(item);
                }
            }
            decoration.setNodes(decoratedNodes);
            return decoration;
        } catch (Exception ex) {
            throw new IllegalStateException("route experience decoration failed: model output is not valid json", ex);
        }
    }

    private String extractJsonObject(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineBreak >= 0 && lastFence > firstLineBreak) {
                trimmed = trimmed.substring(firstLineBreak + 1, lastFence).trim();
            }
        }
        int left = trimmed.indexOf('{');
        int right = trimmed.lastIndexOf('}');
        if (left < 0 || right <= left) {
            return null;
        }
        return trimmed.substring(left, right + 1);
    }
}
