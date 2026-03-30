package com.citytrip.mapper;

import com.citytrip.model.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("""
            select id, username, password_hash, password_salt, nickname, create_time, update_time
            from trip_user
            where username = #{username}
            limit 1
            """)
    @Results(id = "userResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "username", column = "username"),
            @Result(property = "passwordHash", column = "password_hash"),
            @Result(property = "passwordSalt", column = "password_salt"),
            @Result(property = "nickname", column = "nickname"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time")
    })
    User selectByUsername(String username);

    @Select("""
            select id, username, password_hash, password_salt, nickname, create_time, update_time
            from trip_user
            where id = #{id}
            limit 1
            """)
    @ResultMap("userResultMap")
    User selectById(Long id);

    @Insert("""
            insert into trip_user (username, password_hash, password_salt, nickname, create_time, update_time)
            values (#{username}, #{passwordHash}, #{passwordSalt}, #{nickname}, now(), now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}
