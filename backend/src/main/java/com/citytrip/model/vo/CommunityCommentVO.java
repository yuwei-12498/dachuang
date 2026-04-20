package com.citytrip.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
public class CommunityCommentVO {
    private Long id;
    private Long itineraryId;
    private Long parentId;
    private String content;
    private String authorLabel;
    private LocalDateTime createTime;
    private Boolean mine;
    private List<CommunityCommentVO> replies = Collections.emptyList();
}
