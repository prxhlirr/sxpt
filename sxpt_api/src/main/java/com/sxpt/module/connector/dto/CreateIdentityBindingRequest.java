package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建原平台身份绑定请求。
 *
 * 业务功能：
 * 1. 承载管理员维护教学用户与原平台账号绑定时提交的参数。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段。
 *
 * 关键流程：
 * 1. Controller 接收请求体并触发基础参数校验。
 * 2. Controller 将请求转换为 IdentityBinding 实体后交给 Service 执行绑定写入。
 */
public class CreateIdentityBindingRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "教学用户 ID 不能为空")
    @Size(max = 64, message = "教学用户 ID 长度不能超过 64")
    private String userId;

    @NotBlank(message = "原平台配置 ID 不能为空")
    @Size(max = 64, message = "原平台配置 ID 长度不能超过 64")
    private String connectorSystemId;

    @NotBlank(message = "原平台用户 ID 不能为空")
    @Size(max = 128, message = "原平台用户 ID 长度不能超过 128")
    private String externalUserId;

    @Size(max = 128, message = "原平台账号长度不能超过 128")
    private String externalUsername;

    @Size(max = 4096, message = "原平台角色快照长度不能超过 4096")
    private String externalRoleJson;

    @Size(max = 4096, message = "原平台组织快照长度不能超过 4096")
    private String externalOrgJson;

    @NotBlank(message = "绑定类型不能为空")
    @Size(max = 32, message = "绑定类型长度不能超过 32")
    private String bindingType;

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
}
