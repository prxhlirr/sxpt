package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 原平台角色字典实体。
 *
 * 业务功能：
 * 1. 映射 origin_role 表，保存某个原业务系统暴露给教学平台配置使用的角色编码、名称和原平台角色标识。
 * 2. 为业务模块流程参与方配置提供下拉数据源，避免管理员在模块、模板、策略和数据准备链路中反复手填角色文本。
 *
 * 关键流程：
 * 1. 管理员先在原平台系统下维护角色字典。
 * 2. 业务模块流程参与方后续引用 roleCode 或 externalRoleId，数据准备时再由后端生成稳定的原平台角色参数。
 */
@TableName("origin_role")
public class OriginRole {

    @TableId
    private String id;

    private String tenantId;

    private String connectorSystemId;

    private String roleCode;

    private String roleName;

    private String externalRoleId;

    private String roleType;

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

    public String getExternalRoleId() {
        return externalRoleId;
    }

    public void setExternalRoleId(String externalRoleId) {
        this.externalRoleId = externalRoleId;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
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
