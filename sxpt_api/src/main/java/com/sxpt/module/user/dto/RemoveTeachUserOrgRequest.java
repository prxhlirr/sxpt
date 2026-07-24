package com.sxpt.module.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 从教学组织移除用户请求。
 *
 * 业务功能：
 * 1. 承载管理员将用户从班级、课程班或分组中移除时提交的参数。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段。
 *
 * 关键流程：
 * 1. Controller 接收请求体并触发基础参数校验。
 * 2. Controller 将租户、组织和用户三元组交给 Service 执行软删除。
 */
public class RemoveTeachUserOrgRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "组织 ID 不能为空")
    @Size(max = 64, message = "组织 ID 长度不能超过 64")
    private String orgId;

    @NotBlank(message = "用户 ID 不能为空")
    @Size(max = 64, message = "用户 ID 长度不能超过 64")
    private String userId;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
