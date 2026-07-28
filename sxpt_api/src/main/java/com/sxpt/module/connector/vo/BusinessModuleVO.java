package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 原平台业务模块返回对象。
 *
 * 业务功能：
 * 1. 向后台管理页面返回业务模块的展示字段和策略引用字段。
 * 2. 隔离数据库实体，避免前端依赖软删除、内部版本等持久化细节之外的隐式行为。
 *
 * 关键流程：
 * 1. Controller 从 BusinessModule 实体中提取可展示字段。
 * 2. 前端使用 id 作为编辑、启停、策略绑定和模板引用的稳定标识。
 */
public class BusinessModuleVO {

    private String id;

    private String tenantId;

    private String connectorSystemId;

    private String moduleCode;

    private String moduleName;

    private String externalModuleId;

    private String entryUrl;

    private String moduleType;

    private String supportScenes;

    private Boolean needPreData;

    private String defaultInitialStatus;

    private String defaultTargetStatus;

    private String capabilityCodesJson;

    private String defaultTemplateId;

    private String remark;

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

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getExternalModuleId() {
        return externalModuleId;
    }

    public void setExternalModuleId(String externalModuleId) {
        this.externalModuleId = externalModuleId;
    }

    public String getEntryUrl() {
        return entryUrl;
    }

    public void setEntryUrl(String entryUrl) {
        this.entryUrl = entryUrl;
    }

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public String getSupportScenes() {
        return supportScenes;
    }

    public void setSupportScenes(String supportScenes) {
        this.supportScenes = supportScenes;
    }

    public Boolean getNeedPreData() {
        return needPreData;
    }

    public void setNeedPreData(Boolean needPreData) {
        this.needPreData = needPreData;
    }

    public String getDefaultInitialStatus() {
        return defaultInitialStatus;
    }

    public void setDefaultInitialStatus(String defaultInitialStatus) {
        this.defaultInitialStatus = defaultInitialStatus;
    }

    public String getDefaultTargetStatus() {
        return defaultTargetStatus;
    }

    public void setDefaultTargetStatus(String defaultTargetStatus) {
        this.defaultTargetStatus = defaultTargetStatus;
    }

    public String getCapabilityCodesJson() {
        return capabilityCodesJson;
    }

    public void setCapabilityCodesJson(String capabilityCodesJson) {
        this.capabilityCodesJson = capabilityCodesJson;
    }

    public String getDefaultTemplateId() {
        return defaultTemplateId;
    }

    public void setDefaultTemplateId(String defaultTemplateId) {
        this.defaultTemplateId = defaultTemplateId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
