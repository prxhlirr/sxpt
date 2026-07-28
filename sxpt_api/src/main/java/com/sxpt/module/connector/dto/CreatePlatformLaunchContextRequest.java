package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建原平台启动上下文请求。
 *
 * 业务功能：
 * 1. 承载教学平台跳转原平台前提交的用户、场景、SDK 模式、目标地址和执行身份要求。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发基础参数校验。
 * 2. Controller 将请求转换为 PlatformLaunchContext 实体后交给 Service 生成 launchToken。
 */
public class CreatePlatformLaunchContextRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "教学用户 ID 不能为空")
    @Size(max = 64, message = "教学用户 ID 长度不能超过 64")
    private String userId;

    @NotBlank(message = "原平台配置 ID 不能为空")
    @Size(max = 64, message = "原平台配置 ID 长度不能超过 64")
    private String connectorSystemId;

    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @Size(max = 64, message = "任务执行 ID 长度不能超过 64")
    private String executionId;

    @NotBlank(message = "教学业务数据实例 ID 不能为空")
    @Size(max = 64, message = "教学业务数据实例 ID 长度不能超过 64")
    private String dataInstanceId;

    @NotBlank(message = "场景类型不能为空")
    @Size(max = 32, message = "场景类型长度不能超过 32")
    private String sceneType;

    @NotBlank(message = "SDK 模式不能为空")
    @Size(max = 32, message = "SDK 模式长度不能超过 32")
    private String sdkMode;

    @NotBlank(message = "原平台目标地址不能为空")
    @Size(max = 1024, message = "原平台目标地址长度不能超过 1024")
    private String targetUrl;

    private Long segmentNo;

    @Size(max = 64, message = "业务参与方类型长度不能超过 64")
    private String actorType;

    @Size(max = 128, message = "原平台单位 ID 长度不能超过 128")
    private String requiredExternalOrgId;

    @Size(max = 256, message = "原平台单位名称长度不能超过 256")
    private String requiredExternalOrgName;

    @Size(max = 128, message = "原平台角色 ID 长度不能超过 128")
    private String requiredExternalRoleId;

    @Size(max = 256, message = "原平台角色名称长度不能超过 256")
    private String requiredExternalRoleName;

    @Size(max = 128, message = "原平台业务数据 ID 长度不能超过 128")
    private String externalBusinessId;

    @Size(max = 128, message = "原平台业务单据号长度不能超过 128")
    private String externalBusinessNo;

    @Size(max = 32768, message = "数据范围摘要长度不能超过 32768")
    private String dataScopeJson;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTeachingPointId() {
        return teachingPointId;
    }

    public void setTeachingPointId(String teachingPointId) {
        this.teachingPointId = teachingPointId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getDataInstanceId() {
        return dataInstanceId;
    }

    public void setDataInstanceId(String dataInstanceId) {
        this.dataInstanceId = dataInstanceId;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getSdkMode() {
        return sdkMode;
    }

    public void setSdkMode(String sdkMode) {
        this.sdkMode = sdkMode;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Long getSegmentNo() {
        return segmentNo;
    }

    public void setSegmentNo(Long segmentNo) {
        this.segmentNo = segmentNo;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public String getRequiredExternalOrgId() {
        return requiredExternalOrgId;
    }

    public void setRequiredExternalOrgId(String requiredExternalOrgId) {
        this.requiredExternalOrgId = requiredExternalOrgId;
    }

    public String getRequiredExternalOrgName() {
        return requiredExternalOrgName;
    }

    public void setRequiredExternalOrgName(String requiredExternalOrgName) {
        this.requiredExternalOrgName = requiredExternalOrgName;
    }

    public String getRequiredExternalRoleId() {
        return requiredExternalRoleId;
    }

    public void setRequiredExternalRoleId(String requiredExternalRoleId) {
        this.requiredExternalRoleId = requiredExternalRoleId;
    }

    public String getRequiredExternalRoleName() {
        return requiredExternalRoleName;
    }

    public void setRequiredExternalRoleName(String requiredExternalRoleName) {
        this.requiredExternalRoleName = requiredExternalRoleName;
    }

    public String getExternalBusinessId() {
        return externalBusinessId;
    }

    public void setExternalBusinessId(String externalBusinessId) {
        this.externalBusinessId = externalBusinessId;
    }

    public String getExternalBusinessNo() {
        return externalBusinessNo;
    }

    public void setExternalBusinessNo(String externalBusinessNo) {
        this.externalBusinessNo = externalBusinessNo;
    }

    public String getDataScopeJson() {
        return dataScopeJson;
    }

    public void setDataScopeJson(String dataScopeJson) {
        this.dataScopeJson = dataScopeJson;
    }
}
