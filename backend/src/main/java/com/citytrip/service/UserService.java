package com.citytrip.service;

import com.citytrip.model.dto.LoginReqDTO;
import com.citytrip.model.dto.RegisterReqDTO;
import com.citytrip.model.vo.UserSessionVO;

public interface UserService {
    UserSessionVO register(RegisterReqDTO req);

    UserSessionVO login(LoginReqDTO req);

    UserSessionVO getSessionUser(Long userId);
}
