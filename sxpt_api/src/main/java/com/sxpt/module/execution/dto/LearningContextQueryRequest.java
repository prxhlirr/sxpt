package com.sxpt.module.execution.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 学习模式上下文查询请求。
 *
 * 业务功能：
 * 1. 承载 SDK 进入学习模式时定位运行上下文所需的执行身份。
 * 2. 支持任务 ID、学生 ID 辅助校验，避免学习模式误用其他执行快照。
 *
 * 关键流程：
 * 1. Controller 从 query 参数组装请求对象。
 * 2. Service 基于该请求读取 runtime-context，并校验 sdkMode 必须为 LEARNING。
 */
public class LearningContextQueryRequest {

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
