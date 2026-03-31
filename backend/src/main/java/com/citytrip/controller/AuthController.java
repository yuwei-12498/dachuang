package com.citytrip.controller;

import com.citytrip.annotation.LoginRequired;
import com.citytrip.common.AuthConstants;
import com.citytrip.common.UnauthorizedException;
import com.citytrip.model.dto.LoginReqDTO;
import com.citytrip.model.dto.RegisterReqDTO;
import com.citytrip.model.vo.UserSessionVO;
import com.citytrip.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public ResponseEntity<UserSessionVO> register(@RequestBody RegisterReqDTO req, HttpServletRequest request) {
        UserSessionVO vo = userService.register(req);
        saveLoginSession(request.getSession(true), vo.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(vo);
    }

    @PostMapping("/sessions")
    public UserSessionVO login(@RequestBody LoginReqDTO req, HttpServletRequest request) {
        UserSessionVO vo = userService.login(req);
        saveLoginSession(request.getSession(true), vo.getId());
        return vo;
    }

    @DeleteMapping("/sessions/current")
    public ResponseEntity<Void> logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @LoginRequired
    @GetMapping("/users/me")
    public UserSessionVO me(HttpSession session) {
        Long userId = (Long) session.getAttribute(AuthConstants.LOGIN_USER_ID);
        UserSessionVO vo = userService.getSessionUser(userId);
        if (vo == null) {
            session.invalidate();
            throw new UnauthorizedException("登录状态已失效，请重新登录");
        }
        return vo;
    }

    private void saveLoginSession(HttpSession session, Long userId) {
        session.setAttribute(AuthConstants.LOGIN_USER_ID, userId);
        session.setMaxInactiveInterval(7 * 24 * 60 * 60);
    }
}
