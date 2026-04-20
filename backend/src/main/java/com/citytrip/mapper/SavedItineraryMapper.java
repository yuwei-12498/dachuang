package com.citytrip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.citytrip.model.entity.SavedItinerary;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SavedItineraryMapper extends BaseMapper<SavedItinerary> {
    @Select("""
            select id, user_id, request_json, itinerary_json, custom_title, share_note,
                   favorited, favorite_time, is_public, node_count, total_duration,
                   total_cost, route_signature, create_time, update_time
            from saved_itinerary
            where id = #{itineraryId} and user_id = #{userId}
            for update
            """)
    SavedItinerary selectOwnedForUpdate(@Param("userId") Long userId, @Param("itineraryId") Long itineraryId);
}
