package com.sxpt.module.execution.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 任务执行上下文快照实体。
 *
 * 业务功能：
 * 1. 映射 task_execution_context 表，保存学生进入任务时固化的运行上下文。
 * 2. 承载 SDK 模式、完整上下文、遮罩策略、教学点、资源和评分规则快照。
 *
 * 关键流程：
 * 1. 学生进入任务并生成 task_execution 后，立即创建一条执行上下文快照。
 * 2. 后续学习、练习、考试都读取该快照，避免教师修改任务配置影响历史执行。
 */
@TableName("task_execution_context")
public class TaskExecutionContext {

    @TableId
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
