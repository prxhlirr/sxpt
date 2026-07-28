package com.sxpt.module.connector.dto;

import javax.validation.constraints.Size;

/**
 * 原平台业务模块维护请求。
 *
 * 业务功能：
 * 1. 承载管理员创建或编辑业务模块时提交的业务名称、租户、原平台、入口、支持场景和默认状态。
 * 2. 作为 Controller 与 Service 之间的输入边界，避免前端直接依赖数据库实体结构。
 *
 * 关键流程：
 * 1. Controller 接收请求后生成或绑定业务模块 ID。
 * 2. Controller 将请求转换为 BusinessModule 实体。
 * 3. Service 统一执行必填字段、状态和生命周期字段校验。
 */
public class BusinessModuleRequest {

    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @Size(max = 64, message = "原平台配置 ID 长度不能超过 64")
    private String connectorSystemId;

    @Size(max = 64, message = "业务模块编码长度不能超过 64")
    private String moduleCode;

    @Size(max = 128, message = "业务模块名称长度不能超过 128")
    private String moduleName;

    @Size(max = 128, message = "原平台业务模块 ID 长度不能超过 128")
    private String externalModuleId;

    @Size(max = 512, message = "业务入口地址长度不能超过 512")
    private String entryUrl;

    @Size(max = 64, message = "业务模块类型长度不能超过 64")
    private String moduleType;

    @Size(max = 512, message = "支持场景长度不能超过 512")
    private String supportScenes;

    private Boolean needPreData;

    @Size(max = 64, message = "默认初始状态长度不能超过 64")
    private String defaultInitialStatus;

    @Size(max = 64, message = "默认目标状态长度不能超过 64")
    private String defaultTargetStatus;

    @Size(max = 2048, message = "能力编码 JSON 长度不能超过 2048")
    private String capabilityCodesJson;

    @Size(max = 64, message = "默认模板 ID 长度不能超过 64")
    private String defaultTemplateId;

    @Size(max = 512, message = "备注长度不能超过 512")
    private String remark;

    @Size(max = 64, message = "更新人长度不能超过 64")
    private String updateBy;

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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
}
