package com.citytrip.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ItineraryVO {
    private Integer totalDuration; // 总计耗时(分钟)
    private BigDecimal totalCost;  // 预计总开销
    private String recommendReason;// 大模型生成的综合推荐理由
    private String tips;           // 大模型生成的综合友情提示
    private List<ItineraryNodeVO> nodes; // 具体游览点位列表
}
