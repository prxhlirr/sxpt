package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建原平台正式资源请求。
 *
 * 业务功能：
 * 1. 承载教师确认后需要沉淀的页面资源信息。
 * 2. 只面向教学步骤、遮罩或评分需要引用的关键资源，避免批量保存无教学价值 DOM。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发 Bean Validation 基础校验。
 * 2. Controller 将请求转换为 ConnectorResource 实体后交给 Service 写入。
 */
public class CreateConnectorResourceRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "原平台 ID 不能为空")
    @Size(max = 64, message = "原平台 ID 长度不能超过 64")
    private String connectorSystemId;

    @NotBlank(message = "资源编码不能为空")
    @Size(max = 128, message = "资源编码长度不能超过 128")
    private String resourceCode;

    @NotBlank(message = "资源名称不能为空")
    @Size(max = 128, message = "资源名称长度不能超过 128")
    private String resourceName;

    @NotBlank(message = "资源类型不能为空")
    @Size(max = 32, message = "资源类型长度不能超过 32")
    private String resourceType;

    @Size(max = 1024, message = "页面地址长度不能超过 1024")
    private String pageUrl;

    @Size(max = 1024, message = "资源定位长度不能超过 1024")
    private String locator;

    @Size(max = 256, message = "稳定键长度不能超过 256")
    private String stableKey;

    @Size(max = 32768, message = "元数据 JSON 长度不能超过 32768")
    private String metadataJson;

    @Size(max = 64, message = "来源采集会话 ID 长度不能超过 64")
    private String sourceCaptureId;

    @Size(max = 64, message = "创建人 ID 长度不能超过 64")
    private String createBy;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(String resourceCode) {
        this.resourceCode = resourceCode;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getLocator() {
        return locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

    public String getStableKey() {
        return stableKey;
    }

    public void setStableKey(String stableKey) {
        this.stableKey = stableKey;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public String getSourceCaptureId() {
        return sourceCaptureId;
    }

    public void setSourceCaptureId(String sourceCaptureId) {
        this.sourceCaptureId = sourceCaptureId;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
}
