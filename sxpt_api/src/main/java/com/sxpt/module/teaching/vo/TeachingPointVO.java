package com.sxpt.module.teaching.vo;

import java.time.LocalDateTime;

/**
 * 教学点返回对象。
 *
 * 业务功能：
 * 1. 向教师端、任务发布和后续步骤配置返回已发布教学点信息。
 * 2. 隔离数据库实体，避免软删除等内部持久化字段泄露给前端。
 *
 * 关键流程：
 * 1. Controller 从 TeachingPoint 实体提取可展示字段。
 * 2. 后续教学步骤、评分项和任务通过 id 引用教学点。
 */
public class TeachingPointVO {

    private String id;

    private String tenantId;

    private String connectorSystemId;

    private String pointCode;

    private String pointName;

    private String pointType;

    private Long versionNo;

    private String sourceCaptureSessionId;

    private String businessOverviewJson;

    private String flowFileId;

    private String flowFileUrl;

    private String recordPathJson;

    private String requiredExternalRoleId;

    private String requiredExternalRoleName;

    private String executionStrategy;

    private LocalDateTime defaultGrantStartTime;

    private LocalDateTime defaultGrantEndTime;

    private String dataScopeJson;

    private String overlayPolicyJson;

    private String description;

    private String pointStatus;

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

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getPointCode() {
        return pointCode;
    }

    public void setPointCode(String pointCode) {
        this.pointCode = pointCode;
    }

    public String getPointName() {
        return pointName;
    }

    public void setPointName(String pointName) {
        this.pointName = pointName;
    }

    public String getPointType() {
        return pointType;
    }

    public void setPointType(String pointType) {
        this.pointType = pointType;
    }

    public Long getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Long versionNo) {
        this.versionNo = versionNo;
    }

    public String getSourceCaptureSessionId() {
        return sourceCaptureSessionId;
    }

    public void setSourceCaptureSessionId(String sourceCaptureSessionId) {
        this.sourceCaptureSessionId = sourceCaptureSessionId;
    }

    public String getBusinessOverviewJson() {
        return businessOverviewJson;
    }

    public void setBusinessOverviewJson(String businessOverviewJson) {
        this.businessOverviewJson = businessOverviewJson;
    }

    public String getFlowFileId() {
        return flowFileId;
    }

    public void setFlowFileId(String flowFileId) {
        this.flowFileId = flowFileId;
    }

    public String getFlowFileUrl() {
        return flowFileUrl;
    }

    public void setFlowFileUrl(String flowFileUrl) {
        this.flowFileUrl = flowFileUrl;
    }

    public String getRecordPathJson() {
        return recordPathJson;
    }

    public void setRecordPathJson(String recordPathJson) {
        this.recordPathJson = recordPathJson;
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

    public String getExecutionStrategy() {
        return executionStrategy;
    }

    public void setExecutionStrategy(String executionStrategy) {
        this.executionStrategy = executionStrategy;
    }

    public LocalDateTime getDefaultGrantStartTime() {
        return defaultGrantStartTime;
    }

    public void setDefaultGrantStartTime(LocalDateTime defaultGrantStartTime) {
        this.defaultGrantStartTime = defaultGrantStartTime;
    }

    public LocalDateTime getDefaultGrantEndTime() {
        return defaultGrantEndTime;
    }

    public void setDefaultGrantEndTime(LocalDateTime defaultGrantEndTime) {
        this.defaultGrantEndTime = defaultGrantEndTime;
    }

    public String getDataScopeJson() {
        return dataScopeJson;
    }

    public void setDataScopeJson(String dataScopeJson) {
        this.dataScopeJson = dataScopeJson;
    }

    public String getOverlayPolicyJson() {
        return overlayPolicyJson;
    }

    public void setOverlayPolicyJson(String overlayPolicyJson) {
        this.overlayPolicyJson = overlayPolicyJson;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPointStatus() {
        return pointStatus;
    }

    public void setPointStatus(String pointStatus) {
        this.pointStatus = pointStatus;
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
