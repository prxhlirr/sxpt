package com.sxpt.module.execution.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生任务执行主记录实体。
 *
 * 业务功能：
 * 1. 映射 task_execution 表，记录学生一次学习、练习或考试执行。
 * 2. 作为学生端主链路的聚合根，统一承载开始、提交、评分完成和失败状态。
 *
 * 关键流程：
 * 1. 学生开始任务时创建 RUNNING 执行记录。
 * 2. 学生提交后进入 SUBMITTED，并由后续评分流程回写 COMPLETED、score 和 resultSummary。
 */
@TableName("task_execution")
public class TaskExecution {

    @TableId
    private String id;

    private String tenantId;

    private String taskId;

    private String studentId;

    private String connectorSystemId;

    private String executionMode;

    private String sdkMode;

    private String executionIdentityStatus;

    private String executionIdentityJson;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String executionStatus;

    private BigDecimal score;

    private String resultSummary;

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

    public String getExecutionIdentityJson() {
        return executionIdentityJson;
    }

    public void setExecutionIdentityJson(String executionIdentityJson) {
        this.executionIdentityJson = executionIdentityJson;
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
