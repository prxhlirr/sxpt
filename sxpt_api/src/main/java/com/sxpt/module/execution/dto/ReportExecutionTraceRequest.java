package com.sxpt.module.execution.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 执行轨迹上报请求。
 *
 * 业务功能：
 * 1. 承载 SDK 在学生运行任务时上报的点击、输入、选择、上传、状态和提示请求轨迹。
 * 2. 强制携带 clientTraceId，确保 SDK 网络重试时服务端可以幂等去重。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发 Bean Validation 基础校验。
 * 2. Controller 将请求转换为 ExecutionTrace 实体后交给 Service 写入或命中幂等记录。
 */
public class ReportExecutionTraceRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "执行 ID 不能为空")
    @Size(max = 64, message = "执行 ID 长度不能超过 64")
    private String executionId;

    @Size(max = 128, message = "SDK 会话 ID 长度不能超过 128")
    private String sdkSessionId;

    @NotBlank(message = "客户端轨迹 ID 不能为空")
    @Size(max = 128, message = "客户端轨迹 ID 长度不能超过 128")
    private String clientTraceId;

    @Size(max = 64, message = "任务步骤 ID 长度不能超过 64")
    private String taskStepId;

    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @Size(max = 64, message = "资源 ID 长度不能超过 64")
    private String resourceId;

    @Size(max = 64, message = "数据会话 ID 长度不能超过 64")
    private String dataSessionId;

    @Size(max = 64, message = "教学数据实例 ID 长度不能超过 64")
    private String dataInstanceId;

    @Size(max = 128, message = "业务场景编码长度不能超过 128")
    private String businessSceneCode;

    @Size(max = 128, message = "原平台业务数据 ID 长度不能超过 128")
    private String externalBusinessId;

    @NotBlank(message = "轨迹类型不能为空")
    @Size(max = 32, message = "轨迹类型长度不能超过 32")
    private String traceType;

    @NotNull(message = "轨迹时间不能为空")
    private LocalDateTime traceTime;

    @NotNull(message = "轨迹顺序号不能为空")
    private Long sequenceNo;

    private Long retryCount;

    @Size(max = 32768, message = "输入数据摘要长度不能超过 32768")
    private String inputDataJson;

    @Size(max = 32768, message = "输出数据摘要长度不能超过 32768")
    private String outputDataJson;

    @Size(max = 32768, message = "操作前状态长度不能超过 32768")
    private String beforeStateJson;

    @Size(max = 32768, message = "操作后状态长度不能超过 32768")
    private String afterStateJson;

    @Size(max = 32768, message = "证据摘要长度不能超过 32768")
    private String evidenceJson;

    private Boolean success;

    @Size(max = 1024, message = "错误信息长度不能超过 1024")
    private String errorMessage;

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
}
