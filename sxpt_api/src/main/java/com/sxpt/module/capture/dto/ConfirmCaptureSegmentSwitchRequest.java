package com.sxpt.module.capture.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 确认备案片段切换请求。
 *
 * 业务功能：
 * 1. 承载教师完成当前备案片段后，确认进入下一角色片段所需的上下文。
 * 2. 强制由教师提交下一片段编号、原平台单位、角色和目标地址，避免系统自动判断下一办理角色。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发 Bean Validation。
 * 2. Service 根据请求创建下一片段的 RECORD/CAPTURE launchToken。
 */
public class ConfirmCaptureSegmentSwitchRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "教师用户 ID 不能为空")
    @Size(max = 64, message = "教师用户 ID 长度不能超过 64")
    private String teacherId;

    @NotBlank(message = "原平台配置 ID 不能为空")
    @Size(max = 64, message = "原平台配置 ID 长度不能超过 64")
    private String connectorSystemId;

    @Size(max = 64, message = "采集会话 ID 长度不能超过 64")
    private String captureSessionId;

    @Size(max = 64, message = "教学业务数据实例 ID 长度不能超过 64")
    private String dataInstanceId;

    @NotNull(message = "当前片段编号不能为空")
    private Long currentSegmentNo;

    @NotNull(message = "下一片段编号不能为空")
    private Long nextSegmentNo;

    @Size(max = 64, message = "业务参与方类型长度不能超过 64")
    private String actorType;

    @NotBlank(message = "原平台单位 ID 不能为空")
    @Size(max = 128, message = "原平台单位 ID 长度不能超过 128")
    private String requiredExternalOrgId;

    @Size(max = 256, message = "原平台单位名称长度不能超过 256")
    private String requiredExternalOrgName;

    @NotBlank(message = "原平台角色 ID 不能为空")
    @Size(max = 128, message = "原平台角色 ID 长度不能超过 128")
    private String requiredExternalRoleId;

    @Size(max = 256, message = "原平台角色名称长度不能超过 256")
    private String requiredExternalRoleName;

    @Size(max = 128, message = "原平台业务数据 ID 长度不能超过 128")
    private String externalBusinessId;

    @Size(max = 128, message = "原平台业务单据号长度不能超过 128")
    private String externalBusinessNo;

    @NotBlank(message = "原平台目标地址不能为空")
    @Size(max = 1024, message = "原平台目标地址长度不能超过 1024")
    private String targetUrl;

    @Size(max = 512, message = "切换原因长度不能超过 512")
    private String switchReason;

    @Size(max = 32768, message = "数据范围摘要长度不能超过 32768")
    private String dataScopeJson;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getCaptureSessionId() {
        return captureSessionId;
    }

    public void setCaptureSessionId(String captureSessionId) {
        this.captureSessionId = captureSessionId;
    }

    public String getDataInstanceId() {
        return dataInstanceId;
    }

    public void setDataInstanceId(String dataInstanceId) {
        this.dataInstanceId = dataInstanceId;
    }

    public Long getCurrentSegmentNo() {
        return currentSegmentNo;
    }

    public void setCurrentSegmentNo(Long currentSegmentNo) {
        this.currentSegmentNo = currentSegmentNo;
    }

    public Long getNextSegmentNo() {
        return nextSegmentNo;
    }

    public void setNextSegmentNo(Long nextSegmentNo) {
        this.nextSegmentNo = nextSegmentNo;
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

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getSwitchReason() {
        return switchReason;
    }

    public void setSwitchReason(String switchReason) {
        this.switchReason = switchReason;
    }

    public String getDataScopeJson() {
        return dataScopeJson;
    }

    public void setDataScopeJson(String dataScopeJson) {
        this.dataScopeJson = dataScopeJson;
    }
}
