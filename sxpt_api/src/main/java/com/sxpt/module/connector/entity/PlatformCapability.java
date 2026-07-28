package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 原平台能力注册实体。
 *
 * 业务功能：
 * 1. 映射 platform_capability 表，记录某个原平台实际开放的数据创建、查询、锁定、结果校验和归档能力。
 * 2. 为数据策略启用、数据准备执行和原平台联调提供统一的能力注册依据。
 *
 * 关键流程：
 * 1. 管理端或同步任务先把原平台支持的能力注册到本表。
 * 2. 数据策略启用前读取本表，确认策略依赖的能力已启用且被原平台声明支持。
 * 3. 后续执行数据准备时再根据 endpointUrl、method、schema 和重试策略完成真实调用。
 */
@TableName("platform_capability")
public class PlatformCapability {

    @TableId
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

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    private String status;

    private Boolean deleted;

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
