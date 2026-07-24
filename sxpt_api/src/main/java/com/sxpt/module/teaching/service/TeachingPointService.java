package com.sxpt.module.teaching.service;

import com.sxpt.module.teaching.entity.TeachingPoint;

import java.util.List;

/**
 * 教学点服务。
 *
 * 业务功能：
 * 1. 发布教师确认后的教学点，使其可被任务、步骤和评分规则引用。
 * 2. 支持按租户和原平台查询已发布教学点，供后续任务编排选择。
 *
 * 关键流程：
 * 1. 创建教学点时校验租户、原平台、编码、名称、类型和执行策略。
 * 2. 新教学点默认发布为 PUBLISHED，并补齐版本号和生命周期字段。
 */
public interface TeachingPointService {

    /**
     * 发布教学点。
     *
     * @param teachingPoint 教学点实体。
     * @return 已发布的教学点。
     */
    TeachingPoint createTeachingPoint(TeachingPoint teachingPoint);

    /**
     * 按原平台查询教学点。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @return 教学点列表。
     */
    List<TeachingPoint> listByConnector(String tenantId, String connectorSystemId);
}
