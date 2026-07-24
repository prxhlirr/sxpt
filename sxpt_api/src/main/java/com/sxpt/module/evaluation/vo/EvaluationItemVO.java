package com.sxpt.module.evaluation.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评分项返回对象。
 *
 * 业务功能：
 * 1. 向教师端和自动评分模块返回评分项配置。
 * 2. 隔离数据库实体，避免软删除等内部持久化字段泄露给前端。
 *
 * 关键流程：
 * 1. Controller 从 EvaluationItem 实体提取可展示字段。
 * 2. 自动评分按 assertionType 和 assertionConfigJson 执行断言。
 */
public class EvaluationItemVO {

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

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
