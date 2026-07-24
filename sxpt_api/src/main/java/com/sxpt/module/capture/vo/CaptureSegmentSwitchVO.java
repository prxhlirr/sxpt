package com.sxpt.module.capture.vo;

import java.time.LocalDateTime;

/**
 * 备案片段切换返回对象。
 *
 * 业务功能：
 * 1. 向前端返回教师确认后的下一片段 launchToken 和执行身份摘要。
 * 2. 明确标识切换决策来源于教师确认的备案路径，而不是系统自动判断。
 *
 * 关键流程：
 * 1. Service 创建下一片段启动上下文后组装该对象。
 * 2. 前端携带 launchToken 跳转原平台，并按 segmentNo 展示下一片段提示。
 */
public class CaptureSegmentSwitchVO {

    private String launchContextId;

    private String launchToken;

    private String tenantId;

    private String teacherId;

    private String connectorSystemId;

    private Long currentSegmentNo;

    private Long nextSegmentNo;

    private String actorType;

    private String requiredExternalOrgId;

    private String requiredExternalOrgName;

    private String requiredExternalRoleId;

    private String requiredExternalRoleName;

    private String targetUrl;

    private Boolean switchConfirmRequired;

    private String switchDecisionSource;

    private String switchReason;

    private LocalDateTime expireTime;

    public String getLaunchContextId() {
        return launchContextId;
    }

    public void setLaunchContextId(String launchContextId) {
        this.launchContextId = launchContextId;
    }

    public String getLaunchToken() {
        return launchToken;
    }

    public void setLaunchToken(String launchToken) {
        this.launchToken = launchToken;
    }

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

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Boolean getSwitchConfirmRequired() {
        return switchConfirmRequired;
    }

    public void setSwitchConfirmRequired(Boolean switchConfirmRequired) {
        this.switchConfirmRequired = switchConfirmRequired;
    }

    public String getSwitchDecisionSource() {
        return switchDecisionSource;
    }

    public void setSwitchDecisionSource(String switchDecisionSource) {
        this.switchDecisionSource = switchDecisionSource;
    }

    public String getSwitchReason() {
        return switchReason;
    }

    public void setSwitchReason(String switchReason) {
        this.switchReason = switchReason;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
}
