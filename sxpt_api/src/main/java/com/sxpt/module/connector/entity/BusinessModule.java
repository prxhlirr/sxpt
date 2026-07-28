package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 原平台业务模块实体。
 *
 * 业务功能：
 * 1. 映射 business_module 表，维护原平台中可被教学化的业务入口主数据。
 * 2. 将业务模块主数据从 module_data_strategy 中拆出，避免同一模块在备案、学习、练习、考试策略中重复维护。
 * 3. 为数据模板、数据策略、数据需求规格和 launchToken 上下文提供稳定模块引用。
 *
 * 关键流程：
 * 1. 管理员先为某个 connector_system 配置业务模块编码、名称、入口和支持场景。
 * 2. 后续数据策略按 businessModuleId 或 moduleCode 绑定该模块。
 * 3. 数据准备服务根据模块默认状态、默认模板和能力要求生成 DataRequirement。
 */
@TableName("business_module")
public class BusinessModule {

    @TableId
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

    private Long lockVersion;

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

    public Long getLockVersion() {
        return lockVersion;
    }

    public void setLockVersion(Long lockVersion) {
        this.lockVersion = lockVersion;
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
