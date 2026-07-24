package com.sxpt.module.execution.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 学生开始任务执行请求。
 *
 * 业务功能：
 * 1. 承载学生进入任务时创建 task_execution 主记录所需的最小参数。
 * 2. 明确任务、学生、原平台和执行模式，避免后续轨迹、评分、练习数据失去统一归属。
 *
 * 关键流程：
 * 1. Controller 接收请求并完成基础参数校验。
 * 2. Service 校验任务发布状态后创建 RUNNING 执行记录。
 */
public class StartTaskExecutionRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "任务 ID 不能为空")
    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @NotBlank(message = "学生 ID 不能为空")
    @Size(max = 64, message = "学生 ID 长度不能超过 64")
    private String studentId;

    @NotBlank(message = "原平台 ID 不能为空")
    @Size(max = 64, message = "原平台 ID 长度不能超过 64")
    private String connectorSystemId;

    @NotBlank(message = "执行模式不能为空")
    @Size(max = 32, message = "执行模式长度不能超过 32")
    private String executionMode;

    @NotBlank(message = "SDK 模式不能为空")
    @Size(max = 32, message = "SDK 模式长度不能超过 32")
    private String sdkMode;

    @Size(max = 32768, message = "执行身份 JSON 长度不能超过 32768")
    private String executionIdentityJson;

    @Size(max = 64, message = "创建人 ID 长度不能超过 64")
    private String createBy;

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

    public String getExecutionIdentityJson() {
        return executionIdentityJson;
    }

    public void setExecutionIdentityJson(String executionIdentityJson) {
        this.executionIdentityJson = executionIdentityJson;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
}
