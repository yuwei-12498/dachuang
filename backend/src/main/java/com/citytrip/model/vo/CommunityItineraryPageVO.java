package com.citytrip.model.vo;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class CommunityItineraryPageVO {
    private Integer page;
    private Integer size;
    private Long total;
    private List<CommunityItineraryVO> records = Collections.emptyList();
}
