package com.citytrip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.citytrip.common.NotFoundException;
import com.citytrip.mapper.SavedItineraryMapper;
import com.citytrip.model.dto.FavoriteReqDTO;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.SavedItinerary;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryOptionVO;
import com.citytrip.model.vo.ItinerarySummaryVO;
import com.citytrip.model.vo.ItineraryVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class SavedItineraryStore {

    private final SavedItineraryMapper savedItineraryMapper;
    private final ObjectMapper objectMapper;

    public SavedItineraryStore(SavedItineraryMapper savedItineraryMapper, ObjectMapper objectMapper) {
        this.savedItineraryMapper = savedItineraryMapper;
        this.objectMapper = objectMapper;
    }

    public ItineraryVO loadLatest(Long userId) {
        if (userId == null) {
            return null;
        }
        QueryWrapper<SavedItinerary> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("update_time").last("limit 1");
        SavedItinerary entity = savedItineraryMapper.selectOne(wrapper);
        return entity == null ? null : deserialize(entity);
    }

    public ItineraryVO load(Long userId, Long itineraryId) {
        SavedItinerary entity = requireOwned(userId, itineraryId);
        return deserialize(entity);
    }

    public List<ItinerarySummaryVO> listSummaries(Long userId, boolean favoriteOnly, Integer limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        QueryWrapper<SavedItinerary> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (favoriteOnly) {
            wrapper.eq("favorited", 1).orderByDesc("favorite_time").orderByDesc("update_time");
        } else {
            wrapper.orderByDesc("update_time");
        }
        if (limit != null && limit > 0) {
            wrapper.last("limit " + limit);
        }

        List<SavedItinerary> entities = savedItineraryMapper.selectList(wrapper);
        List<ItinerarySummaryVO> result = new ArrayList<>(entities.size());
        for (SavedItinerary entity : entities) {
            result.add(toSummary(entity));
        }
        return result;
    }

    public ItineraryVO favorite(Long userId, Long itineraryId, FavoriteReqDTO req) {
        SavedItinerary entity = requireOwned(userId, itineraryId);
        ItineraryVO itinerary = deserialize(entity);
        ItineraryVO selected = selectOptionInPlace(itinerary, req == null ? null : req.getSelectedOptionKey());
        String customTitle = normalizeTitle(req == null ? null : req.getTitle());

        selected.setCustomTitle(customTitle);
        entity.setItineraryJson(writeJson(selected));
        entity.setNodeCount(selected.getNodes() == null ? 0 : selected.getNodes().size());
        entity.setTotalDuration(selected.getTotalDuration());
        entity.setTotalCost(selected.getTotalCost());
        entity.setRouteSignature(signature(selected));
        entity.setCustomTitle(customTitle);
        entity.setFavorited(1);
        entity.setFavoriteTime(LocalDateTime.now());
        savedItineraryMapper.updateById(entity);

        selected.setId(entity.getId());
        selected.setCustomTitle(entity.getCustomTitle());
        selected.setFavorited(true);
        selected.setFavoriteTime(entity.getFavoriteTime());
        selected.setLastSavedAt(entity.getUpdateTime() == null ? LocalDateTime.now() : entity.getUpdateTime());
        return selected;
    }

    public void markFavorite(Long userId, Long itineraryId, boolean favorited) {
        SavedItinerary entity = requireOwned(userId, itineraryId);
        entity.setFavorited(favorited ? 1 : 0);
        entity.setFavoriteTime(favorited ? LocalDateTime.now() : null);
        savedItineraryMapper.updateById(entity);
    }

    public ItineraryVO save(Long userId, Long itineraryId, GenerateReqDTO req, ItineraryVO itinerary) {
        itinerary.setOriginalReq(req);
        if (userId == null) {
            return itinerary;
        }

        SavedItinerary entity = findOwned(userId, itineraryId);
        if (entity == null) {
            entity = new SavedItinerary();
            entity.setUserId(userId);
            entity.setFavorited(0);
        }
        if (StringUtils.hasText(entity.getCustomTitle()) && !StringUtils.hasText(itinerary.getCustomTitle())) {
            itinerary.setCustomTitle(entity.getCustomTitle());
        }

        entity.setRequestJson(writeJson(req));
        entity.setItineraryJson(writeJson(itinerary));
        entity.setNodeCount(itinerary.getNodes() == null ? 0 : itinerary.getNodes().size());
        entity.setTotalDuration(itinerary.getTotalDuration());
        entity.setTotalCost(itinerary.getTotalCost());
        entity.setRouteSignature(signature(itinerary));

        if (entity.getId() == null) {
            savedItineraryMapper.insert(entity);
        } else {
            savedItineraryMapper.updateById(entity);
        }

        itinerary.setId(entity.getId());
        itinerary.setCustomTitle(entity.getCustomTitle());
        itinerary.setFavorited(entity.getFavorited() != null && entity.getFavorited() == 1);
        itinerary.setFavoriteTime(entity.getFavoriteTime());
        itinerary.setLastSavedAt(entity.getUpdateTime() == null ? LocalDateTime.now() : entity.getUpdateTime());
        return itinerary;
    }

    private SavedItinerary findOwned(Long userId, Long itineraryId) {
        if (userId == null || itineraryId == null) {
            return null;
        }
        SavedItinerary entity = savedItineraryMapper.selectById(itineraryId);
        if (entity == null || !Objects.equals(entity.getUserId(), userId)) {
            return null;
        }
        return entity;
    }

    private SavedItinerary requireOwned(Long userId, Long itineraryId) {
        SavedItinerary entity = findOwned(userId, itineraryId);
        if (entity == null) {
            throw new NotFoundException("未找到对应的行程记录。");
        }
        return entity;
    }

    private ItinerarySummaryVO toSummary(SavedItinerary entity) {
        try {
            GenerateReqDTO req = objectMapper.readValue(entity.getRequestJson(), GenerateReqDTO.class);
            ItineraryVO itinerary = objectMapper.readValue(entity.getItineraryJson(), ItineraryVO.class);

            ItinerarySummaryVO summary = new ItinerarySummaryVO();
            summary.setId(entity.getId());
            summary.setTitle(buildTitle(req, itinerary));
            summary.setTripDate(req == null ? null : req.getTripDate());
            summary.setStartTime(req == null ? null : req.getStartTime());
            summary.setEndTime(req == null ? null : req.getEndTime());
            summary.setNodeCount(entity.getNodeCount());
            summary.setTotalDuration(entity.getTotalDuration());
            summary.setTotalCost(entity.getTotalCost());
            summary.setBudgetLevel(req == null ? null : req.getBudgetLevel());
            summary.setCompanionType(req == null ? null : req.getCompanionType());
            summary.setRainy(req != null && Boolean.TRUE.equals(req.getIsRainy()));
            summary.setNight(req != null && Boolean.TRUE.equals(req.getIsNight()));
            summary.setFavorited(entity.getFavorited() != null && entity.getFavorited() == 1);
            summary.setFavoriteTime(entity.getFavoriteTime());
            summary.setUpdatedAt(entity.getUpdateTime());
            summary.setThemes(req == null || req.getThemes() == null ? Collections.emptyList() : req.getThemes());

            List<ItineraryNodeVO> nodes = itinerary == null || itinerary.getNodes() == null ? Collections.emptyList() : itinerary.getNodes();
            if (!nodes.isEmpty()) {
                summary.setFirstPoiName(nodes.get(0).getPoiName());
                summary.setLastPoiName(nodes.get(nodes.size() - 1).getPoiName());
            }
            return summary;
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("无法读取历史行程摘要。", ex);
        }
    }

    private ItineraryVO deserialize(SavedItinerary entity) {
        try {
            ItineraryVO itinerary = objectMapper.readValue(entity.getItineraryJson(), ItineraryVO.class);
            GenerateReqDTO req = objectMapper.readValue(entity.getRequestJson(), GenerateReqDTO.class);
            itinerary.setId(entity.getId());
            itinerary.setCustomTitle(entity.getCustomTitle());
            itinerary.setOriginalReq(req);
            itinerary.setFavorited(entity.getFavorited() != null && entity.getFavorited() == 1);
            itinerary.setFavoriteTime(entity.getFavoriteTime());
            itinerary.setLastSavedAt(entity.getUpdateTime());
            return itinerary;
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("无法恢复历史行程。", ex);
        }
    }

    private String buildTitle(GenerateReqDTO req, ItineraryVO itinerary) {
        if (StringUtils.hasText(itinerary == null ? null : itinerary.getCustomTitle())) {
            return itinerary.getCustomTitle().trim();
        }
        List<String> themes = req == null || req.getThemes() == null ? Collections.emptyList() : req.getThemes();
        if (!themes.isEmpty()) {
            return String.join(" / ", themes) + "路线";
        }
        List<ItineraryNodeVO> nodes = itinerary == null || itinerary.getNodes() == null ? Collections.emptyList() : itinerary.getNodes();
        if (!nodes.isEmpty()) {
            return nodes.get(0).getPoiName() + "出发路线";
        }
        String tripDate = req == null ? null : req.getTripDate();
        return StringUtils.hasText(tripDate) ? tripDate + "行程" : "我的行程";
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("保存行程失败。", ex);
        }
    }

    private String signature(ItineraryVO itinerary) {
        if (itinerary.getNodes() == null || itinerary.getNodes().isEmpty()) {
            return "";
        }
        return itinerary.getNodes().stream()
                .map(node -> String.valueOf(node.getPoiId()))
                .reduce((left, right) -> left + "-" + right)
                .orElse("");
    }

    private ItineraryVO selectOptionInPlace(ItineraryVO itinerary, String selectedOptionKey) {
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

    private String normalizeTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return null;
        }
        String value = title.trim();
        return value.length() > 60 ? value.substring(0, 60) : value;
    }
}
