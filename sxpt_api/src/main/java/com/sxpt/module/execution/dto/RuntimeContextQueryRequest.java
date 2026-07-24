package com.sxpt.module.execution.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * SDK 运行上下文查询请求。
 *
 * 业务功能：
 * 1. 承载 SDK 初始化时定位任务执行上下文所需的租户和执行记录参数。
 * 2. 支持按任务 ID、学生 ID 进行辅助校验，避免 SDK 误拿其他学生或任务的上下文。
 *
 * 关键流程：
 * 1. Controller 从 query 参数组装请求对象并触发 Bean Validation。
 * 2. Service 使用请求对象读取已固化的 task_execution_context 快照。
 */
public class RuntimeContextQueryRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "任务执行 ID 不能为空")
    @Size(max = 64, message = "任务执行 ID 长度不能超过 64")
    private String executionId;

    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @Size(max = 64, message = "学生 ID 长度不能超过 64")
    private String studentId;

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
}
