package com.sxpt.module.teaching.service;

import com.sxpt.module.teaching.entity.TaskStep;

import java.util.List;

/**
 * 教学任务步骤服务。
 *
 * 业务功能：
 * 1. 发布教学任务步骤，使学习和练习可以按顺序加载步骤与提示。
 * 2. 支持按任务和教学点查询步骤，供 SDK runtime 和步骤配置页面使用。
 *
 * 关键流程：
 * 1. 创建步骤时校验任务、教学点、步骤编码、步骤名称和排序号。
 * 2. 新步骤默认必做、不可跳过，并补齐生命周期字段。
 */
public interface TaskStepService {

    /**
     * 创建教学任务步骤。
     *
     * @param taskStep 教学任务步骤实体。
     * @return 已保存的教学任务步骤。
     */
    TaskStep createTaskStep(TaskStep taskStep);

    /**
     * 按任务和教学点查询教学步骤。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @return 教学步骤列表。
     */
    List<TaskStep> listByTaskAndTeachingPoint(String tenantId, String taskId, String teachingPointId);
}
