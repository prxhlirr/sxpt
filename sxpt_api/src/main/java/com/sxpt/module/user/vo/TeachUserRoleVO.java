package com.sxpt.module.user.vo;

import java.time.LocalDateTime;

/**
 * 教学平台用户角色关系返回对象。
 *
 * 业务功能：
 * 1. 向前端返回用户角色授权关系的展示字段。
 * 2. 隔离数据库 Entity，避免前端依赖软删除等持久化细节。
 *
 * 关键流程：
 * 1. Controller 从 TeachUserRole 实体提取可展示字段。
 * 2. 前端使用 id 作为后续撤销授权或查询授权关系的引用。
 */
public class TeachUserRoleVO {

    private String id;

    private String tenantId;

    private String userId;

    private String roleId;

    private String grantSource;

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

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getGrantSource() {
        return grantSource;
    }

    public void setGrantSource(String grantSource) {
        this.grantSource = grantSource;
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
