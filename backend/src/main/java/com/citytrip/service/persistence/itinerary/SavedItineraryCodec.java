package com.citytrip.service.persistence.itinerary;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.SavedItinerary;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryOptionVO;
import com.citytrip.model.vo.ItineraryVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
public class SavedItineraryCodec {

    private final ObjectMapper objectMapper;

    public SavedItineraryCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ItineraryVO deserialize(SavedItinerary entity) {
        try {
            ItineraryVO itinerary = readItinerary(entity);
            GenerateReqDTO req = readRequest(entity);
            itinerary.setId(entity.getId());
            itinerary.setCustomTitle(entity.getCustomTitle());
            itinerary.setShareNote(entity.getShareNote());
            itinerary.setOriginalReq(req);
            itinerary.setFavorited(entity.getFavorited() != null && entity.getFavorited() == 1);
            itinerary.setFavoriteTime(entity.getFavoriteTime());
            itinerary.setIsPublic(entity.getIsPublic() != null && entity.getIsPublic() == 1);
            itinerary.setLastSavedAt(entity.getUpdateTime());
            return itinerary;
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("反序列化行程失败", ex);
        }
    }

    public GenerateReqDTO readRequest(SavedItinerary entity) throws JsonProcessingException {
        return objectMapper.readValue(entity.getRequestJson(), GenerateReqDTO.class);
    }

    public ItineraryVO readItinerary(SavedItinerary entity) throws JsonProcessingException {
        return objectMapper.readValue(entity.getItineraryJson(), ItineraryVO.class);
    }

    public String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("序列化行程失败", ex);
        }
    }

    public String signature(ItineraryVO itinerary) {
        if (itinerary.getNodes() == null || itinerary.getNodes().isEmpty()) {
            return "";
        }
        return itinerary.getNodes().stream()
                .map(ItineraryNodeVO::getPoiId)
                .map(String::valueOf)
                .reduce((left, right) -> left + "-" + right)
                .orElse("");
    }

    public ItineraryVO selectOptionInPlace(ItineraryVO itinerary, String selectedOptionKey) {
        if (itinerary == null) {
            return null;
        }
        if (itinerary.getOptions() == null || itinerary.getOptions().isEmpty()) {
            itinerary.setSelectedOptionKey(null);
            return itinerary;
        }

        ItineraryOptionVO selected = itinerary.getOptions().stream()
                .filter(option -> Objects.equals(option.getOptionKey(), selectedOptionKey))
                .findFirst()
                .orElse(itinerary.getOptions().get(0));

        itinerary.setSelectedOptionKey(selected.getOptionKey());
        itinerary.setNodes(selected.getNodes());
        itinerary.setTotalDuration(selected.getTotalDuration());
        itinerary.setTotalCost(selected.getTotalCost());
        itinerary.setRecommendReason(selected.getRecommendReason());
        itinerary.setAlerts(selected.getAlerts());
        return itinerary;
    }

    public ItineraryVO applyEntityMetadata(ItineraryVO itinerary, SavedItinerary entity) {
        if (itinerary == null || entity == null) {
            return itinerary;
        }
        itinerary.setId(entity.getId());
        itinerary.setCustomTitle(entity.getCustomTitle());
        itinerary.setShareNote(entity.getShareNote());
        itinerary.setFavorited(entity.getFavorited() != null && entity.getFavorited() == 1);
        itinerary.setFavoriteTime(entity.getFavoriteTime());
        itinerary.setIsPublic(entity.getIsPublic() != null && entity.getIsPublic() == 1);
        itinerary.setLastSavedAt(entity.getUpdateTime() == null ? LocalDateTime.now() : entity.getUpdateTime());
        return itinerary;
    }
}
