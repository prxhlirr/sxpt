package com.sxpt.module.evaluation.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 教师复核评分结果请求。
 *
 * 业务功能：
 * 1. 承载教师确认某次执行评分结果所需的最小字段。
 * 2. 用人工分覆盖最终分，保留自动分和证据摘要作为复核依据。
 *
 * 关键流程：
 * 1. Controller 校验租户、执行、评价规则和人工分，复核人由服务端当前用户上下文提供。
 * 2. Service 定位已有 evaluation_result，并写入人工分、最终分和复核状态。
 */
public class ReviewEvaluationResultRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "执行 ID 不能为空")
    @Size(max = 64, message = "执行 ID 长度不能超过 64")
    private String executionId;

    @NotBlank(message = "评价规则 ID 不能为空")
    @Size(max = 64, message = "评价规则 ID 长度不能超过 64")
    private String evaluationRuleId;

    @NotNull(message = "人工分不能为空")
    @DecimalMin(value = "0.00", message = "人工分不能小于 0")
    @Digits(integer = 8, fraction = 2, message = "人工分最多保留 2 位小数")
    private BigDecimal manualScore;

    @NotBlank(message = "教师评语不能为空")
    @Size(max = 512, message = "教师评语长度不能超过 512")
    private String reviewReason;

    @Size(max = 64, message = "复核人长度不能超过 64")
    private String reviewedBy;

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

    public BigDecimal getManualScore() {
        return manualScore;
    }

    public void setManualScore(BigDecimal manualScore) {
        this.manualScore = manualScore;
    }

    public String getReviewReason() {
        return reviewReason;
    }

    public void setReviewReason(String reviewReason) {
        this.reviewReason = reviewReason;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}
