package com.citytrip.service.impl;

import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.service.PoiService;
import com.citytrip.service.TravelTimeService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItineraryRouteOptimizerDpTest {

    private static final double SCORE_WEIGHT = 6.0D;
    private static final double WAIT_PENALTY_WEIGHT = 0.5D;
    private static final double TRAVEL_PENALTY_WEIGHT = 1.0D;
    private static final double CROWD_PENALTY_WEIGHT = 4.0D;

    @Test
    void paretoDpFindsExactBestRouteWithinOneSecond() {
        PoiService poiService = mock(PoiService.class);
        when(poiService.enrichOperatingStatus(anyList(), any(LocalDate.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Poi> pois = invocation.getArgument(0);
            for (Poi poi : pois) {
                poi.setAvailableOnTripDate(true);
                poi.setStatusStale(false);
                poi.setOperatingStatus("OPEN");
            }
            return pois;
        });

        List<Poi> pois = buildChengduPois();
        Map<Long, Integer> indexByPoiId = buildIndexByPoiId(pois);
        int[][] travelMatrix = buildChengduTravelMatrix();
        TravelTimeService travelTimeService = new MatrixTravelTimeService(indexByPoiId, travelMatrix);
        ItineraryRouteOptimizer optimizer = new ItineraryRouteOptimizer(poiService, travelTimeService);
        GenerateReqDTO request = buildRequest();

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            List<Poi> prepared = optimizer.prepareCandidates(pois, request, false);
            List<ItineraryRouteOptimizer.RouteOption> ranked = optimizer.rankRoutes(prepared, request, 4);

            assertThat(prepared).hasSize(6);
            assertThat(ranked).isNotEmpty();

            ItineraryRouteOptimizer.RouteOption top = ranked.get(0);
            ExhaustiveBestRoute bruteForce = bruteForceBestRoute(prepared, request, travelTimeService, 4, optimizer);

            assertThat(top.signature()).isEqualTo(bruteForce.signature());
            assertThat(top.utility()).isCloseTo(bruteForce.utility(), withinDelta(0.0001D));
            assertThat(top.path()).hasSizeLessThanOrEqualTo(4);
            assertThat(top.path()).extracting(Poi::getName).doesNotContain("Chunxi Road");
        });
    }

    @Test
    void fallsBackToBeamSearchWhenCandidateCountExceedsThreshold() {
        PoiService poiService = mock(PoiService.class);
        when(poiService.enrichOperatingStatus(anyList(), any(LocalDate.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Poi> pois = invocation.getArgument(0);
            for (Poi poi : pois) {
                poi.setAvailableOnTripDate(true);
                poi.setStatusStale(false);
                poi.setOperatingStatus("OPEN");
            }
            return pois;
        });

        List<Poi> pois = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Poi poi = createPoi(
                    1000L + i,
                    "Linear POI " + i,
                    "culture",
                    "Qingyang",
                    "09:00",
                    "18:00",
                    60,
                    20,
                    4.0D + (16 - i) * 0.05D,
                    0.30D + i * 0.05D,
                    "culture,history"
            );
            pois.add(poi);
        }
        Map<Long, Integer> indexByPoiId = buildIndexByPoiId(pois);
        int[][] travelMatrix = new int[16][16];
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                travelMatrix[i][j] = i == j ? 0 : Math.abs(i - j) * 6 + 8;
            }
        }
        TravelTimeService travelTimeService = new MatrixTravelTimeService(indexByPoiId, travelMatrix);
        ItineraryRouteOptimizer optimizer = new ItineraryRouteOptimizer(poiService, travelTimeService);
        GenerateReqDTO request = buildRequest();

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            List<Poi> prepared = optimizer.prepareCandidates(pois, request, false);
            List<ItineraryRouteOptimizer.RouteOption> ranked = optimizer.rankRoutes(prepared, request, 5);

            assertThat(prepared).hasSize(16);
            assertThat(ranked).isNotEmpty();
            assertThat(ranked.get(0).path().size()).isLessThanOrEqualTo(5);
        });
    }

    @Test
    void handlesSingleCandidateZeroBudgetAndTenMinuteWindowWithoutThrowing() {
        PoiService poiService = mock(PoiService.class);
        when(poiService.enrichOperatingStatus(anyList(), any(LocalDate.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Poi> pois = invocation.getArgument(0);
            for (Poi poi : pois) {
                poi.setAvailableOnTripDate(true);
                poi.setStatusStale(false);
                poi.setOperatingStatus("OPEN");
            }
            return pois;
        });

        Poi onlyPoi = createPoi(
                501L,
                "Free Mini Museum",
                "museum",
                "Qingyang",
                "09:00",
                "18:00",
                30,
                0,
                4.20D,
                0.10D,
                "culture,history,indoor"
        );
        List<Poi> pois = List.of(onlyPoi);
        Map<Long, Integer> indexByPoiId = buildIndexByPoiId(pois);
        TravelTimeService travelTimeService = new MatrixTravelTimeService(indexByPoiId, new int[][]{{0}});
        ItineraryRouteOptimizer optimizer = new ItineraryRouteOptimizer(poiService, travelTimeService);

        GenerateReqDTO request = new GenerateReqDTO();
        request.setTripDays(1.0D);
        request.setTripDate("2026-04-18");
        request.setBudgetLevel("0");
        request.setThemes(List.of("culture"));
        request.setIsRainy(false);
        request.setIsNight(false);
        request.setWalkingLevel("low");
        request.setCompanionType("solo");
        request.setStartTime("09:00");
        request.setEndTime("09:10");

        List<Poi> prepared = optimizer.prepareCandidates(pois, request, false);
        List<ItineraryRouteOptimizer.RouteOption> ranked = optimizer.rankRoutes(prepared, request, 3);
        ItineraryRouteOptimizer.RouteOption best = optimizer.bestRoute(prepared, request, 3);

        assertThat(prepared).hasSize(1);
        assertThat(prepared.get(0).getTempScore()).isNotNull().isPositive();
        assertThat(ranked).isEmpty();
        assertThat(best.path()).isEmpty();
        assertThat(best.signature()).isEmpty();
        assertThat(best.utility()).isZero();
    }

    private GenerateReqDTO buildRequest() {
        GenerateReqDTO request = new GenerateReqDTO();
        request.setTripDays(1.0D);
        request.setTripDate("2026-04-15");
        request.setBudgetLevel("medium");
        request.setThemes(List.of("culture", "history"));
        request.setIsRainy(false);
        request.setIsNight(false);
        request.setWalkingLevel("medium");
        request.setCompanionType("friends");
        request.setStartTime("09:00");
        request.setEndTime("18:00");
        return request;
    }

    private List<Poi> buildChengduPois() {
        List<Poi> pois = new ArrayList<>();
        pois.add(createPoi(101L, "Chengdu Museum", "museum", "Qingyang", "09:00", "17:00", 120, 0, 4.80D, 0.60D, "culture,history,indoor"));
        pois.add(createPoi(102L, "Wenshu Monastery", "temple", "Qingyang", "08:30", "17:30", 60, 0, 3.80D, 0.20D, "culture,quiet"));
        pois.add(createPoi(103L, "Kuanzhai Alley", "district", "Qingyang", "12:00", "22:00", 100, 90, 4.00D, 3.00D, "culture,food"));
        pois.add(createPoi(104L, "Wuhou Shrine", "heritage", "Wuhou", "09:00", "18:00", 90, 50, 4.60D, 0.80D, "culture,history"));
        pois.add(createPoi(105L, "Du Fu Cottage", "heritage", "Qingyang", "09:00", "18:00", 90, 50, 4.40D, 0.70D, "culture,history"));
        pois.add(createPoi(106L, "Chunxi Road", "shopping", "Jinjiang", "10:00", "23:00", 120, 200, 3.90D, 3.50D, "shopping,night"));
        return pois;
    }

    private Poi createPoi(long id,
                          String name,
                          String category,
                          String district,
                          String openTime,
                          String closeTime,
                          int stayDuration,
                          int avgCost,
                          double priorityScore,
                          double crowdPenalty,
                          String tags) {
        Poi poi = new Poi();
        poi.setId(id);
        poi.setName(name);
        poi.setCategory(category);
        poi.setDistrict(district);
        poi.setOpenTime(LocalTime.parse(openTime));
        poi.setCloseTime(LocalTime.parse(closeTime));
        poi.setStayDuration(stayDuration);
        poi.setAvgCost(BigDecimal.valueOf(avgCost));
        poi.setPriorityScore(BigDecimal.valueOf(priorityScore));
        poi.setCrowdPenalty(BigDecimal.valueOf(crowdPenalty));
        poi.setIndoor(1);
        poi.setNightAvailable(0);
        poi.setRainFriendly(1);
        poi.setWalkingLevel("medium");
        poi.setTags(tags);
        poi.setSuitableFor("friends,solo,family");
        poi.setDescription(name + " sample");
        return poi;
    }

    private Map<Long, Integer> buildIndexByPoiId(List<Poi> pois) {
        Map<Long, Integer> indexByPoiId = new LinkedHashMap<>();
        for (int i = 0; i < pois.size(); i++) {
            indexByPoiId.put(pois.get(i).getId(), i);
        }
        return indexByPoiId;
    }

    private int[][] buildChengduTravelMatrix() {
        return new int[][]{
                {0, 10, 16, 22, 25, 18},
                {10, 0, 14, 28, 26, 16},
                {16, 14, 0, 18, 16, 20},
                {22, 28, 18, 0, 12, 30},
                {25, 26, 16, 12, 0, 32},
                {18, 16, 20, 30, 32, 0}
        };
    }

    private ExhaustiveBestRoute bruteForceBestRoute(List<Poi> candidates,
                                                    GenerateReqDTO request,
                                                    TravelTimeService travelTimeService,
                                                    int maxStops,
                                                    ItineraryRouteOptimizer optimizer) {
        BestHolder holder = new BestHolder();
        dfs(candidates, request, travelTimeService, optimizer, 0, -1,
                optimizer.parseTimeMinutes(request.getStartTime(), ItineraryRouteOptimizer.DEFAULT_START_MINUTE),
                0.0D, new ArrayList<>(), maxStops, holder);
        return new ExhaustiveBestRoute(holder.bestSignature, holder.bestUtility);
    }

    private void dfs(List<Poi> candidates,
                     GenerateReqDTO request,
                     TravelTimeService travelTimeService,
                     ItineraryRouteOptimizer optimizer,
                     int mask,
                     int lastIndex,
                     int currentMinute,
                     double utility,
                     List<Integer> path,
                     int maxStops,
                     BestHolder holder) {
        if (!path.isEmpty()) {
            String signature = path.stream()
                    .map(index -> String.valueOf(candidates.get(index).getId()))
                    .reduce((left, right) -> left + "-" + right)
                    .orElse("");
            if (utility > holder.bestUtility
                    || (Math.abs(utility - holder.bestUtility) < 1e-9 && path.size() > holder.bestSize)
                    || (Math.abs(utility - holder.bestUtility) < 1e-9 && path.size() == holder.bestSize
                    && currentMinute < holder.bestFinishMinute)) {
                holder.bestUtility = utility;
                holder.bestSignature = signature;
                holder.bestSize = path.size();
                holder.bestFinishMinute = currentMinute;
            }
        }

        if (path.size() >= maxStops) {
            return;
        }

        int endMinute = optimizer.parseTimeMinutes(request.getEndTime(), ItineraryRouteOptimizer.DEFAULT_END_MINUTE);
        int startMinute = optimizer.parseTimeMinutes(request.getStartTime(), ItineraryRouteOptimizer.DEFAULT_START_MINUTE);

        for (int nextIndex = 0; nextIndex < candidates.size(); nextIndex++) {
            if ((mask & (1 << nextIndex)) != 0) {
                continue;
            }
            Poi poi = candidates.get(nextIndex);
            int travelTime = lastIndex < 0 ? 0 : travelTimeService.estimateTravelTimeMinutes(candidates.get(lastIndex), poi);
            int arrival = currentMinute + travelTime;
            int visitStart = Math.max(arrival, optimizer.resolveOpenMinute(poi, startMinute));
            int waitTime = Math.max(0, visitStart - arrival);
            int visitEnd = visitStart + poi.getStayDuration();

            if (visitEnd > optimizer.resolveCloseMinute(poi, endMinute) || visitEnd > endMinute) {
                continue;
            }

            double nextUtility = utility
                    + poi.getTempScore() * SCORE_WEIGHT
                    - travelTime * TRAVEL_PENALTY_WEIGHT
                    - waitTime * WAIT_PENALTY_WEIGHT
                    - resolveVisitCrowdPenalty(poi, visitStart) * CROWD_PENALTY_WEIGHT;

            path.add(nextIndex);
            dfs(candidates, request, travelTimeService, optimizer,
                    mask | (1 << nextIndex), nextIndex, visitEnd, nextUtility, path, maxStops, holder);
            path.remove(path.size() - 1);
        }
    }

    private double resolveVisitCrowdPenalty(Poi poi, int visitStartMinute) {
        double penalty = poi.getCrowdPenalty() == null ? 0.0D : poi.getCrowdPenalty().doubleValue();
        double factor = 1.0D;
        if (visitStartMinute >= 11 * 60 && visitStartMinute < 14 * 60) {
            factor += 0.25D;
        }
        return penalty * factor;
    }

    private org.assertj.core.data.Offset<Double> withinDelta(double delta) {
        return org.assertj.core.data.Offset.offset(delta);
    }

    private record ExhaustiveBestRoute(String signature, double utility) {
    }

    private static final class BestHolder {
        private double bestUtility = Double.NEGATIVE_INFINITY;
        private String bestSignature = "";
        private int bestSize = 0;
        private int bestFinishMinute = Integer.MAX_VALUE;
    }

    private static final class MatrixTravelTimeService implements TravelTimeService {
        private final Map<Long, Integer> indexByPoiId;
        private final int[][] travelMatrix;

        private MatrixTravelTimeService(Map<Long, Integer> indexByPoiId, int[][] travelMatrix) {
            this.indexByPoiId = new HashMap<>(indexByPoiId);
            this.travelMatrix = travelMatrix;
        }

        @Override
        public int estimateTravelTimeMinutes(Poi from, Poi to) {
            if (from == null || to == null || from.getId() == null || to.getId() == null) {
                return 0;
            }
            Integer fromIndex = indexByPoiId.get(from.getId());
            Integer toIndex = indexByPoiId.get(to.getId());
            if (fromIndex == null || toIndex == null) {
                return 0;
            }
            return travelMatrix[fromIndex][toIndex];
        }
    }
}
