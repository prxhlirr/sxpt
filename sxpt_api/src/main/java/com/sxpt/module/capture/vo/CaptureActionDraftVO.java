package com.sxpt.module.capture.vo;

import java.time.LocalDateTime;

/**
 * 页面动作草稿返回对象。
 *
 * 业务功能：
 * 1. 向教师端返回系统生成的动作草稿和业务操作建议。
 * 2. 隔离数据库实体，避免软删除等内部持久化字段泄露给前端。
 *
 * 关键流程：
 * 1. Controller 从 CaptureActionDraft 实体中提取可展示字段。
 * 2. 教师端根据草稿内容进行确认、修改或丢弃。
 */
public class CaptureActionDraftVO {

    private String id;

    private String tenantId;

    private String captureSessionId;

    private String eventId;

    private String actionName;

    private String actionType;

    private Long sequenceNo;

    private String connectorResourceId;

    private String suggestedOperationName;

    private String confirmedOperationName;

    private String confirmedStepName;

    private String guideContent;

    private String practiceHint;

    private String confirmStatus;

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

    public String getCaptureSessionId() {
        return captureSessionId;
    }

    public void setCaptureSessionId(String captureSessionId) {
        this.captureSessionId = captureSessionId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public String getConnectorResourceId() {
        return connectorResourceId;
    }

    public void setConnectorResourceId(String connectorResourceId) {
        this.connectorResourceId = connectorResourceId;
    }

    public String getSuggestedOperationName() {
        return suggestedOperationName;
    }

    public void setSuggestedOperationName(String suggestedOperationName) {
        this.suggestedOperationName = suggestedOperationName;
    }

    public String getConfirmedOperationName() {
        return confirmedOperationName;
    }

    public void setConfirmedOperationName(String confirmedOperationName) {
        this.confirmedOperationName = confirmedOperationName;
    }

    public String getConfirmedStepName() {
        return confirmedStepName;
    }

    public void setConfirmedStepName(String confirmedStepName) {
        this.confirmedStepName = confirmedStepName;
    }

    public String getGuideContent() {
        return guideContent;
    }

    public void setGuideContent(String guideContent) {
        this.guideContent = guideContent;
    }

    public String getPracticeHint() {
        return practiceHint;
    }

    public void setPracticeHint(String practiceHint) {
        this.practiceHint = practiceHint;
    }

    public String getConfirmStatus() {
        return confirmStatus;
    }

    public void setConfirmStatus(String confirmStatus) {
        this.confirmStatus = confirmStatus;
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
