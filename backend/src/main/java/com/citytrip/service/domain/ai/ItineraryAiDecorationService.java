package com.citytrip.service.domain.ai;

import com.citytrip.assembler.ItineraryComparisonAssembler;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryOptionVO;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.LlmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItineraryAiDecorationService {

    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final ItineraryComparisonAssembler itineraryComparisonAssembler;

    public ItineraryAiDecorationService(LlmService llmService,
                                        ObjectMapper objectMapper,
                                        ItineraryComparisonAssembler itineraryComparisonAssembler) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.itineraryComparisonAssembler = itineraryComparisonAssembler;
    }

    public ItineraryVO decorateWithLlm(ItineraryVO baseItinerary, GenerateReqDTO req) {
        ItineraryVO itinerary = copyItinerary(baseItinerary);
        if (itinerary == null) {
            return null;
        }
        try {
            if (itinerary.getOptions() != null) {
                for (ItineraryOptionVO option : itinerary.getOptions()) {
                    if (option == null) {
                        continue;
                    }
                    String optionReason = llmService.explainOptionRecommendation(req, option);
                    if (StringUtils.hasText(optionReason)) {
                        option.setRecommendReason(optionReason.trim());
                    }
                    if (option.getNodes() != null) {
                        for (ItineraryNodeVO node : option.getNodes()) {
                            if (node == null) {
                                continue;
                            }
                            String nodeReason = llmService.explainPoiChoice(req, node);
                            if (StringUtils.hasText(nodeReason)) {
                                node.setSysReason(nodeReason.trim());
                            }
                        }
                    }
                }
            }
            String itineraryReason = llmService.explainItinerary(req, itinerary.getNodes());
            if (StringUtils.hasText(itineraryReason)) {
                itinerary.setRecommendReason(itineraryReason.trim());
            }
        } catch (RuntimeException ignore) {
            // 上层编排器会根据超时或异常决定是否整体回退到 rule-based 结果
        }
        applyWarmTips(itinerary, req);
        return itinerary;
    }

    public void applyWarmTips(ItineraryVO itinerary, GenerateReqDTO req) {
        if (itinerary == null) {
            return;
        }

        String comparisonTips = itineraryComparisonAssembler.buildComparisonTips(
                req,
                itinerary.getOptions(),
                itinerary.getSelectedOptionKey()
        );
        String warmTips = buildWarmTips(req);
        if (StringUtils.hasText(comparisonTips) && StringUtils.hasText(warmTips)) {
            itinerary.setTips(comparisonTips + " AI补充：" + warmTips);
            return;
        }
        itinerary.setTips(StringUtils.hasText(warmTips) ? warmTips : comparisonTips);
    }

    private String buildWarmTips(GenerateReqDTO req) {
        String generatedTips = null;
        try {
            generatedTips = llmService.generateTips(req);
        } catch (RuntimeException ex) {
            generatedTips = null;
        }

        List<String> tips = new ArrayList<>();
        if (StringUtils.hasText(generatedTips)) {
            tips.add(generatedTips.trim());
        }

        List<String> fallbackTips = buildFallbackWarmTips(req);
        if (tips.isEmpty()) {
            tips.addAll(fallbackTips);
        } else {
            final String finalGeneratedTips = generatedTips;
            fallbackTips.stream()
                    .filter(StringUtils::hasText)
                    .filter(tip -> !finalGeneratedTips.contains(stripTipKeyword(tip)))
                    .limit(2)
                    .forEach(tips::add);
        }
        return String.join(" ", tips);
    }

    private List<String> buildFallbackWarmTips(GenerateReqDTO req) {
        List<String> tips = new ArrayList<>();
        if (req == null) {
            tips.add("出发前记得带好饮水、充电设备和舒适的步行装备。");
            return tips;
        }

        if (matchesOutdoorTheme(req)
                || "高".equals(req.getWalkingLevel())
                || "high".equalsIgnoreCase(req.getWalkingLevel())) {
            tips.add("这条路线可能包含较多步行或爬坡，建议穿防滑鞋，并预留补水时间。");
        }
        if (Boolean.TRUE.equals(req.getIsRainy())) {
            tips.add("雨天出行请备好雨具，并注意台阶、石板路和临水区域的防滑。");
        }
        if (Boolean.TRUE.equals(req.getIsNight())) {
            tips.add("夜间返程前建议提前确认交通方式，并适当增加保暖衣物。");
        }
        if ("亲子".equals(req.getCompanionType())) {
            tips.add("亲子出行建议适当增加休息频次，并提前准备零食和备用衣物。");
        }
        if ("长者".equals(req.getCompanionType()) || "老人".equals(req.getCompanionType())) {
            tips.add("若有长者同行，建议优先控制步行强度，并尽量选择可随时休息的点位。");
        }
        if (tips.isEmpty()) {
            tips.add("出发前记得带好饮水、充电设备和舒适的步行装备。");
        }
        return tips;
    }

    private boolean matchesOutdoorTheme(GenerateReqDTO req) {
        if (req == null || req.getThemes() == null || req.getThemes().isEmpty()) {
            return false;
        }
        return req.getThemes().stream()
                .filter(StringUtils::hasText)
                .anyMatch(theme -> theme.contains("山") || theme.contains("徒步") || theme.contains("户外") || theme.contains("自然"));
    }

    private String stripTipKeyword(String tip) {
        if (!StringUtils.hasText(tip)) {
            return "";
        }
        if (tip.contains("登山") || tip.contains("徒步") || tip.contains("户外")) {
            return "户外";
        }
        if (tip.contains("雨") || tip.contains("防滑") || tip.contains("雨具")) {
            return "雨天";
        }
        if (tip.contains("夜") || tip.contains("返程") || tip.contains("保暖")) {
            return "夜间";
        }
        if (tip.contains("亲子")) {
            return "亲子";
        }
        if (tip.contains("长者") || tip.contains("老人")) {
            return "长者";
        }
        return tip;
    }

    private ItineraryVO copyItinerary(ItineraryVO source) {
        if (source == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(source, ItineraryVO.class);
        } catch (IllegalArgumentException ex) {
            return source;
        }
    }
}
