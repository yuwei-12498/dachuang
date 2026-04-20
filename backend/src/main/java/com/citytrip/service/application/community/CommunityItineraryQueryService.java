package com.citytrip.service.application.community;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.citytrip.assembler.ItinerarySummaryAssembler;
import com.citytrip.mapper.CommunityCommentMapper;
import com.citytrip.mapper.CommunityLikeMapper;
import com.citytrip.mapper.UserMapper;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.CommunityComment;
import com.citytrip.model.entity.CommunityLike;
import com.citytrip.model.entity.SavedItinerary;
import com.citytrip.model.entity.User;
import com.citytrip.model.vo.CommunityItineraryDetailVO;
import com.citytrip.model.vo.CommunityItineraryPageVO;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.impl.CommunityItineraryCacheService;
import com.citytrip.service.persistence.itinerary.SavedItineraryCodec;
import com.citytrip.service.persistence.itinerary.SavedItineraryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommunityItineraryQueryService {

    private final SavedItineraryRepository savedItineraryRepository;
    private final CommunityCommentMapper communityCommentMapper;
    private final CommunityLikeMapper communityLikeMapper;
    private final UserMapper userMapper;
    private final SavedItineraryCodec savedItineraryCodec;
    private final ItinerarySummaryAssembler itinerarySummaryAssembler;
    private final CommunityItineraryCacheService communityItineraryCacheService;

    public CommunityItineraryQueryService(SavedItineraryRepository savedItineraryRepository,
                                          CommunityCommentMapper communityCommentMapper,
                                          CommunityLikeMapper communityLikeMapper,
                                          UserMapper userMapper,
                                          SavedItineraryCodec savedItineraryCodec,
                                          ItinerarySummaryAssembler itinerarySummaryAssembler,
                                          CommunityItineraryCacheService communityItineraryCacheService) {
        this.savedItineraryRepository = savedItineraryRepository;
        this.communityCommentMapper = communityCommentMapper;
        this.communityLikeMapper = communityLikeMapper;
        this.userMapper = userMapper;
        this.savedItineraryCodec = savedItineraryCodec;
        this.itinerarySummaryAssembler = itinerarySummaryAssembler;
        this.communityItineraryCacheService = communityItineraryCacheService;
    }

    public CommunityItineraryPageVO listPublic(int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 30);
        return communityItineraryCacheService.getCommunityPage(
                normalizedPage,
                normalizedSize,
                () -> loadPublicPage(normalizedPage, normalizedSize)
        );
    }

    public CommunityItineraryDetailVO getPublicDetail(Long itineraryId, Long currentUserId) {
        SavedItinerary entity = savedItineraryRepository.requirePublic(itineraryId);
        User author = loadUserMap(List.of(entity)).get(entity.getUserId());
        try {
            GenerateReqDTO req = savedItineraryCodec.readRequest(entity);
            ItineraryVO itinerary = savedItineraryCodec.readItinerary(entity);

            CommunityItineraryDetailVO detail = new CommunityItineraryDetailVO();
            detail.setId(entity.getId());
            detail.setTitle(itinerarySummaryAssembler.buildTitle(req, itinerary));
            detail.setCityName(req == null ? null : req.getCityName());
            detail.setCoverImageUrl(itinerarySummaryAssembler.resolveCoverImage(itinerary));
            detail.setShareNote(itinerarySummaryAssembler.resolveShareNote(entity, itinerary));
            detail.setAuthorLabel(itinerarySummaryAssembler.resolveAuthorLabel(author));
            detail.setTripDate(req == null ? null : req.getTripDate());
            detail.setStartTime(req == null ? null : req.getStartTime());
            detail.setEndTime(req == null ? null : req.getEndTime());
            detail.setThemes(req == null || req.getThemes() == null ? Collections.emptyList() : req.getThemes());
            detail.setTotalDuration(entity.getTotalDuration());
            detail.setTotalCost(entity.getTotalCost());
            detail.setNodeCount(entity.getNodeCount());
            detail.setRouteSummary(itinerarySummaryAssembler.buildRouteSummary(itinerary));
            detail.setRecommendReason(itinerary == null ? null : itinerary.getRecommendReason());
            detail.setSelectedOptionKey(itinerary == null ? null : itinerary.getSelectedOptionKey());
            detail.setHighlights(itinerarySummaryAssembler.resolveHighlights(itinerary));
            detail.setAlerts(itinerary == null || itinerary.getAlerts() == null ? Collections.emptyList() : itinerary.getAlerts());
            detail.setNodes(itinerary == null || itinerary.getNodes() == null ? Collections.emptyList() : itinerary.getNodes());
            detail.setLikeCount(countLikes(entity.getId()));
            detail.setLiked(isLikedByCurrentUser(entity.getId(), currentUserId));
            detail.setCommentCount(countComments(entity.getId()));
            detail.setUpdatedAt(entity.getUpdateTime());
            return detail;
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to deserialize public itinerary detail", ex);
        }
    }

    private CommunityItineraryPageVO loadPublicPage(int page, int size) {
        List<SavedItinerary> entities = savedItineraryRepository.listPublic(page, size);
        Map<Long, User> userMap = loadUserMap(entities);
        List<Long> itineraryIds = entities.stream().map(SavedItinerary::getId).toList();
        Map<Long, Long> commentCountMap = loadCommentCountMap(itineraryIds);
        Map<Long, Long> likeCountMap = loadLikeCountMap(itineraryIds);

        CommunityItineraryPageVO result = new CommunityItineraryPageVO();
        result.setPage(page);
        result.setSize(size);
        result.setTotal(savedItineraryRepository.countPublic());
        result.setRecords(entities.stream()
                .map(entity -> toCommunitySummary(
                        entity,
                        userMap.get(entity.getUserId()),
                        commentCountMap.get(entity.getId()),
                        likeCountMap.get(entity.getId())
                ))
                .toList());
        return result;
    }

    private com.citytrip.model.vo.CommunityItineraryVO toCommunitySummary(SavedItinerary entity,
                                                                           User author,
                                                                           Long commentCount,
                                                                           Long likeCount) {
        try {
            GenerateReqDTO req = savedItineraryCodec.readRequest(entity);
            ItineraryVO itinerary = savedItineraryCodec.readItinerary(entity);
            return itinerarySummaryAssembler.toCommunitySummary(entity, author, req, itinerary, commentCount, likeCount);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to deserialize public itinerary summary", ex);
        }
    }

    private Map<Long, User> loadUserMap(List<SavedItinerary> entities) {
        Set<Long> userIds = entities.stream()
                .map(SavedItinerary::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return loadUsersByIds(userIds);
    }

    Map<Long, User> loadUsersByIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left, LinkedHashMap::new));
    }

    private long countComments(Long itineraryId) {
        try {
            QueryWrapper<CommunityComment> wrapper = new QueryWrapper<>();
            wrapper.eq("itinerary_id", itineraryId);
            return communityCommentMapper.selectCount(wrapper);
        } catch (DataAccessException ex) {
            log.warn("Community comment table unavailable, fallback to zero comments for itineraryId={}", itineraryId, ex);
            return 0L;
        }
    }

    private long countLikes(Long itineraryId) {
        try {
            QueryWrapper<CommunityLike> wrapper = new QueryWrapper<>();
            wrapper.eq("itinerary_id", itineraryId);
            return communityLikeMapper.selectCount(wrapper);
        } catch (DataAccessException ex) {
            log.warn("Community like table unavailable, fallback to zero likes for itineraryId={}", itineraryId, ex);
            return 0L;
        }
    }

    private Map<Long, Long> loadCommentCountMap(List<Long> itineraryIds) {
        if (itineraryIds == null || itineraryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            QueryWrapper<CommunityComment> wrapper = new QueryWrapper<>();
            wrapper.in("itinerary_id", itineraryIds);
            return communityCommentMapper.selectList(wrapper).stream()
                    .filter(item -> item.getItineraryId() != null)
                    .collect(Collectors.groupingBy(CommunityComment::getItineraryId, LinkedHashMap::new, Collectors.counting()));
        } catch (DataAccessException ex) {
            log.warn("Community comment table unavailable, fallback to zero comment counts", ex);
            return Collections.emptyMap();
        }
    }

    private Map<Long, Long> loadLikeCountMap(List<Long> itineraryIds) {
        if (itineraryIds == null || itineraryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            QueryWrapper<CommunityLike> wrapper = new QueryWrapper<>();
            wrapper.in("itinerary_id", itineraryIds);
            return communityLikeMapper.selectList(wrapper).stream()
                    .filter(item -> item.getItineraryId() != null)
                    .collect(Collectors.groupingBy(CommunityLike::getItineraryId, LinkedHashMap::new, Collectors.counting()));
        } catch (DataAccessException ex) {
            log.warn("Community like table unavailable, fallback to zero like counts", ex);
            return Collections.emptyMap();
        }
    }

    private boolean isLikedByCurrentUser(Long itineraryId, Long currentUserId) {
        if (itineraryId == null || currentUserId == null) {
            return false;
        }
        try {
            QueryWrapper<CommunityLike> wrapper = new QueryWrapper<>();
            wrapper.eq("itinerary_id", itineraryId).eq("user_id", currentUserId).last("limit 1");
            return communityLikeMapper.selectOne(wrapper) != null;
        } catch (DataAccessException ex) {
            log.warn("Community like table unavailable, fallback to not-liked state", ex);
            return false;
        }
    }
}
