package com.citytrip.service;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.vo.ItineraryNodeVO;

import java.util.List;

public interface LlmService {
    /**
     * 生成总体路线推荐理由
     */
    String explainItinerary(GenerateReqDTO userReq, List<ItineraryNodeVO> nodes);

    /**
     * 生成对应的友情提示
     */
    String generateTips(GenerateReqDTO userReq);

    /**
     * 解释某一个具体点位为什么入选
     */
    String explainPoiChoice(GenerateReqDTO userReq, ItineraryNodeVO node);
}
