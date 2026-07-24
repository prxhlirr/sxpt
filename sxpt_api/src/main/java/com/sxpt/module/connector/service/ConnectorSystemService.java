package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.ConnectorSystem;

import java.util.List;

/**
 * 原业务平台配置服务。
 *
 * 业务功能：
 * 1. 维护教学平台可接入的原业务平台配置。
 * 2. 为 launchToken、SDK、采集和教学数据实例提供稳定的原平台配置来源。
 *
 * 关键流程：
 * 1. Service 层负责校验原平台配置的业务完整性。
 * 2. Mapper 层只负责数据库写入，不承载业务判断。
 */
public interface ConnectorSystemService {

    /**
     * 创建原业务平台配置。
     *
     * @param connectorSystem 原平台配置实体，必须包含租户、编码、名称、类型、基础地址和认证方式。
     * @return 已保存的原平台配置实体。
     */
    ConnectorSystem createConnectorSystem(ConnectorSystem connectorSystem);

    /**
     * 更新原业务平台配置。
     *
     * @param connectorSystem 原平台配置实体，必须包含 ID、名称、类型、基础地址和认证方式。
     * @return 已更新的原平台配置实体。
     */
    ConnectorSystem updateConnectorSystem(ConnectorSystem connectorSystem);

    /**
     * 启用原业务平台配置。
     *
     * @param id 原平台配置 ID。
     * @return 已启用的原平台配置实体。
     */
    ConnectorSystem enableConnectorSystem(String id);

    /**
     * 禁用原业务平台配置。
     *
     * @param id 原平台配置 ID。
     * @return 已禁用的原平台配置实体。
     */
    ConnectorSystem disableConnectorSystem(String id);

    /**
     * 查询原业务平台配置详情。
     *
     * @param id 原平台配置 ID。
     * @return 未删除的原平台配置实体。
     */
    ConnectorSystem getConnectorSystemById(String id);

    /**
     * 查询指定租户下的原业务平台配置列表。
     *
     * @param tenantId 租户 ID。
     * @return 指定租户下未删除的原平台配置列表。
     */
    List<ConnectorSystem> listConnectorSystemsByTenantId(String tenantId);
}
