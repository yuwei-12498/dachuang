package com.citytrip.controller;

import com.citytrip.annotation.LoginRequired;
import com.citytrip.common.AuthConstants;
import com.citytrip.common.Result;
import com.citytrip.model.dto.LoginReqDTO;
import com.citytrip.model.dto.RegisterReqDTO;
import com.citytrip.model.vo.UserSessionVO;
import com.citytrip.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<UserSessionVO> register(@RequestBody RegisterReqDTO req, HttpServletRequest request) {
        try {
            UserSessionVO vo = userService.register(req);
            saveLoginSession(request.getSession(true), vo.getId());
            return Result.success(vo);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<UserSessionVO> login(@RequestBody LoginReqDTO req, HttpServletRequest request) {
        try {
            UserSessionVO vo = userService.login(req);
            saveLoginSession(request.getSession(true), vo.getId());
            return Result.success(vo);
        } catch (IllegalArgumentException e) {
            return Result.error(401, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return Result.success();
    }

    @LoginRequired
    @GetMapping("/me")
    public Result<UserSessionVO> me(HttpSession session) {
        Long userId = (Long) session.getAttribute(AuthConstants.LOGIN_USER_ID);
        UserSessionVO vo = userService.getSessionUser(userId);
        if (vo == null) {
            session.invalidate();
            return Result.error(401, "登录状态已失效，请重新登录");
        }
        return Result.success(vo);
    }

    private void saveLoginSession(HttpSession session, Long userId) {
        session.setAttribute(AuthConstants.LOGIN_USER_ID, userId);
        session.setMaxInactiveInterval(7 * 24 * 60 * 60);
    }
}
