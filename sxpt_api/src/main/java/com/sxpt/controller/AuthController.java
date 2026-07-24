package com.sxpt.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.security.JwtService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证测试接口。
 *
 * 业务功能：
 * 1. 提供最小 Token 签发入口，便于联调 JWT 认证链路。
 * 2. 后续接入用户模块后，应替换为真实用户名密码校验。
 *
 * 关键流程：
 * 1. 接收用户 ID 和用户名。
 * 2. 调用 JwtService 生成 Token。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * 签发测试 Token。
     *
     * @param userId 用户 ID。
     * @param username 用户名。
     * @return Token 信息。
     */
    @PostMapping("/token")
    public ApiResult<Map<String, Object>> token(@RequestParam String userId, @RequestParam String username) {
        Map<String, Object> tokenInfo = new LinkedHashMap<String, Object>();
        tokenInfo.put("token", jwtService.generateToken(userId, username));
        tokenInfo.put("tokenType", "Bearer");
        return ApiResult.success(tokenInfo);
    }
}
