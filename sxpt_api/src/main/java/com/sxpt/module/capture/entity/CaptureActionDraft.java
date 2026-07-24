package com.sxpt.module.capture.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 页面动作草稿实体。
 *
 * 业务功能：
 * 1. 映射 capture_action_draft 表，保存由采集事件整理出的教师可确认页面动作。
 * 2. 在 MVP 阶段直接承载业务操作建议、教学步骤建议、学习提示和练习提示。
 *
 * 关键流程：
 * 1. 采集事件和资源摘要形成事实后，系统生成 PENDING 状态的动作草稿。
 * 2. 后续教师确认或丢弃草稿，再进入正式资源沉淀和教学步骤发布流程。
 */
@TableName("capture_action_draft")
public class CaptureActionDraft {

    @TableId
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
