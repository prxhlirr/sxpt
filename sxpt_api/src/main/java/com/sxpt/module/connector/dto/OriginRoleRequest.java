package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 原平台角色字典维护请求。
 *
 * 业务功能：
 * 1. 承载管理员创建或更新原平台角色字典时提交的字段。
 * 2. 用显式字段替代自由文本，保证后续模块流程参与方只能绑定可追溯的原平台角色。
 *
 * 关键流程：
 * 1. Controller 先执行基础长度和必填校验。
 * 2. Service 再校验原平台存在性、角色编码唯一性和生命周期默认值。
 */
public class OriginRoleRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "原平台 ID 不能为空")
    @Size(max = 64, message = "原平台 ID 长度不能超过 64")
    private String connectorSystemId;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 128, message = "角色编码长度不能超过 128")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 128, message = "角色名称长度不能超过 128")
    private String roleName;

    @Size(max = 128, message = "原平台角色 ID 长度不能超过 128")
    private String externalRoleId;

    @Size(max = 64, message = "角色类型长度不能超过 64")
    private String roleType;

    @Size(max = 512, message = "备注长度不能超过 512")
    private String remark;

    @Size(max = 64, message = "创建人长度不能超过 64")
    private String createBy;

    @Size(max = 64, message = "更新人长度不能超过 64")
    private String updateBy;

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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
}
