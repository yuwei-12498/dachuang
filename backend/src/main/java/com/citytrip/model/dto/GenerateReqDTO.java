package com.citytrip.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenerateReqDTO {
    @JsonProperty("cityName")
    private String cityName;

    private Double tripDays;
    private String tripDate;
    @JsonProperty("totalBudget")
    private Double totalBudget;
    private String budgetLevel;
    private List<String> themes;
    private Boolean isRainy;
    private Boolean isNight;
    private String walkingLevel;
    private String companionType;
    private String startTime;
    private String endTime;
}
