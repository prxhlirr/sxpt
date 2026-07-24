package com.sxpt.module.teaching.vo;

import java.time.LocalDateTime;

/**
 * 教学任务步骤返回对象。
 *
 * 业务功能：
 * 1. 向教师端、任务发布和 SDK runtime 返回教学步骤信息。
 * 2. 隔离数据库实体，避免软删除等内部持久化字段泄露给前端。
 *
 * 关键流程：
 * 1. Controller 从 TaskStep 实体提取可展示字段。
 * 2. 后续学习、练习和评分通过 id 引用教学步骤。
 */
public class TaskStepVO {

    private String id;

    private String tenantId;

    private String taskId;

    private String teachingPointId;

    private String stepCode;

    private String stepName;

    private String stepDescription;

    private Long sequenceNo;

    private Long segmentNo;

    private String actorType;

    private String requiredExternalOrgId;

    private String requiredExternalOrgName;

    private String requiredExternalRoleId;

    private String requiredExternalRoleName;

    private String switchStrategy;

    private Long nextSegmentNo;

    private Boolean switchConfirmRequired;

    private String switchDecisionSource;

    private String switchReason;

    private String rollbackPolicy;

    private String relatedResourceIds;

    private String guideContent;

    private String practiceHint;

    private Boolean required;

    private Boolean allowSkip;

    private String sourceActionDraftId;

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

    public String getStepCode() {
        return stepCode;
    }

    public void setStepCode(String stepCode) {
        this.stepCode = stepCode;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStepDescription() {
        return stepDescription;
    }

    public void setStepDescription(String stepDescription) {
        this.stepDescription = stepDescription;
    }

    public Long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public Long getSegmentNo() {
        return segmentNo;
    }

    public void setSegmentNo(Long segmentNo) {
        this.segmentNo = segmentNo;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public String getRequiredExternalOrgId() {
        return requiredExternalOrgId;
    }

    public void setRequiredExternalOrgId(String requiredExternalOrgId) {
        this.requiredExternalOrgId = requiredExternalOrgId;
    }

    public String getRequiredExternalOrgName() {
        return requiredExternalOrgName;
    }

    public void setRequiredExternalOrgName(String requiredExternalOrgName) {
        this.requiredExternalOrgName = requiredExternalOrgName;
    }

    public String getRequiredExternalRoleId() {
        return requiredExternalRoleId;
    }

    public void setRequiredExternalRoleId(String requiredExternalRoleId) {
        this.requiredExternalRoleId = requiredExternalRoleId;
    }

    public String getRequiredExternalRoleName() {
        return requiredExternalRoleName;
    }

    public void setRequiredExternalRoleName(String requiredExternalRoleName) {
        this.requiredExternalRoleName = requiredExternalRoleName;
    }

    public String getSwitchStrategy() {
        return switchStrategy;
    }

    public void setSwitchStrategy(String switchStrategy) {
        this.switchStrategy = switchStrategy;
    }

    public Long getNextSegmentNo() {
        return nextSegmentNo;
    }

    public void setNextSegmentNo(Long nextSegmentNo) {
        this.nextSegmentNo = nextSegmentNo;
    }

    public Boolean getSwitchConfirmRequired() {
        return switchConfirmRequired;
    }

    public void setSwitchConfirmRequired(Boolean switchConfirmRequired) {
        this.switchConfirmRequired = switchConfirmRequired;
    }

    public String getSwitchDecisionSource() {
        return switchDecisionSource;
    }

    public void setSwitchDecisionSource(String switchDecisionSource) {
        this.switchDecisionSource = switchDecisionSource;
    }

    public String getSwitchReason() {
        return switchReason;
    }

    public void setSwitchReason(String switchReason) {
        this.switchReason = switchReason;
    }

    public String getRollbackPolicy() {
        return rollbackPolicy;
    }

    public void setRollbackPolicy(String rollbackPolicy) {
        this.rollbackPolicy = rollbackPolicy;
    }

    public String getRelatedResourceIds() {
        return relatedResourceIds;
    }

    public void setRelatedResourceIds(String relatedResourceIds) {
        this.relatedResourceIds = relatedResourceIds;
    }

    public String getGuideContent() {
        return guideContent;
    }

    public void setGuideContent(String guideContent) {
        this.guideContent = guideContent;
    }

    public String getPracticeHint() {
        return practiceHint;
    }

    public void setPracticeHint(String practiceHint) {
        this.practiceHint = practiceHint;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean getAllowSkip() {
        return allowSkip;
    }

    public void setAllowSkip(Boolean allowSkip) {
        this.allowSkip = allowSkip;
    }

    public String getSourceActionDraftId() {
        return sourceActionDraftId;
    }

    public void setSourceActionDraftId(String sourceActionDraftId) {
        this.sourceActionDraftId = sourceActionDraftId;
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
