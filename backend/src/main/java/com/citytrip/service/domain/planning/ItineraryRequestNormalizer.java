package com.citytrip.service.domain.planning;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.service.geo.CityResolverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ItineraryRequestNormalizer {

    public static final int DEFAULT_START_MINUTE = 9 * 60;
    public static final int DEFAULT_END_MINUTE = 18 * 60;
    public static final String DEFAULT_CITY_NAME = "成都";

    private final CityResolverService cityResolverService;

    @Autowired
    public ItineraryRequestNormalizer(@Autowired(required = false) CityResolverService cityResolverService) {
        this.cityResolverService = cityResolverService;
    }

    public ItineraryRequestNormalizer() {
        this(null);
    }

    public GenerateReqDTO normalize(GenerateReqDTO req) {
        GenerateReqDTO normalized = new GenerateReqDTO();
        String requestedCityName = req == null ? null : req.getCityName();
        String requestedCityCode = req == null ? null : req.getCityCode();
        String normalizedCityName = cityResolverService == null
                ? textOrDefault(requestedCityName, DEFAULT_CITY_NAME)
                : cityResolverService.resolveCityName(requestedCityName, requestedCityCode);
        String normalizedCityCode = cityResolverService == null
                ? textOrDefault(requestedCityCode, null)
                : cityResolverService.resolveCityCode(requestedCityCode, normalizedCityName);
        normalized.setCityName(normalizedCityName);
        normalized.setCityCode(normalizedCityCode);
        normalized.setTripDays(req == null || req.getTripDays() == null ? 1.0 : req.getTripDays());
        normalized.setTripDate(textOrDefault(req == null ? null : req.getTripDate(), LocalDate.now().toString()));
        normalized.setTotalBudget(req == null ? null : req.getTotalBudget());
        normalized.setBudgetLevel(req == null ? null : req.getBudgetLevel());
        normalized.setThemes(req == null || req.getThemes() == null
                ? Collections.emptyList()
                : new ArrayList<>(req.getThemes()));
        normalized.setIsRainy(req != null && Boolean.TRUE.equals(req.getIsRainy()));
        normalized.setIsNight(req != null && Boolean.TRUE.equals(req.getIsNight()));
        normalized.setWalkingLevel(textOrDefault(req == null ? null : req.getWalkingLevel(), "medium"));
        normalized.setCompanionType(req == null ? null : req.getCompanionType());
        normalized.setStartTime(textOrDefault(req == null ? null : req.getStartTime(), "09:00"));
        normalized.setEndTime(textOrDefault(req == null ? null : req.getEndTime(), "18:00"));
        normalized.setMustVisitPoiNames(normalizeMustVisitPoiNames(req == null ? null : req.getMustVisitPoiNames()));
        normalized.setDeparturePlaceName(req == null ? null : req.getDeparturePlaceName());
        normalized.setDepartureLatitude(req == null ? null : req.getDepartureLatitude());
        normalized.setDepartureLongitude(req == null ? null : req.getDepartureLongitude());
        return normalized;
    }

    public List<String> normalizeMustVisitPoiNames(List<String> mustVisitPoiNames) {
        if (mustVisitPoiNames == null || mustVisitPoiNames.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String item : mustVisitPoiNames) {
            if (StringUtils.hasText(item)) {
                String keyword = item.trim();
                normalized.add(keyword);
                if (keyword.toLowerCase(Locale.ROOT).contains("ifs")) {
                    normalized.add("ifs");
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    private String textOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}