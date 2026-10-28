package com.zykj.ai_backend.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zykj.ai_backend.common.Result;
import com.zykj.ai_backend.entity.User;
import com.zykj.ai_backend.mapper.UserMapper;
import com.zykj.ai_backend.util.JwtUtil;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Resource
    public UserMapper userMapper;
    @Resource
    public JwtUtil jwtUtil;

    @RequestMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> map) {
        String username = map.get("username");
        String password = map.get("password");
        System.out.println("username:" + username + " password:" + password);
        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", username)
                .eq("password", password));
        if (user != null) {
            String token = jwtUtil.generateToken(String.valueOf(user.getId()));
            return Result.ok(200, "登录成功", token);
        }
        return Result.fail(400, "用户名或密码错误");
    }

    @RequestMapping("/register")
    public Result<String> register(@RequestBody Map<String, String> map) {
        String username = map.get("username");
        String password = map.get("password");
        String nickname = map.get("nickname");
        // 检查用户名是否已存在
        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", username));
        if (user != null) {
            return Result.fail(400, "该用户名已存在");
        }
        // 创建用户
        user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(nickname);
        user.setDeleted(0);
        int n = userMapper.insert(user);
        if (n > 0) {
            return Result.ok(200, "注册成功！", null);
        }
        return Result.fail(400, "注册失败");
    }
}
