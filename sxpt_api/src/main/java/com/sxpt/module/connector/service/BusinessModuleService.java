package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.BusinessModule;

import java.util.List;

/**
 * 原平台业务模块管理服务。
 *
 * 业务功能：
 * 1. 维护原平台中可教学化的业务模块主数据，作为数据准备策略、数据需求和跳转上下文的共同引用。
 * 2. 按租户和原平台隔离模块列表，避免跨租户、跨原平台误用业务入口和默认状态。
 *
 * 关键流程：
 * 1. 创建时校验模块最小可用字段，并补齐生命周期字段。
 * 2. 更新时只覆盖允许后台维护的字段，保留租户、原平台和模块编码等稳定身份字段。
 * 3. 启停时只切换通用状态，后续数据准备只能选择启用模块。
 */
public interface BusinessModuleService {

    /**
     * 创建原平台业务模块。
     *
     * @param businessModule 原平台业务模块实体，必须包含租户、原平台、模块编码、模块名称、入口地址和支持场景。
     * @return 已保存并补齐默认字段的业务模块实体。
     */
    BusinessModule createBusinessModule(BusinessModule businessModule);

    /**
     * 更新原平台业务模块。
     *
     * @param businessModule 原平台业务模块实体，必须包含 ID 以及允许后台编辑的字段。
     * @return 已更新的业务模块实体。
     */
    BusinessModule updateBusinessModule(BusinessModule businessModule);

    /**
     * 启用原平台业务模块。
     *
     * @param id 业务模块 ID。
     * @return 已启用的业务模块实体。
     */
    BusinessModule enableBusinessModule(String id);

    /**
     * 禁用原平台业务模块。
     *
     * @param id 业务模块 ID。
     * @return 已禁用的业务模块实体。
     */
    BusinessModule disableBusinessModule(String id);

    /**
     * 查询业务模块详情。
     *
     * @param id 业务模块 ID。
     * @return 未删除的业务模块实体。
     */
    BusinessModule getBusinessModuleById(String id);

    /**
     * 查询某租户在某原平台下的全部未删除业务模块。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @return 业务模块列表。
     */
    List<BusinessModule> listBusinessModules(String tenantId, String connectorSystemId);

    /**
     * 查询某租户在某原平台下可用于策略选择的启用业务模块。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @return 启用业务模块列表。
     */
    List<BusinessModule> listActiveBusinessModules(String tenantId, String connectorSystemId);
}
