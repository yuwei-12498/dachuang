package com.citytrip.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatReqDTO {
    private String question;
    
    // 我们将原来的 String context 拆开或封装，以便前端传真实的参数
    private ChatContext context;
    
    @Data
    public static class ChatContext {
        private String pageType;         // "home" 或 "result" 之类的页面标识
        private List<String> preferences; // 用户的打勾主题 "文化","拍照" 等
        private Boolean rainy;           // 是否雨天
        private Boolean nightMode;       // 是否夜游
        private String companionType;    // "亲子", "独自" 等
    }
}
