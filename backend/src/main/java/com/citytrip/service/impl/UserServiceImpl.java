package com.citytrip.service.impl;

import com.citytrip.common.BadRequestException;
import com.citytrip.common.UnauthorizedException;
import com.citytrip.mapper.UserMapper;
import com.citytrip.model.dto.LoginReqDTO;
import com.citytrip.model.dto.RegisterReqDTO;
import com.citytrip.model.entity.User;
import com.citytrip.model.vo.UserSessionVO;
import com.citytrip.service.UserService;
import com.citytrip.util.JwtUtil;
import com.citytrip.util.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserSessionVO register(RegisterReqDTO req) {
        String username = normalize(req == null ? null : req.getUsername());
        String nickname = normalize(req == null ? null : req.getNickname());
        String password = req == null ? null : req.getPassword();

        validateRegister(username, nickname, password);

        User existing = userMapper.selectByUsername(username);
        if (existing != null) {
            throw new BadRequestException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPasswordSalt(PasswordUtils.bcryptStorageMarker());
        user.setPasswordHash(PasswordUtils.hashPassword(password));
        user.setRole(0);
        user.setStatus(1);
        userMapper.insert(user);
        return toSessionVO(user);
    }

    @Override
    public UserSessionVO login(LoginReqDTO req) {
        String username = normalize(req == null ? null : req.getUsername());
        String password = req == null ? null : req.getPassword();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BadRequestException("用户名和密码不能为空");
        }

        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        if (!PasswordUtils.matchesPassword(password, user.getPasswordHash(), user.getPasswordSalt())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new UnauthorizedException("抱歉，您的账号已被冻结");
        }

        if (PasswordUtils.needsRehash(user.getPasswordHash())) {
            user.setPasswordHash(PasswordUtils.hashPassword(password));
            user.setPasswordSalt(PasswordUtils.bcryptStorageMarker());
            userMapper.updateById(user);
        }

        return toSessionVO(user);
    }

    @Override
    public UserSessionVO getSessionUser(Long userId) {
        if (userId == null) {
            return null;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return null;
        }
        return toSessionVO(user);
    }

    private void validateRegister(String username, String nickname, String password) {
        if (!StringUtils.hasText(username)) {
            throw new BadRequestException("用户名不能为空");
        }
        if (!StringUtils.hasText(nickname)) {
            throw new BadRequestException("昵称不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new BadRequestException("密码不能为空");
        }
        if (username.length() < 4 || username.length() > 20) {
            throw new BadRequestException("用户名长度需在 4 到 20 位之间");
        }
        if (!username.matches("^[A-Za-z0-9_]+$")) {
            throw new BadRequestException("用户名仅支持字母、数字和下划线");
        }
        if (nickname.length() < 2 || nickname.length() > 20) {
            throw new BadRequestException("昵称长度需在 2 到 20 位之间");
        }
        if (password.length() < 6 || password.length() > 20) {
            throw new BadRequestException("密码长度需在 6 到 20 位之间");
        }
    }

    private UserSessionVO toSessionVO(User user) {
        UserSessionVO vo = new UserSessionVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole() == null ? 0 : user.getRole());
        vo.setToken(jwtUtil.generateToken(vo.getId(), vo.getRole()));
        return vo;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
