package com.citytrip.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.LocalDateTime;

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
    private Integer stayDuration; // 建议停留时间，单位分钟
    private Integer indoor; // 1 室内 0 室外
    private Integer nightAvailable; // 1 支持夜游 0 否
    private Integer rainFriendly; // 1 雨天友好 0 否
    private String walkingLevel; // 步数强度：低/中/高
    private String tags; // 文化,历史,美食等
    private String suitableFor; // 亲子,情侣,朋友等
    private String description;
    private BigDecimal priorityScore; // 基础热度打分

    // 数据库中不存在此字段，仅用于行程生成过程中的动态打分
    @TableField(exist = false)
    private Double tempScore;
}
