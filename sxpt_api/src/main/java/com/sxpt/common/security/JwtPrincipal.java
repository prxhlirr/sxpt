package com.sxpt.common.security;

import java.util.Collections;
import java.util.List;

/**
 * JWT 认证主体。
 *
 * 业务功能：
 * 1. 表达通过 JWT 认证后的用户身份。
 * 2. 为后续权限、审计、数据范围控制提供稳定用户标识。
 *
 * 关键流程：
 * 1. JwtService 从 Token 解析用户 ID 和用户名。
 * 2. JWT Realm 将该对象放入 Shiro Principal。
 */
public class JwtPrincipal {

    private final String userId;

    private final String username;

    private final String tenantId;

    private final List<String> roles;

    public JwtPrincipal(String userId, String username) {
        this(userId, username, null, Collections.<String>emptyList());
    }

    public JwtPrincipal(String userId, String username, String tenantId, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.tenantId = tenantId;
        this.roles = roles == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(roles);
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getTenantId() {
        return tenantId;
    }

    public List<String> getRoles() {
        return roles;
    }
}
