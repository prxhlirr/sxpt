package com.sxpt.module.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建教学组织请求。
 *
 * 业务功能：
 * 1. 承载管理员新增班级、课程班或分组时提交的参数。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段。
 *
 * 关键流程：
 * 1. Controller 接收请求体并触发基础参数校验。
 * 2. Controller 将请求转换为 TeachOrg 实体后交给 Service 执行业务校验和写入。
 */
public class CreateTeachOrgRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @Size(max = 64, message = "父级组织 ID 长度不能超过 64")
    private String parentId;

    @NotBlank(message = "组织编码不能为空")
    @Size(max = 64, message = "组织编码长度不能超过 64")
    private String orgCode;

    @NotBlank(message = "组织名称不能为空")
    @Size(max = 128, message = "组织名称长度不能超过 128")
    private String orgName;

    @NotBlank(message = "组织类型不能为空")
    @Size(max = 32, message = "组织类型长度不能超过 32")
    private String orgType;

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
}
