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
     * 查询指定租户和原平台下的教学业务数据模板。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @return 教学业务数据模板列表。
     */
    List<TeachingDataTemplate> listTemplatesByConnector(String tenantId, String connectorSystemId);

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
