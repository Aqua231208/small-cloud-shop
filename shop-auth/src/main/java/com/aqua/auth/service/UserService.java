package com.aqua.auth.service;

import com.aqua.auth.dto.LoginDTO;
import com.aqua.auth.dto.LoginVO;
import com.aqua.auth.dto.RegisterDTO;
import com.aqua.auth.entity.User;
import com.aqua.auth.mapper.UserMapper;
import com.aqua.common.exception.BusinessException;
import com.aqua.common.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public void register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes(StandardCharsets.UTF_8)));
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        userMapper.insert(user);
    }

    public LoginVO login(LoginDTO dto) {
        String encryptedPwd = DigestUtils.md5DigestAsHex(dto.getPassword().getBytes(StandardCharsets.UTF_8));
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
                        .eq(User::getPassword, encryptedPwd));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        return LoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();
    }
}
