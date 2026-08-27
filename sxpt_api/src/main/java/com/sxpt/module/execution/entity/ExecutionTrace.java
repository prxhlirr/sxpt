package com.sxpt.module.execution.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 学生执行轨迹实体。
 *
 * 业务功能：
 * 1. 映射 execution_trace 表，保存学生在原平台学习、练习和考试过程中的操作轨迹。
 * 2. 只保存评分和复核需要的输入、输出、状态和证据摘要，避免保存完整原平台响应或敏感明文。
 *
 * 关键流程：
 * 1. SDK 为每条轨迹生成 clientTraceId 并携带 sequenceNo 上报。
 * 2. Service 负责幂等、默认值和查询排序，Mapper 只承载基础持久化能力。
 */
@TableName("execution_trace")
public class ExecutionTrace {

    @TableId
    private String id;

    private String tenantId;

    private String executionId;

    private String sdkSessionId;

    private String clientTraceId;

    private String taskStepId;

    private String teachingPointId;

    private String resourceId;

    private String dataSessionId;

    private String dataInstanceId;

    private String businessSceneCode;

    private String externalBusinessId;

    private String traceType;

    private LocalDateTime traceTime;

    private Long sequenceNo;

    private Long retryCount;

    private String inputDataJson;

    private String outputDataJson;

    private String beforeStateJson;

    private String afterStateJson;

    private String evidenceJson;

    private Boolean success;

    private String errorMessage;

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

    public String getDataSessionId() {
        return dataSessionId;
    }

    public void setDataSessionId(String dataSessionId) {
        this.dataSessionId = dataSessionId;
    }

    public String getDataInstanceId() {
        return dataInstanceId;
    }

    public void setDataInstanceId(String dataInstanceId) {
        this.dataInstanceId = dataInstanceId;
    }

    public String getBusinessSceneCode() {
        return businessSceneCode;
    }

    public void setBusinessSceneCode(String businessSceneCode) {
        this.businessSceneCode = businessSceneCode;
    }

    public String getExternalBusinessId() {
        return externalBusinessId;
    }

    public void setExternalBusinessId(String externalBusinessId) {
        this.externalBusinessId = externalBusinessId;
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

    public String getInputDataJson() {
        return inputDataJson;
    }

    public void setInputDataJson(String inputDataJson) {
        this.inputDataJson = inputDataJson;
    }

    public String getOutputDataJson() {
        return outputDataJson;
    }

    public void setOutputDataJson(String outputDataJson) {
        this.outputDataJson = outputDataJson;
    }

    public String getBeforeStateJson() {
        return beforeStateJson;
    }

    public void setBeforeStateJson(String beforeStateJson) {
        this.beforeStateJson = beforeStateJson;
    }

    public String getAfterStateJson() {
        return afterStateJson;
    }

    public void setAfterStateJson(String afterStateJson) {
        this.afterStateJson = afterStateJson;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
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
