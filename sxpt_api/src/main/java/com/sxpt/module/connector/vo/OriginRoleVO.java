package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 原平台角色字典返回对象。
 *
 * 业务功能：
 * 1. 向后台页面展示可选原平台角色。
 * 2. 屏蔽软删除等持久化细节，只返回配置、选择和审计需要的字段。
 *
 * 关键流程：
 * 1. Controller 从 OriginRole 实体提取展示字段。
 * 2. 前端用 id 或 roleCode 作为后续流程参与方绑定依据。
 */
public class OriginRoleVO {

    private String id;

    private String tenantId;

    private String connectorSystemId;

    private String roleCode;

    private String roleName;

    private String externalRoleId;

    private String roleType;

    private String remark;

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
