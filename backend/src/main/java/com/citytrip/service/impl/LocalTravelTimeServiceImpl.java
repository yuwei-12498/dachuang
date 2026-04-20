package com.citytrip.service.impl;

import com.citytrip.model.entity.Poi;
import com.citytrip.service.TravelTimeService;
import org.springframework.stereotype.Service;

@Service
public class LocalTravelTimeServiceImpl implements TravelTimeService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double WALKING_SPEED_KM_PER_MIN = 4.8 / 60.0;
    private static final double TRANSIT_SPEED_KM_PER_MIN = 22.0 / 60.0;
    private static final double TAXI_SPEED_KM_PER_MIN = 28.0 / 60.0;

    @Override
    public int estimateTravelTimeMinutes(Poi from, Poi to) {
        if (from == null || to == null || samePoi(from, to)) {
            return 0;
        }
        if (from.getLatitude() == null || from.getLongitude() == null
                || to.getLatitude() == null || to.getLongitude() == null) {
            return 30;
        }

        double straightDistance = haversineDistanceKm(from, to);
        double roadDistance = straightDistance * roadFactor(straightDistance, from, to);

        if (roadDistance <= 1.2) {
            return Math.max(6, (int) Math.ceil(roadDistance / WALKING_SPEED_KM_PER_MIN) + 4);
        }
        if (roadDistance <= 5.0) {
            return Math.max(12, (int) Math.ceil(roadDistance / TRANSIT_SPEED_KM_PER_MIN) + 8);
        }
        return Math.max(20, (int) Math.ceil(roadDistance / TAXI_SPEED_KM_PER_MIN) + 12);
    }

    private boolean samePoi(Poi from, Poi to) {
        return from.getId() != null && from.getId().equals(to.getId());
    }

    private double haversineDistanceKm(Poi from, Poi to) {
        double lat1 = Math.toRadians(from.getLatitude().doubleValue());
        double lat2 = Math.toRadians(to.getLatitude().doubleValue());
        double deltaLat = lat1 - lat2;
        double deltaLon = Math.toRadians(from.getLongitude().doubleValue() - to.getLongitude().doubleValue());
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    private double roadFactor(double straightDistance, Poi from, Poi to) {
        double factor;
        if (straightDistance <= 1.0) {
            factor = 1.18;
        } else if (straightDistance <= 4.0) {
            factor = 1.28;
        } else if (straightDistance <= 10.0) {
            factor = 1.38;
        } else {
            factor = 1.48;
        }

        if (from.getDistrict() != null && to.getDistrict() != null && !from.getDistrict().equals(to.getDistrict())) {
            factor += 0.08;
        }

        return factor;
    }
}
