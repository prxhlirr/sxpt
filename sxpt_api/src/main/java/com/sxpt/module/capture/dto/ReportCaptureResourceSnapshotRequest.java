package com.sxpt.module.capture.dto;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 采集资源快照上报请求。
 *
 * 业务功能：
 * 1. 承载 SDK 上报的页面摘要、目标元素摘要和排障快照。
 * 2. 默认面向 TARGET_ELEMENT 和 PAGE_SUMMARY，DEBUG_FULL_DOM 必须显式携带短期过期时间。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发 Bean Validation 基础校验。
 * 2. Controller 将请求转换为 CaptureResourceSnapshot 实体后交给 Service 写入。
 */
public class ReportCaptureResourceSnapshotRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "采集会话 ID 不能为空")
    @Size(max = 64, message = "采集会话 ID 长度不能超过 64")
    private String captureSessionId;

    @NotBlank(message = "页面地址不能为空")
    @Size(max = 1024, message = "页面地址长度不能超过 1024")
    private String pageUrl;

    @Size(max = 256, message = "页面标题长度不能超过 256")
    private String pageTitle;

    @NotBlank(message = "资源类型不能为空")
    @Size(max = 32, message = "资源类型长度不能超过 32")
    private String resourceType;

    @NotBlank(message = "快照范围不能为空")
    @Size(max = 32, message = "快照范围长度不能超过 32")
    private String snapshotScope;

    @Size(max = 128, message = "资源名称长度不能超过 128")
    private String resourceName;

    @Size(max = 1024, message = "资源定位长度不能超过 1024")
    private String resourceLocator;

    @NotBlank(message = "元素摘要 JSON 不能为空")
    @Size(max = 32768, message = "元素摘要 JSON 长度不能超过 32768")
    private String elementSnapshotJson;

    @Size(max = 32768, message = "元数据 JSON 长度不能超过 32768")
    private String metadataJson;

    @Size(max = 128, message = "快照 Hash 长度不能超过 128")
    private String snapshotHash;

    private LocalDateTime expireTime;

    /**
     * 校验 DEBUG_FULL_DOM 必须设置过期时间。
     *
     * @return true 表示请求满足 DEBUG 快照前置约束。
     */
    @AssertTrue(message = "DEBUG_FULL_DOM 必须设置过期时间")
    public boolean isDebugFullDomExpireTimeValid() {
        return !"DEBUG_FULL_DOM".equals(snapshotScope) || expireTime != null;
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

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getSnapshotScope() {
        return snapshotScope;
    }

    public void setSnapshotScope(String snapshotScope) {
        this.snapshotScope = snapshotScope;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceLocator() {
        return resourceLocator;
    }

    public void setResourceLocator(String resourceLocator) {
        this.resourceLocator = resourceLocator;
    }

    public String getElementSnapshotJson() {
        return elementSnapshotJson;
    }

    public void setElementSnapshotJson(String elementSnapshotJson) {
        this.elementSnapshotJson = elementSnapshotJson;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public String getSnapshotHash() {
        return snapshotHash;
    }

    public void setSnapshotHash(String snapshotHash) {
        this.snapshotHash = snapshotHash;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
}
