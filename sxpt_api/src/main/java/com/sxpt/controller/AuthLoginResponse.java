package com.sxpt.controller;

import java.util.Collections;
import java.util.List;

/**
 * 正式登录返回对象。
 *
 * 业务功能：
 * 1. 返回登录成功后的 JWT 和当前用户摘要。
 * 2. 隔离密码哈希、salt、失败次数等认证凭据字段，避免前端获得安全内部状态。
 *
 * 关键流程：
 * 1. AuthLoginService 校验凭据后签发 Token。
 * 2. 前端保存 token，并通过 auth/me 或当前返回的 user 摘要初始化工作台。
 */
public class AuthLoginResponse {

    private String token;

    private String tokenType;

    private Long expiresIn;

    private UserSummary user;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserSummary getUser() {
        return user;
    }

    public void setUser(UserSummary user) {
        this.user = user;
    }

    /**
     * 当前用户摘要。
     *
     * 业务功能：
     * 1. 表达已通过服务端认证的教学平台用户。
     * 2. 预留角色、组织和身份绑定摘要，后续接对象级授权时补齐。
     */
    public static class UserSummary {

        private String userId;

        private String tenantId;

        private String username;

        private String displayName;

        private String userType;

        private String studentNo;

        private String employeeNo;

        private List<String> roles = Collections.emptyList();

        private List<String> orgIds = Collections.emptyList();

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
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

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getUserType() {
            return userType;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }

        public String getStudentNo() {
            return studentNo;
        }

        public void setStudentNo(String studentNo) {
            this.studentNo = studentNo;
        }

        public String getEmployeeNo() {
            return employeeNo;
        }

        public void setEmployeeNo(String employeeNo) {
            this.employeeNo = employeeNo;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public List<String> getOrgIds() {
            return orgIds;
        }

        public void setOrgIds(List<String> orgIds) {
            this.orgIds = orgIds;
        }
    }
}
