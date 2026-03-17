package com.citytrip.service.impl;

import com.citytrip.model.entity.Poi;
import com.citytrip.service.TravelTimeService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class LocalTravelTimeServiceImpl implements TravelTimeService {

    // 地球半径 (千米)
    private static final double EARTH_RADIUS = 6371.0;

    // 城市短途打车/公交混合平均时速估算：20 km/h = 约 3 分钟/公里
    private static final double CITY_SPEED_KM_PER_MIN = 20.0 / 60.0;

    @Override
    public int estimateTravelTimeMinutes(Poi from, Poi to) {
        if (from == null || to == null)
            return 0;
        if (from.getLatitude() == null || from.getLongitude() == null ||
                to.getLatitude() == null || to.getLongitude() == null) {
            return 30; // 如果经纬度数据不全，兜底返回30分钟
        }

        double radLat1 = Math.toRadians(from.getLatitude().doubleValue());
        double radLat2 = Math.toRadians(to.getLatitude().doubleValue());
        double a = radLat1 - radLat2;
        double b = Math.toRadians(from.getLongitude().doubleValue()) - Math.toRadians(to.getLongitude().doubleValue());

        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS; // 直线距离 千米

        // 由于城市道路不是直线，乘以一个 1.3 的折线系数
        double roadDistance = s * 1.3;

        // 如果距离极近(小于 1km)，认为是步行，步行按 5km/h = 约 12 分钟/公里
        if (roadDistance < 1.0) {
            int walkingTime = (int) Math.ceil(roadDistance * 12);
            return Math.max(5, walkingTime); // 最低给5分钟缓冲
        }

        // 距离较远的话，坐车时间
        int travelTime = (int) Math.ceil(roadDistance / CITY_SPEED_KM_PER_MIN);

        // 加上上下车/等红绿灯的额外摩擦时间（比如5分钟）
        return travelTime + 5;
    }
}
