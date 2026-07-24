package com.sxpt.common.security;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * Shiro JWT 认证 Token。
 *
 * 业务功能：
 * 1. 让 Shiro Realm 能识别 JWT 类型认证请求。
 * 2. 保持 JWT 字符串作为凭证，避免在过滤器中提前展开权限逻辑。
 *
 * 关键流程：
 * 1. 认证拦截器从请求头提取 JWT 字符串。
 * 2. Shiro Realm 使用该对象完成认证。
 */
public class JwtAuthenticationToken implements AuthenticationToken {

    private final String token;

    public JwtAuthenticationToken(String token) {
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
