package com.citytrip.service.application.itinerary;

import com.citytrip.common.BadRequestException;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.dto.PublicStatusReqDTO;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.application.community.CommunityCacheInvalidationService;
import com.citytrip.service.persistence.itinerary.SavedItineraryCodec;
import com.citytrip.service.persistence.itinerary.SavedItineraryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SavedItineraryCommandServiceTest {

    @Mock
    private SavedItineraryRepository savedItineraryRepository;

    @Mock
    private SavedItineraryCodec savedItineraryCodec;

    @Mock
    private CommunityCacheInvalidationService communityCacheInvalidationService;

    @InjectMocks
    private SavedItineraryCommandService savedItineraryCommandService;

    @Test
    void saveReturnsOriginalItineraryWhenUserIsAnonymous() {
        GenerateReqDTO req = new GenerateReqDTO();
        ItineraryVO itinerary = new ItineraryVO();

        ItineraryVO result = savedItineraryCommandService.save(null, null, req, itinerary);

        assertThat(result).isSameAs(itinerary);
        assertThat(itinerary.getOriginalReq()).isSameAs(req);
        verifyNoInteractions(savedItineraryRepository, savedItineraryCodec, communityCacheInvalidationService);
    }

    @Test
    void updatePublicStatusRejectsMissingIsPublicFlag() {
        PublicStatusReqDTO req = new PublicStatusReqDTO();

        assertThatThrownBy(() -> savedItineraryCommandService.updatePublicStatus(1L, 2L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("isPublic is required");

        verifyNoInteractions(savedItineraryRepository, savedItineraryCodec, communityCacheInvalidationService);
    }
}
