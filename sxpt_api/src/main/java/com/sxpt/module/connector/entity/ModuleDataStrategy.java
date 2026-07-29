package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 原平台业务模块数据准备策略实体。
 *
 * 业务功能：
 * 1. 映射 module_data_strategy 表，定义某租户、某原平台、某业务模块在备案、学习、练习、考试场景下如何准备数据。
 * 2. 将数据来源、单位角色、共享、重置、锁定、校验和归档策略集中维护，避免散落在发布、练习、考试代码中。
 * 3. 为 DataRequirement 生成、数据池创建、练习重置和考试每题独占数据提供可追溯的策略快照来源。
 *
 * 关键流程：
 * 1. 管理员先选择业务模块和数据模板，再按场景创建或更新数据策略。
 * 2. 策略启用后，发布任务或学生进入运行态时按 strategyId 读取当前策略。
 * 3. 后续生成需求批次时保存 strategyVersion 和策略 JSON 快照，保证历史评分与重置不受后续策略修改影响。
 */
@TableName("module_data_strategy")
public class ModuleDataStrategy {

    @TableId
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
