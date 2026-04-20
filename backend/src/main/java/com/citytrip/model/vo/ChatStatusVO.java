package com.citytrip.model.vo;

import lombok.Data;

@Data
public class ChatStatusVO {
    private String provider;
    private boolean configured;
    private boolean realModelAvailable;
    private boolean fallbackToMock;
    private int timeoutSeconds;
    private String model;
    private String baseUrl;
    private String message;
}
