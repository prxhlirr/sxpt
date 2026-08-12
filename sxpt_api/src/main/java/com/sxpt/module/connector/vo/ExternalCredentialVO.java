package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 第三方原平台系统级凭证返回对象。
 *
 * 业务功能：
 * 1. 后台生成 API Key 时返回凭证摘要和一次性明文。
 * 2. 列表或详情展示时可复用摘要字段，但不应返回 apiKey 明文。
 *
 * 关键流程：
 * 1. apiKey 只在生成或重置后返回一次。
 * 2. 后续页面展示使用 apiKeyPrefix 定位凭证，避免明文泄露。
 */
public class ExternalCredentialVO {

    private String id;

    private String tenantId;

    private String connectorSystemId;

    private String credentialName;

    private String apiKeyPrefix;

    private String apiKey;

    private LocalDateTime lastUsedTime;

    private LocalDateTime expireTime;

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

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public String getApiKeyPrefix() {
        return apiKeyPrefix;
    }

    public void setApiKeyPrefix(String apiKeyPrefix) {
        this.apiKeyPrefix = apiKeyPrefix;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public LocalDateTime getLastUsedTime() {
        return lastUsedTime;
    }

    public void setLastUsedTime(LocalDateTime lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
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
