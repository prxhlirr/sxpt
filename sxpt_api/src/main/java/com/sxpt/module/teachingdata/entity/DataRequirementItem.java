package com.sxpt.module.teachingdata.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 数据需求明细实体。
 *
 * 业务功能：
 * 1. 映射 data_requirement_item 表，保存每条原平台教学用业务数据的生成约束和返回结果。
 * 2. 表达某个学生、题目、协作片段在指定单位、角色、状态、动作下需要的数据。
 * 3. 支撑批量造数逐条幂等、逐条回填、逐条校验和逐条失败重试。
 *
 * 关键流程：
 * 1. 数据需求批次创建后，系统按学生、题目、单位、角色展开本表明细。
 * 2. 数据准备任务按 requestBatchId 和 requestItemId 调用原平台并逐条回填结果。
 * 3. 校验通过的明细才允许创建 teaching_data_instance 并参与 launchToken 绑定。
 */
@TableName("data_requirement_item")
public class DataRequirementItem {

    @TableId
    private String id;

    private String tenantId;

    private String requirementId;

    private String requestBatchId;

    private String requestItemId;

    private String connectorSystemId;

    private String businessModuleId;

    private String moduleCode;

    private String templateId;

    private String sceneType;

    private String taskId;

    private String executionId;

    private String studentId;

    private String questionId;

    private String examAttemptId;

    private String questionAttemptId;

    private String collaborationUnitId;

    private Long segmentNo;

    private String actorType;

    private String ownerExternalOrgId;

    private String ownerExternalOrgName;

    private String requiredExternalOrgId;

    private String requiredExternalOrgName;

    private String requiredExternalRoleId;

    private String requiredExternalRoleName;

    private String initExternalStatus;

    private String targetExternalStatus;

    private String targetUrl;

    private String dataScopeJson;

    private String requiredActionsJson;

    private String validationPolicyJson;

    private String scorePointSnapshotJson;

    private String itemStatus;

    private String externalBusinessId;

    private String externalBusinessNo;

    private String externalBusinessName;

    private String externalStatus;

    private String currentStepCode;

    private Integer currentActorNo;

    private String currentOrgId;

    private String currentOrgName;

    private String currentRoleId;

    private String currentRoleName;

    private String processChainSnapshotJson;

    private String validationStatus;

    private LocalDateTime validationTime;

    private String validationResultJson;

    private String failureReason;

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

    public String getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    public String getRequestBatchId() {
        return requestBatchId;
    }

    public void setRequestBatchId(String requestBatchId) {
        this.requestBatchId = requestBatchId;
    }

    public String getRequestItemId() {
        return requestItemId;
    }

    public void setRequestItemId(String requestItemId) {
        this.requestItemId = requestItemId;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getBusinessModuleId() {
        return businessModuleId;
    }

    public void setBusinessModuleId(String businessModuleId) {
        this.businessModuleId = businessModuleId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getExamAttemptId() {
        return examAttemptId;
    }

    public void setExamAttemptId(String examAttemptId) {
        this.examAttemptId = examAttemptId;
    }

    public String getQuestionAttemptId() {
        return questionAttemptId;
    }

    public void setQuestionAttemptId(String questionAttemptId) {
        this.questionAttemptId = questionAttemptId;
    }

    public String getCollaborationUnitId() {
        return collaborationUnitId;
    }

    public void setCollaborationUnitId(String collaborationUnitId) {
        this.collaborationUnitId = collaborationUnitId;
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

    public String getOwnerExternalOrgId() {
        return ownerExternalOrgId;
    }

    public void setOwnerExternalOrgId(String ownerExternalOrgId) {
        this.ownerExternalOrgId = ownerExternalOrgId;
    }

    public String getOwnerExternalOrgName() {
        return ownerExternalOrgName;
    }

    public void setOwnerExternalOrgName(String ownerExternalOrgName) {
        this.ownerExternalOrgName = ownerExternalOrgName;
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

    public String getInitExternalStatus() {
        return initExternalStatus;
    }

    public void setInitExternalStatus(String initExternalStatus) {
        this.initExternalStatus = initExternalStatus;
    }

    public String getTargetExternalStatus() {
        return targetExternalStatus;
    }

    public void setTargetExternalStatus(String targetExternalStatus) {
        this.targetExternalStatus = targetExternalStatus;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getDataScopeJson() {
        return dataScopeJson;
    }

    public void setDataScopeJson(String dataScopeJson) {
        this.dataScopeJson = dataScopeJson;
    }

    public String getRequiredActionsJson() {
        return requiredActionsJson;
    }

    public void setRequiredActionsJson(String requiredActionsJson) {
        this.requiredActionsJson = requiredActionsJson;
    }

    public String getValidationPolicyJson() {
        return validationPolicyJson;
    }

    public void setValidationPolicyJson(String validationPolicyJson) {
        this.validationPolicyJson = validationPolicyJson;
    }

    public String getScorePointSnapshotJson() {
        return scorePointSnapshotJson;
    }

    public void setScorePointSnapshotJson(String scorePointSnapshotJson) {
        this.scorePointSnapshotJson = scorePointSnapshotJson;
    }

    public String getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(String itemStatus) {
        this.itemStatus = itemStatus;
    }

    public String getExternalBusinessId() {
        return externalBusinessId;
    }

    public void setExternalBusinessId(String externalBusinessId) {
        this.externalBusinessId = externalBusinessId;
    }

    public String getExternalBusinessNo() {
        return externalBusinessNo;
    }

    public void setExternalBusinessNo(String externalBusinessNo) {
        this.externalBusinessNo = externalBusinessNo;
    }

    public String getExternalBusinessName() {
        return externalBusinessName;
    }

    public void setExternalBusinessName(String externalBusinessName) {
        this.externalBusinessName = externalBusinessName;
    }

    public String getExternalStatus() {
        return externalStatus;
    }

    public void setExternalStatus(String externalStatus) {
        this.externalStatus = externalStatus;
    }

    public String getCurrentStepCode() {
        return currentStepCode;
    }

    public void setCurrentStepCode(String currentStepCode) {
        this.currentStepCode = currentStepCode;
    }

    public Integer getCurrentActorNo() {
        return currentActorNo;
    }

    public void setCurrentActorNo(Integer currentActorNo) {
        this.currentActorNo = currentActorNo;
    }

    public String getCurrentOrgId() {
        return currentOrgId;
    }

    public void setCurrentOrgId(String currentOrgId) {
        this.currentOrgId = currentOrgId;
    }

    public String getCurrentOrgName() {
        return currentOrgName;
    }

    public void setCurrentOrgName(String currentOrgName) {
        this.currentOrgName = currentOrgName;
    }

    public String getCurrentRoleId() {
        return currentRoleId;
    }

    public void setCurrentRoleId(String currentRoleId) {
        this.currentRoleId = currentRoleId;
    }

    public String getCurrentRoleName() {
        return currentRoleName;
    }

    public void setCurrentRoleName(String currentRoleName) {
        this.currentRoleName = currentRoleName;
    }

    public String getProcessChainSnapshotJson() {
        return processChainSnapshotJson;
    }

    public void setProcessChainSnapshotJson(String processChainSnapshotJson) {
        this.processChainSnapshotJson = processChainSnapshotJson;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public LocalDateTime getValidationTime() {
        return validationTime;
    }

    public void setValidationTime(LocalDateTime validationTime) {
        this.validationTime = validationTime;
    }

    public String getValidationResultJson() {
        return validationResultJson;
    }

    public void setValidationResultJson(String validationResultJson) {
        this.validationResultJson = validationResultJson;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
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
