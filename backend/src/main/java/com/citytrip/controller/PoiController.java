package com.citytrip.controller;

import com.citytrip.common.Result;
import com.citytrip.model.entity.Poi;
import com.citytrip.service.PoiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/poi")
public class PoiController {

    @Autowired
    private PoiService poiService;

    @GetMapping("/list")
    public Result<List<Poi>> list() {
        return Result.success(poiService.list());
    }

    @GetMapping("/{id}")
    public Result<Poi> detail(@PathVariable("id") Long id) {
        Poi poi = poiService.getById(id);
        if (poi == null) {
            return Result.error("未找到对应景点信息");
        }
        return Result.success(poi);
    }
}
