package com.citytrip.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class GenerateReqDTO {
    private Double tripDays;        // 行程天数 0.5/1.0/2.0 等
    private String budgetLevel;     // 预算：低/中/高
    private List<String> themes;    // 主题偏好，如文化、美食
    private Boolean isRainy;        // 是否下雨
    private Boolean isNight;        // 是否包含夜游
    private String walkingLevel;    // 出行强度要求
    private String companionType;   // 出行同伴
    private String startTime;       // 每天出发时间，如 "09:00"
    private String endTime;         // 每天结束时间，如 "18:00"
}
