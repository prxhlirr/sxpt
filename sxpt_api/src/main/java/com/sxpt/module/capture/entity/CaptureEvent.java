package com.sxpt.module.capture.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * SDK 采集事件实体。
 *
 * 业务功能：
 * 1. 映射 capture_event 表，保存 SDK 上报的页面操作事实。
 * 2. 只承载关键目标、脱敏输入和摘要 JSON，避免默认保存完整 DOM 或敏感明文。
 *
 * 关键流程：
 * 1. SDK 每次点击、输入、选择、上传或页面变化后生成事件并携带 clientEventId 上报。
 * 2. Service 负责必填字段、默认值和幂等判断，Mapper 只提供基础持久化能力。
 */
@TableName("capture_event")
public class CaptureEvent {

    @TableId
    private String id;

    private String tenantId;

    private String captureSessionId;

    private String sdkSessionId;

    private String clientEventId;

    private String eventType;

    private LocalDateTime eventTime;

    private Long sequenceNo;

    private Long retryCount;

    private String pageUrl;

    private String targetText;

    private String targetLocator;

    private String targetStableKey;

    private String inputValueMasked;

    private String eventPayloadJson;

    private String dataSessionId;

    private String dataInstanceId;

    private String businessSceneCode;

    private String externalBusinessId;

    private String archiveStatus;

    private LocalDateTime archiveTime;

    private LocalDateTime expireTime;

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

    public String getSdkSessionId() {
        return sdkSessionId;
    }

    public void setSdkSessionId(String sdkSessionId) {
        this.sdkSessionId = sdkSessionId;
    }

    public String getClientEventId() {
        return clientEventId;
    }

    public void setClientEventId(String clientEventId) {
        this.clientEventId = clientEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public Long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public Long getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Long retryCount) {
        this.retryCount = retryCount;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getTargetText() {
        return targetText;
    }

    public void setTargetText(String targetText) {
        this.targetText = targetText;
    }

    public String getTargetLocator() {
        return targetLocator;
    }

    public void setTargetLocator(String targetLocator) {
        this.targetLocator = targetLocator;
    }

    public String getTargetStableKey() {
        return targetStableKey;
    }

    public void setTargetStableKey(String targetStableKey) {
        this.targetStableKey = targetStableKey;
    }

    public String getInputValueMasked() {
        return inputValueMasked;
    }

    public void setInputValueMasked(String inputValueMasked) {
        this.inputValueMasked = inputValueMasked;
    }

    public String getEventPayloadJson() {
        return eventPayloadJson;
    }

    public void setEventPayloadJson(String eventPayloadJson) {
        this.eventPayloadJson = eventPayloadJson;
    }

    public String getDataSessionId() {
        return dataSessionId;
    }

    public void setDataSessionId(String dataSessionId) {
        this.dataSessionId = dataSessionId;
    }

    public String getDataInstanceId() {
        return dataInstanceId;
    }

    public void setDataInstanceId(String dataInstanceId) {
        this.dataInstanceId = dataInstanceId;
    }

    public String getBusinessSceneCode() {
        return businessSceneCode;
    }

    public void setBusinessSceneCode(String businessSceneCode) {
        this.businessSceneCode = businessSceneCode;
    }

    public String getExternalBusinessId() {
        return externalBusinessId;
    }

    public void setExternalBusinessId(String externalBusinessId) {
        this.externalBusinessId = externalBusinessId;
    }

    public String getArchiveStatus() {
        return archiveStatus;
    }

    public void setArchiveStatus(String archiveStatus) {
        this.archiveStatus = archiveStatus;
    }

    public LocalDateTime getArchiveTime() {
        return archiveTime;
    }

    public void setArchiveTime(LocalDateTime archiveTime) {
        this.archiveTime = archiveTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
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
