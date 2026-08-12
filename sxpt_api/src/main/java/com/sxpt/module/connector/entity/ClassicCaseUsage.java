package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 经典案例使用记录实体。
 *
 * 业务功能：
 * 1. 映射 classic_case_usage 表，记录一次经典案例在学习环境中的复刻或 demo 生成结果。
 * 2. 只保存学习环境返回的业务数据引用，不接管原平台真实业务数据。
 *
 * 关键流程：
 * 1. 老师/专家备案教学时记录 TEACHING_REPLICA。
 * 2. 学生练习时记录 STUDENT_DEMO，并按学生或题目维度保留隔离追踪信息。
 */
@TableName("classic_case_usage")
public class ClassicCaseUsage {

    @TableId
    private String id;

    private String tenantId;

    private String caseAssetId;

    private String caseVersionId;

    private String usageScene;

    private String sceneType;

    private String taskId;

    private String requestBatchId;

    private String requestItemId;

    private String ownerUserId;

    private String questionId;

    private String generatedInstanceId;

    private String teachingDataInstanceId;

    private String externalBusinessNo;

    private String externalBusinessName;

    private String externalStatus;

    private String targetUrl;

    private String requestJson;

    private String resultJson;

    private String useBy;

    private LocalDateTime useTime;

    private String traceId;

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

    public String getGeneratedInstanceId() {
        return generatedInstanceId;
    }

    public void setGeneratedInstanceId(String generatedInstanceId) {
        this.generatedInstanceId = generatedInstanceId;
    }

    public String getTeachingDataInstanceId() {
        return teachingDataInstanceId;
    }

    public void setTeachingDataInstanceId(String teachingDataInstanceId) {
        this.teachingDataInstanceId = teachingDataInstanceId;
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

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public String getUseBy() {
        return useBy;
    }

    public void setUseBy(String useBy) {
        this.useBy = useBy;
    }

    public LocalDateTime getUseTime() {
        return useTime;
    }

    public void setUseTime(LocalDateTime useTime) {
        this.useTime = useTime;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
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
