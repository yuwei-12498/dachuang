package com.citytrip.model.dto;

import lombok.Data;

@Data
public class RegisterReqDTO {
    private String username;
    private String password;
    private String nickname;
}
