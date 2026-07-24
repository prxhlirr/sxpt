package com.sxpt.module.execution.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 学生提交任务执行请求。
 *
 * 业务功能：
 * 1. 承载学生提交一次运行中任务所需的最小参数。
 * 2. 后续提交服务会基于 executionId 触发评分和结果回写，调用方不再手工串联评分接口。
 *
 * 关键流程：
 * 1. Controller 校验租户、执行记录和操作人。
 * 2. Service 校验执行状态，只允许 RUNNING 状态进入提交流程。
 */
public class SubmitTaskExecutionRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "执行记录 ID 不能为空")
    @Size(max = 64, message = "执行记录 ID 长度不能超过 64")
    private String executionId;

    @NotBlank(message = "操作人 ID 不能为空")
    @Size(max = 64, message = "操作人 ID 长度不能超过 64")
    private String operatorId;

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

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }
}
