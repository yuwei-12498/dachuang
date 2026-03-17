package com.citytrip.model.vo;

import lombok.Data;
import java.util.List;

@Data
public class ChatVO {
    private String answer;
    private List<String> relatedTips; // 相关的提示或推荐问题
}
