package com.citytrip.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class GenerateReqDTO {
    private Double tripDays;
    private String budgetLevel;
    private List<String> themes;
    private Boolean isRainy;
    private Boolean isNight;
    private String walkingLevel;
    private String companionType;
    private String startTime;
    private String endTime;
}
