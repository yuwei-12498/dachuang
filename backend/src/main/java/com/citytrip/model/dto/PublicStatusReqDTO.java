package com.citytrip.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class PublicStatusReqDTO {
    private Boolean isPublic;
    private String title;
    private String shareNote;
    private String selectedOptionKey;
    private List<String> themes;
}