package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 经典案例资产返回对象。
 *
 * 业务功能：
 * 1. 向原平台和后台页面返回经典案例资产的索引和绑定信息。
 * 2. 不直接回显完整脱敏 payload，避免大 JSON 在接口响应中被无意义传播。
 */
public class ClassicCaseAssetVO {

    private String id;

    private String tenantId;

    private String caseCode;

    private String caseTitle;

    private String caseSummary;

    private String sourceConnectorSystemId;

    private String learningConnectorSystemId;

    private String environmentGroupCode;

    private String businessModuleId;

    private String moduleCode;

    private String teachingPointId;

    private String sceneTypesJson;

    private String currentVersionId;

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

    public String getCaseCode() {
        return caseCode;
    }

    public void setCaseCode(String caseCode) {
        this.caseCode = caseCode;
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public String getCaseSummary() {
        return caseSummary;
    }

    public void setCaseSummary(String caseSummary) {
        this.caseSummary = caseSummary;
    }

    public String getSourceConnectorSystemId() {
        return sourceConnectorSystemId;
    }

    public void setSourceConnectorSystemId(String sourceConnectorSystemId) {
        this.sourceConnectorSystemId = sourceConnectorSystemId;
    }

    public String getLearningConnectorSystemId() {
        return learningConnectorSystemId;
    }

    public void setLearningConnectorSystemId(String learningConnectorSystemId) {
        this.learningConnectorSystemId = learningConnectorSystemId;
    }

    public String getEnvironmentGroupCode() {
        return environmentGroupCode;
    }

    public void setEnvironmentGroupCode(String environmentGroupCode) {
        this.environmentGroupCode = environmentGroupCode;
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

    public String getTeachingPointId() {
        return teachingPointId;
    }

    public void setTeachingPointId(String teachingPointId) {
        this.teachingPointId = teachingPointId;
    }

    public String getSceneTypesJson() {
        return sceneTypesJson;
    }

    public void setSceneTypesJson(String sceneTypesJson) {
        this.sceneTypesJson = sceneTypesJson;
    }

    public String getCurrentVersionId() {
        return currentVersionId;
    }

    public void setCurrentVersionId(String currentVersionId) {
        this.currentVersionId = currentVersionId;
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
