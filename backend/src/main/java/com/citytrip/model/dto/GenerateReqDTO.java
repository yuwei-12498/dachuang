package com.citytrip.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenerateReqDTO {
    @JsonProperty("cityName")
    private String cityName;
    @JsonProperty("cityCode")
    private String cityCode;

    private Double tripDays;
    private String tripDate;
    @JsonProperty("totalBudget")
    private Double totalBudget;
    private String budgetLevel;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> themes;
    private Boolean isRainy;
    private Boolean isNight;
    private String walkingLevel;
    private String companionType;
    private String startTime;
    private String endTime;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> mustVisitPoiNames;
    private String departurePlaceName;
    private Double departureLatitude;
    private Double departureLongitude;
}
