package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 经典案例资产实体。
 *
 * 业务功能：
 * 1. 映射 classic_case_asset 表，保存经典案例的业务归属、来源正式环境和目标学习环境。
 * 2. 只保存脱敏案例的索引信息，不保存生产环境真实身份 ID。
 *
 * 关键流程：
 * 1. 原平台正式环境推送脱敏案例后，教学平台按 caseCode 创建或更新资产。
 * 2. currentVersionId 指向最新脱敏内容版本，教学/练习运行时始终从版本表读取 payload。
 */
@TableName("classic_case_asset")
public class ClassicCaseAsset {

    @TableId
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

    private Long lockVersion;

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

    public Long getLockVersion() {
        return lockVersion;
    }

    public void setLockVersion(Long lockVersion) {
        this.lockVersion = lockVersion;
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
