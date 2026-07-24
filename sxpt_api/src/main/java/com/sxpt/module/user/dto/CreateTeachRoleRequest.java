package com.sxpt.module.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建教学平台角色请求。
 *
 * 业务功能：
 * 1. 承载管理员新增教学平台内部角色时提交的参数。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段，避免无效请求进入 Service。
 *
 * 关键流程：
 * 1. Controller 接收请求体并触发基础参数校验。
 * 2. Controller 将请求转换为 TeachRole 实体后交给 Service 执行业务校验和写入。
 */
public class CreateTeachRoleRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64, message = "角色编码长度不能超过 64")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 128, message = "角色名称长度不能超过 128")
    private String roleName;

    @Size(max = 512, message = "角色说明长度不能超过 512")
    private String description;

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
}
