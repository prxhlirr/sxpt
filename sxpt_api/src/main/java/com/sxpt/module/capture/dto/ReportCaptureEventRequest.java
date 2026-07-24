package com.sxpt.module.capture.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * SDK 采集事件上报请求。
 *
 * 业务功能：
 * 1. 承载 SDK 从原平台页面上报的点击、输入、选择、上传和页面变化事件。
 * 2. 强制携带 clientEventId，确保 SDK 网络重试时可以由服务端幂等去重。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发 Bean Validation 基础校验。
 * 2. Controller 将请求转换为 CaptureEvent 实体后交给 Service 写入或命中幂等记录。
 */
public class ReportCaptureEventRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "采集会话 ID 不能为空")
    @Size(max = 64, message = "采集会话 ID 长度不能超过 64")
    private String captureSessionId;

    @Size(max = 128, message = "SDK 会话 ID 长度不能超过 128")
    private String sdkSessionId;

    @NotBlank(message = "客户端事件 ID 不能为空")
    @Size(max = 128, message = "客户端事件 ID 长度不能超过 128")
    private String clientEventId;

    @NotBlank(message = "事件类型不能为空")
    @Size(max = 32, message = "事件类型长度不能超过 32")
    private String eventType;

    @NotNull(message = "事件时间不能为空")
    private LocalDateTime eventTime;

    @NotNull(message = "事件顺序号不能为空")
    private Long sequenceNo;

    private Long retryCount;

    @Size(max = 1024, message = "页面地址长度不能超过 1024")
    private String pageUrl;

    @Size(max = 512, message = "目标文本长度不能超过 512")
    private String targetText;

    @Size(max = 1024, message = "目标定位长度不能超过 1024")
    private String targetLocator;

    @Size(max = 256, message = "目标稳定键长度不能超过 256")
    private String targetStableKey;

    @Size(max = 1024, message = "脱敏输入值长度不能超过 1024")
    private String inputValueMasked;

    @Size(max = 32768, message = "事件摘要 JSON 长度不能超过 32768")
    private String eventPayloadJson;

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
}
