package com.citytrip.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItineraryNodeVO {
    private Integer stepOrder;
    private Long poiId;
    private String poiName;
    private String category;
    private String district;
    private String startTime;
    private String endTime;
    private Integer stayDuration;
    private Integer travelTime;
    private BigDecimal cost;
    private String sysReason;
}
