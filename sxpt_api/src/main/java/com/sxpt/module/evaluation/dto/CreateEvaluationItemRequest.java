package com.sxpt.module.evaluation.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 创建评分项请求。
 *
 * 业务功能：
 * 1. 承载教师在评价规则下配置关键动作、业务规则或外部结果校验评分项。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失、超长和非正分值。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发基础参数校验。
 * 2. Controller 将请求转换为 EvaluationItem 实体后交给 Service 写入。
 */
public class CreateEvaluationItemRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "评价规则 ID 不能为空")
    @Size(max = 64, message = "评价规则 ID 长度不能超过 64")
    private String evaluationRuleId;

    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @NotBlank(message = "评分项编码不能为空")
    @Size(max = 64, message = "评分项编码长度不能超过 64")
    private String itemCode;

    @NotBlank(message = "评分项名称不能为空")
    @Size(max = 128, message = "评分项名称长度不能超过 128")
    private String itemName;

    @NotBlank(message = "评分项类型不能为空")
    @Size(max = 32, message = "评分项类型长度不能超过 32")
    private String itemType;

    @Size(max = 64, message = "关联资源 ID 长度不能超过 64")
    private String relatedResourceId;

    @Size(max = 64, message = "关联 API 资源 ID 长度不能超过 64")
    private String relatedApiResourceId;

    @Size(max = 64, message = "关联教学步骤 ID 长度不能超过 64")
    private String relatedTaskStepId;

    @NotNull(message = "分值不能为空")
    @DecimalMin(value = "0.01", message = "分值必须大于 0")
    private BigDecimal score;

    private Boolean required;

    @NotBlank(message = "断言类型不能为空")
    @Size(max = 32, message = "断言类型长度不能超过 32")
    private String assertionType;

    @NotBlank(message = "断言配置不能为空")
    @Size(max = 32768, message = "断言配置 JSON 长度不能超过 32768")
    private String assertionConfigJson;

    @NotBlank(message = "失败策略不能为空")
    @Size(max = 32, message = "失败策略长度不能超过 32")
    private String failPolicy;

    @Size(max = 64, message = "创建人 ID 长度不能超过 64")
    private String createBy;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEvaluationRuleId() {
        return evaluationRuleId;
    }

    public void setEvaluationRuleId(String evaluationRuleId) {
        this.evaluationRuleId = evaluationRuleId;
    }

    public String getTeachingPointId() {
        return teachingPointId;
    }

    public void setTeachingPointId(String teachingPointId) {
        this.teachingPointId = teachingPointId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getRelatedResourceId() {
        return relatedResourceId;
    }

    public void setRelatedResourceId(String relatedResourceId) {
        this.relatedResourceId = relatedResourceId;
    }

    public String getRelatedApiResourceId() {
        return relatedApiResourceId;
    }

    public void setRelatedApiResourceId(String relatedApiResourceId) {
        this.relatedApiResourceId = relatedApiResourceId;
    }

    public String getRelatedTaskStepId() {
        return relatedTaskStepId;
    }

    public void setRelatedTaskStepId(String relatedTaskStepId) {
        this.relatedTaskStepId = relatedTaskStepId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getAssertionType() {
        return assertionType;
    }

    public void setAssertionType(String assertionType) {
        this.assertionType = assertionType;
    }

    public String getAssertionConfigJson() {
        return assertionConfigJson;
    }

    public void setAssertionConfigJson(String assertionConfigJson) {
        this.assertionConfigJson = assertionConfigJson;
    }

    public String getFailPolicy() {
        return failPolicy;
    }

    public void setFailPolicy(String failPolicy) {
        this.failPolicy = failPolicy;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
}
