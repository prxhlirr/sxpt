package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建原业务平台配置请求。
 *
 * 业务功能：
 * 1. 承载管理员新增原平台接入配置时提交的参数。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段，避免无效请求进入 Service。
 *
 * 关键流程：
 * 1. Controller 接收请求体并触发基础参数校验。
 * 2. Controller 将请求转换为 ConnectorSystem 实体后交给 Service 执行业务校验和写入。
 */
public class CreateConnectorSystemRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "原平台编码不能为空")
    @Size(max = 64, message = "原平台编码长度不能超过 64")
    private String systemCode;

    @NotBlank(message = "原平台名称不能为空")
    @Size(max = 128, message = "原平台名称长度不能超过 128")
    private String systemName;

    @NotBlank(message = "原平台类型不能为空")
    @Size(max = 64, message = "原平台类型长度不能超过 64")
    private String systemType;

    @Size(max = 32, message = "原平台环境类型长度不能超过 32")
    private String environmentType;

    @Size(max = 128, message = "原平台环境组编码长度不能超过 128")
    private String environmentGroupCode;

    @NotBlank(message = "原平台基础地址不能为空")
    @Size(max = 512, message = "原平台基础地址长度不能超过 512")
    private String baseUrl;

    @NotBlank(message = "认证方式不能为空")
    @Size(max = 32, message = "认证方式长度不能超过 32")
    private String authType;

    @Size(max = 4096, message = "原平台配置 JSON 长度不能超过 4096")
    private String configJson;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getSystemType() {
        return systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public String getEnvironmentType() {
        return environmentType;
    }

    public void setEnvironmentType(String environmentType) {
        this.environmentType = environmentType;
    }

    public String getEnvironmentGroupCode() {
        return environmentGroupCode;
    }

    public void setEnvironmentGroupCode(String environmentGroupCode) {
        this.environmentGroupCode = environmentGroupCode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }
}
