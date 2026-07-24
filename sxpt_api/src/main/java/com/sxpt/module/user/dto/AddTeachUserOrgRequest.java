package com.sxpt.module.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 添加用户到教学组织请求。
 *
 * 业务功能：
 * 1. 承载管理员将学生、教师或管理人员加入班级、课程班或分组时提交的参数。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段。
 *
 * 关键流程：
 * 1. Controller 接收请求体并触发基础参数校验。
 * 2. Controller 将请求转换为 TeachUserOrg 实体后交给 Service 执行关系写入。
 */
public class AddTeachUserOrgRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "组织 ID 不能为空")
    @Size(max = 64, message = "组织 ID 长度不能超过 64")
    private String orgId;

    @NotBlank(message = "用户 ID 不能为空")
    @Size(max = 64, message = "用户 ID 长度不能超过 64")
    private String userId;

    @NotBlank(message = "关系类型不能为空")
    @Size(max = 32, message = "关系类型长度不能超过 32")
    private String relationType;

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

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }
}
