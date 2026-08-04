package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 原平台能力返回对象。
 *
 * 业务功能：
 * 1. 向前端展示某个原平台已声明的数据准备能力。
 * 2. 屏蔽软删除等持久化细节，只暴露管理端需要核对和维护的字段。
 *
 * 关键流程：
 * 1. Controller 从 PlatformCapability 实体提取展示字段。
 * 2. 前端基于 id 执行编辑、启用、停用等后续操作。
 */
public class PlatformCapabilityVO {

    private String id;

    private String tenantId;

    private String connectorSystemId;

    private String capabilityCode;

    private String capabilityName;

    private String capabilityType;

    private Boolean supportFlag;

    private String endpointUrl;

    private String method;

    private String requestSchemaJson;

    private String responseSchemaJson;

    private Long timeoutMs;

    private String retryPolicyJson;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
