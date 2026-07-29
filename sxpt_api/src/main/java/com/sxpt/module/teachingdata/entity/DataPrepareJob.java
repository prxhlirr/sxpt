package com.sxpt.module.teachingdata.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 数据准备任务实体。
 *
 * 业务功能：
 * 1. 映射 data_prepare_job 表，记录批量造数、拉取、锁定、校验和归档任务。
 * 2. 保存调用原平台的幂等键、外部请求 ID、请求批次和重试信息。
 * 3. 支撑数据准备过程审计、失败补偿和后台任务状态查询。
 *
 * 关键流程：
 * 1. 数据需求批次生成后，系统创建数据准备任务。
 * 2. 任务服务按 requestBatchId 调用原平台 Adapter。
 * 3. 原平台返回后回写 successCount、failedCount、resultJson 和 jobStatus。
 */
@TableName("data_prepare_job")
public class DataPrepareJob {

    @TableId
    private String id;

    private String tenantId;

    private String connectorSystemId;

    private String poolId;

    private String taskId;

    private String moduleCode;

    private String sceneType;

    private String jobType;

    private Long expectedCount;

    private Long successCount;

    private Long failedCount;

    private String jobStatus;

    private String requestJson;

    private String resultJson;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String idempotencyKey;

    private String externalRequestId;

    private String requestBatchId;

    private Long retryCount;

    private LocalDateTime nextRetryTime;

    private String triggerType;

    private String traceId;

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

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getPoolId() {
        return poolId;
    }

    public void setPoolId(String poolId) {
        this.poolId = poolId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public Long getExpectedCount() {
        return expectedCount;
    }

    public void setExpectedCount(Long expectedCount) {
        this.expectedCount = expectedCount;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Long successCount) {
        this.successCount = successCount;
    }

    public Long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Long failedCount) {
        this.failedCount = failedCount;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getExternalRequestId() {
        return externalRequestId;
    }

    public void setExternalRequestId(String externalRequestId) {
        this.externalRequestId = externalRequestId;
    }

    public String getRequestBatchId() {
        return requestBatchId;
    }

    public void setRequestBatchId(String requestBatchId) {
        this.requestBatchId = requestBatchId;
    }

    public Long getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Long retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getNextRetryTime() {
        return nextRetryTime;
    }

    public void setNextRetryTime(LocalDateTime nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
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
