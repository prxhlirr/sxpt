package com.sxpt.module.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 授予教学平台用户角色请求。
 *
 * 业务功能：
 * 1. 承载管理员为教学用户分配教师、学生、管理员等角色时提交的参数。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段。
 *
 * 关键流程：
 * 1. Controller 接收请求体并触发基础参数校验。
 * 2. Controller 将请求转换为 TeachUserRole 实体后交给 Service 执行授权写入。
 */
public class GrantTeachUserRoleRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "用户 ID 不能为空")
    @Size(max = 64, message = "用户 ID 长度不能超过 64")
    private String userId;

    @NotBlank(message = "角色 ID 不能为空")
    @Size(max = 64, message = "角色 ID 长度不能超过 64")
    private String roleId;

    @Size(max = 32, message = "授权来源长度不能超过 32")
    private String grantSource;

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
}
