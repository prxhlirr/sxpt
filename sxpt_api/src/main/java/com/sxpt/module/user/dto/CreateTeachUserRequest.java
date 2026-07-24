package com.sxpt.module.user.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建教学平台用户请求。
 *
 * 业务功能：
 * 1. 承载管理员新增教师、学生、管理员时提交的参数。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段，避免无效请求进入 Service。
 *
 * 关键流程：
 * 1. Controller 接收请求体并触发基础参数校验。
 * 2. Controller 将请求转换为 TeachUser 实体后交给 Service 执行业务校验和写入。
 */
public class CreateTeachUserRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "用户账号不能为空")
    @Size(max = 128, message = "用户账号长度不能超过 128")
    private String username;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 128, message = "真实姓名长度不能超过 128")
    private String realName;

    @Size(max = 32, message = "手机号长度不能超过 32")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    @NotBlank(message = "用户类型不能为空")
    @Size(max = 32, message = "用户类型长度不能超过 32")
    private String userType;

    @NotBlank(message = "用户来源不能为空")
    @Size(max = 32, message = "用户来源长度不能超过 32")
    private String sourceType;

    @Size(max = 4096, message = "外部信息 JSON 长度不能超过 4096")
    private String externalInfoJson;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getExternalInfoJson() {
        return externalInfoJson;
    }

    public void setExternalInfoJson(String externalInfoJson) {
        this.externalInfoJson = externalInfoJson;
    }
}
