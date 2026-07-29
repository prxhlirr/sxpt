package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.ModuleDataStrategy;

import java.util.List;

/**
 * 原平台业务模块数据准备策略服务。
 *
 * 业务功能：
 * 1. 维护业务模块在备案、学习、练习、考试场景下的数据准备策略。
 * 2. 为数据需求生成、数据池创建、练习重置和考试数据绑定提供统一策略入口。
 *
 * 关键流程：
 * 1. 创建策略时保存租户、原平台、业务模块、场景和策略规则，默认先保持停用。
 * 2. 更新策略时只覆盖可编辑规则字段，保留租户、原平台、业务模块、模块编码和场景等稳定边界。
 * 3. 启用策略前校验模板、来源、共享、重置、锁定和准备时机，避免半成品策略进入运行链路。
 */
public interface ModuleDataStrategyService {

    /**
     * 创建数据准备策略。
     *
     * @param strategy 数据准备策略实体。
     * @return 已保存的策略实体。
     */
    ModuleDataStrategy createModuleDataStrategy(ModuleDataStrategy strategy);

    /**
     * 更新数据准备策略。
     *
     * @param strategy 数据准备策略实体。
     * @return 已更新的策略实体。
     */
    ModuleDataStrategy updateModuleDataStrategy(ModuleDataStrategy strategy);

    /**
     * 启用数据准备策略。
     *
     * @param id 策略 ID。
     * @return 已启用的策略实体。
     */
    ModuleDataStrategy enableModuleDataStrategy(String id);

    /**
     * 禁用数据准备策略。
     *
     * @param id 策略 ID。
     * @return 已禁用的策略实体。
     */
    ModuleDataStrategy disableModuleDataStrategy(String id);

    /**
     * 查询数据准备策略详情。
     *
     * @param id 策略 ID。
     * @return 未删除的策略实体。
     */
    ModuleDataStrategy getModuleDataStrategyById(String id);

    /**
     * 查询某业务模块下的全部未删除策略。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param businessModuleId 业务模块 ID。
     * @return 策略列表。
     */
    List<ModuleDataStrategy> listStrategiesByBusinessModule(String tenantId,
                                                            String connectorSystemId,
                                                            String businessModuleId);

    /**
     * 查询某业务模块下的启用策略。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param businessModuleId 业务模块 ID。
     * @return 启用策略列表。
     */
    List<ModuleDataStrategy> listActiveStrategiesByBusinessModule(String tenantId,
                                                                  String connectorSystemId,
                                                                  String businessModuleId);

    /**
     * 查询某模块编码和场景对应的启用策略。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param moduleCode 业务模块编码。
     * @param sceneType 教学场景。
     * @return 启用策略实体。
     */
    ModuleDataStrategy getActiveStrategyByModuleCodeAndScene(String tenantId,
                                                             String connectorSystemId,
                                                             String moduleCode,
                                                             String sceneType);
}
