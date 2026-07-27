package com.sxpt.controller;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 正式登录请求。
 *
 * 业务功能：
 * 1. 承载教学平台用户登录时提交的可信凭据。
 * 2. 明确前端只能提交登录凭据，不能声明 userId、role、org 等服务端身份结果。
 *
 * 关键流程：
 * 1. Controller 使用 Bean Validation 校验基础字段。
 * 2. AuthLoginService 根据 loginType 选择账号密码或外部身份源校验方式。
 */
public class AuthLoginRequest {

    @NotBlank(message = "登录类型不能为空")
    @Size(max = 32, message = "登录类型长度不能超过 32")
    private String loginType;

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 128, message = "用户名长度不能超过 128")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 256, message = "密码长度不能超过 256")
    private String password;

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
