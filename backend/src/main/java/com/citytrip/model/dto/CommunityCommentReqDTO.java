package com.citytrip.model.dto;

import lombok.Data;

@Data
public class CommunityCommentReqDTO {
    private Long parentId;
    private String content;
}
