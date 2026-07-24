package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 原平台身份绑定返回对象。
 *
 * 业务功能：
 * 1. 向前端返回教学用户与原平台账号绑定的展示字段。
 * 2. 隔离数据库 Entity，避免前端依赖原平台角色和组织快照结构。
 *
 * 关键流程：
 * 1. Controller 从 IdentityBinding 实体提取可展示字段。
 * 2. 前端使用 id 作为后续查询、更新或禁用绑定关系的引用。
 */
public class IdentityBindingVO {

    private String id;

    private String tenantId;

    private String userId;

    private String connectorSystemId;

    private String externalUserId;

    private String externalUsername;

    private String bindingType;

    private LocalDateTime lastLoginTime;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public String getExternalUsername() {
        return externalUsername;
    }

    public void setExternalUsername(String externalUsername) {
        this.externalUsername = externalUsername;
    }

    public String getBindingType() {
        return bindingType;
    }

    public void setBindingType(String bindingType) {
        this.bindingType = bindingType;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
