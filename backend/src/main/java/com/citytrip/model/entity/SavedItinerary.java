package com.citytrip.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("saved_itinerary")
public class SavedItinerary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String requestJson;
    private String itineraryJson;
    private String customTitle;
    private Integer favorited;
    private LocalDateTime favoriteTime;
    private Integer nodeCount;
    private Integer totalDuration;
    private BigDecimal totalCost;
    private String routeSignature;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
