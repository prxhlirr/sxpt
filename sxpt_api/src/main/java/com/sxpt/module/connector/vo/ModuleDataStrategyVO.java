package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 原平台业务模块数据准备策略返回对象。
 *
 * 业务功能：
 * 1. 向后台管理页面返回数据准备策略的展示字段和运行规则字段。
 * 2. 隔离数据库实体，避免前端依赖软删除、乐观锁等持久化细节。
 *
 * 关键流程：
 * 1. Controller 从 ModuleDataStrategy 实体中提取可展示字段。
 * 2. 前端使用 id 作为编辑、启停、策略发布校验和数据需求生成的稳定标识。
 */
public class ModuleDataStrategyVO {

    private String id;

    private String tenantId;

    private String connectorSystemId;

    private String businessModuleId;

    private String moduleCode;

    private String moduleName;

    private String sceneType;

    private Boolean needPreData;

    private String dataSourceStrategy;

    private String initExternalStatus;

    private String targetExternalStatus;

    private String defaultOrgRolePolicyJson;

    private String sharePolicy;

    private String regeneratePolicy;

    private String lockPolicy;

    private String expirePolicyJson;

    private String resultCheckPolicyJson;

    private String strategyCode;

    private String templateId;

    private String prepareTiming;

    private String poolSizePolicyJson;

    private String validationPolicyJson;

    private String archivePolicyJson;

    private Long strategyVersion;

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

    public String getBusinessModuleId() {
        return businessModuleId;
    }

    public void setBusinessModuleId(String businessModuleId) {
        this.businessModuleId = businessModuleId;
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

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public Boolean getNeedPreData() {
        return needPreData;
    }

    public void setNeedPreData(Boolean needPreData) {
        this.needPreData = needPreData;
    }

    public String getDataSourceStrategy() {
        return dataSourceStrategy;
    }

    public void setDataSourceStrategy(String dataSourceStrategy) {
        this.dataSourceStrategy = dataSourceStrategy;
    }

    public String getInitExternalStatus() {
        return initExternalStatus;
    }

    public void setInitExternalStatus(String initExternalStatus) {
        this.initExternalStatus = initExternalStatus;
    }

    public String getTargetExternalStatus() {
        return targetExternalStatus;
    }

    public void setTargetExternalStatus(String targetExternalStatus) {
        this.targetExternalStatus = targetExternalStatus;
    }

    public String getDefaultOrgRolePolicyJson() {
        return defaultOrgRolePolicyJson;
    }

    public void setDefaultOrgRolePolicyJson(String defaultOrgRolePolicyJson) {
        this.defaultOrgRolePolicyJson = defaultOrgRolePolicyJson;
    }

    public String getSharePolicy() {
        return sharePolicy;
    }

    public void setSharePolicy(String sharePolicy) {
        this.sharePolicy = sharePolicy;
    }

    public String getRegeneratePolicy() {
        return regeneratePolicy;
    }

    public void setRegeneratePolicy(String regeneratePolicy) {
        this.regeneratePolicy = regeneratePolicy;
    }

    public String getLockPolicy() {
        return lockPolicy;
    }

    public void setLockPolicy(String lockPolicy) {
        this.lockPolicy = lockPolicy;
    }

    public String getExpirePolicyJson() {
        return expirePolicyJson;
    }

    public void setExpirePolicyJson(String expirePolicyJson) {
        this.expirePolicyJson = expirePolicyJson;
    }

    public String getResultCheckPolicyJson() {
        return resultCheckPolicyJson;
    }

    public void setResultCheckPolicyJson(String resultCheckPolicyJson) {
        this.resultCheckPolicyJson = resultCheckPolicyJson;
    }

    public String getStrategyCode() {
        return strategyCode;
    }

    public void setStrategyCode(String strategyCode) {
        this.strategyCode = strategyCode;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getPrepareTiming() {
        return prepareTiming;
    }

    public void setPrepareTiming(String prepareTiming) {
        this.prepareTiming = prepareTiming;
    }

    public String getPoolSizePolicyJson() {
        return poolSizePolicyJson;
    }

    public void setPoolSizePolicyJson(String poolSizePolicyJson) {
        this.poolSizePolicyJson = poolSizePolicyJson;
    }

    public String getValidationPolicyJson() {
        return validationPolicyJson;
    }

    public void setValidationPolicyJson(String validationPolicyJson) {
        this.validationPolicyJson = validationPolicyJson;
    }

    public String getArchivePolicyJson() {
        return archivePolicyJson;
    }

    public void setArchivePolicyJson(String archivePolicyJson) {
        this.archivePolicyJson = archivePolicyJson;
    }

    public Long getStrategyVersion() {
        return strategyVersion;
    }

    public void setStrategyVersion(Long strategyVersion) {
        this.strategyVersion = strategyVersion;
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
