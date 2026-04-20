package com.citytrip.model.dto;

import lombok.Data;

@Data
public class SaveItineraryReqDTO {
    private Long sourceItineraryId;
    private String selectedOptionKey;
    private String title;
}
