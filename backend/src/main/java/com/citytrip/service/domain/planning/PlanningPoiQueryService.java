package com.citytrip.service.domain.planning;

import com.citytrip.mapper.PoiMapper;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.entity.Poi;
import com.citytrip.model.vo.ItineraryNodeVO;
import com.citytrip.service.impl.ItineraryRouteOptimizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PlanningPoiQueryService {

    private final PoiMapper poiMapper;
    private final ItineraryRouteOptimizer routeOptimizer;

    public PlanningPoiQueryService(PoiMapper poiMapper, ItineraryRouteOptimizer routeOptimizer) {
        this.poiMapper = poiMapper;
        this.routeOptimizer = routeOptimizer;
    }

    public List<Poi> loadPlanningPool(GenerateReqDTO req) {
        GenerateReqDTO normalized = routeOptimizer.normalizeRequest(req);
        return routeOptimizer.prepareCandidates(poiMapper.selectList(null), normalized, true);
    }

    public List<Poi> loadOrderedPois(List<ItineraryNodeVO> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = nodes.stream()
                .map(ItineraryNodeVO::getPoiId)
                .filter(Objects::nonNull)
                .toList();
        return loadOrderedPoisByIds(ids);
    }

    public List<Poi> loadOrderedPoisByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Poi> byId = poiMapper.selectBatchIds(ids).stream()
                .filter(Objects::nonNull)
                .filter(poi -> poi.getId() != null)
                .collect(Collectors.toMap(Poi::getId, poi -> poi));

        List<Poi> ordered = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Poi poi = byId.get(id);
            if (poi != null) {
                ordered.add(poi);
            }
        }
        return ordered;
    }
}
