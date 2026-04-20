package com.citytrip.service.impl;

import com.citytrip.mapper.PoiMapper;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.model.vo.ItineraryOptionVO;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.domain.policy.MaxStopsPolicy;
import com.citytrip.service.PoiService;
import com.citytrip.service.TravelTimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanningOrchestratorTest {

    @Test
    @DisplayName("数据库候选查询超时时，规划编排器降级为空结果而不是抛 500")
    void fallsBackToEmptyPlanningResultWhenCandidateQueryTimesOut() {
        PoiMapper poiMapper = mock(PoiMapper.class);
        HybridPoiRecallService hybridPoiRecallService = mock(HybridPoiRecallService.class);
        ItineraryRouteOptimizer routeOptimizer = mock(ItineraryRouteOptimizer.class);
        GenerateReqDTO request = buildRequest();
        GenerateReqDTO normalized = buildRequest();
        when(routeOptimizer.normalizeRequest(request)).thenReturn(normalized);
        when(poiMapper.selectPlanningCandidates(eq(false), eq("medium"), eq(200)))
                .thenThrow(new QueryTimeoutException("planning db timeout"));

        Executor directExecutor = Runnable::run;
        PlanningOrchestrator orchestrator = new PlanningOrchestrator(
                poiMapper,
                routeOptimizer,
                hybridPoiRecallService,
                new MaxStopsPolicy(),
                directExecutor,
                directExecutor,
                50
        );

        PlanningOrchestrator.PlanningResult result = orchestrator.generate(
                7L,
                request,
                snapshot -> {
                    assertThat(snapshot.rawCandidateCount()).isZero();
                    assertThat(snapshot.filteredCandidateCount()).isZero();
                    assertThat(snapshot.finalCandidateCount()).isZero();
                    assertThat(snapshot.maxStops()).isZero();
                    assertThat(snapshot.generatedRouteCount()).isZero();
                    return buildEmptyItinerary(snapshot.normalizedRequest());
                },
                (normalizedRequest, baseItinerary) -> baseItinerary
        );

        assertThat(result.success()).isFalse();
        assertThat(result.generatedRouteCount()).isZero();
        assertThat(result.displayedOptionCount()).isZero();
        assertThat(result.recallStrategy()).endsWith("-degraded");
        assertThat(result.itinerary()).isNotNull();
        assertThat(result.itinerary().getOptions()).isEmpty();
        verify(hybridPoiRecallService, never()).recall(eq(7L), any(GenerateReqDTO.class), anyList(), eq(18));
    }

    @Test
    @DisplayName("大模型返回乱码导致解析失败时，规划编排器回退到 rule-based itinerary")
    void fallsBackToRuleBasedItineraryWhenAiDecoratorThrowsOnGarbledResponse() {
        PoiMapper poiMapper = mock(PoiMapper.class);
        HybridPoiRecallService hybridPoiRecallService = mock(HybridPoiRecallService.class);
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

        List<Poi> candidates = buildCandidates();
        when(poiMapper.selectPlanningCandidates(eq(false), eq("medium"), eq(200))).thenReturn(candidates);
        when(hybridPoiRecallService.recall(eq(99L), any(GenerateReqDTO.class), eq(candidates), eq(18)))
                .thenReturn(new HybridPoiRecallService.RecallResult(candidates, candidates, "hybrid-usercf-content-v1", 2, 2, false));

        Map<Long, Integer> indexByPoiId = buildIndex(candidates);
        ItineraryRouteOptimizer routeOptimizer = new ItineraryRouteOptimizer(poiService, new MatrixTravelTimeService(indexByPoiId));
        Executor directExecutor = Runnable::run;
        PlanningOrchestrator orchestrator = new PlanningOrchestrator(
                poiMapper,
                routeOptimizer,
                hybridPoiRecallService,
                new MaxStopsPolicy(),
                directExecutor,
                directExecutor,
                50
        );

        PlanningOrchestrator.PlanningResult result = orchestrator.generate(
                99L,
                buildRequest(),
                snapshot -> buildBaseItinerary(snapshot.normalizedRequest(), snapshot.rankedRoutes()),
                (normalizedRequest, baseItinerary) -> {
                    throw new IllegalStateException("LLM returned garbled payload: ��{json");
                }
        );

        assertThat(result.success()).isTrue();
        assertThat(result.itinerary()).isNotNull();
        assertThat(result.itinerary().getNodes()).isNotEmpty();
        assertThat(result.itinerary().getRecommendReason())
                .isEqualTo("Rule recommendation: prioritize nearby stops with better theme fit.");
        assertThat(result.itinerary().getTips()).contains("rule-based tips");
    }

    @Test
    void returnsRouteImmediatelyWhenLlmStageTimesOut() {
        PoiMapper poiMapper = mock(PoiMapper.class);
        HybridPoiRecallService hybridPoiRecallService = mock(HybridPoiRecallService.class);
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

        List<Poi> candidates = buildCandidates();
        when(poiMapper.selectPlanningCandidates(eq(false), eq("medium"), eq(200))).thenReturn(candidates);
        when(hybridPoiRecallService.recall(eq(88L), any(GenerateReqDTO.class), eq(candidates), eq(18)))
                .thenReturn(new HybridPoiRecallService.RecallResult(candidates, candidates, "hybrid-usercf-content-v1", 2, 2, false));

        Map<Long, Integer> indexByPoiId = buildIndex(candidates);
        ItineraryRouteOptimizer routeOptimizer = new ItineraryRouteOptimizer(poiService, new MatrixTravelTimeService(indexByPoiId));

        ExecutorService planningExecutor = Executors.newFixedThreadPool(2);
        ExecutorService aiExecutor = Executors.newFixedThreadPool(1);
        PlanningOrchestrator orchestrator = new PlanningOrchestrator(
                poiMapper,
                routeOptimizer,
                hybridPoiRecallService,
                new MaxStopsPolicy(),
                planningExecutor,
                aiExecutor,
                50
        );

        try {
            assertTimeoutPreemptively(Duration.ofMillis(400), () -> {
                PlanningOrchestrator.PlanningResult result = orchestrator.generate(
                        88L,
                        buildRequest(),
                        snapshot -> buildBaseItinerary(snapshot.normalizedRequest(), snapshot.rankedRoutes()),
                        (normalizedRequest, baseItinerary) -> {
                            sleep(200L);
                            ItineraryVO decorated = cloneItinerary(baseItinerary);
                            decorated.setTips("AI fallback note");
                            decorated.setRecommendReason("LLM explanation");
                            return decorated;
                        }
                );

                assertThat(result.success()).isTrue();
                assertThat(result.itinerary()).isNotNull();
                assertThat(result.itinerary().getNodes()).isNotEmpty();
                assertThat(result.itinerary().getRecommendReason()).isEqualTo("Rule recommendation: prioritize nearby stops with better theme fit.");
                assertThat(result.itinerary().getRecommendReason()).doesNotContain("LLM");
                assertThat(result.itinerary().getTips()).contains("rule-based tips");
                assertThat(result.recallStrategy()).isEqualTo("hybrid-usercf-content-v1");
            });
        } finally {
            planningExecutor.shutdownNow();
            aiExecutor.shutdownNow();
        }
    }

    private GenerateReqDTO buildRequest() {
        GenerateReqDTO request = new GenerateReqDTO();
        request.setTripDays(1.0D);
        request.setTripDate("2026-04-18");
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

    private List<Poi> buildCandidates() {
        List<Poi> pois = new ArrayList<>();
        pois.add(createPoi(201L, "Du Fu Cottage", "heritage", "Qingyang", 4.9D, 0.4D));
        pois.add(createPoi(202L, "Wuhou Shrine", "heritage", "Wuhou", 4.7D, 0.3D));
        pois.add(createPoi(203L, "Chengdu Museum", "museum", "Qingyang", 4.6D, 0.2D));
        return pois;
    }

    private Poi createPoi(long id, String name, String category, String district, double priorityScore, double crowdPenalty) {
        Poi poi = new Poi();
        poi.setId(id);
        poi.setName(name);
        poi.setCategory(category);
        poi.setDistrict(district);
        poi.setOpenTime(LocalTime.of(9, 0));
        poi.setCloseTime(LocalTime.of(18, 0));
        poi.setStayDuration(90);
        poi.setAvgCost(BigDecimal.valueOf(60));
        poi.setPriorityScore(BigDecimal.valueOf(priorityScore));
        poi.setCrowdPenalty(BigDecimal.valueOf(crowdPenalty));
        poi.setIndoor(1);
        poi.setNightAvailable(0);
        poi.setRainFriendly(1);
        poi.setWalkingLevel("medium");
        poi.setTags("culture,history,indoor");
        poi.setSuitableFor("friends,solo,family");
        poi.setDescription(name + " sample");
        return poi;
    }

    private Map<Long, Integer> buildIndex(List<Poi> pois) {
        Map<Long, Integer> indexByPoiId = new LinkedHashMap<>();
        for (int i = 0; i < pois.size(); i++) {
            indexByPoiId.put(pois.get(i).getId(), i);
        }
        return indexByPoiId;
    }

    private ItineraryVO buildBaseItinerary(GenerateReqDTO request, List<ItineraryRouteOptimizer.RouteOption> rankedRoutes) {
        ItineraryVO itinerary = new ItineraryVO();
        itinerary.setOriginalReq(request);
        itinerary.setSelectedOptionKey("balanced");
        itinerary.setTips("system generated rule-based tips");
        itinerary.setRecommendReason("Rule recommendation: prioritize nearby stops with better theme fit.");

        List<ItineraryNodeVO> nodes = new ArrayList<>();
        int startMinute = 9 * 60;
        for (int i = 0; i < rankedRoutes.get(0).path().size(); i++) {
            Poi poi = rankedRoutes.get(0).path().get(i);
            ItineraryNodeVO node = new ItineraryNodeVO();
            node.setStepOrder(i + 1);
            node.setPoiId(poi.getId());
            node.setPoiName(poi.getName());
            node.setCategory(poi.getCategory());
            node.setDistrict(poi.getDistrict());
            node.setStayDuration(poi.getStayDuration());
            node.setTravelTime(i == 0 ? 0 : 12);
            node.setCost(poi.getAvgCost());
            node.setStartTime(formatTime(startMinute));
            startMinute += poi.getStayDuration();
            node.setEndTime(formatTime(startMinute));
            node.setSysReason("rule matched for theme and route continuity");
            nodes.add(node);
        }

        ItineraryOptionVO option = new ItineraryOptionVO();
        option.setOptionKey("balanced");
        option.setTitle("Balanced plan");
        option.setSubtitle("Rule-based option");
        option.setSignature(rankedRoutes.get(0).signature());
        option.setNodes(nodes);
        option.setTotalDuration(nodes.stream().mapToInt(ItineraryNodeVO::getStayDuration).sum());
        option.setTotalCost(nodes.stream().map(ItineraryNodeVO::getCost).reduce(BigDecimal.ZERO, BigDecimal::add));
        option.setRecommendReason("Rule recommendation: prioritize nearby stops with better theme fit.");
        option.setAlerts(List.of("Popular stops may need advance reservation."));
        option.setRouteUtility(rankedRoutes.get(0).utility());

        itinerary.setOptions(List.of(option));
        itinerary.setNodes(nodes);
        itinerary.setTotalDuration(option.getTotalDuration());
        itinerary.setTotalCost(option.getTotalCost());
        itinerary.setAlerts(option.getAlerts());
        return itinerary;
    }

    private ItineraryVO buildEmptyItinerary(GenerateReqDTO request) {
        ItineraryVO itinerary = new ItineraryVO();
        itinerary.setOriginalReq(request);
        itinerary.setSelectedOptionKey("balanced");
        itinerary.setTips("system generated rule-based tips");
        itinerary.setRecommendReason("Rule recommendation: no feasible route under current hard constraints.");
        itinerary.setOptions(List.of());
        itinerary.setNodes(List.of());
        itinerary.setAlerts(List.of());
        itinerary.setTotalDuration(0);
        itinerary.setTotalCost(BigDecimal.ZERO);
        return itinerary;
    }

    private ItineraryVO cloneItinerary(ItineraryVO source) {
        ItineraryVO itinerary = new ItineraryVO();
        itinerary.setOriginalReq(source.getOriginalReq());
        itinerary.setSelectedOptionKey(source.getSelectedOptionKey());
        itinerary.setTips(source.getTips());
        itinerary.setRecommendReason(source.getRecommendReason());
        itinerary.setTotalDuration(source.getTotalDuration());
        itinerary.setTotalCost(source.getTotalCost());
        itinerary.setAlerts(source.getAlerts());
        itinerary.setOptions(source.getOptions());
        itinerary.setNodes(source.getNodes());
        return itinerary;
    }

    private String formatTime(int minuteOfDay) {
        int hour = minuteOfDay / 60;
        int minute = minuteOfDay % 60;
        return String.format("%02d:%02d", hour, minute);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class MatrixTravelTimeService implements TravelTimeService {
        private final Map<Long, Integer> indexByPoiId;
        private final int[][] matrix = {
                {0, 10, 12},
                {10, 0, 15},
                {12, 15, 0}
        };

        private MatrixTravelTimeService(Map<Long, Integer> indexByPoiId) {
            this.indexByPoiId = new HashMap<>(indexByPoiId);
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
            return matrix[fromIndex][toIndex];
        }
    }
}
