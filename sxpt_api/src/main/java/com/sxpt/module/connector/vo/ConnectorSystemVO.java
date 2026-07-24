package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 原业务平台配置返回对象。
 *
 * 业务功能：
 * 1. 向前端返回原平台接入配置的展示字段。
 * 2. 隔离数据库 Entity，避免直接暴露内部字段和后续持久化细节。
 *
 * 关键流程：
 * 1. Controller 或转换器从 ConnectorSystem 实体提取可展示字段。
 * 2. 前端使用 id 作为后续启停、编辑、launchToken 配置引用。
 */
public class ConnectorSystemVO {

    private String id;

    private String tenantId;

    private String systemCode;

    private String systemName;

    private String systemType;

    private String baseUrl;

    private String authType;

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
