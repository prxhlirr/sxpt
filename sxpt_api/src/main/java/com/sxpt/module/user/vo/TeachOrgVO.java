package com.sxpt.module.user.vo;

import java.time.LocalDateTime;

/**
 * 教学组织返回对象。
 *
 * 业务功能：
 * 1. 向前端返回班级、课程班或分组的展示字段。
 * 2. 隔离数据库 Entity，避免前端依赖软删除等持久化细节。
 *
 * 关键流程：
 * 1. Controller 从 TeachOrg 实体提取可展示字段。
 * 2. 前端使用 id 作为后续添加成员、发布课程和发布任务的组织引用。
 */
public class TeachOrgVO {

    private String id;

    private String tenantId;

    private String parentId;

    private String orgCode;

    private String orgName;

    private String orgType;

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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getOrgType() {
        return orgType;
    }

    public void setOrgType(String orgType) {
        this.orgType = orgType;
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
