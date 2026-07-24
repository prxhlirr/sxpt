package com.sxpt.module.user.vo;

import java.time.LocalDateTime;

/**
 * 教学用户组织关系返回对象。
 *
 * 业务功能：
 * 1. 向前端返回班级成员或课程班成员关系的展示字段。
 * 2. 隔离数据库 Entity，避免前端依赖软删除等持久化细节。
 *
 * 关键流程：
 * 1. Controller 从 TeachUserOrg 实体提取可展示字段。
 * 2. 前端使用 id 作为后续移除成员或查询成员关系的引用。
 */
public class TeachUserOrgVO {

    private String id;

    private String tenantId;

    private String userId;

    private String orgId;

    private String relationType;

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

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
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
