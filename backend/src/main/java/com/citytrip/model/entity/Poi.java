package com.citytrip.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@TableName("poi")
public class Poi {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String district;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalTime openTime;
    private LocalTime closeTime;
    private BigDecimal avgCost;
    private Integer stayDuration;
    private Integer indoor;
    private Integer nightAvailable;
    private Integer rainFriendly;
    private String walkingLevel;
    private String tags;
    private String suitableFor;
    private String description;
    private BigDecimal priorityScore;

    @TableField(exist = false)
    private Double tempScore;
}
