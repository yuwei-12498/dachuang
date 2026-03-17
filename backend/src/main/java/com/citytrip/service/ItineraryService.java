package com.citytrip.service;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.dto.ReplaceReqDTO;
import com.citytrip.model.dto.ReplanReqDTO;
import com.citytrip.model.dto.ReplanRespDTO;
import com.citytrip.model.vo.ItineraryVO;

public interface ItineraryService {
    /**
     * 根据规则生成用户个性化行程
     */
    ItineraryVO generateUserItinerary(GenerateReqDTO req);

    /**
     * 替换行程中的某个点位
     */
    ItineraryVO replaceNode(ReplaceReqDTO req);

    /**
     * 智能梳理与重排时间轴（多次尝试返回不同排列）
     */
    ReplanRespDTO replan(ReplanReqDTO req);
}
