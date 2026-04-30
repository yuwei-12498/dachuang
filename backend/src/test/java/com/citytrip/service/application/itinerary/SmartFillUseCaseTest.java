package com.citytrip.service.application.itinerary;

import com.citytrip.mapper.PoiMapper;
import com.citytrip.model.dto.SmartFillReqDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.model.vo.SmartFillVO;
import com.citytrip.service.LlmService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmartFillUseCaseTest {

    @Test
    void shouldMapIfsAliasToCanonicalPoiName() {
        LlmService llmService = mock(LlmService.class);
        PoiMapper poiMapper = mock(PoiMapper.class);

        Poi poi = new Poi();
        poi.setName("IFS\u56fd\u9645\u91d1\u878d\u4e2d\u5fc3");
        when(poiMapper.selectPlanningCandidates(false, null, 200)).thenReturn(List.of(poi));

        SmartFillVO llmOutput = new SmartFillVO();
        llmOutput.setSummary(List.of("\u8d2d\u7269"));
        llmOutput.setMustVisitPoiNames(List.of("IFS\u91d1\u878d\u4e2d\u5fc3"));
        when(llmService.parseSmartFill(eq("\u6211\u60f3\u53bbIFS\u91d1\u878d\u4e2d\u5fc3"), anyList())).thenReturn(llmOutput);

        SmartFillUseCase useCase = new SmartFillUseCase(llmService, poiMapper);
        SmartFillReqDTO req = new SmartFillReqDTO();
        req.setText("\u6211\u60f3\u53bbIFS\u91d1\u878d\u4e2d\u5fc3");

        SmartFillVO result = useCase.parse(req);

        assertThat(result.getMustVisitPoiNames()).contains("IFS\u56fd\u9645\u91d1\u878d\u4e2d\u5fc3");
        assertThat(result.getSummary().stream().anyMatch(item -> item.contains("IFS\u56fd\u9645\u91d1\u878d\u4e2d\u5fc3"))).isTrue();
    }
}
