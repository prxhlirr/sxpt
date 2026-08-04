package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 原平台组织字典实体。
 *
 * 业务功能：
 * 1. 映射 origin_org 表，保存某个原业务系统暴露给教学平台配置使用的组织编码、名称和原平台组织标识。
 * 2. 为业务模块流程参与方配置提供组织下拉数据源，避免管理员手填组织文本导致造数参数不可追溯。
 *
 * 关键流程：
 * 1. 管理员先在原平台系统下维护组织字典。
 * 2. 业务模块流程参与方后续引用 orgCode 或 externalOrgId，数据准备时再由后端生成稳定的原平台组织参数。
 */
@TableName("origin_org")
public class OriginOrg {

    @TableId
    private String id;

    private String tenantId;

    private String connectorSystemId;

    private String orgCode;

    private String orgName;

    private String externalOrgId;

    private String parentExternalOrgId;

    private String orgType;

    private String remark;

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

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
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

    public String getExternalOrgId() {
        return externalOrgId;
    }

    public void setExternalOrgId(String externalOrgId) {
        this.externalOrgId = externalOrgId;
    }

    public String getParentExternalOrgId() {
        return parentExternalOrgId;
    }

    public void setParentExternalOrgId(String parentExternalOrgId) {
        this.parentExternalOrgId = parentExternalOrgId;
    }

    public String getOrgType() {
        return orgType;
    }

    public void setOrgType(String orgType) {
        this.orgType = orgType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
