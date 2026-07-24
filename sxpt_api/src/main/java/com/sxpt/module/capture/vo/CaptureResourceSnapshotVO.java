package com.sxpt.module.capture.vo;

import java.time.LocalDateTime;

/**
 * 采集资源快照返回对象。
 *
 * 业务功能：
 * 1. 向前端返回已保存的页面摘要或关键元素摘要。
 * 2. 隔离数据库实体，避免软删除和归档时间等内部字段泄露给调用方。
 *
 * 关键流程：
 * 1. Controller 从 CaptureResourceSnapshot 实体中提取可展示字段。
 * 2. 前端或后续草稿生成流程使用资源类型、范围、定位和摘要 JSON 识别教学资源。
 */
public class CaptureResourceSnapshotVO {

    private String id;

    private String tenantId;

    private String captureSessionId;

    private String pageUrl;

    private String pageTitle;

    private String resourceType;

    private String snapshotScope;

    private String resourceName;

    private String resourceLocator;

    private String elementSnapshotJson;

    private String metadataJson;

    private String snapshotHash;

    private String archiveStatus;

    private LocalDateTime expireTime;

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

    public String getArchiveStatus() {
        return archiveStatus;
    }

    public void setArchiveStatus(String archiveStatus) {
        this.archiveStatus = archiveStatus;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
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
