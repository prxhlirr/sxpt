package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.ConnectorResource;

import java.util.List;

/**
 * 原平台正式资源服务。
 *
 * 业务功能：
 * 1. 将被教师确认、被引导或被评分引用的关键元素沉淀为正式资源。
 * 2. 支持按原平台页面查询正式资源，供 SDK runtime 和教学步骤配置使用。
 *
 * 关键流程：
 * 1. 创建正式资源时校验租户、原平台、资源编码、资源名称和资源类型。
 * 2. 只保存教学需要的资源，不把普通 DOM 批量沉淀进正式资源表。
 */
public interface ConnectorResourceService {

    /**
     * 创建正式资源。
     *
     * @param connectorResource 原平台正式资源。
     * @return 已保存的正式资源。
     */
    ConnectorResource createConnectorResource(ConnectorResource connectorResource);

    /**
     * 按页面查询原平台正式资源。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @param pageUrl 页面地址。
     * @return 页面下的正式资源列表。
     */
    List<ConnectorResource> listByPage(String tenantId, String connectorSystemId, String pageUrl);
}
