package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.PlatformCapability;

import java.util.List;

/**
 * 原平台能力维护服务。
 *
 * 业务功能：
 * 1. 维护某个原平台是否支持数据创建、查询、锁定、结果校验和归档等数据准备能力。
 * 2. 为模块策略启用前的能力校验提供可审计、可维护的事实来源。
 *
 * 关键流程：
 * 1. Service 层校验能力声明的唯一性、JSON 配置和生命周期字段。
 * 2. Mapper 层只负责 platform_capability 表读写，不承载业务判断。
 */
public interface PlatformCapabilityService {

    /**
     * 创建原平台能力声明。
     *
     * @param capability 原平台能力实体。
     * @return 已保存的原平台能力实体。
     */
    PlatformCapability createPlatformCapability(PlatformCapability capability);

    /**
     * 更新原平台能力声明。
     *
     * @param capability 原平台能力实体。
     * @return 已更新的原平台能力实体。
     */
    PlatformCapability updatePlatformCapability(PlatformCapability capability);

    /**
     * 启用原平台能力声明。
     *
     * @param id 能力 ID。
     * @return 已启用的原平台能力实体。
     */
    PlatformCapability enablePlatformCapability(String id);

    /**
     * 停用原平台能力声明。
     *
     * @param id 能力 ID。
     * @return 已停用的原平台能力实体。
     */
    PlatformCapability disablePlatformCapability(String id);

    /**
     * 查询能力详情。
     *
     * @param id 能力 ID。
     * @return 未删除的原平台能力实体。
     */
    PlatformCapability getPlatformCapabilityById(String id);

    /**
     * 查询某个原平台下的全部能力声明。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @return 能力声明列表。
     */
    List<PlatformCapability> listPlatformCapabilities(String tenantId, String connectorSystemId);
}
