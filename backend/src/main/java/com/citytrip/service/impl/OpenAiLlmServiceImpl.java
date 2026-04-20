package com.citytrip.service.impl;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryOptionVO;
import com.citytrip.service.LlmService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OpenAiLlmServiceImpl implements LlmService {

    private final RealLlmGatewayService realLlmGatewayService;

    public OpenAiLlmServiceImpl(RealLlmGatewayService realLlmGatewayService) {
        this.realLlmGatewayService = realLlmGatewayService;
    }

    @Override
    public String explainItinerary(GenerateReqDTO userReq, List<ItineraryNodeVO> nodes) {
        return realLlmGatewayService.explainItinerary(userReq, nodes);
    }

    @Override
    public String generateTips(GenerateReqDTO userReq) {
        return realLlmGatewayService.generateTips(userReq);
    }

    @Override
    public String explainOptionRecommendation(GenerateReqDTO userReq, ItineraryOptionVO option) {
        return realLlmGatewayService.explainOptionRecommendation(userReq, option);
    }

    @Override
    public String explainPoiChoice(GenerateReqDTO userReq, ItineraryNodeVO node) {
        return realLlmGatewayService.explainPoiChoice(userReq, node);
    }
}
