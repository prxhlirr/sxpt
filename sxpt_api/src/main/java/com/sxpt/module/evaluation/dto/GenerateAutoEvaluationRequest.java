package com.sxpt.module.evaluation.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 自动评分生成请求。
 *
 * 业务功能：
 * 1. 承载一次任务执行自动评分所需的最小范围。
 * 2. 支持提交后触发或教师手动重新评分复用同一入口。
 *
 * 关键流程：
 * 1. Controller 校验租户、执行 ID 和评价规则 ID。
 * 2. Service 读取评分项和执行轨迹，生成评价结果。
 */
public class GenerateAutoEvaluationRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "执行 ID 不能为空")
    @Size(max = 64, message = "执行 ID 长度不能超过 64")
    private String executionId;

    @NotBlank(message = "评价规则 ID 不能为空")
    @Size(max = 64, message = "评价规则 ID 长度不能超过 64")
    private String evaluationRuleId;

    @Size(max = 64, message = "创建人长度不能超过 64")
    private String createBy;

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

    public String getEvaluationRuleId() {
        return evaluationRuleId;
    }

    public void setEvaluationRuleId(String evaluationRuleId) {
        this.evaluationRuleId = evaluationRuleId;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
}
