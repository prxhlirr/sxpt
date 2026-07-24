package com.sxpt.module.teaching.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 教学点实体。
 *
 * 业务功能：
 * 1. 映射 teaching_point 表，保存教师发布后的可教学业务单元。
 * 2. 承载业务说明、流程图附件、备案路径、执行角色、数据范围和默认遮罩策略。
 *
 * 关键流程：
 * 1. 教师完成采集、草稿确认和资源沉淀后发布教学点。
 * 2. 后续教学步骤、评分项、任务发布和 SDK runtime 均通过教学点 ID 引用该实体。
 */
@TableName("teaching_point")
public class TeachingPoint {

    @TableId
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

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    private String pointStatus;

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

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
