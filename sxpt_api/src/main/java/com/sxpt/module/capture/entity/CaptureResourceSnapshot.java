package com.sxpt.module.capture.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 采集资源快照实体。
 *
 * 业务功能：
 * 1. 映射 capture_resource_snapshot 表，保存 SDK 上报的关键元素摘要和页面摘要。
 * 2. 默认不承载完整 DOM，仅在 DEBUG_FULL_DOM 且短期过期时允许保存排障快照。
 *
 * 关键流程：
 * 1. SDK 在页面加载、目标元素识别或关键状态变化时上报摘要快照。
 * 2. Service 负责校验快照范围、必填字段和 DEBUG 全量 DOM 的短期保留规则。
 */
@TableName("capture_resource_snapshot")
public class CaptureResourceSnapshot {

    @TableId
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

    private String promotedResourceId;

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

    public String getPromotedResourceId() {
        return promotedResourceId;
    }

    public void setPromotedResourceId(String promotedResourceId) {
        this.promotedResourceId = promotedResourceId;
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
