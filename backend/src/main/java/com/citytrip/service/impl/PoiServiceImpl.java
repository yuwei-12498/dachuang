package com.citytrip.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citytrip.mapper.PoiMapper;
import com.citytrip.model.entity.Poi;
import com.citytrip.service.PoiService;
import org.springframework.stereotype.Service;

@Service
public class PoiServiceImpl extends ServiceImpl<PoiMapper, Poi> implements PoiService {
}
