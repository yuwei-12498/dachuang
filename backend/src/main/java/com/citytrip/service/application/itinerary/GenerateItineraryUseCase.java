package com.citytrip.service.application.itinerary;

import com.citytrip.analytics.RoutePlanFactPublisher;
import com.citytrip.assembler.ItineraryComparisonAssembler;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.domain.ai.ItineraryAiDecorationService;
import com.citytrip.service.impl.PlanningOrchestrator;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GenerateItineraryUseCase {

    private final PlanningOrchestrator planningOrchestrator;
    private final ItineraryComparisonAssembler itineraryComparisonAssembler;
    private final ItineraryAiDecorationService itineraryAiDecorationService;
    private final SavedItineraryCommandService savedItineraryCommandService;
    private final RoutePlanFactPublisher routePlanFactPublisher;

    public GenerateItineraryUseCase(PlanningOrchestrator planningOrchestrator,
                                    ItineraryComparisonAssembler itineraryComparisonAssembler,
                                    ItineraryAiDecorationService itineraryAiDecorationService,
                                    SavedItineraryCommandService savedItineraryCommandService,
                                    RoutePlanFactPublisher routePlanFactPublisher) {
        this.planningOrchestrator = planningOrchestrator;
        this.itineraryComparisonAssembler = itineraryComparisonAssembler;
        this.itineraryAiDecorationService = itineraryAiDecorationService;
        this.savedItineraryCommandService = savedItineraryCommandService;
        this.routePlanFactPublisher = routePlanFactPublisher;
    }

    public ItineraryVO generate(Long userId, GenerateReqDTO req) {
        PlanningOrchestrator.PlanningResult planningResult = planningOrchestrator.generate(
                userId,
                req,
                snapshot -> itineraryComparisonAssembler.buildComparedItinerary(
                        snapshot.rankedRoutes(),
                        snapshot.normalizedRequest(),
                        Collections.emptyMap(),
                        null,
                        Collections.emptySet()
                ),
                (normalizedRequest, baseItinerary) -> itineraryAiDecorationService.decorateWithLlm(baseItinerary, normalizedRequest)
        );

        ItineraryVO itinerary = savedItineraryCommandService.save(
                userId,
                null,
                planningResult.normalizedRequest(),
                planningResult.itinerary()
        );
        routePlanFactPublisher.publish(
                userId,
                itinerary.getId(),
                "generate",
                planningResult.normalizedRequest(),
                itinerary,
                planningResult.rawCandidateCount(),
                planningResult.filteredCandidateCount(),
                planningResult.finalCandidateCount(),
                planningResult.maxStops(),
                planningResult.generatedRouteCount(),
                planningResult.displayedOptionCount(),
                planningResult.success(),
                planningResult.failReason(),
                planningResult.algorithmVersion(),
                planningResult.recallStrategy(),
                planningResult.planningStartedAt()
        );
        return itinerary;
    }
}
