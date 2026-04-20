package com.citytrip.model.dto;

import lombok.Data;

@Data
public class PublicStatusReqDTO {
    private Boolean isPublic;
    private String title;
    private String shareNote;
    private String selectedOptionKey;
}
