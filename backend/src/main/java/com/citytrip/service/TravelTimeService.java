package com.citytrip.service;

import com.citytrip.model.entity.Poi;

/**
 * 通行时间估算服务
 */
public interface TravelTimeService {
    
    /**
     * 根据经纬度估算从 from 点到 to 点的通行时间（单位：分钟）
     * 如果考虑更复杂的交通工具，可增加 mode 参数，这里暂时用本地哈弗辛距离+平均配速模拟
     */
    int estimateTravelTimeMinutes(Poi from, Poi to);
}
