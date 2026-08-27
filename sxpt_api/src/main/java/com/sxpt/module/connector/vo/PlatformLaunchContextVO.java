package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 原平台启动上下文返回对象。
 *
 * 业务功能：
 * 1. 向前端返回本次跳转原平台所需的明文 launchToken 和上下文摘要。
 * 2. 隔离数据库实体，禁止返回 launchTokenHash、软删除标记和错误堆栈类字段。
 *
 * 关键流程：
 * 1. Controller 从 Service 返回值中提取明文 launchToken。
 * 2. 前端只在跳转原平台时携带该 token，后续校验由原平台 verify 接口完成。
 */
public class PlatformLaunchContextVO {

    private String id;

    private String tenantId;

    private String launchToken;

    private String userId;

    private String connectorSystemId;

    private String taskId;

    private String teachingPointId;

    private String executionId;

    private String captureSessionId;

    private String practiceAttemptId;

    private String examAttemptId;

    private String questionAttemptId;

    private String sceneType;

    private String sdkMode;

    private String targetUrl;

    private String launchEntryType;

    private String originHomeUrl;

    private String launchStatus;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

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

    public String getLaunchToken() {
        return launchToken;
    }

    public void setLaunchToken(String launchToken) {
        this.launchToken = launchToken;
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

    public String getCaptureSessionId() {
        return captureSessionId;
    }

    public void setCaptureSessionId(String captureSessionId) {
        this.captureSessionId = captureSessionId;
    }

    public String getPracticeAttemptId() {
        return practiceAttemptId;
    }

    public void setPracticeAttemptId(String practiceAttemptId) {
        this.practiceAttemptId = practiceAttemptId;
    }

    public String getExamAttemptId() {
        return examAttemptId;
    }

    public void setExamAttemptId(String examAttemptId) {
        this.examAttemptId = examAttemptId;
    }

    public String getQuestionAttemptId() {
        return questionAttemptId;
    }

    public void setQuestionAttemptId(String questionAttemptId) {
        this.questionAttemptId = questionAttemptId;
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

    public String getLaunchEntryType() {
        return launchEntryType;
    }

    public void setLaunchEntryType(String launchEntryType) {
        this.launchEntryType = launchEntryType;
    }

    public String getOriginHomeUrl() {
        return originHomeUrl;
    }

    public void setOriginHomeUrl(String originHomeUrl) {
        this.originHomeUrl = originHomeUrl;
    }

    public String getLaunchStatus() {
        return launchStatus;
    }

    public void setLaunchStatus(String launchStatus) {
        this.launchStatus = launchStatus;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
