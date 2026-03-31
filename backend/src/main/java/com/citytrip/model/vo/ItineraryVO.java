package com.citytrip.model.vo;

import com.citytrip.model.dto.GenerateReqDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItineraryVO {
    private Long id;
    private String customTitle;
    private Integer totalDuration;
    private BigDecimal totalCost;
    private String recommendReason;
    private String tips;
    private Boolean favorited;
    private LocalDateTime favoriteTime;
    private GenerateReqDTO originalReq;
    private List<String> alerts;
    private LocalDateTime lastSavedAt;
    private String selectedOptionKey;
    private List<ItineraryOptionVO> options;
    private List<ItineraryNodeVO> nodes;
}
