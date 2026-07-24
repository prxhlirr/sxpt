package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 原平台身份绑定实体。
 *
 * 业务功能：
 * 1. 映射 identity_binding 表，保存教学用户与原平台账号之间的绑定关系。
 * 2. 为后续生成 launchToken 前查找原平台执行账号、角色快照和组织快照提供基础数据。
 *
 * 关键流程：
 * 1. 管理员或同步任务维护教学用户与原平台用户 ID 的绑定关系。
 * 2. 跳转原平台前通过教学用户、原平台配置和租户定位可用绑定身份。
 * 3. 原平台角色和组织只保存快照，不直接等同教学平台角色和组织。
 */
@TableName("identity_binding")
public class IdentityBinding {

    @TableId
    private String id;

    private String tenantId;

    private String userId;

    private String connectorSystemId;

    private String externalUserId;

    private String externalUsername;

    private String externalRoleJson;

    private String externalOrgJson;

    private String bindingType;

    private LocalDateTime lastLoginTime;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    private String status;

    private Boolean deleted;

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

    public String getExternalRoleJson() {
        return externalRoleJson;
    }

    public void setExternalRoleJson(String externalRoleJson) {
        this.externalRoleJson = externalRoleJson;
    }

    public String getExternalOrgJson() {
        return externalOrgJson;
    }

    public void setExternalOrgJson(String externalOrgJson) {
        this.externalOrgJson = externalOrgJson;
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

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
