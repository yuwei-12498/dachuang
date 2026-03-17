package com.citytrip.controller;

import com.citytrip.common.Result;
import com.citytrip.model.dto.GenerateReqDTO;
import com.citytrip.model.dto.ReplaceReqDTO;
import com.citytrip.model.dto.ReplanReqDTO;
import com.citytrip.model.dto.ReplanRespDTO;
import com.citytrip.model.vo.ItineraryVO;
import com.citytrip.service.ItineraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/itinerary")
public class ItineraryController {

    @Autowired
    private ItineraryService itineraryService;

    @PostMapping("/generate")
    public Result<ItineraryVO> generateItinerary(@RequestBody GenerateReqDTO req) {
        ItineraryVO vo = itineraryService.generateUserItinerary(req);
        return Result.success(vo);
    }

    @PostMapping("/replace")
    public Result<ItineraryVO> replacePoi(@RequestBody ReplaceReqDTO req) {
        ItineraryVO vo = itineraryService.replaceNode(req);
        return Result.success(vo);
    }

    @PostMapping("/replan")
    public Result<ReplanRespDTO> replanItinerary(@RequestBody ReplanReqDTO req) {
        ReplanRespDTO resp = itineraryService.replan(req);
        return Result.success(resp);
    }
}
