package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 教学业务数据模板实体。
 *
 * 业务功能：
 * 1. 映射 teaching_data_template 表，保存备案、学习、练习、考试所需的原平台业务数据模板。
 * 2. 模板只表达“需要什么数据”，真实业务数据仍由原平台创建，教学平台只保存模板配置摘要。
 *
 * 关键流程：
 * 1. 教师或管理员按原平台、教学点和场景维护数据模板。
 * 2. 后续创建 teaching_data_instance 时根据模板定位原平台造数规则或数据池策略。
 */
@TableName("teaching_data_template")
public class TeachingDataTemplate {

    @TableId
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
