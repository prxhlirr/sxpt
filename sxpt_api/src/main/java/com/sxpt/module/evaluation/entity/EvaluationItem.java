package com.sxpt.module.evaluation.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评分项实体。
 *
 * 业务功能：
 * 1. 映射 evaluation_item 表，保存教师确认的关键评分点和断言配置。
 * 2. 支持关键动作、业务规则、最终结果和外部 API 结果等评分类型。
 *
 * 关键流程：
 * 1. 教师在评价规则下创建多个评分项。
 * 2. 自动评分和教师复核按评分项读取断言配置和证据要求。
 */
@TableName("evaluation_item")
public class EvaluationItem {

    @TableId
    private String id;

    private String tenantId;

    private String evaluationRuleId;

    private String teachingPointId;

    private String itemCode;

    private String itemName;

    private String itemType;

    private String relatedResourceId;

    private String relatedApiResourceId;

    private String relatedTaskStepId;

    private BigDecimal score;

    private Boolean required;

    private String assertionType;

    private String assertionConfigJson;

    private String failPolicy;

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
