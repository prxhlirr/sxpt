package com.sxpt.module.user.vo;

import java.time.LocalDateTime;

/**
 * 教学平台角色返回对象。
 *
 * 业务功能：
 * 1. 向前端返回教学角色的展示字段。
 * 2. 隔离数据库 Entity，避免直接暴露后续持久化细节。
 *
 * 关键流程：
 * 1. Controller 或转换器从 TeachRole 实体提取可展示字段。
 * 2. 前端使用 id 作为后续用户授权和权限判断引用。
 */
public class TeachRoleVO {

    private String id;

    private String tenantId;

    private String roleCode;

    private String roleName;

    private String description;

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

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
