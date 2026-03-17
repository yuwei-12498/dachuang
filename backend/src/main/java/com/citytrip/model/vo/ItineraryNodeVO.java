package com.citytrip.model.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ItineraryNodeVO {
    private Integer stepOrder;     // 步骤顺序号
    private Long poiId;            // 地点ID
    private String poiName;        // 地点名
    private String category;       // 地点分类
    private String district;       // 所在区
    private String startTime;      // 预计开始时间，例如 "09:00"
    private String endTime;        // 预计结束时间，例如 "11:30"
    private Integer stayDuration;  // 预计停留时长 (分钟)
    private Integer travelTime;    // 距离上一个节点的通行时长 (分钟)，第一个节点为0
    private BigDecimal cost;       // 预估花费
    private String sysReason;      // 系统排入这点的理由
}
