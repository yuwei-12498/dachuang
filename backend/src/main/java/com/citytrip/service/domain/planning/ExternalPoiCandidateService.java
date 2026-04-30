package com.citytrip.service.domain.planning;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.service.geo.GeoPoiCandidate;
import com.citytrip.service.geo.GeoSearchService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExternalPoiCandidateService {
    private static final LocalTime DEFAULT_OPEN_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_CLOSE_TIME = LocalTime.of(21, 0);
    private static final BigDecimal DEFAULT_AVG_COST = BigDecimal.valueOf(80);
    private static final int DEFAULT_STAY_DURATION_MINUTES = 90;

    private final GeoSearchService geoSearchService;

    public ExternalPoiCandidateService(GeoSearchService geoSearchService) {
        this.geoSearchService = geoSearchService;
    }

    public List<Poi> recallForReplacement(Poi target, GenerateReqDTO request, int limit) {
        if (target == null || !StringUtils.hasText(target.getName())) {
            return Collections.emptyList();
        }
        String cityName = request == null ? null : request.getCityName();
        int bounded = Math.max(1, Math.min(limit, 8));

        List<GeoPoiCandidate> raw = new ArrayList<>();
        if (StringUtils.hasText(target.getCategory())) {
            safeAddCandidates(raw, geoSearchService.searchByKeyword(target.getCategory(), cityName, bounded));
        }
        if (StringUtils.hasText(target.getDistrict()) && StringUtils.hasText(target.getCategory())) {
            safeAddCandidates(raw, geoSearchService.searchByKeyword(target.getDistrict() + " " + target.getCategory(), cityName, bounded));
        }
        safeAddCandidates(raw, geoSearchService.searchByKeyword(target.getName(), cityName, bounded));

        return dedupeAndMap(raw, target.getCategory(), request, bounded);
    }

    public List<Poi> recallForReplan(List<Poi> currentPois, GenerateReqDTO request, int limit) {
        if (currentPois == null || currentPois.isEmpty()) {
            return Collections.emptyList();
        }
        int bounded = Math.max(1, Math.min(limit, 10));
        String cityName = request == null ? null : request.getCityName();
        Set<String> topCategories = currentPois.stream()
                .filter(Objects::nonNull)
                .map(Poi::getCategory)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (topCategories.isEmpty()) {
            return Collections.emptyList();
        }

        List<GeoPoiCandidate> raw = new ArrayList<>();
        for (String category : topCategories) {
            safeAddCandidates(raw, geoSearchService.searchByKeyword(category, cityName, bounded));
        }
        String district = currentPois.stream()
                .filter(Objects::nonNull)
                .map(Poi::getDistrict)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
        if (StringUtils.hasText(district)) {
            safeAddCandidates(raw, geoSearchService.searchByKeyword(district + " 景点", cityName, bounded));
        }
        return dedupeAndMap(raw, null, request, bounded);
    }

    private List<Poi> dedupeAndMap(List<GeoPoiCandidate> raw,
                                   String fallbackCategory,
                                   GenerateReqDTO request,
                                   int limit) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, GeoPoiCandidate> unique = new LinkedHashMap<>();
        for (GeoPoiCandidate candidate : raw) {
            if (candidate == null || !StringUtils.hasText(candidate.getName())) {
                continue;
            }
            if (candidate.getLatitude() == null || candidate.getLongitude() == null) {
                continue;
            }
            String key = (candidate.getName().trim() + "|" + candidate.getLatitude() + "|" + candidate.getLongitude())
                    .toLowerCase(Locale.ROOT);
            unique.putIfAbsent(key, candidate);
        }
        List<Poi> mapped = unique.values().stream()
                .limit(limit)
                .map(candidate -> enrichBusinessDefaults(candidate, request))
                .map(candidate -> mapToPoi(candidate, fallbackCategory, request))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
        return mapped;
    }

    private GeoPoiCandidate enrichBusinessDefaults(GeoPoiCandidate candidate, GenerateReqDTO request) {
        if (candidate == null || !StringUtils.hasText(candidate.getName())) {
            return candidate;
        }
        String cityName = StringUtils.hasText(candidate.getCityName())
                ? candidate.getCityName()
                : (request == null ? null : request.getCityName());
        try {
            List<GeoPoiCandidate> details = geoSearchService.searchByKeyword(candidate.getName().trim(), cityName, 1);
            GeoPoiCandidate detail = selectBestDetailCandidate(candidate, details);
            if (detail != null && detail != candidate) {
                mergeDetail(candidate, detail);
            }
        } catch (Exception ignored) {
            // 外部 GEO 详情补全失败时继续使用原始候选，路线生成不能因此中断。
        }
        return candidate;
    }

    private GeoPoiCandidate selectBestDetailCandidate(GeoPoiCandidate original, List<GeoPoiCandidate> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        String originalName = normalizeKey(original == null ? null : original.getName());
        return details.stream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.hasText(item.getName()))
                .filter(item -> normalizeKey(item.getName()).equals(originalName))
                .findFirst()
                .orElse(details.stream().filter(Objects::nonNull).findFirst().orElse(null));
    }

    private void mergeDetail(GeoPoiCandidate target, GeoPoiCandidate detail) {
        if (target == null || detail == null) {
            return;
        }
        if (StringUtils.hasText(detail.getExternalId())) {
            target.setExternalId(detail.getExternalId());
        }
        if (!StringUtils.hasText(target.getAddress()) && StringUtils.hasText(detail.getAddress())) {
            target.setAddress(detail.getAddress());
        }
        if (!StringUtils.hasText(target.getCategory()) && StringUtils.hasText(detail.getCategory())) {
            target.setCategory(detail.getCategory());
        }
        if (!StringUtils.hasText(target.getDistrict()) && StringUtils.hasText(detail.getDistrict())) {
            target.setDistrict(detail.getDistrict());
        }
        if (!StringUtils.hasText(target.getCityName()) && StringUtils.hasText(detail.getCityName())) {
            target.setCityName(detail.getCityName());
        }
        if (detail.getScore() != null) {
            target.setScore(detail.getScore());
        }
        if (StringUtils.hasText(detail.getOpeningHours())) {
            target.setOpeningHours(detail.getOpeningHours());
        }
        if (StringUtils.hasText(detail.getOpenTime())) {
            target.setOpenTime(detail.getOpenTime());
        }
        if (StringUtils.hasText(detail.getCloseTime())) {
            target.setCloseTime(detail.getCloseTime());
        }
        if (detail.getAvgCost() != null) {
            target.setAvgCost(detail.getAvgCost());
        }
        if (detail.getStayDurationMinutes() != null) {
            target.setStayDurationMinutes(detail.getStayDurationMinutes());
        }
    }

    private Poi mapToPoi(GeoPoiCandidate candidate, String fallbackCategory, GenerateReqDTO request) {
        if (candidate == null || !StringUtils.hasText(candidate.getName())) {
            return null;
        }
        Poi poi = new Poi();
        poi.setId(buildTemporaryPoiId(candidate));
        poi.setExternalId(candidate.getExternalId());
        poi.setSourceType("external");
        poi.setCityCode(request == null ? null : request.getCityCode());
        poi.setCityName(StringUtils.hasText(candidate.getCityName()) ? candidate.getCityName() : (request == null ? null : request.getCityName()));
        poi.setName(candidate.getName().trim());
        poi.setCategory(StringUtils.hasText(candidate.getCategory()) ? candidate.getCategory().trim() : fallbackCategory);
        poi.setDistrict(candidate.getDistrict());
        poi.setAddress(candidate.getAddress());
        poi.setLatitude(candidate.getLatitude().setScale(6, RoundingMode.HALF_UP));
        poi.setLongitude(candidate.getLongitude().setScale(6, RoundingMode.HALF_UP));
        poi.setOpenTime(resolveOpenTime(candidate));
        poi.setCloseTime(resolveCloseTime(candidate));
        poi.setAvgCost(resolveAvgCost(candidate));
        poi.setStayDuration(resolveStayDuration(candidate));
        poi.setIndoor(0);
        poi.setNightAvailable(0);
        poi.setRainFriendly(0);
        poi.setWalkingLevel("中");
        poi.setTags(StringUtils.hasText(candidate.getCategory()) ? candidate.getCategory() : fallbackCategory);
        poi.setSuitableFor("朋友,情侣,独自");
        poi.setDescription("External candidate from GEO API");
        poi.setPriorityScore(BigDecimal.valueOf(5.8));
        poi.setCrowdPenalty(BigDecimal.valueOf(0.15));
        poi.setTempScore(candidate.getScore() == null ? 10D : 10D + candidate.getScore() * 5D);
        poi.setOperatingStatus("CHECK_REQUIRED");
        poi.setAvailabilityNote(buildAvailabilityNote(candidate));
        poi.setAvailableOnTripDate(Boolean.TRUE);
        poi.setStatusStale(Boolean.TRUE);
        return poi;
    }

    private LocalTime resolveOpenTime(GeoPoiCandidate candidate) {
        LocalTime explicit = parseFirstTime(candidate == null ? null : candidate.getOpenTime());
        if (explicit != null) {
            return explicit;
        }
        List<LocalTime> hours = parseOpeningHours(candidate == null ? null : candidate.getOpeningHours());
        return hours.isEmpty() ? DEFAULT_OPEN_TIME : hours.get(0);
    }

    private LocalTime resolveCloseTime(GeoPoiCandidate candidate) {
        LocalTime explicit = parseFirstTime(candidate == null ? null : candidate.getCloseTime());
        if (explicit != null) {
            return explicit;
        }
        List<LocalTime> hours = parseOpeningHours(candidate == null ? null : candidate.getOpeningHours());
        return hours.size() < 2 ? DEFAULT_CLOSE_TIME : hours.get(1);
    }

    private BigDecimal resolveAvgCost(GeoPoiCandidate candidate) {
        BigDecimal value = candidate == null ? null : candidate.getAvgCost();
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.valueOf(10000)) > 0) {
            return DEFAULT_AVG_COST;
        }
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private int resolveStayDuration(GeoPoiCandidate candidate) {
        Integer minutes = candidate == null ? null : candidate.getStayDurationMinutes();
        if (minutes == null || minutes < 20 || minutes > 360) {
            return DEFAULT_STAY_DURATION_MINUTES;
        }
        return minutes;
    }

    private String buildAvailabilityNote(GeoPoiCandidate candidate) {
        if (hasBusinessDefault(candidate)) {
            return "外部POI，营业时间、人均消费或建议停留已按地图信息补全，出发前仍建议以地图实时信息为准。";
        }
        return "外部POI，地图未返回完整营业时间、人均消费和建议停留，已使用默认估算；出发前请以地图实时信息为准。";
    }

    private boolean hasBusinessDefault(GeoPoiCandidate candidate) {
        return candidate != null
                && (StringUtils.hasText(candidate.getOpeningHours())
                || StringUtils.hasText(candidate.getOpenTime())
                || StringUtils.hasText(candidate.getCloseTime())
                || candidate.getAvgCost() != null
                || candidate.getStayDurationMinutes() != null);
    }

    private List<LocalTime> parseOpeningHours(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        List<LocalTime> times = new ArrayList<>();
        String normalized = text.trim().replace('：', ':');
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(normalized);
        while (matcher.find() && times.size() < 2) {
            LocalTime parsed = parseHourMinute(matcher.group(1), matcher.group(2));
            if (parsed != null) {
                times.add(parsed);
            }
        }
        return times;
    }

    private LocalTime parseFirstTime(String text) {
        List<LocalTime> times = parseOpeningHours(text);
        return times.isEmpty() ? null : times.get(0);
    }

    private LocalTime parseHourMinute(String hourText, String minuteText) {
        try {
            int hour = Integer.parseInt(hourText);
            int minute = Integer.parseInt(minuteText);
            if (hour == 24 && minute == 0) {
                return LocalTime.of(23, 59);
            }
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return LocalTime.of(hour, minute);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String normalizeKey(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    private long buildTemporaryPoiId(GeoPoiCandidate candidate) {
        String seed = (candidate.getName() == null ? "" : candidate.getName())
                + "|"
                + (candidate.getLatitude() == null ? "" : candidate.getLatitude().toPlainString())
                + "|"
                + (candidate.getLongitude() == null ? "" : candidate.getLongitude().toPlainString());
        long hash = Integer.toUnsignedLong(seed.hashCode());
        return -(10_000_000L + hash);
    }

    private void safeAddCandidates(List<GeoPoiCandidate> container, List<GeoPoiCandidate> incoming) {
        if (container == null || incoming == null || incoming.isEmpty()) {
            return;
        }
        container.addAll(incoming);
    }
}
