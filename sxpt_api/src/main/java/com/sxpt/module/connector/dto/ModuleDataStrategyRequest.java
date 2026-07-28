package com.sxpt.module.connector.dto;

import javax.validation.constraints.Size;

/**
 * 原平台业务模块数据准备策略维护请求。
 *
 * 业务功能：
 * 1. 承载管理员按业务模块和场景维护数据来源、单位角色、共享、重置、锁定、校验和归档策略时提交的参数。
 * 2. 作为 Controller 与 Service 之间的输入边界，避免前端直接依赖数据库实体结构。
 *
 * 关键流程：
 * 1. 创建策略时接收租户、原平台、业务模块、模块编码和场景等身份字段。
 * 2. 更新策略时 Controller 只提取可编辑规则字段，避免稳定身份字段被请求体误改。
 * 3. Service 统一执行策略完整性校验和启停规则。
 */
public class ModuleDataStrategyRequest {

    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @Size(max = 64, message = "原平台配置 ID 长度不能超过 64")
    private String connectorSystemId;

    @Size(max = 64, message = "业务模块 ID 长度不能超过 64")
    private String businessModuleId;

    @Size(max = 128, message = "模块编码长度不能超过 128")
    private String moduleCode;

    @Size(max = 256, message = "模块名称长度不能超过 256")
    private String moduleName;

    @Size(max = 32, message = "场景类型长度不能超过 32")
    private String sceneType;

    private Boolean needPreData;

    @Size(max = 32, message = "数据来源策略长度不能超过 32")
    private String dataSourceStrategy;

    @Size(max = 64, message = "初始外部状态长度不能超过 64")
    private String initExternalStatus;

    @Size(max = 64, message = "目标外部状态长度不能超过 64")
    private String targetExternalStatus;

    @Size(max = 4096, message = "默认单位角色策略 JSON 长度不能超过 4096")
    private String defaultOrgRolePolicyJson;

    @Size(max = 32, message = "共享策略长度不能超过 32")
    private String sharePolicy;

    @Size(max = 32, message = "重生成策略长度不能超过 32")
    private String regeneratePolicy;

    @Size(max = 32, message = "锁定策略长度不能超过 32")
    private String lockPolicy;

    @Size(max = 2048, message = "过期策略 JSON 长度不能超过 2048")
    private String expirePolicyJson;

    @Size(max = 4096, message = "结果校验策略 JSON 长度不能超过 4096")
    private String resultCheckPolicyJson;

    @Size(max = 128, message = "策略编码长度不能超过 128")
    private String strategyCode;

    @Size(max = 64, message = "模板 ID 长度不能超过 64")
    private String templateId;

    @Size(max = 32, message = "准备时机长度不能超过 32")
    private String prepareTiming;

    @Size(max = 2048, message = "数据池容量策略 JSON 长度不能超过 2048")
    private String poolSizePolicyJson;

    @Size(max = 4096, message = "校验策略 JSON 长度不能超过 4096")
    private String validationPolicyJson;

    @Size(max = 2048, message = "归档策略 JSON 长度不能超过 2048")
    private String archivePolicyJson;

    @Size(max = 64, message = "创建人长度不能超过 64")
    private String createBy;

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
