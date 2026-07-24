package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.TeachingDataInstance;

import java.util.List;

/**
 * 教学业务数据实例服务。
 *
 * 业务功能：
 * 1. 维护备案、学习、练习、考试实际使用的原平台业务数据引用。
 * 2. 为 launchToken、任务执行和后续数据锁定提供可追踪的教学业务数据上下文。
 *
 * 关键流程：
 * 1. 创建实例时校验租户、模板、原平台、场景和原平台业务数据 ID。
 * 2. 查询实例时始终按租户和软删除边界过滤，避免跨租户读取历史业务数据。
 */
public interface TeachingDataInstanceService {

    /**
     * 创建教学业务数据实例。
     *
     * @param instance 教学业务数据实例实体。
     * @return 已保存的教学业务数据实例。
     */
    TeachingDataInstance createTeachingDataInstance(TeachingDataInstance instance);

    /**
     * 按原平台业务数据 ID 查询教学业务数据实例。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param externalBusinessId 原平台业务数据 ID。
     * @return 教学业务数据实例；不存在时返回 null。
     */
    TeachingDataInstance getByExternalBusiness(String tenantId, String connectorSystemId, String externalBusinessId);

    /**
     * 查询指定使用人和场景下的教学业务数据实例。
     *
     * @param tenantId 租户 ID。
     * @param ownerUserId 使用人 ID。
     * @param sceneType 场景类型。
     * @return 教学业务数据实例列表。
     */
    List<TeachingDataInstance> listByOwnerAndScene(String tenantId, String ownerUserId, String sceneType);

    /**
     * 查询指定任务和场景下的教学业务数据实例。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param sceneType 场景类型。
     * @return 教学业务数据实例列表。
     */
    List<TeachingDataInstance> listByTaskAndScene(String tenantId, String taskId, String sceneType);

    /**
     * 锁定教学业务数据实例。
     *
     * @param id 教学业务数据实例 ID。
     * @return 已锁定的教学业务数据实例。
     */
    TeachingDataInstance lockTeachingDataInstance(String id);

    /**
     * 废弃教学业务数据实例。
     *
     * @param id 教学业务数据实例 ID。
     * @return 已废弃的教学业务数据实例。
     */
    TeachingDataInstance discardTeachingDataInstance(String id);

    /**
     * 重置教学业务数据实例。
     *
     * @param id 教学业务数据实例 ID。
     * @param externalBusinessId 新的原平台业务数据 ID。
     * @param externalBusinessNo 新的原平台业务单据号。
     * @param externalStatus 新的原平台业务状态。
     * @param metadataJson 新的原平台业务数据摘要。
     * @return 已重置的教学业务数据实例。
     */
    TeachingDataInstance resetTeachingDataInstance(String id,
                                                   String externalBusinessId,
                                                   String externalBusinessNo,
                                                   String externalStatus,
                                                   String metadataJson);
}
