package com.citytrip.service.domain.planning;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.service.geo.GeoPoiCandidate;
import com.citytrip.service.geo.GeoSearchService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalPoiCandidateServiceTest {

    @Test
    void recallForReplanShouldSearchEachExternalPoiAgainAndUseReturnedBusinessDefaults() throws Exception {
        GeoSearchService geoSearchService = mock(GeoSearchService.class);
        ExternalPoiCandidateService service = new ExternalPoiCandidateService(geoSearchService);

        GenerateReqDTO request = new GenerateReqDTO();
        request.setCityName("成都");
        request.setCityCode("CD");

        Poi local = new Poi();
        local.setName("本地博物馆");
        local.setCategory("博物馆");
        local.setDistrict("锦江区");

        GeoPoiCandidate raw = candidate("外部新馆", "博物馆", "锦江区", "104.080001", "30.650001");
        GeoPoiCandidate detail = candidate("外部新馆", "博物馆", "锦江区", "104.080001", "30.650001");
        setDetail(detail, "setOpeningHours", String.class, "10:30-21:45");
        setDetail(detail, "setAvgCost", BigDecimal.class, BigDecimal.valueOf(128));
        setDetail(detail, "setStayDurationMinutes", Integer.class, 75);

        when(geoSearchService.searchByKeyword("博物馆", "成都", 8)).thenReturn(List.of(raw));
        when(geoSearchService.searchByKeyword("锦江区 景点", "成都", 8)).thenReturn(List.of());
        when(geoSearchService.searchByKeyword("外部新馆", "成都", 1)).thenReturn(List.of(detail));

        List<Poi> result = service.recallForReplan(List.of(local), request, 8);

        assertThat(result).hasSize(1);
        Poi poi = result.get(0);
        assertThat(poi.getSourceType()).isEqualTo("external");
        assertThat(poi.getOpenTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(poi.getCloseTime()).isEqualTo(LocalTime.of(21, 45));
        assertThat(poi.getAvgCost()).isEqualByComparingTo("128");
        assertThat(poi.getStayDuration()).isEqualTo(75);
        assertThat(poi.getAvailabilityNote()).contains("地图信息补全");
        verify(geoSearchService).searchByKeyword("外部新馆", "成都", 1);
    }

    private GeoPoiCandidate candidate(String name,
                                      String category,
                                      String district,
                                      String lng,
                                      String lat) {
        GeoPoiCandidate candidate = new GeoPoiCandidate();
        candidate.setName(name);
        candidate.setCategory(category);
        candidate.setDistrict(district);
        candidate.setCityName("成都");
        candidate.setLongitude(new BigDecimal(lng));
        candidate.setLatitude(new BigDecimal(lat));
        return candidate;
    }

    private void setDetail(GeoPoiCandidate candidate,
                           String setterName,
                           Class<?> parameterType,
                           Object value) throws Exception {
        Method method = GeoPoiCandidate.class.getMethod(setterName, parameterType);
        method.invoke(candidate, value);
    }
}
