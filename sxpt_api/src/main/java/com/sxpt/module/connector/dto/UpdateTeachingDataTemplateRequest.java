package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 更新教学业务数据模板请求。
 *
 * 业务功能：
 * 1. 承载管理员维护模板名称、场景、模块归属和模板配置时提交的字段。
 * 2. 将可编辑字段限制在模板规则本身，避免通过编辑接口修改租户和原平台边界。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发基础参数校验。
 * 2. Controller 将请求转换为 TeachingDataTemplate 实体后交给 Service 合并到现有模板。
 */
public class UpdateTeachingDataTemplateRequest {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 256, message = "模板名称长度不能超过 256")
    private String templateName;

    @NotBlank(message = "场景类型不能为空")
    @Size(max = 32, message = "场景类型长度不能超过 32")
    private String sceneType;

    @NotBlank(message = "模块编码不能为空")
    @Size(max = 128, message = "模块编码长度不能超过 128")
    private String moduleCode;

    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @Size(max = 64, message = "策略 ID 长度不能超过 64")
    private String strategyId;

    @Size(max = 64, message = "初始状态长度不能超过 64")
    private String initState;

    @Size(max = 128, message = "支持模式长度不能超过 128")
    private String supportMode;

    @Size(max = 32, message = "模板用途长度不能超过 32")
    private String templateUsage;

    @Size(max = 32768, message = "模板配置长度不能超过 32768")
    private String configJson;

    @Size(max = 32768, message = "数据结构配置长度不能超过 32768")
    private String dataSchemaJson;

    @Size(max = 32768, message = "模拟规则配置长度不能超过 32768")
    private String mockRuleJson;

    private Boolean readonlyFlag;

    @Size(max = 32768, message = "请求结构配置长度不能超过 32768")
    private String requestSchemaJson;

    @Size(max = 32768, message = "单位角色配置长度不能超过 32768")
    private String requiredOrgRoleJson;

    @Size(max = 32768, message = "结果校验配置长度不能超过 32768")
    private String resultCheckSchemaJson;

    @Size(max = 32768, message = "敏感字段策略长度不能超过 32768")
    private String sensitiveFieldPolicyJson;

    @Size(max = 64, message = "更新人长度不能超过 64")
    private String updateBy;

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

    public String getTeachingPointId() {
        return teachingPointId;
    }

    public void setTeachingPointId(String teachingPointId) {
        this.teachingPointId = teachingPointId;
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

    public String getTemplateUsage() {
        return templateUsage;
    }

    public void setTemplateUsage(String templateUsage) {
        this.templateUsage = templateUsage;
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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
}
