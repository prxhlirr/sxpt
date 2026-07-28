package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.TeachingDataTemplate;

import java.util.List;

/**
 * 教学业务数据模板服务。
 *
 * 业务功能：
 * 1. 维护原平台在不同教学场景下可用的数据模板。
 * 2. 为后续教学业务数据实例创建提供模板选择能力。
 *
 * 关键流程：
 * 1. 创建模板时校验租户、原平台、模板编码、模板名称和场景类型。
 * 2. 查询模板时始终按租户和软删除标记隔离数据。
 */
public interface TeachingDataTemplateService {

    /**
     * 创建教学业务数据模板。
     *
     * @param template 教学业务数据模板实体。
     * @return 已保存的教学业务数据模板。
     */
    TeachingDataTemplate createTeachingDataTemplate(TeachingDataTemplate template);

    /**
     * 更新教学业务数据模板。
     *
     * @param template 教学业务数据模板实体，必须包含 ID 和可编辑模板字段。
     * @return 已更新的教学业务数据模板。
     */
    TeachingDataTemplate updateTeachingDataTemplate(TeachingDataTemplate template);

    /**
     * 启用教学业务数据模板。
     *
     * @param id 模板 ID。
     * @return 已启用的教学业务数据模板。
     */
    TeachingDataTemplate enableTeachingDataTemplate(String id);

    /**
     * 停用教学业务数据模板。
     *
     * @param id 模板 ID。
     * @return 已停用的教学业务数据模板。
     */
    TeachingDataTemplate disableTeachingDataTemplate(String id);

    /**
     * 查询教学业务数据模板详情。
     *
     * @param id 模板 ID。
     * @return 未删除的教学业务数据模板。
     */
    TeachingDataTemplate getTeachingDataTemplateById(String id);

    /**
     * 查询指定租户和原平台下的教学业务数据模板。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @return 教学业务数据模板列表。
     */
    List<TeachingDataTemplate> listTemplatesByConnector(String tenantId, String connectorSystemId);

    /**
     * 查询指定平台、模块和场景下的模板，避免跨业务模块误选模板。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param moduleCode 业务模块编码。
     * @param sceneType 教学场景。
     * @return 模板列表。
     */
    List<TeachingDataTemplate> listTemplatesByModuleAndScene(String tenantId,
                                                             String connectorSystemId,
                                                             String moduleCode,
                                                             String sceneType);

    /**
     * 查询指定教学点和场景下的可用教学业务数据模板。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param teachingPointId 教学点 ID。
     * @param sceneType 场景类型。
     * @return 可用教学业务数据模板列表。
     */
    List<TeachingDataTemplate> listActiveTemplatesByTeachingPointAndScene(String tenantId,
                                                                           String connectorSystemId,
                                                                           String teachingPointId,
                                                                           String sceneType);
}
