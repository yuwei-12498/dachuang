package com.citytrip.service;

import com.citytrip.model.dto.FavoriteReqDTO;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.dto.ReplaceReqDTO;
import com.citytrip.model.dto.ReplanReqDTO;
import com.citytrip.model.dto.ReplanRespDTO;
import com.citytrip.model.vo.ItinerarySummaryVO;
import com.citytrip.model.vo.ItineraryVO;

import java.util.List;

public interface ItineraryService {
    ItineraryVO generateUserItinerary(Long userId, GenerateReqDTO req);

    ItineraryVO replaceNode(Long userId, Long itineraryId, Long targetPoiId, ReplaceReqDTO req);

    ReplanRespDTO replan(Long userId, Long itineraryId, ReplanReqDTO req);

    ItineraryVO getLatestItinerary(Long userId);

    ItineraryVO getItinerary(Long userId, Long itineraryId);

    List<ItinerarySummaryVO> listItineraries(Long userId, boolean favoriteOnly, Integer limit);

    ItineraryVO favoriteItinerary(Long userId, Long itineraryId, FavoriteReqDTO req);

    void unfavoriteItinerary(Long userId, Long itineraryId);
}
