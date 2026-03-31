package com.citytrip.model.dto;

import com.citytrip.model.vo.ItineraryNodeVO;
import lombok.Data;

import java.util.List;

@Data
public class ReplanReqDTO {
    private List<ItineraryNodeVO> currentNodes;
    private GenerateReqDTO originalReq;
    private List<String> excludedSignatures;
}
