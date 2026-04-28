package com.citytrip.service.impl;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.service.PoiService;
import com.citytrip.service.TravelTimeService;
import com.citytrip.service.geo.CityResolverService;
import com.citytrip.service.geo.GeoPoint;
import com.citytrip.service.geo.GeoSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ItineraryRouteOptimizer {

    public static final int DEFAULT_START_MINUTE = 9 * 60;
    public static final int DEFAULT_END_MINUTE = 18 * 60;
    public static final String DEFAULT_CITY_NAME = "成都";

    private static final int CANDIDATE_LIMIT = 18;
    private static final int EXACT_DP_THRESHOLD = 15;
    private static final int BEAM_WIDTH = 80;

    private static final double SCORE_WEIGHT = 6.0D;
    private static final double WAIT_PENALTY_WEIGHT = 0.5D;
    private static final double TRAVEL_PENALTY_WEIGHT = 1.0D;
    private static final double CROWD_PENALTY_WEIGHT = 4.0D;
    private static final double CANDIDATE_CROWD_SCORE_WEIGHT = 1.5D;
    private static final double FIRST_LEG_TIME_PENALTY_WEIGHT = 0.9D;
    private static final double FIRST_LEG_DISTANCE_PENALTY_WEIGHT = 4.8D;
    private static final double FIRST_LEG_TRANSFER_PENALTY_WEIGHT = 7.0D;

    private final PoiService poiService;
    private final TravelTimeService travelTimeService;
    @Autowired(required = false)
    private GeoSearchService geoSearchService;
    @Autowired(required = false)
    private CityResolverService cityResolverService;

    public ItineraryRouteOptimizer(PoiService poiService, TravelTimeService travelTimeService) {
        this.poiService = poiService;
        this.travelTimeService = travelTimeService;
    }

    public GenerateReqDTO normalizeRequest(GenerateReqDTO req) {
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

    public List<Poi> prepareCandidates(List<Poi> source, GenerateReqDTO req, boolean applyLimit) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        GenerateReqDTO normalized = normalizeRequest(req);
        List<Poi> filtered = source.stream()
                .filter(Objects::nonNull)
                .filter(poi -> matchesWeatherConstraint(normalized, poi))
                .filter(poi -> matchesWalkingConstraint(normalized, poi))
                .collect(Collectors.toCollection(ArrayList::new));
        poiService.enrichOperatingStatus(filtered, resolveTripDate(normalized));
        filtered.forEach(poi -> poi.setTempScore(scorePoi(normalized, poi)));
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

        GenerateReqDTO normalized = normalizeRequest(req);
        List<Poi> searchCandidates = candidates;
        if (candidates.size() > Long.SIZE) {
            searchCandidates = capCandidatesForBeamMask(candidates, normalized);
        }
        if (searchCandidates.isEmpty()) {
            return Collections.emptyList();
        }

        int normalizedMaxStops = Math.min(maxStops, searchCandidates.size());
        int startMinute = parseTimeMinutes(normalized.getStartTime(), DEFAULT_START_MINUTE);
        int endMinute = parseTimeMinutes(normalized.getEndTime(), DEFAULT_END_MINUTE);
        StartAccessProfile[] startAccessProfiles = buildStartAccessProfiles(searchCandidates, normalized);
        int[][] travelMatrix = buildTravelTimeMatrix(searchCandidates);

        if (searchCandidates.size() <= EXACT_DP_THRESHOLD) {
            return prioritizeMustVisitRoutes(
                    rankRoutesWithParetoDp(searchCandidates, normalized, normalizedMaxStops, startMinute, endMinute, travelMatrix, startAccessProfiles),
                    normalized
            );
        }
        return prioritizeMustVisitRoutes(
                rankRoutesWithBeamSearch(searchCandidates, normalized, normalizedMaxStops, startMinute, endMinute, travelMatrix, startAccessProfiles),
                normalized
        );
    }

    public double replacementScore(Poi targetPoi, Poi candidate) {
        if (targetPoi == null || candidate == null) {
            return Double.NEGATIVE_INFINITY;
        }
        double score = candidate.getTempScore() == null ? 0 : candidate.getTempScore();
        if (Objects.equals(targetPoi.getCategory(), candidate.getCategory())) {
            score += 6.0;
        }
        if (Objects.equals(targetPoi.getDistrict(), candidate.getDistrict())) {
            score += 4.0;
        }
        score += Math.max(0, 20 - travelTimeService.estimateTravelTimeMinutes(targetPoi, candidate)) / 3.0;
        score -= resolveBaseCrowdPenalty(candidate);
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
            if (parts.length < 2) {
                return defaultMinutes;
            }
            long hours = Long.parseLong(parts[0].trim());
            long minutes = Long.parseLong(parts[1].trim());
            long total = Math.addExact(Math.multiplyExact(hours, 60L), minutes);
            return clampToInt(total);
        } catch (RuntimeException ex) {
            return defaultMinutes;
        }
    }

    public int resolveOpenMinute(Poi poi, int defaultMinute) {
        if (poi == null) {
            return defaultMinute;
        }
        LocalTime openTime = poi.getOpenTime();
        return openTime == null ? defaultMinute : openTime.getHour() * 60 + openTime.getMinute();
    }

    public int resolveCloseMinute(Poi poi, int defaultMinute) {
        if (poi == null) {
            return defaultMinute;
        }
        LocalTime closeTime = poi.getCloseTime();
        return closeTime == null ? defaultMinute : closeTime.getHour() * 60 + closeTime.getMinute();
    }

    public String formatTime(int totalMinutes) {
        int normalizedMinutes = Math.floorMod(totalMinutes, 24 * 60);
        int hour = normalizedMinutes / 60;
        int minute = normalizedMinutes % 60;
        return String.format("%02d:%02d", hour, minute);
    }

    public String signature(Collection<Poi> pois) {
        if (pois == null || pois.isEmpty()) {
            return "";
        }
        return pois.stream()
                .filter(Objects::nonNull)
                .map(Poi::getId)
                .map(String::valueOf)
                .collect(Collectors.joining("-"));
    }

    private List<RouteOption> rankRoutesWithParetoDp(List<Poi> candidates,
                                                     GenerateReqDTO req,
                                                     int maxStops,
                                                     int startMinute,
                                                     int endMinute,
                                                     int[][] travelMatrix,
                                                     StartAccessProfile[] startAccessProfiles) {
        ArrayDeque<DpLabel> queue = new ArrayDeque<>();
        Map<Long, List<DpLabel>> paretoFrontier = new HashMap<>();

        DpLabel seed = DpLabel.seed(startMinute);
        queue.offer(seed);
        paretoFrontier.computeIfAbsent(dpBucketKey(seed.mask, seed.lastIndex), key -> new ArrayList<>()).add(seed);

        while (!queue.isEmpty()) {
            DpLabel current = queue.poll();
            if (current.stopCount >= maxStops) {
                continue;
            }
            for (int nextIndex = 0; nextIndex < candidates.size(); nextIndex++) {
                if ((current.mask & (1 << nextIndex)) != 0) {
                    continue;
                }
                DpLabel next = expandDp(current, nextIndex, candidates, req, startMinute, endMinute, travelMatrix, startAccessProfiles);
                if (next == null) {
                    continue;
                }
                List<DpLabel> bucket = paretoFrontier.computeIfAbsent(
                        dpBucketKey(next.mask, next.lastIndex),
                        key -> new ArrayList<>()
                );
                if (insertIfNonDominated(bucket, next)) {
                    queue.offer(next);
                }
            }
        }

        List<DpLabel> finalStates = paretoFrontier.values().stream()
                .flatMap(Collection::stream)
                .filter(label -> label.stopCount > 0)
                .sorted(this::compareDpLabel)
                .toList();

        return deduplicateRouteOptions(finalStates.stream()
                .map(label -> toRouteOption(label, candidates))
                .toList());
    }

    private List<RouteOption> rankRoutesWithBeamSearch(List<Poi> candidates,
                                                       GenerateReqDTO req,
                                                       int maxStops,
                                                       int startMinute,
                                                       int endMinute,
                                                       int[][] travelMatrix,
                                                       StartAccessProfile[] startAccessProfiles) {
        List<SearchState> beam = List.of(SearchState.seed(startMinute));
        List<SearchState> completed = new ArrayList<>();

        for (int depth = 0; depth < maxStops; depth++) {
            Map<String, SearchState> nextLevel = new HashMap<>();
            for (SearchState state : beam) {
                for (int nextIndex = 0; nextIndex < candidates.size(); nextIndex++) {
                    if ((state.mask & (1L << nextIndex)) != 0L) {
                        continue;
                    }
                    SearchState next = expandBeam(state, nextIndex, candidates, req, startMinute, endMinute, travelMatrix, startAccessProfiles);
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
                    .sorted(this::compareBeamState)
                    .limit(BEAM_WIDTH)
                    .toList();
        }

        List<RouteOption> ranked = completed.stream()
                .filter(state -> !state.path.isEmpty())
                .sorted(this::compareBeamState)
                .map(state -> toRouteOption(state, candidates))
                .toList();
        return deduplicateRouteOptions(ranked);
    }

    private int[][] buildTravelTimeMatrix(List<Poi> candidates) {
        int size = candidates.size();
        int[][] matrix = new int[size][size];
        for (int i = 0; i < size; i++) {
            matrix[i][i] = 0;
            for (int j = i + 1; j < size; j++) {
                int minutes = Math.max(0, travelTimeService.estimateTravelTimeMinutes(candidates.get(i), candidates.get(j)));
                matrix[i][j] = minutes;
                matrix[j][i] = Math.max(0, travelTimeService.estimateTravelTimeMinutes(candidates.get(j), candidates.get(i)));
            }
        }
        return matrix;
    }

    private StartAccessProfile[] buildStartAccessProfiles(List<Poi> candidates, GenerateReqDTO req) {
        StartAccessProfile[] profiles = new StartAccessProfile[candidates.size()];
        Poi departurePoi = buildDeparturePoi(req);
        for (int i = 0; i < candidates.size(); i++) {
            if (departurePoi == null) {
                profiles[i] = new StartAccessProfile(0, null, null, 0D);
                continue;
            }
            TravelTimeService.TravelLegEstimate estimate = travelTimeService.estimateTravelLeg(departurePoi, candidates.get(i));
            int minutes = estimate == null
                    ? Math.max(0, travelTimeService.estimateTravelTimeMinutes(departurePoi, candidates.get(i)))
                    : Math.max(0, estimate.estimatedMinutes());
            BigDecimal distanceKm = estimate == null ? null : estimate.estimatedDistanceKm();
            String transportMode = estimate == null ? null : estimate.transportMode();
            double accessPenalty = resolveStartAccessPenalty(req, minutes, distanceKm, transportMode);
            profiles[i] = new StartAccessProfile(minutes, distanceKm, transportMode, accessPenalty);
        }
        return profiles;
    }

    private DpLabel expandDp(DpLabel state,
                             int nextIndex,
                             List<Poi> candidates,
                             GenerateReqDTO req,
                             int startMinute,
                             int endMinute,
                             int[][] travelMatrix,
                             StartAccessProfile[] startAccessProfiles) {
        Poi nextPoi = candidates.get(nextIndex);
        StartAccessProfile startAccessProfile = resolveStartAccessProfile(startAccessProfiles, nextIndex);
        int travelTime = Math.max(0, state.lastIndex < 0 ? startAccessProfile.travelMinutes() : travelMatrix[state.lastIndex][nextIndex]);
        int arrival = safeAddMinutes(state.currentMinute, travelTime);
        int visitStart = Math.max(arrival, resolveOpenMinute(nextPoi, startMinute));
        int waitTime = Math.max(0, visitStart - arrival);
        int stayDuration = normalizeStayDuration(nextPoi.getStayDuration());
        int visitEnd = safeAddMinutes(visitStart, stayDuration);
        if (visitEnd > resolveCloseMinute(nextPoi, endMinute) || visitEnd > endMinute) {
            return null;
        }

        double utility = state.utility
                + resolveRouteValue(req, nextPoi) * SCORE_WEIGHT
                - travelTime * TRAVEL_PENALTY_WEIGHT
                - waitTime * WAIT_PENALTY_WEIGHT
                - resolveVisitCrowdPenalty(nextPoi, req, visitStart) * CROWD_PENALTY_WEIGHT;
        if (state.lastIndex < 0) {
            utility -= startAccessProfile.accessPenalty();
        }

        return new DpLabel(
                state.mask | (1 << nextIndex),
                nextIndex,
                visitEnd,
                state.totalCost + resolvePoiCost(nextPoi),
                utility,
                state.stopCount + 1,
                state
        );
    }

    private SearchState expandBeam(SearchState state,
                                   int nextIndex,
                                   List<Poi> candidates,
                                   GenerateReqDTO req,
                                   int startMinute,
                                   int endMinute,
                                   int[][] travelMatrix,
                                   StartAccessProfile[] startAccessProfiles) {
        Poi nextPoi = candidates.get(nextIndex);
        StartAccessProfile startAccessProfile = resolveStartAccessProfile(startAccessProfiles, nextIndex);
        int travelTime = Math.max(0, state.lastIndex < 0 ? startAccessProfile.travelMinutes() : travelMatrix[state.lastIndex][nextIndex]);
        int arrival = safeAddMinutes(state.currentMinute, travelTime);
        int visitStart = Math.max(arrival, resolveOpenMinute(nextPoi, startMinute));
        int waitTime = Math.max(0, visitStart - arrival);
        int visitEnd = safeAddMinutes(visitStart, normalizeStayDuration(nextPoi.getStayDuration()));
        if (visitEnd > resolveCloseMinute(nextPoi, endMinute) || visitEnd > endMinute) {
            return null;
        }
        List<Integer> path = new ArrayList<>(state.path);
        path.add(nextIndex);
        double utility = state.utility
                + resolveRouteValue(req, nextPoi) * SCORE_WEIGHT
                - travelTime * TRAVEL_PENALTY_WEIGHT
                - waitTime * WAIT_PENALTY_WEIGHT
                - resolveVisitCrowdPenalty(nextPoi, req, visitStart) * CROWD_PENALTY_WEIGHT;
        if (state.lastIndex < 0) {
            utility -= startAccessProfile.accessPenalty();
        }
        return new SearchState(state.mask | (1L << nextIndex), nextIndex, visitEnd, utility, path);
    }

    private StartAccessProfile resolveStartAccessProfile(StartAccessProfile[] profiles, int index) {
        if (profiles == null || index < 0 || index >= profiles.length || profiles[index] == null) {
            return new StartAccessProfile(0, null, null, 0D);
        }
        return profiles[index];
    }

    private double resolveStartAccessPenalty(GenerateReqDTO req,
                                             int minutes,
                                             BigDecimal distanceKm,
                                             String transportMode) {
        if (!hasDepartureCoordinate(req)) {
            return 0D;
        }
        double normalizedDistanceKm = distanceKm == null ? 0D : Math.max(0D, distanceKm.doubleValue());
        double distancePenalty = normalizedDistanceKm * FIRST_LEG_DISTANCE_PENALTY_WEIGHT;
        double distanceThresholdPenalty = resolveStartDistanceThresholdPenalty(normalizedDistanceKm);
        double timeThresholdPenalty = resolveStartTimeThresholdPenalty(minutes);
        double transferPenalty = resolveStartModePenalty(req, transportMode);
        return Math.max(0, minutes) * FIRST_LEG_TIME_PENALTY_WEIGHT
                + distancePenalty
                + distanceThresholdPenalty
                + timeThresholdPenalty
                + transferPenalty;
    }

    private double resolveStartModePenalty(GenerateReqDTO req, String transportMode) {
        if (!StringUtils.hasText(transportMode)) {
            return 2.0D;
        }
        String normalizedMode = transportMode.trim().toLowerCase(Locale.ROOT);
        if (normalizedMode.contains("步行") || normalizedMode.contains("walk")) {
            return "low".equalsIgnoreCase(req == null ? null : req.getWalkingLevel()) ? 1.5D : 0D;
        }
        if (normalizedMode.contains("骑行") || normalizedMode.contains("bike") || normalizedMode.contains("cycle")) {
            return 1.5D;
        }
        if (normalizedMode.contains("地铁") || normalizedMode.contains("metro") || normalizedMode.contains("subway")) {
            return FIRST_LEG_TRANSFER_PENALTY_WEIGHT;
        }
        if (normalizedMode.contains("公交") || normalizedMode.contains("bus") || normalizedMode.contains("transit")) {
            return FIRST_LEG_TRANSFER_PENALTY_WEIGHT + 1.5D;
        }
        if (normalizedMode.contains("打车") || normalizedMode.contains("taxi") || normalizedMode.contains("drive")) {
            return FIRST_LEG_TRANSFER_PENALTY_WEIGHT + 2.5D;
        }
        return 3.0D;
    }

    private double resolveStartDistanceThresholdPenalty(double distanceKm) {
        double penalty = 0D;
        if (distanceKm > 2.5D) {
            penalty += (distanceKm - 2.5D) * 2.4D;
        }
        if (distanceKm > 5D) {
            penalty += 10D + (distanceKm - 5D) * 3.6D;
        }
        if (distanceKm > 9D) {
            penalty += 20D + (distanceKm - 9D) * 5.2D;
        }
        return penalty;
    }

    private double resolveStartTimeThresholdPenalty(int minutes) {
        int safeMinutes = Math.max(0, minutes);
        double penalty = 0D;
        if (safeMinutes > 15) {
            penalty += (safeMinutes - 15) * 0.9D;
        }
        if (safeMinutes > 28) {
            penalty += 8D + (safeMinutes - 28) * 1.25D;
        }
        if (safeMinutes > 40) {
            penalty += 16D + (safeMinutes - 40) * 1.8D;
        }
        return penalty;
    }

    private boolean hasDepartureCoordinate(GenerateReqDTO req) {
        return req != null
                && req.getDepartureLatitude() != null
                && req.getDepartureLongitude() != null
                && Math.abs(req.getDepartureLatitude()) <= 90D
                && Math.abs(req.getDepartureLongitude()) <= 180D;
    }

    private boolean insertIfNonDominated(List<DpLabel> bucket, DpLabel candidate) {
        for (DpLabel existing : bucket) {
            if (dominates(existing, candidate)) {
                return false;
            }
        }
        bucket.removeIf(existing -> dominates(candidate, existing));
        bucket.add(candidate);
        return true;
    }

    private boolean dominates(DpLabel left, DpLabel right) {
        return left.currentMinute <= right.currentMinute
                && left.totalCost <= right.totalCost + 1e-6
                && left.utility >= right.utility - 1e-6;
    }

    private long dpBucketKey(int mask, int lastIndex) {
        return (((long) mask) << 6) | (lastIndex + 1L);
    }

    private void keepBetter(Map<String, SearchState> level, SearchState candidate) {
        String key = candidate.mask + "-" + candidate.lastIndex;
        SearchState existing = level.get(key);
        if (existing == null || compareBeamState(candidate, existing) < 0) {
            level.put(key, candidate);
        }
    }

    private int compareDpLabel(DpLabel left, DpLabel right) {
        int byUtility = Double.compare(right.utility, left.utility);
        if (byUtility != 0) {
            return byUtility;
        }
        int byStops = Integer.compare(right.stopCount, left.stopCount);
        if (byStops != 0) {
            return byStops;
        }
        int byTime = Integer.compare(left.currentMinute, right.currentMinute);
        if (byTime != 0) {
            return byTime;
        }
        return Double.compare(left.totalCost, right.totalCost);
    }

    private int compareBeamState(SearchState left, SearchState right) {
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

    private RouteOption toRouteOption(DpLabel label, List<Poi> candidates) {
        List<Poi> path = reconstructDpPath(label, candidates);
        return new RouteOption(path, signature(path), label.utility);
    }

    private RouteOption toRouteOption(SearchState state, List<Poi> candidates) {
        List<Poi> path = state.path.stream().map(candidates::get).collect(Collectors.toList());
        return new RouteOption(path, signature(path), state.utility);
    }

    private List<Poi> reconstructDpPath(DpLabel label, List<Poi> candidates) {
        ArrayDeque<Poi> stack = new ArrayDeque<>();
        DpLabel cursor = label;
        while (cursor != null && cursor.lastIndex >= 0) {
            stack.push(candidates.get(cursor.lastIndex));
            cursor = cursor.prev;
        }
        return new ArrayList<>(stack);
    }

    private List<RouteOption> deduplicateRouteOptions(List<RouteOption> routes) {
        Map<String, RouteOption> ranked = new LinkedHashMap<>();
        for (RouteOption route : routes) {
            if (route == null || route.path() == null || route.path().isEmpty()) {
                continue;
            }
            ranked.putIfAbsent(route.signature(), route);
        }
        return new ArrayList<>(ranked.values());
    }

    private boolean matchesWeatherConstraint(GenerateReqDTO req, Poi poi) {
        return req == null
                || !Boolean.TRUE.equals(req.getIsRainy())
                || Integer.valueOf(1).equals(poi.getIndoor())
                || Integer.valueOf(1).equals(poi.getRainFriendly());
    }

    private boolean matchesWalkingConstraint(GenerateReqDTO req, Poi poi) {
        return walkingRank(req == null ? null : req.getWalkingLevel()) >= walkingRank(poi.getWalkingLevel()) - 1;
    }

    private int walkingRank(String value) {
        if (!StringUtils.hasText(value)) {
            return 2;
        }
        if (value.contains("低") || value.equalsIgnoreCase("low")) {
            return 1;
        }
        if (value.contains("高") || value.equalsIgnoreCase("high")) {
            return 3;
        }
        return 2;
    }

    private double scorePoi(GenerateReqDTO req, Poi poi) {
        GenerateReqDTO normalized = normalizeRequest(req);
        double score = poi.getPriorityScore() == null ? 7.5 : poi.getPriorityScore().doubleValue() * 2.5;
        if (normalized.getThemes() != null && poi.getTags() != null) {
            score += normalized.getThemes().stream()
                    .filter(theme -> StringUtils.hasText(theme) && poi.getTags().contains(theme))
                    .count() * 3.5;
        }
        if (StringUtils.hasText(normalized.getCompanionType())
                && StringUtils.hasText(poi.getSuitableFor())
                && poi.getSuitableFor().contains(normalized.getCompanionType())) {
            score += 2.5;
        }
        if (matchesMustVisitPoi(normalized, poi)) {
            score += 120.0;
        }
        if (Boolean.TRUE.equals(normalized.getIsNight()) && Integer.valueOf(1).equals(poi.getNightAvailable())) {
            score += 2.0;
        }
        if (Boolean.TRUE.equals(normalized.getIsRainy())
                && (Integer.valueOf(1).equals(poi.getIndoor()) || Integer.valueOf(1).equals(poi.getRainFriendly()))) {
            score += 1.5;
        }
        if (walkingRank(normalized.getWalkingLevel()) >= walkingRank(poi.getWalkingLevel())) {
            score += 1.0;
        }
        if (StringUtils.hasText(normalized.getBudgetLevel()) && poi.getAvgCost() != null) {
            double cost = poi.getAvgCost().doubleValue();
            String budget = normalized.getBudgetLevel();
            if (budget.contains("低") || budget.equalsIgnoreCase("low")) {
                if (cost <= 20) {
                    score += 2.0;
                } else if (cost > 100) {
                    score -= 15.0;
                } else if (cost > 50) {
                    score -= 8.0;
                }
            } else if (budget.contains("中") || budget.equalsIgnoreCase("medium")) {
                if (cost > 300) {
                    score -= 12.0;
                } else if (cost > 150) {
                    score -= 5.0;
                }
            } else if (budget.contains("高") || budget.equalsIgnoreCase("high")) {
                if (cost >= 100) {
                    score += 2.0;
                }
            }
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
        score -= resolveBaseCrowdPenalty(poi) * CANDIDATE_CROWD_SCORE_WEIGHT;
        return Math.max(score, 1.0);
    }

    private double resolveRouteValue(GenerateReqDTO req, Poi poi) {
        return poi.getTempScore() != null ? poi.getTempScore() : scorePoi(req, poi);
    }

    private double resolveBaseCrowdPenalty(Poi poi) {
        if (poi == null || poi.getCrowdPenalty() == null) {
            return 0D;
        }
        return Math.max(0D, poi.getCrowdPenalty().doubleValue());
    }

    private double resolveVisitCrowdPenalty(Poi poi, GenerateReqDTO req, int visitStartMinute) {
        double penalty = resolveBaseCrowdPenalty(poi);
        if (penalty <= 0D) {
            return 0D;
        }

        double factor = 1.0D;
        if (visitStartMinute >= 11 * 60 && visitStartMinute < 14 * 60) {
            factor += 0.25D;
        }
        if (visitStartMinute >= 18 * 60 && Integer.valueOf(1).equals(poi.getNightAvailable())) {
            factor += 0.20D;
        }
        LocalDate tripDate = resolveTripDate(req);
        DayOfWeek dayOfWeek = tripDate.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            factor += 0.15D;
        }
        return penalty * factor;
    }

    private double resolvePoiCost(Poi poi) {
        if (poi == null || poi.getAvgCost() == null) {
            return 0D;
        }
        return Math.max(0D, poi.getAvgCost().doubleValue());
    }

    private int normalizeStayDuration(Integer stayDuration) {
        if (stayDuration == null) {
            return 90;
        }
        return Math.max(0, stayDuration);
    }

    private int safeAddMinutes(int base, int delta) {
        long total = (long) base + delta;
        return clampToInt(total);
    }

    private int clampToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    private List<Poi> capCandidatesForBeamMask(List<Poi> candidates, GenerateReqDTO normalizedRequest) {
        if (candidates == null || candidates.size() <= Long.SIZE) {
            return candidates;
        }
        return candidates.stream()
                .filter(Objects::nonNull)
                .sorted((left, right) -> {
                    int byMustVisit = Boolean.compare(
                            matchesMustVisitPoi(normalizedRequest, right),
                            matchesMustVisitPoi(normalizedRequest, left)
                    );
                    if (byMustVisit != 0) {
                        return byMustVisit;
                    }
                    int byScore = Double.compare(resolveRouteValue(normalizedRequest, right), resolveRouteValue(normalizedRequest, left));
                    if (byScore != 0) {
                        return byScore;
                    }
                    BigDecimal rightPriority = right.getPriorityScore() == null ? BigDecimal.ZERO : right.getPriorityScore();
                    BigDecimal leftPriority = left.getPriorityScore() == null ? BigDecimal.ZERO : left.getPriorityScore();
                    return rightPriority.compareTo(leftPriority);
                })
                .limit(Long.SIZE)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Poi buildDeparturePoi(GenerateReqDTO req) {
        if (req == null) {
            return null;
        }
        BigDecimal latitude = null;
        BigDecimal longitude = null;
        if (req.getDepartureLatitude() != null && req.getDepartureLongitude() != null) {
            double lat = req.getDepartureLatitude();
            double lng = req.getDepartureLongitude();
            if (Math.abs(lat) <= 90 && Math.abs(lng) <= 180) {
                latitude = BigDecimal.valueOf(lat);
                longitude = BigDecimal.valueOf(lng);
            }
        }
        if (latitude == null || longitude == null) {
            GeoPoint geoPoint = resolveDepartureByGeo(req);
            if (geoPoint != null && geoPoint.valid()) {
                latitude = geoPoint.latitude();
                longitude = geoPoint.longitude();
            }
        }
        if (latitude == null || longitude == null) {
            return null;
        }
        Poi departure = new Poi();
        departure.setId(-1L);
        departure.setName(StringUtils.hasText(req.getDeparturePlaceName()) ? req.getDeparturePlaceName() : "CURRENT_LOCATION");
        departure.setCityCode(req.getCityCode());
        departure.setCityName(req.getCityName());
        departure.setLatitude(latitude);
        departure.setLongitude(longitude);
        departure.setSourceType("departure");
        return departure;
    }

    private GeoPoint resolveDepartureByGeo(GenerateReqDTO req) {
        if (geoSearchService == null) {
            return null;
        }
        String cityName = StringUtils.hasText(req.getCityName()) ? req.getCityName() : DEFAULT_CITY_NAME;
        if (StringUtils.hasText(req.getDeparturePlaceName())) {
            GeoPoint byDepartureName = geoSearchService.geocode(req.getDeparturePlaceName(), cityName).orElse(null);
            if (byDepartureName != null && byDepartureName.valid()) {
                return byDepartureName;
            }
        }
        return geoSearchService.geocode(cityName + "市中心", cityName).orElse(null);
    }

    private String textOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private List<String> normalizeMustVisitPoiNames(List<String> mustVisitPoiNames) {
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

    private List<RouteOption> prioritizeMustVisitRoutes(List<RouteOption> routes, GenerateReqDTO request) {
        if (routes == null || routes.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> mustVisitKeywords = normalizeMustVisitPoiNames(request == null ? null : request.getMustVisitPoiNames());
        if (mustVisitKeywords.isEmpty()) {
            return routes;
        }
        return routes.stream()
                .sorted((left, right) -> {
                    int rightCoverage = countMustVisitCoverage(right.path(), mustVisitKeywords);
                    int leftCoverage = countMustVisitCoverage(left.path(), mustVisitKeywords);
                    int byCoverage = Integer.compare(rightCoverage, leftCoverage);
                    if (byCoverage != 0) {
                        return byCoverage;
                    }
                    return Double.compare(right.utility(), left.utility());
                })
                .toList();
    }

    private boolean matchesMustVisitPoi(GenerateReqDTO request, Poi poi) {
        if (request == null || poi == null || !StringUtils.hasText(poi.getName())) {
            return false;
        }
        List<String> mustVisitKeywords = normalizeMustVisitPoiNames(request.getMustVisitPoiNames());
        if (mustVisitKeywords.isEmpty()) {
            return false;
        }
        String poiNameLower = poi.getName().toLowerCase();
        for (String keyword : mustVisitKeywords) {
            if (!StringUtils.hasText(keyword)) {
                continue;
            }
            String normalizedKeyword = keyword.trim().toLowerCase();
            if (poiNameLower.contains(normalizedKeyword) || normalizedKeyword.contains(poiNameLower)) {
                return true;
            }
        }
        return false;
    }

    private int countMustVisitCoverage(List<Poi> path, List<String> mustVisitKeywords) {
        if (path == null || path.isEmpty() || mustVisitKeywords == null || mustVisitKeywords.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String keyword : mustVisitKeywords) {
            if (!StringUtils.hasText(keyword)) {
                continue;
            }
            String normalizedKeyword = keyword.trim().toLowerCase();
            boolean matched = path.stream()
                    .filter(Objects::nonNull)
                    .map(Poi::getName)
                    .filter(StringUtils::hasText)
                    .map(String::toLowerCase)
                    .anyMatch(name -> name.contains(normalizedKeyword) || normalizedKeyword.contains(name));
            if (matched) {
                count++;
            }
        }
        return count;
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

    private record StartAccessProfile(int travelMinutes,
                                      BigDecimal distanceKm,
                                      String transportMode,
                                      double accessPenalty) {
    }

    private static final class DpLabel {
        private final int mask;
        private final int lastIndex;
        private final int currentMinute;
        private final double totalCost;
        private final double utility;
        private final int stopCount;
        private final DpLabel prev;

        private DpLabel(int mask,
                        int lastIndex,
                        int currentMinute,
                        double totalCost,
                        double utility,
                        int stopCount,
                        DpLabel prev) {
            this.mask = mask;
            this.lastIndex = lastIndex;
            this.currentMinute = currentMinute;
            this.totalCost = totalCost;
            this.utility = utility;
            this.stopCount = stopCount;
            this.prev = prev;
        }

        private static DpLabel seed(int startMinute) {
            return new DpLabel(0, -1, startMinute, 0D, 0D, 0, null);
        }
    }

    public record RouteOption(List<Poi> path, String signature, double utility) {
    }
}
