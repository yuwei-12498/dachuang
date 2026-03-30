package com.citytrip.service.impl;

import com.citytrip.mapper.UserMapper;
import com.citytrip.model.dto.LoginReqDTO;
import com.citytrip.model.dto.RegisterReqDTO;
import com.citytrip.model.entity.User;
import com.citytrip.model.vo.UserSessionVO;
import com.citytrip.service.UserService;
import com.citytrip.util.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserSessionVO register(RegisterReqDTO req) {
        String username = normalize(req == null ? null : req.getUsername());
        String nickname = normalize(req == null ? null : req.getNickname());
        String password = req == null ? null : req.getPassword();

        validateRegister(username, nickname, password);

        User existing = userMapper.selectByUsername(username);
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        String salt = PasswordUtils.generateSalt();
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPasswordSalt(salt);
        user.setPasswordHash(PasswordUtils.hashPassword(password, salt));
        userMapper.insert(user);
        return toSessionVO(user);
    }

    @Override
    public UserSessionVO login(LoginReqDTO req) {
        String username = normalize(req == null ? null : req.getUsername());
        String password = req == null ? null : req.getPassword();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }

        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String passwordHash = PasswordUtils.hashPassword(password, user.getPasswordSalt());
        if (!passwordHash.equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        return toSessionVO(user);
    }

    @Override
    public UserSessionVO getSessionUser(Long userId) {
        if (userId == null) {
            return null;
        }

        User user = userMapper.selectById(userId);
        return user == null ? null : toSessionVO(user);
    }

    private void validateRegister(String username, String nickname, String password) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!StringUtils.hasText(nickname)) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (username.length() < 4 || username.length() > 20) {
            throw new IllegalArgumentException("用户名长度需为 4 到 20 位");
        }
        if (!username.matches("^[A-Za-z0-9_]+$")) {
            throw new IllegalArgumentException("用户名仅支持字母、数字和下划线");
        }
        if (nickname.length() < 2 || nickname.length() > 20) {
            throw new IllegalArgumentException("昵称长度需为 2 到 20 位");
        }
        if (password.length() < 6 || password.length() > 20) {
            throw new IllegalArgumentException("密码长度需为 6 到 20 位");
        }
    }

    private UserSessionVO toSessionVO(User user) {
        UserSessionVO vo = new UserSessionVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        return vo;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
