package com.sxpt.module.execution.vo;

import java.time.LocalDateTime;

/**
 * SDK 运行上下文返回对象。
 *
 * 业务功能：
 * 1. 向 SDK 返回任务执行所需的模式、执行身份和已固化配置快照。
 * 2. 将 task_execution_context 的快照字段整理成 SDK 专用响应，避免 SDK 直接依赖数据库实体。
 *
 * 关键流程：
 * 1. RuntimeContextService 读取唯一有效上下文快照。
 * 2. Controller 返回本 VO，SDK 使用 contextJson 和分项快照初始化运行态。
 */
public class RuntimeContextVO {

    private String contextId;

    private String tenantId;

    private String executionId;

    private String taskId;

    private String studentId;

    private String sdkMode;

    private String contextJson;

    private String overlayPolicyJson;

    private String teachingPointSnapshotJson;

    private String resourceSnapshotJson;

    private String evaluationSnapshotJson;

    private String archiveStatus;

    private LocalDateTime expireTime;

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getSdkMode() {
        return sdkMode;
    }

    public void setSdkMode(String sdkMode) {
        this.sdkMode = sdkMode;
    }

    public String getContextJson() {
        return contextJson;
    }

    public void setContextJson(String contextJson) {
        this.contextJson = contextJson;
    }

    public String getOverlayPolicyJson() {
        return overlayPolicyJson;
    }

    public void setOverlayPolicyJson(String overlayPolicyJson) {
        this.overlayPolicyJson = overlayPolicyJson;
    }

    public String getTeachingPointSnapshotJson() {
        return teachingPointSnapshotJson;
    }

    public void setTeachingPointSnapshotJson(String teachingPointSnapshotJson) {
        this.teachingPointSnapshotJson = teachingPointSnapshotJson;
    }

    public String getResourceSnapshotJson() {
        return resourceSnapshotJson;
    }

    public void setResourceSnapshotJson(String resourceSnapshotJson) {
        this.resourceSnapshotJson = resourceSnapshotJson;
    }

    public String getEvaluationSnapshotJson() {
        return evaluationSnapshotJson;
    }

    public void setEvaluationSnapshotJson(String evaluationSnapshotJson) {
        this.evaluationSnapshotJson = evaluationSnapshotJson;
    }

    public String getArchiveStatus() {
        return archiveStatus;
    }

    public void setArchiveStatus(String archiveStatus) {
        this.archiveStatus = archiveStatus;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
}
