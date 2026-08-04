package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 原平台能力维护请求。
 *
 * 业务功能：
 * 1. 承载管理员为某个原平台登记数据创建、校验、锁定、结果检查等能力时提交的参数。
 * 2. 把能力声明作为原平台接入边界的一部分，避免策略启用时才依赖不可追溯的隐式配置。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发基础字段校验。
 * 2. Service 继续校验 JSON 结构、唯一能力编码和运行态必填字段。
 */
public class PlatformCapabilityRequest {

    @Size(max = 64, message = "能力 ID 长度不能超过 64")
    private String id;

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "原平台 ID 不能为空")
    @Size(max = 64, message = "原平台 ID 长度不能超过 64")
    private String connectorSystemId;

    @NotBlank(message = "能力编码不能为空")
    @Size(max = 64, message = "能力编码长度不能超过 64")
    private String capabilityCode;

    @NotBlank(message = "能力名称不能为空")
    @Size(max = 128, message = "能力名称长度不能超过 128")
    private String capabilityName;

    @NotBlank(message = "能力类型不能为空")
    @Size(max = 64, message = "能力类型长度不能超过 64")
    private String capabilityType;

    @NotNull(message = "是否支持不能为空")
    private Boolean supportFlag;

    @NotBlank(message = "能力接口地址不能为空")
    @Size(max = 512, message = "能力接口地址长度不能超过 512")
    private String endpointUrl;

    @NotBlank(message = "HTTP 方法不能为空")
    @Size(max = 16, message = "HTTP 方法长度不能超过 16")
    private String method;

    @Size(max = 4096, message = "请求 Schema JSON 长度不能超过 4096")
    private String requestSchemaJson;

    @Size(max = 4096, message = "响应 Schema JSON 长度不能超过 4096")
    private String responseSchemaJson;

    private Long timeoutMs;

    @Size(max = 2048, message = "重试策略 JSON 长度不能超过 2048")
    private String retryPolicyJson;

    @Size(max = 64, message = "创建人长度不能超过 64")
    private String createBy;

    @Size(max = 64, message = "更新人长度不能超过 64")
    private String updateBy;

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

    public String getCapabilityCode() {
        return capabilityCode;
    }

    public void setCapabilityCode(String capabilityCode) {
        this.capabilityCode = capabilityCode;
    }

    public String getCapabilityName() {
        return capabilityName;
    }

    public void setCapabilityName(String capabilityName) {
        this.capabilityName = capabilityName;
    }

    public String getCapabilityType() {
        return capabilityType;
    }

    public void setCapabilityType(String capabilityType) {
        this.capabilityType = capabilityType;
    }

    public Boolean getSupportFlag() {
        return supportFlag;
    }

    public void setSupportFlag(Boolean supportFlag) {
        this.supportFlag = supportFlag;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getRequestSchemaJson() {
        return requestSchemaJson;
    }

    public void setRequestSchemaJson(String requestSchemaJson) {
        this.requestSchemaJson = requestSchemaJson;
    }

    public String getResponseSchemaJson() {
        return responseSchemaJson;
    }

    public void setResponseSchemaJson(String responseSchemaJson) {
        this.responseSchemaJson = responseSchemaJson;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getRetryPolicyJson() {
        return retryPolicyJson;
    }

    public void setRetryPolicyJson(String retryPolicyJson) {
        this.retryPolicyJson = retryPolicyJson;
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
