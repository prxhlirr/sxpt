package com.sxpt.module.execution.vo;

import java.time.LocalDateTime;

/**
 * 身份切换运行返回对象。
 *
 * 业务功能：
 * 1. 向 SDK 返回下一步骤的 launchToken、步骤和原平台身份摘要。
 * 2. 不暴露 launchTokenHash、deleted 等内部持久化字段，降低令牌和软删除状态外泄风险。
 *
 * 关键流程：
 * 1. Service 创建下一步骤启动上下文后组装该对象。
 * 2. SDK 携带 launchToken 跳转或请求原平台，由原平台继续执行 verify 和 used 回写。
 */
public class IdentitySwitchRunVO {

    private String launchContextId;

    private String launchToken;

    private String tenantId;

    private String studentId;

    private String connectorSystemId;

    private String taskId;

    private String teachingPointId;

    private String executionId;

    private String currentStepId;

    private String nextStepId;

    private Long currentSegmentNo;

    private Long nextSegmentNo;

    private String sdkMode;

    private String sceneType;

    private String actorType;

    private String requiredExternalOrgId;

    private String requiredExternalOrgName;

    private String requiredExternalRoleId;

    private String requiredExternalRoleName;

    private String targetUrl;

    private Boolean switchConfirmRequired;

    private String switchDecisionSource;

    private String switchStrategy;

    private LocalDateTime expireTime;

    public String getLaunchContextId() {
        return launchContextId;
    }

    public void setLaunchContextId(String launchContextId) {
        this.launchContextId = launchContextId;
    }

    public String getLaunchToken() {
        return launchToken;
    }

    public void setLaunchToken(String launchToken) {
        this.launchToken = launchToken;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
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

    public String getCurrentStepId() {
        return currentStepId;
    }

    public void setCurrentStepId(String currentStepId) {
        this.currentStepId = currentStepId;
    }

    public String getNextStepId() {
        return nextStepId;
    }

    public void setNextStepId(String nextStepId) {
        this.nextStepId = nextStepId;
    }

    public Long getCurrentSegmentNo() {
        return currentSegmentNo;
    }

    public void setCurrentSegmentNo(Long currentSegmentNo) {
        this.currentSegmentNo = currentSegmentNo;
    }

    public Long getNextSegmentNo() {
        return nextSegmentNo;
    }

    public void setNextSegmentNo(Long nextSegmentNo) {
        this.nextSegmentNo = nextSegmentNo;
    }

    public String getSdkMode() {
        return sdkMode;
    }

    public void setSdkMode(String sdkMode) {
        this.sdkMode = sdkMode;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
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

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Boolean getSwitchConfirmRequired() {
        return switchConfirmRequired;
    }

    public void setSwitchConfirmRequired(Boolean switchConfirmRequired) {
        this.switchConfirmRequired = switchConfirmRequired;
    }

    public String getSwitchDecisionSource() {
        return switchDecisionSource;
    }

    public void setSwitchDecisionSource(String switchDecisionSource) {
        this.switchDecisionSource = switchDecisionSource;
    }

    public String getSwitchStrategy() {
        return switchStrategy;
    }

    public void setSwitchStrategy(String switchStrategy) {
        this.switchStrategy = switchStrategy;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
}
