package com.citytrip.service.application.community;

import com.citytrip.assembler.ItinerarySummaryAssembler;
import com.citytrip.mapper.CommunityCommentMapper;
import com.citytrip.mapper.CommunityLikeMapper;
import com.citytrip.mapper.UserMapper;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.SavedItinerary;
import com.citytrip.model.entity.User;
import com.citytrip.model.vo.CommunityItineraryPageVO;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.impl.CommunityItineraryCacheService;
import com.citytrip.service.persistence.itinerary.SavedItineraryCodec;
import com.citytrip.service.persistence.itinerary.SavedItineraryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommunityItineraryQueryServiceTest {

    @Test
    void listPublicFallsBackToZeroCountsWhenCommunityTablesAreUnavailable() throws Exception {
        SavedItineraryRepository repository = mock(SavedItineraryRepository.class);
        CommunityCommentMapper commentMapper = mock(CommunityCommentMapper.class);
        CommunityLikeMapper likeMapper = mock(CommunityLikeMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        SavedItineraryCodec codec = mock(SavedItineraryCodec.class);

        CommunityItineraryQueryService service = new CommunityItineraryQueryService(
                repository,
                commentMapper,
                likeMapper,
                userMapper,
                codec,
                new ItinerarySummaryAssembler(),
                new CommunityItineraryCacheService(false, null, null, new ObjectMapper())
        );

        SavedItinerary entity = new SavedItinerary();
        entity.setId(88L);
        entity.setUserId(7L);
        entity.setNodeCount(1);
        entity.setTotalDuration(90);
        entity.setTotalCost(new BigDecimal("30"));
        entity.setUpdateTime(LocalDateTime.now());

        User author = new User();
        author.setId(7L);
        author.setNickname("测试作者");

        GenerateReqDTO req = new GenerateReqDTO();
        req.setThemes(List.of("museum"));
        ItineraryNodeVO node = new ItineraryNodeVO();
        node.setPoiName("City Museum");
        ItineraryVO itinerary = new ItineraryVO();
        itinerary.setNodes(List.of(node));

        when(repository.listPublic(1, 12)).thenReturn(List.of(entity));
        when(repository.countPublic()).thenReturn(1L);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(author));
        when(codec.readRequest(entity)).thenReturn(req);
        when(codec.readItinerary(entity)).thenReturn(itinerary);
        when(commentMapper.selectList(any())).thenThrow(new DataAccessResourceFailureException("comment table down"));
        when(likeMapper.selectList(any())).thenThrow(new DataAccessResourceFailureException("like table down"));

        CommunityItineraryPageVO page = service.listPublic(1, 12);

        assertThat(page.getTotal()).isEqualTo(1L);
        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getCommentCount()).isZero();
        assertThat(page.getRecords().get(0).getLikeCount()).isZero();
        assertThat(page.getRecords().get(0).getAuthorLabel()).isEqualTo("测试作者");
    }
}
