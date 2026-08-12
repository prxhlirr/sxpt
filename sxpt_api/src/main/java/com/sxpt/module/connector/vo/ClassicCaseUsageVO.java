package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 经典案例使用记录返回对象。
 *
 * 业务功能：
 * 1. 返回一次经典案例生成后的学习环境业务数据引用。
 * 2. 供备案、教学或学生练习入口继续生成 launchToken 或展示生成结果。
 */
public class ClassicCaseUsageVO {

    private String id;

    private String caseAssetId;

    private String caseVersionId;

    private String usageScene;

    private String sceneType;

    private String requestBatchId;

    private String requestItemId;

    private String ownerUserId;

    private String generatedInstanceId;

    private String teachingDataInstanceId;

    private String externalBusinessNo;

    private String externalBusinessName;

    private String externalStatus;

    private String targetUrl;

    private String status;

    private LocalDateTime useTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUseTime() {
        return useTime;
    }

    public void setUseTime(LocalDateTime useTime) {
        this.useTime = useTime;
    }
}
