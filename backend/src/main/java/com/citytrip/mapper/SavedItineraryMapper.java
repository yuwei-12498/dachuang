package com.citytrip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.citytrip.model.entity.SavedItinerary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SavedItineraryMapper extends BaseMapper<SavedItinerary> {
}
