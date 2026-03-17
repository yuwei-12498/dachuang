package com.citytrip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.citytrip.model.entity.Poi;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PoiMapper extends BaseMapper<Poi> {
}
