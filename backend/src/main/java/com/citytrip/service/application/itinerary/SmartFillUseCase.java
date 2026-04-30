package com.citytrip.service.application.itinerary;

import com.citytrip.mapper.PoiMapper;
import com.citytrip.model.dto.SmartFillReqDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.model.vo.SmartFillVO;
import com.citytrip.service.LlmService;
import com.citytrip.service.geo.CityResolverService;
import com.citytrip.service.geo.GeoPoint;
import com.citytrip.service.geo.GeoSearchService;
import com.citytrip.service.geo.PlaceDisambiguationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SmartFillUseCase {

    private static final Pattern TIME_PATTERN = Pattern.compile("^(?:[01]\\d|2[0-3]):[0-5]\\d$");
    private static final Pattern DEPARTURE_FROM_PATTERN = Pattern.compile("(?:从|從)([^，。,\\s]{2,24})(?:出发|開始|开始|过去|前往|到)");
    private static final Pattern DEPARTURE_STAY_PATTERN = Pattern.compile("(?:住在|住|酒店在)([^，。,\\s]{2,24})");

    private static final Set<String> ALLOWED_THEMES = Set.of("文化", "美食", "自然", "购物", "网红", "休闲");
    private static final Set<String> ALLOWED_BUDGETS = Set.of("低", "中", "高");
    private static final Set<String> ALLOWED_WALKING = Set.of("低", "中", "高");
    private static final Set<String> ALLOWED_COMPANION = Set.of("独自", "朋友", "情侣", "亲子");
    private static final int POI_HINT_LIMIT = 200;

    private static final String IFS_FALLBACK_NAME = "IFS国际金融中心";
    private static final Set<String> IFS_ALIAS_SKILL = Set.of(
            "ifs",
            "国金",
            "金融中心",
            "ifs金融中心",
            "ifs国际金融中心"
    );

    private final LlmService llmService;
    private final PoiMapper poiMapper;
    private final PlaceDisambiguationService placeDisambiguationService;
    private final GeoSearchService geoSearchService;
    private final CityResolverService cityResolverService;

    @Autowired
    public SmartFillUseCase(LlmService llmService,
                            PoiMapper poiMapper,
                            PlaceDisambiguationService placeDisambiguationService,
                            GeoSearchService geoSearchService,
                            CityResolverService cityResolverService) {
        this.llmService = llmService;
        this.poiMapper = poiMapper;
        this.placeDisambiguationService = placeDisambiguationService;
        this.geoSearchService = geoSearchService;
        this.cityResolverService = cityResolverService;
    }

    SmartFillUseCase(LlmService llmService, PoiMapper poiMapper) {
        this(llmService, poiMapper, null, null, null);
    }

    public SmartFillVO parse(SmartFillReqDTO req) {
        SmartFillVO empty = new SmartFillVO();
        if (req == null || !StringUtils.hasText(req.getText())) {
            return empty;
        }
        String text = req.getText().trim();
        String cityHint = resolveCityName(null, text);
        List<String> poiHints = loadPoiNameHints(cityHint);

        SmartFillVO parsed = llmService.parseSmartFill(text, poiHints);
        if (parsed == null) {
            parsed = new SmartFillVO();
        }

        sanitizeParsedResult(parsed, text, poiHints, cityHint);
        enrichGeoFields(parsed, text);
        return parsed;
    }

    private List<String> loadPoiNameHints(String cityName) {
        try {
            String cityCode = resolveCityCode(cityName);
            List<Poi> pois = poiMapper.selectPlanningCandidates(false, null, cityCode, cityName, POI_HINT_LIMIT);
            if (pois == null || pois.isEmpty()) {
                return List.of();
            }
            Set<String> names = new LinkedHashSet<>();
            for (Poi poi : pois) {
                if (poi != null && StringUtils.hasText(poi.getName())) {
                    names.add(poi.getName().trim());
                }
            }
            return new ArrayList<>(names);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void sanitizeParsedResult(SmartFillVO parsed,
                                      String text,
                                      List<String> poiHints,
                                      String cityHint) {
        parsed.setThemes(filterAllowedThemes(parsed.getThemes()));
        parsed.setBudgetLevel(normalizeAllowed(parsed.getBudgetLevel(), ALLOWED_BUDGETS));
        parsed.setWalkingLevel(normalizeAllowed(parsed.getWalkingLevel(), ALLOWED_WALKING));
        parsed.setCompanionType(normalizeAllowed(parsed.getCompanionType(), ALLOWED_COMPANION));
        parsed.setStartTime(normalizeTime(parsed.getStartTime()));
        parsed.setEndTime(normalizeTime(parsed.getEndTime()));
        parsed.setTripDays(normalizeTripDays(parsed.getTripDays()));
        parsed.setCityName(resolveCityName(parsed.getCityName(), cityHint));
        parsed.setMustVisitPoiNames(resolveMustVisitPoiNames(parsed.getMustVisitPoiNames(), text, poiHints, parsed.getCityName()));
        parsed.setDepartureText(trimToNull(parsed.getDepartureText()));
        parsed.setDepartureCandidates(normalizeCandidateList(parsed.getDepartureCandidates()));

        if (!isValidCoordinate(parsed.getDepartureLatitude(), parsed.getDepartureLongitude())) {
            parsed.setDepartureLatitude(null);
            parsed.setDepartureLongitude(null);
        }

        parsed.setSummary(resolveSummary(parsed));
    }

    private List<String> filterAllowedThemes(List<String> themes) {
        if (themes == null || themes.isEmpty()) {
            return List.of();
        }
        Set<String> filtered = new LinkedHashSet<>();
        for (String item : themes) {
            if (StringUtils.hasText(item) && ALLOWED_THEMES.contains(item.trim())) {
                filtered.add(item.trim());
            }
        }
        return new ArrayList<>(filtered);
    }

    private String normalizeAllowed(String value, Set<String> allowed) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return allowed.contains(normalized) ? normalized : null;
    }

    private String normalizeTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return TIME_PATTERN.matcher(normalized).matches() ? normalized : null;
    }

    private Double normalizeTripDays(Double tripDays) {
        if (tripDays == null) {
            return null;
        }
        if (Math.abs(tripDays - 0.5D) < 0.01D) {
            return 0.5D;
        }
        if (Math.abs(tripDays - 2.0D) < 0.01D) {
            return 2.0D;
        }
        if (Math.abs(tripDays - 1.0D) < 0.01D) {
            return 1.0D;
        }
        return null;
    }

    private List<String> resolveMustVisitPoiNames(List<String> fromModel,
                                                  String text,
                                                  List<String> poiHints,
                                                  String cityName) {
        Set<String> resolved = new LinkedHashSet<>();
        String ifsCanonicalName = resolveIfsCanonicalName(poiHints);

        if (fromModel != null) {
            for (String item : fromModel) {
                if (!StringUtils.hasText(item)) {
                    continue;
                }
                String normalized = normalizePoiByAlias(item.trim(), ifsCanonicalName);
                normalized = normalizeByDisambiguation(normalized, cityName);
                if (StringUtils.hasText(normalized)) {
                    resolved.add(normalized);
                }
            }
        }

        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String alias : IFS_ALIAS_SKILL) {
            if (lower.contains(alias)) {
                resolved.add(ifsCanonicalName);
                break;
            }
        }

        if (poiHints != null && text != null) {
            for (String poiName : poiHints) {
                if (!StringUtils.hasText(poiName)) {
                    continue;
                }
                if (text.contains(poiName)) {
                    resolved.add(normalizeByDisambiguation(poiName.trim(), cityName));
                }
            }
        }

        if (resolved.isEmpty() && containsIfsAlias(lower)) {
            resolved.add(ifsCanonicalName);
        }

        return new ArrayList<>(resolved);
    }

    private void enrichGeoFields(SmartFillVO parsed, String text) {
        String cityName = resolveCityName(parsed.getCityName(), text);
        parsed.setCityName(cityName);

        if (!StringUtils.hasText(parsed.getDepartureText())) {
            parsed.setDepartureText(extractDepartureText(text));
        }

        if (!StringUtils.hasText(parsed.getDepartureText()) && parsed.getMustVisitPoiNames() != null && !parsed.getMustVisitPoiNames().isEmpty()) {
            parsed.setDepartureText(parsed.getMustVisitPoiNames().get(0));
        }

        if (!StringUtils.hasText(parsed.getDepartureText())) {
            return;
        }

        PlaceDisambiguationService.PlaceResolution departureResolution = placeDisambiguationService == null
                ? PlaceDisambiguationService.PlaceResolution.empty()
                : placeDisambiguationService.disambiguate(parsed.getDepartureText(), cityName, null);

        if (departureResolution.candidates() != null && !departureResolution.candidates().isEmpty()) {
            parsed.setDepartureCandidates(departureResolution.candidates().stream()
                    .map(PlaceDisambiguationService.ResolvedPlace::canonicalName)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .limit(3)
                    .toList());
        }

        if (departureResolution.best() != null) {
            PlaceDisambiguationService.ResolvedPlace best = departureResolution.best();
            if (StringUtils.hasText(best.canonicalName())) {
                parsed.setDepartureText(best.canonicalName());
            }
            if (!departureResolution.clarificationRequired()
                    && best.latitude() != null
                    && best.longitude() != null
                    && isValidCoordinate(best.latitude().doubleValue(), best.longitude().doubleValue())) {
                parsed.setDepartureLatitude(best.latitude().doubleValue());
                parsed.setDepartureLongitude(best.longitude().doubleValue());
                return;
            }
        }

        if (!isValidCoordinate(parsed.getDepartureLatitude(), parsed.getDepartureLongitude()) && geoSearchService != null) {
            GeoPoint point = geoSearchService.geocode(parsed.getDepartureText(), cityName).orElse(null);
            if (point != null && point.valid()) {
                parsed.setDepartureLatitude(point.latitude().doubleValue());
                parsed.setDepartureLongitude(point.longitude().doubleValue());
            }
        }
    }

    private List<String> normalizeCandidateList(List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String candidate : candidates) {
            String value = trimToNull(candidate);
            if (value != null) {
                normalized.add(value);
            }
        }
        return new ArrayList<>(normalized);
    }

    private String normalizePoiByAlias(String value, String ifsCanonicalName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (containsIfsAlias(lower)) {
            return ifsCanonicalName;
        }
        return value;
    }

    private boolean containsIfsAlias(String lower) {
        if (!StringUtils.hasText(lower)) {
            return false;
        }
        for (String alias : IFS_ALIAS_SKILL) {
            if (lower.contains(alias)) {
                return true;
            }
        }
        return false;
    }

    private String resolveIfsCanonicalName(List<String> poiHints) {
        if (poiHints != null) {
            for (String poiName : poiHints) {
                if (!StringUtils.hasText(poiName)) {
                    continue;
                }
                String normalized = poiName.trim();
                if (normalized.toLowerCase(Locale.ROOT).contains("ifs")) {
                    return normalized;
                }
            }
        }
        return IFS_FALLBACK_NAME;
    }

    private String normalizeByDisambiguation(String keyword, String cityName) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        if (placeDisambiguationService == null) {
            return keyword;
        }
        PlaceDisambiguationService.PlaceResolution resolution = placeDisambiguationService.disambiguate(keyword, cityName, null);
        if (resolution.best() == null) {
            return keyword;
        }
        return resolution.best().canonicalName();
    }

    private String extractDepartureText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher fromMatcher = DEPARTURE_FROM_PATTERN.matcher(text);
        if (fromMatcher.find()) {
            return trimToNull(fromMatcher.group(1));
        }
        Matcher stayMatcher = DEPARTURE_STAY_PATTERN.matcher(text);
        if (stayMatcher.find()) {
            return trimToNull(stayMatcher.group(1));
        }
        return null;
    }

    private String resolveCityName(String fromPayload, String textHint) {
        String guessed = cityResolverService == null ? null : cityResolverService.guessCityNameFromText(textHint);
        if (cityResolverService != null) {
            return cityResolverService.resolveCityName(fromPayload, resolveCityCode(guessed));
        }
        if (StringUtils.hasText(fromPayload)) {
            return fromPayload.trim();
        }
        if (StringUtils.hasText(guessed)) {
            return guessed.trim();
        }
        return "成都";
    }

    private String resolveCityCode(String cityName) {
        if (cityResolverService == null) {
            return null;
        }
        return cityResolverService.resolveCityCode(null, cityName);
    }

    private boolean isValidCoordinate(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return Math.abs(latitude) <= 90D && Math.abs(longitude) <= 180D;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private List<String> resolveSummary(SmartFillVO parsed) {
        Set<String> summary = new LinkedHashSet<>();
        if (parsed.getSummary() != null) {
            for (String item : parsed.getSummary()) {
                if (StringUtils.hasText(item)) {
                    summary.add(item.trim());
                }
            }
        }

        if (parsed.getMustVisitPoiNames() != null && !parsed.getMustVisitPoiNames().isEmpty()) {
            summary.add("必去：" + String.join("、", parsed.getMustVisitPoiNames()));
        }
        if (StringUtils.hasText(parsed.getDepartureText())) {
            summary.add("出发地：" + parsed.getDepartureText());
        }
        if (summary.isEmpty() && parsed.getThemes() != null) {
            summary.addAll(parsed.getThemes());
        }
        if (summary.isEmpty() && parsed.getCompanionType() != null) {
            summary.add(parsed.getCompanionType());
        }

        return new ArrayList<>(summary);
    }
}
