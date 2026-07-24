package com.sxpt.module.capture.vo;

import java.time.LocalDateTime;

/**
 * SDK 采集事件返回对象。
 *
 * 业务功能：
 * 1. 向 SDK 或前端返回已入库的采集事件摘要。
 * 2. 隔离数据库实体，避免软删除、归档时间等内部持久化字段泄露给调用方。
 *
 * 关键流程：
 * 1. Controller 从 CaptureEvent 实体中提取可展示字段。
 * 2. SDK 可使用 id、clientEventId 和 sequenceNo 判断事件是否被服务端接收。
 */
public class CaptureEventVO {

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

    private String archiveStatus;

    private String status;

    private LocalDateTime createTime;

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

    public String getArchiveStatus() {
        return archiveStatus;
    }

    public void setArchiveStatus(String archiveStatus) {
        this.archiveStatus = archiveStatus;
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
}
