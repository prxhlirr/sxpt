package com.sxpt.module.execution.vo;

import java.time.LocalDateTime;

/**
 * 任务执行上下文快照返回对象。
 *
 * 业务功能：
 * 1. 向前端和 SDK 返回学生本次任务执行绑定的上下文快照。
 * 2. 隐藏内部软删除字段，避免前端依赖数据库生命周期细节。
 *
 * 关键流程：
 * 1. Controller 将 TaskExecutionContext 实体转换为 VO。
 * 2. 调用方按 VO 中的快照 JSON 初始化 SDK 运行态。
 */
public class TaskExecutionContextVO {

    private String id;

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

    private LocalDateTime archiveTime;

    private LocalDateTime expireTime;

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

    public LocalDateTime getArchiveTime() {
        return archiveTime;
    }

    public void setArchiveTime(LocalDateTime archiveTime) {
        this.archiveTime = archiveTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
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
