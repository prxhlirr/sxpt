package com.sxpt.module.teaching.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 创建教学点请求。
 *
 * 业务功能：
 * 1. 承载教师发布教学点时提交的业务说明、流程图、备案路径和执行策略。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发基础参数校验。
 * 2. Controller 将请求转换为 TeachingPoint 实体后交给 Service 写入并发布。
 */
public class CreateTeachingPointRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "原平台 ID 不能为空")
    @Size(max = 64, message = "原平台 ID 长度不能超过 64")
    private String connectorSystemId;

    @NotBlank(message = "教学点编码不能为空")
    @Size(max = 64, message = "教学点编码长度不能超过 64")
    private String pointCode;

    @NotBlank(message = "教学点名称不能为空")
    @Size(max = 128, message = "教学点名称长度不能超过 128")
    private String pointName;

    @NotBlank(message = "教学点类型不能为空")
    @Size(max = 32, message = "教学点类型长度不能超过 32")
    private String pointType;

    @Size(max = 64, message = "来源采集会话 ID 长度不能超过 64")
    private String sourceCaptureSessionId;

    @Size(max = 32768, message = "业务总说明 JSON 长度不能超过 32768")
    private String businessOverviewJson;

    @Size(max = 64, message = "流程图文件 ID 长度不能超过 64")
    private String flowFileId;

    @Size(max = 1024, message = "流程图地址长度不能超过 1024")
    private String flowFileUrl;

    @Size(max = 32768, message = "备案路径 JSON 长度不能超过 32768")
    private String recordPathJson;

    @Size(max = 128, message = "原平台角色 ID 长度不能超过 128")
    private String requiredExternalRoleId;

    @Size(max = 128, message = "原平台角色名称长度不能超过 128")
    private String requiredExternalRoleName;

    @NotBlank(message = "执行策略不能为空")
    @Size(max = 32, message = "执行策略长度不能超过 32")
    private String executionStrategy;

    private LocalDateTime defaultGrantStartTime;

    private LocalDateTime defaultGrantEndTime;

    @Size(max = 32768, message = "数据范围 JSON 长度不能超过 32768")
    private String dataScopeJson;

    @Size(max = 32768, message = "遮罩策略 JSON 长度不能超过 32768")
    private String overlayPolicyJson;

    @Size(max = 1024, message = "教学点说明长度不能超过 1024")
    private String description;

    @Size(max = 64, message = "创建人 ID 长度不能超过 64")
    private String createBy;

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
}
