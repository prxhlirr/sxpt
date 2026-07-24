package com.sxpt.module.teaching.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 创建教学任务步骤请求。
 *
 * 业务功能：
 * 1. 承载教师发布教学步骤时提交的步骤顺序、角色路径、回退策略和提示内容。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发基础参数校验。
 * 2. Controller 将请求转换为 TaskStep 实体后交给 Service 写入。
 */
public class CreateTaskStepRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "任务 ID 不能为空")
    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @NotBlank(message = "教学点 ID 不能为空")
    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @NotBlank(message = "步骤编码不能为空")
    @Size(max = 64, message = "步骤编码长度不能超过 64")
    private String stepCode;

    @NotBlank(message = "步骤名称不能为空")
    @Size(max = 128, message = "步骤名称长度不能超过 128")
    private String stepName;

    @Size(max = 4096, message = "步骤说明长度不能超过 4096")
    private String stepDescription;

    @NotNull(message = "步骤排序号不能为空")
    private Long sequenceNo;

    private Long segmentNo;

    @Size(max = 64, message = "业务参与方类型长度不能超过 64")
    private String actorType;

    @Size(max = 128, message = "原平台单位 ID 长度不能超过 128")
    private String requiredExternalOrgId;

    @Size(max = 256, message = "原平台单位名称长度不能超过 256")
    private String requiredExternalOrgName;

    @Size(max = 128, message = "原平台角色 ID 长度不能超过 128")
    private String requiredExternalRoleId;

    @Size(max = 256, message = "原平台角色名称长度不能超过 256")
    private String requiredExternalRoleName;

    @Size(max = 32, message = "身份切换策略长度不能超过 32")
    private String switchStrategy;

    private Long nextSegmentNo;

    private Boolean switchConfirmRequired;

    @Size(max = 32, message = "切换决策来源长度不能超过 32")
    private String switchDecisionSource;

    @Size(max = 512, message = "切换原因长度不能超过 512")
    private String switchReason;

    @Size(max = 32, message = "回退策略长度不能超过 32")
    private String rollbackPolicy;

    @Size(max = 32768, message = "关联资源 ID JSON 长度不能超过 32768")
    private String relatedResourceIds;

    @Size(max = 4096, message = "学习提示长度不能超过 4096")
    private String guideContent;

    @Size(max = 4096, message = "练习提示长度不能超过 4096")
    private String practiceHint;

    private Boolean required;

    private Boolean allowSkip;

    @Size(max = 64, message = "来源动作草稿 ID 长度不能超过 64")
    private String sourceActionDraftId;

    @Size(max = 64, message = "创建人 ID 长度不能超过 64")
    private String createBy;

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
}
