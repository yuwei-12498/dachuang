package com.citytrip.service.application.community;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.citytrip.assembler.ItinerarySummaryAssembler;
import com.citytrip.common.BadRequestException;
import com.citytrip.mapper.CommunityCommentMapper;
import com.citytrip.mapper.CommunityLikeMapper;
import com.citytrip.mapper.UserMapper;
import com.citytrip.model.dto.CommunityCommentReqDTO;
import com.citytrip.model.entity.CommunityComment;
import com.citytrip.model.entity.CommunityLike;
import com.citytrip.model.entity.SavedItinerary;
import com.citytrip.model.entity.User;
import com.citytrip.model.vo.CommunityCommentVO;
import com.citytrip.model.vo.CommunityItineraryDetailVO;
import com.citytrip.service.persistence.itinerary.SavedItineraryRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommunityInteractionService {

    private final SavedItineraryRepository savedItineraryRepository;
    private final CommunityCommentMapper communityCommentMapper;
    private final CommunityLikeMapper communityLikeMapper;
    private final UserMapper userMapper;
    private final ItinerarySummaryAssembler itinerarySummaryAssembler;
    private final CommunityCacheInvalidationService communityCacheInvalidationService;
    private final CommunityItineraryQueryService communityItineraryQueryService;

    public CommunityInteractionService(SavedItineraryRepository savedItineraryRepository,
                                       CommunityCommentMapper communityCommentMapper,
                                       CommunityLikeMapper communityLikeMapper,
                                       UserMapper userMapper,
                                       ItinerarySummaryAssembler itinerarySummaryAssembler,
                                       CommunityCacheInvalidationService communityCacheInvalidationService,
                                       CommunityItineraryQueryService communityItineraryQueryService) {
        this.savedItineraryRepository = savedItineraryRepository;
        this.communityCommentMapper = communityCommentMapper;
        this.communityLikeMapper = communityLikeMapper;
        this.userMapper = userMapper;
        this.itinerarySummaryAssembler = itinerarySummaryAssembler;
        this.communityCacheInvalidationService = communityCacheInvalidationService;
        this.communityItineraryQueryService = communityItineraryQueryService;
    }

    public List<CommunityCommentVO> listComments(Long itineraryId, Long currentUserId) {
        SavedItinerary entity = savedItineraryRepository.requirePublic(itineraryId);
        QueryWrapper<CommunityComment> wrapper = new QueryWrapper<>();
        wrapper.eq("itinerary_id", entity.getId()).orderByAsc("create_time").orderByAsc("id");
        List<CommunityComment> comments = communityCommentMapper.selectList(wrapper);
        Map<Long, User> userMap = loadUsersByIds(comments.stream()
                .map(CommunityComment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        Map<Long, List<CommunityCommentVO>> repliesMap = comments.stream()
                .filter(comment -> comment.getParentId() != null)
                .map(comment -> toCommunityComment(comment, userMap.get(comment.getUserId()), currentUserId))
                .collect(Collectors.groupingBy(CommunityCommentVO::getParentId, LinkedHashMap::new, Collectors.toList()));

        return comments.stream()
                .filter(comment -> comment.getParentId() == null)
                .map(comment -> {
                    CommunityCommentVO root = toCommunityComment(comment, userMap.get(comment.getUserId()), currentUserId);
                    root.setReplies(repliesMap.getOrDefault(root.getId(), Collections.emptyList()));
                    return root;
                })
                .toList();
    }

    @Transactional
    public CommunityCommentVO addComment(Long userId, Long itineraryId, CommunityCommentReqDTO req) {
        if (userId == null) {
            throw new BadRequestException("login is required");
        }

        SavedItinerary entity = savedItineraryRepository.requirePublic(itineraryId);
        String content = normalizeCommentContent(req == null ? null : req.getContent());
        if (!StringUtils.hasText(content)) {
            throw new BadRequestException("comment content is required");
        }

        Long parentId = req == null ? null : req.getParentId();
        if (parentId != null) {
            CommunityComment parent = communityCommentMapper.selectById(parentId);
            if (parent == null || !Objects.equals(parent.getItineraryId(), entity.getId())) {
                throw new BadRequestException("comment parent does not belong to the itinerary");
            }
            if (parent.getParentId() != null) {
                throw new BadRequestException("only one-level replies are supported");
            }
        }

        CommunityComment comment = new CommunityComment();
        comment.setItineraryId(entity.getId());
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(content);
        communityCommentMapper.insert(comment);
        comment = communityCommentMapper.selectById(comment.getId());
        communityCacheInvalidationService.markDirty();

        User author = userMapper.selectById(userId);
        return toCommunityComment(comment, author, userId);
    }

    @Transactional
    public CommunityItineraryDetailVO like(Long userId, Long itineraryId) {
        if (userId == null) {
            throw new BadRequestException("login is required");
        }

        SavedItinerary entity = savedItineraryRepository.requirePublic(itineraryId);
        try {
            CommunityLike like = new CommunityLike();
            like.setItineraryId(entity.getId());
            like.setUserId(userId);
            communityLikeMapper.insert(like);
        } catch (DuplicateKeyException ignore) {
            // idempotent
        }
        communityCacheInvalidationService.markDirty();
        return communityItineraryQueryService.getPublicDetail(entity.getId(), userId);
    }

    @Transactional
    public CommunityItineraryDetailVO unlike(Long userId, Long itineraryId) {
        if (userId == null) {
            throw new BadRequestException("login is required");
        }

        SavedItinerary entity = savedItineraryRepository.requirePublic(itineraryId);
        QueryWrapper<CommunityLike> wrapper = new QueryWrapper<>();
        wrapper.eq("itinerary_id", entity.getId()).eq("user_id", userId);
        communityLikeMapper.delete(wrapper);
        communityCacheInvalidationService.markDirty();
        return communityItineraryQueryService.getPublicDetail(entity.getId(), userId);
    }

    private Map<Long, User> loadUsersByIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left, LinkedHashMap::new));
    }

    private CommunityCommentVO toCommunityComment(CommunityComment comment, User author, Long currentUserId) {
        CommunityCommentVO vo = new CommunityCommentVO();
        vo.setId(comment.getId());
        vo.setItineraryId(comment.getItineraryId());
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setAuthorLabel(itinerarySummaryAssembler.resolveAuthorLabel(author));
        vo.setCreateTime(comment.getCreateTime());
        vo.setMine(currentUserId != null && Objects.equals(currentUserId, comment.getUserId()));
        return vo;
    }

    private String normalizeCommentContent(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String value = content.trim();
        return value.length() > 300 ? value.substring(0, 300) : value;
    }
}
