package com.citytrip.service.impl;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.service.PoiService;
import com.citytrip.service.TravelTimeService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ItineraryRouteOptimizer {

    public static final int DEFAULT_START_MINUTE = 9 * 60;
    public static final int DEFAULT_END_MINUTE = 18 * 60;
    private static final int CANDIDATE_LIMIT = 18;
    private static final int BEAM_WIDTH = 80;
    private static final double SCORE_WEIGHT = 6.0;

    private final PoiService poiService;
    private final TravelTimeService travelTimeService;

    public ItineraryRouteOptimizer(PoiService poiService, TravelTimeService travelTimeService) {
        this.poiService = poiService;
        this.travelTimeService = travelTimeService;
    }

    public GenerateReqDTO normalizeRequest(GenerateReqDTO req) {
        GenerateReqDTO normalized = new GenerateReqDTO();
        normalized.setTripDays(req == null || req.getTripDays() == null ? 1.0 : req.getTripDays());
        normalized.setTripDate(textOrDefault(req == null ? null : req.getTripDate(), LocalDate.now().toString()));
        normalized.setBudgetLevel(req == null ? null : req.getBudgetLevel());
        normalized.setThemes(req == null || req.getThemes() == null ? Collections.emptyList() : req.getThemes());
        normalized.setIsRainy(req != null && Boolean.TRUE.equals(req.getIsRainy()));
        normalized.setIsNight(req != null && Boolean.TRUE.equals(req.getIsNight()));
        normalized.setWalkingLevel(textOrDefault(req == null ? null : req.getWalkingLevel(), "\u4E2D"));
        normalized.setCompanionType(req == null ? null : req.getCompanionType());
        normalized.setStartTime(textOrDefault(req == null ? null : req.getStartTime(), "09:00"));
        normalized.setEndTime(textOrDefault(req == null ? null : req.getEndTime(), "18:00"));
        return normalized;
    }

    public List<Poi> prepareCandidates(List<Poi> source, GenerateReqDTO req, boolean applyLimit) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<Poi> filtered = source.stream()
                .filter(Objects::nonNull)
                .filter(poi -> matchesWeatherConstraint(req, poi))
                .filter(poi -> matchesWalkingConstraint(req, poi))
                .collect(Collectors.toCollection(ArrayList::new));
        poiService.enrichOperatingStatus(filtered, resolveTripDate(req));
        filtered.forEach(poi -> poi.setTempScore(scorePoi(req, poi)));
        filtered.sort((left, right) -> {
            int byScore = Double.compare(right.getTempScore(), left.getTempScore());
            if (byScore != 0) {
                return byScore;
            }
            BigDecimal rightPriority = right.getPriorityScore() == null ? BigDecimal.ZERO : right.getPriorityScore();
            BigDecimal leftPriority = left.getPriorityScore() == null ? BigDecimal.ZERO : left.getPriorityScore();
            return rightPriority.compareTo(leftPriority);
        });
        List<Poi> available = filtered.stream()
                .filter(poi -> !Boolean.FALSE.equals(poi.getAvailableOnTripDate()))
                .collect(Collectors.toCollection(ArrayList::new));
        return applyLimit && available.size() > CANDIDATE_LIMIT
                ? new ArrayList<>(available.subList(0, CANDIDATE_LIMIT))
                : available;
    }

    public RouteOption bestRoute(List<Poi> candidates, GenerateReqDTO req, int maxStops) {
        List<RouteOption> ranked = rankRoutes(candidates, req, maxStops);
        return ranked.isEmpty() ? new RouteOption(Collections.emptyList(), "", 0D) : ranked.get(0);
    }

    public List<RouteOption> rankRoutes(List<Poi> candidates, GenerateReqDTO req, int maxStops) {
        if (candidates == null || candidates.isEmpty() || maxStops <= 0) {
            return Collections.emptyList();
        }
        int startMinute = parseTimeMinutes(req.getStartTime(), DEFAULT_START_MINUTE);
        int endMinute = parseTimeMinutes(req.getEndTime(), DEFAULT_END_MINUTE);
        List<SearchState> beam = List.of(SearchState.seed(startMinute));
        List<SearchState> completed = new ArrayList<>();

        for (int depth = 0; depth < maxStops; depth++) {
            Map<String, SearchState> nextLevel = new HashMap<>();
            for (SearchState state : beam) {
                for (int i = 0; i < candidates.size(); i++) {
                    if ((state.mask & (1L << i)) != 0L) {
                        continue;
                    }
                    SearchState next = expand(state, i, candidates, startMinute, endMinute);
                    if (next == null) {
                        continue;
                    }
                    keepBetter(nextLevel, next);
                    completed.add(next);
                }
            }
            if (nextLevel.isEmpty()) {
                break;
            }
            beam = nextLevel.values().stream()
                    .sorted(this::compareState)
                    .limit(BEAM_WIDTH)
                    .toList();
        }

        Map<String, RouteOption> ranked = new java.util.LinkedHashMap<>();
        completed.stream()
                .filter(state -> !state.path.isEmpty())
                .sorted(this::compareState)
                .map(state -> toRouteOption(state, candidates))
                .forEach(route -> ranked.putIfAbsent(route.signature(), route));
        return new ArrayList<>(ranked.values());
    }

    public double replacementScore(Poi targetPoi, Poi candidate) {
        double score = candidate.getTempScore() == null ? 0 : candidate.getTempScore();
        if (Objects.equals(targetPoi.getCategory(), candidate.getCategory())) {
            score += 6.0;
        }
        if (Objects.equals(targetPoi.getDistrict(), candidate.getDistrict())) {
            score += 4.0;
        }
        score += Math.max(0, 20 - travelTimeService.estimateTravelTimeMinutes(targetPoi, candidate)) / 3.0;
        return score;
    }

    public LocalDate resolveTripDate(GenerateReqDTO req) {
        if (req == null || !StringUtils.hasText(req.getTripDate())) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(req.getTripDate());
        } catch (DateTimeParseException ex) {
            return LocalDate.now();
        }
    }

    public int parseTimeMinutes(String timeStr, int defaultMinutes) {
        if (!StringUtils.hasText(timeStr) || !timeStr.contains(":")) {
            return defaultMinutes;
        }
        try {
            String[] parts = timeStr.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (RuntimeException ex) {
            return defaultMinutes;
        }
    }

    public int resolveOpenMinute(Poi poi, int defaultMinute) {
        LocalTime openTime = poi.getOpenTime();
        return openTime == null ? defaultMinute : openTime.getHour() * 60 + openTime.getMinute();
    }

    public int resolveCloseMinute(Poi poi, int defaultMinute) {
        LocalTime closeTime = poi.getCloseTime();
        return closeTime == null ? defaultMinute : closeTime.getHour() * 60 + closeTime.getMinute();
    }

    public String formatTime(int totalMinutes) {
        int hour = (totalMinutes / 60) % 24;
        int minute = totalMinutes % 60;
        return String.format("%02d:%02d", hour, minute);
    }

    public String signature(Collection<Poi> pois) {
        return pois.stream().map(Poi::getId).map(String::valueOf).collect(Collectors.joining("-"));
    }

    private boolean matchesWeatherConstraint(GenerateReqDTO req, Poi poi) {
        return !Boolean.TRUE.equals(req.getIsRainy())
                || Integer.valueOf(1).equals(poi.getIndoor())
                || Integer.valueOf(1).equals(poi.getRainFriendly());
    }

    private boolean matchesWalkingConstraint(GenerateReqDTO req, Poi poi) {
        return walkingRank(req.getWalkingLevel()) >= walkingRank(poi.getWalkingLevel()) - 1;
    }

    private int walkingRank(String value) {
        if (!StringUtils.hasText(value)) {
            return 2;
        }
        if (value.contains("\u4F4E") || value.equalsIgnoreCase("low")) {
            return 1;
        }
        if (value.contains("\u9AD8") || value.equalsIgnoreCase("high")) {
            return 3;
        }
        return 2;
    }

    private double scorePoi(GenerateReqDTO req, Poi poi) {
        double score = poi.getPriorityScore() == null ? 7.5 : poi.getPriorityScore().doubleValue() * 2.5;
        if (req.getThemes() != null && poi.getTags() != null) {
            score += req.getThemes().stream()
                    .filter(theme -> StringUtils.hasText(theme) && poi.getTags().contains(theme))
                    .count() * 3.5;
        }
        if (StringUtils.hasText(req.getCompanionType())
                && StringUtils.hasText(poi.getSuitableFor())
                && poi.getSuitableFor().contains(req.getCompanionType())) {
            score += 2.5;
        }
        if (Boolean.TRUE.equals(req.getIsNight()) && Integer.valueOf(1).equals(poi.getNightAvailable())) {
            score += 2.0;
        }
        if (Boolean.TRUE.equals(req.getIsRainy())
                && (Integer.valueOf(1).equals(poi.getIndoor()) || Integer.valueOf(1).equals(poi.getRainFriendly()))) {
            score += 1.5;
        }
        if (walkingRank(req.getWalkingLevel()) >= walkingRank(poi.getWalkingLevel())) {
            score += 1.0;
        }
        if (Boolean.TRUE.equals(poi.getStatusStale())) {
            score -= 2.0;
        }
        int stay = poi.getStayDuration() == null ? 90 : poi.getStayDuration();
        if (stay > 180) {
            score -= Math.min(3.0, (stay - 180) / 45.0);
        }
        if (poi.getOpenTime() == null || poi.getCloseTime() == null) {
            score -= 1.0;
        }
        return Math.max(score, 1.0);
    }

    private SearchState expand(SearchState state, int nextIndex, List<Poi> candidates, int startMinute, int endMinute) {
        Poi nextPoi = candidates.get(nextIndex);
        Poi prevPoi = state.lastIndex < 0 ? null : candidates.get(state.lastIndex);
        int travelTime = prevPoi == null ? 0 : travelTimeService.estimateTravelTimeMinutes(prevPoi, nextPoi);
        int arrival = state.currentMinute + travelTime;
        int visitStart = Math.max(arrival, resolveOpenMinute(nextPoi, startMinute));
        int waitTime = Math.max(0, visitStart - arrival);
        int visitEnd = visitStart + (nextPoi.getStayDuration() == null ? 90 : nextPoi.getStayDuration());
        if (visitEnd > resolveCloseMinute(nextPoi, endMinute) || visitEnd > endMinute) {
            return null;
        }
        List<Integer> path = new ArrayList<>(state.path);
        path.add(nextIndex);
        double utility = state.utility + nextPoi.getTempScore() * SCORE_WEIGHT - travelTime - waitTime * 0.5;
        return new SearchState(state.mask | (1L << nextIndex), nextIndex, visitEnd, utility, path);
    }

    private void keepBetter(Map<String, SearchState> level, SearchState candidate) {
        String key = candidate.mask + "-" + candidate.lastIndex;
        SearchState existing = level.get(key);
        if (existing == null || compareState(candidate, existing) < 0) {
            level.put(key, candidate);
        }
    }

    private int compareState(SearchState left, SearchState right) {
        int byUtility = Double.compare(right.utility, left.utility);
        if (byUtility != 0) {
            return byUtility;
        }
        int byStops = Integer.compare(right.path.size(), left.path.size());
        if (byStops != 0) {
            return byStops;
        }
        return Integer.compare(left.currentMinute, right.currentMinute);
    }

    private RouteOption toRouteOption(SearchState state, List<Poi> candidates) {
        List<Poi> path = state.path.stream().map(candidates::get).collect(Collectors.toList());
        return new RouteOption(path, signature(path), state.utility);
    }

    private String textOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private static final class SearchState {
        private final long mask;
        private final int lastIndex;
        private final int currentMinute;
        private final double utility;
        private final List<Integer> path;

        private SearchState(long mask, int lastIndex, int currentMinute, double utility, List<Integer> path) {
            this.mask = mask;
            this.lastIndex = lastIndex;
            this.currentMinute = currentMinute;
            this.utility = utility;
            this.path = path;
        }

        private static SearchState seed(int startMinute) {
            return new SearchState(0L, -1, startMinute, 0D, Collections.emptyList());
        }
    }

    public record RouteOption(List<Poi> path, String signature, double utility) {
    }
}
