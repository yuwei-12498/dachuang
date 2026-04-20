package com.citytrip.service.persistence.itinerary;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.citytrip.common.NotFoundException;
import com.citytrip.common.SystemBusyException;
import com.citytrip.mapper.SavedItineraryMapper;
import com.citytrip.model.entity.SavedItinerary;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Repository
public class SavedItineraryRepository {

    private final SavedItineraryMapper savedItineraryMapper;

    public SavedItineraryRepository(SavedItineraryMapper savedItineraryMapper) {
        this.savedItineraryMapper = savedItineraryMapper;
    }

    public SavedItinerary findLatestOwned(Long userId) {
        if (userId == null) {
            return null;
        }
        QueryWrapper<SavedItinerary> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("update_time").last("limit 1");
        return savedItineraryMapper.selectOne(wrapper);
    }

    public SavedItinerary findOwned(Long userId, Long itineraryId) {
        if (userId == null || itineraryId == null) {
            return null;
        }
        SavedItinerary entity = savedItineraryMapper.selectById(itineraryId);
        if (entity == null || !Objects.equals(entity.getUserId(), userId)) {
            return null;
        }
        return entity;
    }

    public SavedItinerary findOwnedForUpdate(Long userId, Long itineraryId) {
        if (userId == null || itineraryId == null) {
            return null;
        }
        SavedItinerary entity = savedItineraryMapper.selectOwnedForUpdate(userId, itineraryId);
        if (entity == null) {
            return null;
        }
        if (!Objects.equals(entity.getUserId(), userId)) {
            throw new SystemBusyException("The itinerary is being updated by another request");
        }
        return entity;
    }

    public SavedItinerary requireOwned(Long userId, Long itineraryId) {
        SavedItinerary entity = findOwned(userId, itineraryId);
        if (entity == null) {
            throw new NotFoundException("行程记录不存在");
        }
        return entity;
    }

    public SavedItinerary requireOwnedForUpdate(Long userId, Long itineraryId) {
        SavedItinerary entity = findOwnedForUpdate(userId, itineraryId);
        if (entity == null) {
            throw new NotFoundException("The itinerary record was not found");
        }
        return entity;
    }

    public SavedItinerary requirePublic(Long itineraryId) {
        SavedItinerary entity = itineraryId == null ? null : savedItineraryMapper.selectById(itineraryId);
        if (entity == null || entity.getIsPublic() == null || entity.getIsPublic() != 1) {
            throw new NotFoundException("未找到公开的社区路线");
        }
        return entity;
    }

    public List<SavedItinerary> listOwned(Long userId, boolean favoriteOnly, Integer limit) {
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
        return savedItineraryMapper.selectList(wrapper);
    }

    public long countPublic() {
        QueryWrapper<SavedItinerary> wrapper = new QueryWrapper<>();
        wrapper.eq("is_public", 1);
        return savedItineraryMapper.selectCount(wrapper);
    }

    public List<SavedItinerary> listPublic(int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 30);
        int offset = (normalizedPage - 1) * normalizedSize;

        QueryWrapper<SavedItinerary> wrapper = new QueryWrapper<>();
        wrapper.eq("is_public", 1)
                .orderByDesc("favorite_time")
                .orderByDesc("update_time")
                .last("limit " + offset + "," + normalizedSize);
        return savedItineraryMapper.selectList(wrapper);
    }

    public void saveOrUpdate(SavedItinerary entity) {
        if (entity.getId() == null) {
            savedItineraryMapper.insert(entity);
        } else {
            savedItineraryMapper.updateById(entity);
        }
    }

    public SavedItinerary reload(Long itineraryId) {
        return itineraryId == null ? null : savedItineraryMapper.selectById(itineraryId);
    }
}
