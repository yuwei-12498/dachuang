package com.citytrip.model.dto;

import lombok.Data;
import com.citytrip.model.vo.ItineraryNodeVO;
import java.util.List;

@Data
public class ReplanReqDTO {
    // 包含当前拖拽排序后或需要重新梳理时间的节点
    private List<ItineraryNodeVO> currentNodes; 
    private GenerateReqDTO originalReq;
}
