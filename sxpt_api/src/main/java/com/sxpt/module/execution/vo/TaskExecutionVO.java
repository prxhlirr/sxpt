package com.sxpt.module.execution.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生任务执行返回对象。
 *
 * 业务功能：
 * 1. 对外展示一次学生任务执行的关键业务状态。
 * 2. 隐藏 deleted 等内部治理字段，避免前端依赖数据库实现细节。
 *
 * 关键流程：
 * 1. Controller 将 TaskExecution 实体转换为 VO。
 * 2. 前端基于 executionStatus、score 和 resultSummary 展示执行进度与结果。
 */
public class TaskExecutionVO {

    private String id;

    private String tenantId;

    private String taskId;

    private String studentId;

    private String connectorSystemId;

    private String executionMode;

    private String sdkMode;

    private String executionIdentityStatus;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String executionStatus;

    private BigDecimal score;

    private String resultSummary;

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

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public String getSdkMode() {
        return sdkMode;
    }

    public void setSdkMode(String sdkMode) {
        this.sdkMode = sdkMode;
    }

    public String getExecutionIdentityStatus() {
        return executionIdentityStatus;
    }

    public void setExecutionIdentityStatus(String executionIdentityStatus) {
        this.executionIdentityStatus = executionIdentityStatus;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
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
