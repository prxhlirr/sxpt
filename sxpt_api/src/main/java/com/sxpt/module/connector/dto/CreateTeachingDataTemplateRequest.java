package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建教学业务数据模板请求。
 *
 * 业务功能：
 * 1. 承载管理员或教师创建原平台教学数据模板时提交的配置参数。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发基础参数校验。
 * 2. Controller 将请求转换为 TeachingDataTemplate 实体后交给 Service 写入。
 */
public class CreateTeachingDataTemplateRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "原平台配置 ID 不能为空")
    @Size(max = 64, message = "原平台配置 ID 长度不能超过 64")
    private String connectorSystemId;

    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @NotBlank(message = "模板编码不能为空")
    @Size(max = 128, message = "模板编码长度不能超过 128")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 256, message = "模板名称长度不能超过 256")
    private String templateName;

    @NotBlank(message = "场景类型不能为空")
    @Size(max = 32, message = "场景类型长度不能超过 32")
    private String sceneType;

    @Size(max = 128, message = "模块编码长度不能超过 128")
    private String moduleCode;

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
}
