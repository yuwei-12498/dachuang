package com.citytrip.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String passwordSalt;
    private String nickname;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
