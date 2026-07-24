package com.sxpt.module.capture.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 创建页面动作草稿请求。
 *
 * 业务功能：
 * 1. 承载由采集事件整理出的页面动作草稿内容。
 * 2. 在 MVP 阶段允许系统直接提交动作名称、业务操作建议和教学提示建议。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发 Bean Validation 基础校验。
 * 2. Controller 将请求转换为 CaptureActionDraft 实体后交给 Service 写入。
 */
public class CreateCaptureActionDraftRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "采集会话 ID 不能为空")
    @Size(max = 64, message = "采集会话 ID 长度不能超过 64")
    private String captureSessionId;

    @Size(max = 64, message = "来源事件 ID 长度不能超过 64")
    private String eventId;

    @NotBlank(message = "动作名称不能为空")
    @Size(max = 128, message = "动作名称长度不能超过 128")
    private String actionName;

    @NotBlank(message = "动作类型不能为空")
    @Size(max = 32, message = "动作类型长度不能超过 32")
    private String actionType;

    @NotNull(message = "顺序号不能为空")
    private Long sequenceNo;

    @Size(max = 64, message = "正式资源 ID 长度不能超过 64")
    private String connectorResourceId;

    @Size(max = 128, message = "系统建议业务操作名称长度不能超过 128")
    private String suggestedOperationName;

    @Size(max = 128, message = "教师确认业务操作名称长度不能超过 128")
    private String confirmedOperationName;

    @Size(max = 128, message = "教师确认教学步骤名称长度不能超过 128")
    private String confirmedStepName;

    @Size(max = 4096, message = "学习提示长度不能超过 4096")
    private String guideContent;

    @Size(max = 4096, message = "练习提示长度不能超过 4096")
    private String practiceHint;

    @Size(max = 32, message = "确认状态长度不能超过 32")
    private String confirmStatus;

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
}
