package com.sxpt.module.teaching.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 教学任务步骤实体。
 *
 * 业务功能：
 * 1. 映射 task_step 表，保存学习和练习中需要展示的步骤、提示和身份切换建议。
 * 2. 承载步骤顺序、业务片段、原平台执行单位角色、回退策略和关联资源。
 *
 * 关键流程：
 * 1. 教师发布教学点后，根据备案路径和已确认动作生成任务步骤。
 * 2. 后续 SDK runtime、练习结果和评分项通过步骤 ID 引用该实体。
 */
@TableName("task_step")
public class TaskStep {

    @TableId
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
