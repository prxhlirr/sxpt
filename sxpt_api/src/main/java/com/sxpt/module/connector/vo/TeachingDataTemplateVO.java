package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 教学业务数据模板返回对象。
 *
 * 业务功能：
 * 1. 向前端返回教学业务数据模板的展示字段。
 * 2. 隔离数据库实体，避免前端依赖软删除等内部持久化字段。
 *
 * 关键流程：
 * 1. Controller 从 TeachingDataTemplate 实体提取可展示字段。
 * 2. 后续数据实例创建使用 id、sceneType 和 configJson 定位模板配置。
 */
public class TeachingDataTemplateVO {

    private String id;

    private String tenantId;

    private String connectorSystemId;

    private String teachingPointId;

    private String templateCode;

    private String templateName;

    private String sceneType;

    private String moduleCode;

    private String strategyId;

    private String initState;

    private String supportMode;

    private String configJson;

    private String dataSchemaJson;

    private String mockRuleJson;

    private Boolean readonlyFlag;

    private String requestSchemaJson;

    private String requiredOrgRoleJson;

    private String resultCheckSchemaJson;

    private String sensitiveFieldPolicyJson;

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

    public String getTeachingPointId() {
        return teachingPointId;
    }

    public void setTeachingPointId(String teachingPointId) {
        this.teachingPointId = teachingPointId;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    public String getInitState() {
        return initState;
    }

    public void setInitState(String initState) {
        this.initState = initState;
    }

    public String getSupportMode() {
        return supportMode;
    }

    public void setSupportMode(String supportMode) {
        this.supportMode = supportMode;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getDataSchemaJson() {
        return dataSchemaJson;
    }

    public void setDataSchemaJson(String dataSchemaJson) {
        this.dataSchemaJson = dataSchemaJson;
    }

    public String getMockRuleJson() {
        return mockRuleJson;
    }

    public void setMockRuleJson(String mockRuleJson) {
        this.mockRuleJson = mockRuleJson;
    }

    public Boolean getReadonlyFlag() {
        return readonlyFlag;
    }

    public void setReadonlyFlag(Boolean readonlyFlag) {
        this.readonlyFlag = readonlyFlag;
    }

    public String getRequestSchemaJson() {
        return requestSchemaJson;
    }

    public void setRequestSchemaJson(String requestSchemaJson) {
        this.requestSchemaJson = requestSchemaJson;
    }

    public String getRequiredOrgRoleJson() {
        return requiredOrgRoleJson;
    }

    public void setRequiredOrgRoleJson(String requiredOrgRoleJson) {
        this.requiredOrgRoleJson = requiredOrgRoleJson;
    }

    public String getResultCheckSchemaJson() {
        return resultCheckSchemaJson;
    }

    public void setResultCheckSchemaJson(String resultCheckSchemaJson) {
        this.resultCheckSchemaJson = resultCheckSchemaJson;
    }

    public String getSensitiveFieldPolicyJson() {
        return sensitiveFieldPolicyJson;
    }

    public void setSensitiveFieldPolicyJson(String sensitiveFieldPolicyJson) {
        this.sensitiveFieldPolicyJson = sensitiveFieldPolicyJson;
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
