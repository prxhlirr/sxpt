package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 经典案例生成请求。
 *
 * 业务功能：
 * 1. 承载老师/专家备案教学或学生练习时，基于经典案例在学习环境生成业务数据的请求。
 * 2. 用 usageScene 区分老师一比一复刻和学生 demo 生成，用 sceneType 保持与教学平台现有场景一致。
 *
 * 关键流程：
 * 1. 老师/专家使用 TEACHING_REPLICA，服务端携带 desensitizedCasePayload 调用学习环境。
 * 2. 学生使用 STUDENT_DEMO，服务端仅携带 caseDataFormat 调用学习环境。
 */
public class ClassicCaseGenerateRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "经典案例 ID 不能为空")
    @Size(max = 64, message = "经典案例 ID 长度不能超过 64")
    private String caseAssetId;

    @Size(max = 64, message = "经典案例版本 ID 长度不能超过 64")
    private String caseVersionId;

    @NotBlank(message = "使用方式不能为空")
    @Size(max = 32, message = "使用方式长度不能超过 32")
    private String usageScene;

    @NotBlank(message = "教学场景不能为空")
    @Size(max = 32, message = "教学场景长度不能超过 32")
    private String sceneType;

    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @NotBlank(message = "使用人不能为空")
    @Size(max = 64, message = "使用人长度不能超过 64")
    private String ownerUserId;

    @Size(max = 64, message = "题目 ID 长度不能超过 64")
    private String questionId;

    @Size(max = 128, message = "请求批次 ID 长度不能超过 128")
    private String requestBatchId;

    @Size(max = 128, message = "请求明细 ID 长度不能超过 128")
    private String requestItemId;

    @Size(max = 128, message = "链路追踪 ID 长度不能超过 128")
    private String traceId;

    private String participantContextJson;

    private String requiredExternalOrgId;

    private String requiredExternalRoleId;

    private String actorType;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCaseAssetId() {
        return caseAssetId;
    }

    public void setCaseAssetId(String caseAssetId) {
        this.caseAssetId = caseAssetId;
    }

    public String getCaseVersionId() {
        return caseVersionId;
    }

    public void setCaseVersionId(String caseVersionId) {
        this.caseVersionId = caseVersionId;
    }

    public String getUsageScene() {
        return usageScene;
    }

    public void setUsageScene(String usageScene) {
        this.usageScene = usageScene;
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

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
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

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getParticipantContextJson() {
        return participantContextJson;
    }

    public void setParticipantContextJson(String participantContextJson) {
        this.participantContextJson = participantContextJson;
    }

    public String getRequiredExternalOrgId() {
        return requiredExternalOrgId;
    }

    public void setRequiredExternalOrgId(String requiredExternalOrgId) {
        this.requiredExternalOrgId = requiredExternalOrgId;
    }

    public String getRequiredExternalRoleId() {
        return requiredExternalRoleId;
    }

    public void setRequiredExternalRoleId(String requiredExternalRoleId) {
        this.requiredExternalRoleId = requiredExternalRoleId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }
}
