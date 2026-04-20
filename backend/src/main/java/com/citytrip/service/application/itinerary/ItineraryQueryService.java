package com.citytrip.service.application.itinerary;

import com.citytrip.assembler.ItinerarySummaryAssembler;
import com.citytrip.common.BadRequestException;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.SavedItinerary;
import com.citytrip.model.vo.ItinerarySummaryVO;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.persistence.itinerary.SavedItineraryCodec;
import com.citytrip.service.persistence.itinerary.SavedItineraryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItineraryQueryService {

    private final SavedItineraryRepository savedItineraryRepository;
    private final SavedItineraryCodec savedItineraryCodec;
    private final ItinerarySummaryAssembler itinerarySummaryAssembler;

    public ItineraryQueryService(SavedItineraryRepository savedItineraryRepository,
                                 SavedItineraryCodec savedItineraryCodec,
                                 ItinerarySummaryAssembler itinerarySummaryAssembler) {
        this.savedItineraryRepository = savedItineraryRepository;
        this.savedItineraryCodec = savedItineraryCodec;
        this.itinerarySummaryAssembler = itinerarySummaryAssembler;
    }

    public ItineraryVO getLatest(Long userId) {
        SavedItinerary entity = savedItineraryRepository.findLatestOwned(userId);
        return entity == null ? null : savedItineraryCodec.deserialize(entity);
    }

    public ItineraryVO get(Long userId, Long itineraryId) {
        return savedItineraryCodec.deserialize(savedItineraryRepository.requireOwned(userId, itineraryId));
    }

    public List<ItinerarySummaryVO> list(Long userId, boolean favoriteOnly, Integer limit) {
        List<SavedItinerary> entities = savedItineraryRepository.listOwned(userId, favoriteOnly, limit);
        List<ItinerarySummaryVO> result = new ArrayList<>(entities.size());
        for (SavedItinerary entity : entities) {
            result.add(toSummary(entity));
        }
        return result;
    }

    public List<ItinerarySummaryVO> listProfile(Long userId, String type, Integer limit) {
        String normalizedType = type == null ? "generated" : type.trim().toLowerCase();
        return switch (normalizedType) {
            case "generated" -> list(userId, false, limit);
            case "saved", "favorite", "favorited" -> list(userId, true, limit);
            default -> throw new BadRequestException("不支持的行程类型，只允许 generated 或 saved");
        };
    }

    private ItinerarySummaryVO toSummary(SavedItinerary entity) {
        try {
            GenerateReqDTO req = savedItineraryCodec.readRequest(entity);
            ItineraryVO itinerary = savedItineraryCodec.readItinerary(entity);
            return itinerarySummaryAssembler.toSummary(entity, req, itinerary);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to deserialize itinerary summary", ex);
        }
    }
}
