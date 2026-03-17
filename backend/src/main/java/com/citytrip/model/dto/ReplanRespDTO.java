package com.citytrip.model.dto;

import com.citytrip.model.vo.ItineraryVO;
import lombok.Data;

@Data
public class ReplanRespDTO {
    private Boolean success;
    private String message;
    private Boolean changed;     // 路线是否发生了实质性变化
    private String reason;       // 若无变化的原因提示
    private ItineraryVO itinerary; // 最新的路线结果
}
