package com.citytrip.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ChatReqDTO {
    private String question;
    private ChatContext context;

    @Data
    public static class ChatContext {
        private String pageType;
        private List<String> preferences;
        private Boolean rainy;
        private Boolean nightMode;
        private String companionType;
        private String cityCode;
        private String cityName;
        private Double userLat;
        private Double userLng;
        private ChatItineraryContext itinerary;
        private List<ChatRecentPoi> recentPois;
    }

    @Data
    public static class ChatItineraryContext {
        private Long itineraryId;
        private String selectedOptionKey;
        private String summary;
        private Integer totalDuration;
        private BigDecimal totalCost;
        private List<ChatRouteNode> nodes;
    }

    @Data
    public static class ChatRouteNode {
        private Long poiId;
        private String poiName;
        private String category;
        private String district;
        private String startTime;
        private String endTime;
        private Integer travelTime;
        private String travelTransportMode;
        private BigDecimal travelDistanceKm;
        private Integer departureTravelTime;
        private String departureTransportMode;
        private BigDecimal departureDistanceKm;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String sourceType;
    }

    @Data
    public static class ChatRecentPoi {
        private Long poiId;
        private String poiName;
        private String category;
        private String district;
    }
}
