package com.sxpt.module.execution.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 身份切换运行请求。
 *
 * 业务功能：
 * 1. 承载 SDK 在学习、练习或考试过程中进入下一任务步骤所需的身份切换上下文。
 * 2. 由调用方明确提交下一步骤 ID 和目标地址，避免后端自动推断业务流转路径。
 *
 * 关键流程：
 * 1. Controller 接收请求并执行 Bean Validation。
 * 2. Service 读取 task_step 中固化的下一步骤单位和角色要求。
 * 3. Service 复用 launchToken 能力创建下一角色运行上下文。
 */
public class IdentitySwitchRunRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "学生 ID 不能为空")
    @Size(max = 64, message = "学生 ID 长度不能超过 64")
    private String studentId;

    @NotBlank(message = "原平台配置 ID 不能为空")
    @Size(max = 64, message = "原平台配置 ID 长度不能超过 64")
    private String connectorSystemId;

    @NotBlank(message = "任务 ID 不能为空")
    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @NotBlank(message = "教学点 ID 不能为空")
    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @NotBlank(message = "执行 ID 不能为空")
    @Size(max = 64, message = "执行 ID 长度不能超过 64")
    private String executionId;

    @Size(max = 64, message = "当前步骤 ID 长度不能超过 64")
    private String currentStepId;

    @NotBlank(message = "下一步骤 ID 不能为空")
    @Size(max = 64, message = "下一步骤 ID 长度不能超过 64")
    private String nextStepId;

    @Size(max = 64, message = "教学业务数据实例 ID 长度不能超过 64")
    private String dataInstanceId;

    @NotBlank(message = "SDK 模式不能为空")
    @Size(max = 32, message = "SDK 模式长度不能超过 32")
    private String sdkMode;

    @NotBlank(message = "原平台目标地址不能为空")
    @Size(max = 1024, message = "原平台目标地址长度不能超过 1024")
    private String targetUrl;

    @Size(max = 128, message = "原平台业务数据 ID 长度不能超过 128")
    private String externalBusinessId;

    @Size(max = 128, message = "原平台业务单据号长度不能超过 128")
    private String externalBusinessNo;

    @Size(max = 32768, message = "数据范围摘要长度不能超过 32768")
    private String dataScopeJson;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public String getCurrentStepId() {
        return currentStepId;
    }

    public void setCurrentStepId(String currentStepId) {
        this.currentStepId = currentStepId;
    }

    public String getNextStepId() {
        return nextStepId;
    }

    public void setNextStepId(String nextStepId) {
        this.nextStepId = nextStepId;
    }

    public String getDataInstanceId() {
        return dataInstanceId;
    }

    public void setDataInstanceId(String dataInstanceId) {
        this.dataInstanceId = dataInstanceId;
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

    public String getExternalBusinessId() {
        return externalBusinessId;
    }

    public void setExternalBusinessId(String externalBusinessId) {
        this.externalBusinessId = externalBusinessId;
    }

    public String getExternalBusinessNo() {
        return externalBusinessNo;
    }

    public void setExternalBusinessNo(String externalBusinessNo) {
        this.externalBusinessNo = externalBusinessNo;
    }

    public String getDataScopeJson() {
        return dataScopeJson;
    }

    public void setDataScopeJson(String dataScopeJson) {
        this.dataScopeJson = dataScopeJson;
    }
}
