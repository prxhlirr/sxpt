package com.sxpt.module.execution.vo;

import java.time.LocalDateTime;

/**
 * 执行轨迹返回对象。
 *
 * 业务功能：
 * 1. 向 SDK 或教师端返回已入库的学生执行轨迹摘要。
 * 2. 隔离数据库实体，避免大 JSON 证据、软删除和归档时间等内部字段直接外泄。
 *
 * 关键流程：
 * 1. Controller 从 ExecutionTrace 实体中提取可展示字段。
 * 2. SDK 可使用 id、clientTraceId 和 sequenceNo 判断轨迹是否被服务端接收。
 */
public class ExecutionTraceVO {

    private String id;

    private String tenantId;

    private String executionId;

    private String sdkSessionId;

    private String clientTraceId;

    private String taskStepId;

    private String teachingPointId;

    private String resourceId;

    private String traceType;

    private LocalDateTime traceTime;

    private Long sequenceNo;

    private Long retryCount;

    private Boolean success;

    private String errorMessage;

    private String archiveStatus;

    private String status;

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

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getSdkSessionId() {
        return sdkSessionId;
    }

    public void setSdkSessionId(String sdkSessionId) {
        this.sdkSessionId = sdkSessionId;
    }

    public String getClientTraceId() {
        return clientTraceId;
    }

    public void setClientTraceId(String clientTraceId) {
        this.clientTraceId = clientTraceId;
    }

    public String getTaskStepId() {
        return taskStepId;
    }

    public void setTaskStepId(String taskStepId) {
        this.taskStepId = taskStepId;
    }

    public String getTeachingPointId() {
        return teachingPointId;
    }

    public void setTeachingPointId(String teachingPointId) {
        this.teachingPointId = teachingPointId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getTraceType() {
        return traceType;
    }

    public void setTraceType(String traceType) {
        this.traceType = traceType;
    }

    public LocalDateTime getTraceTime() {
        return traceTime;
    }

    public void setTraceTime(LocalDateTime traceTime) {
        this.traceTime = traceTime;
    }

    public Long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public Long getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Long retryCount) {
        this.retryCount = retryCount;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getArchiveStatus() {
        return archiveStatus;
    }

    public void setArchiveStatus(String archiveStatus) {
        this.archiveStatus = archiveStatus;
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
}
