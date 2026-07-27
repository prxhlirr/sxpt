package com.sxpt.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.security.AuthLoginService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 正式登录接口。
 *
 * 业务功能：
 * 1. 提供教学平台正式账号密码登录入口。
 * 2. 替代测试 Token 签发接口，确保生产 Token 来自服务端可信用户校验。
 *
 * 关键流程：
 * 1. 接收 loginType、tenantId、username、password 等登录凭据。
 * 2. 委托 AuthLoginService 校验凭据并签发 JWT。
 * 3. 返回 Token 和当前用户摘要。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthLoginController {

    private final AuthLoginService authLoginService;

    public AuthLoginController(AuthLoginService authLoginService) {
        this.authLoginService = authLoginService;
    }

    /**
     * 正式登录。
     *
     * @param request 登录请求。
     * @return 登录成功后的 Token 和用户摘要。
     */
    @PostMapping("/login")
    public ApiResult<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ApiResult.success(authLoginService.login(request));
    }
}
