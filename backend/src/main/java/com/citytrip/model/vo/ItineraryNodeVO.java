package com.citytrip.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ItineraryNodeVO {
    private Integer stepOrder;
    private Long poiId;
    private String poiName;
    private String category;
    private String district;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String startTime;
    private String endTime;
    private Integer stayDuration;
    private Integer travelTime;
    private BigDecimal cost;
    private String sysReason;
    private String operatingStatus;
    private String statusNote;
    private LocalDateTime statusUpdatedAt;
}
