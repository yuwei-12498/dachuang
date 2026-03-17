package com.citytrip.model.dto;

import lombok.Data;
import com.citytrip.model.vo.ItineraryNodeVO;
import java.util.List;

@Data
public class ReplaceReqDTO {
    private Long targetPoiId;                    // 需要被替换的景点ID
    private List<ItineraryNodeVO> currentNodes;  // 当前完整的景点列表
    private GenerateReqDTO originalReq;          // 原始偏好请求，用于参考
}
