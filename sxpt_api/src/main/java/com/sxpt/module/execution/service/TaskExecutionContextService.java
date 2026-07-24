package com.sxpt.module.execution.service;

import com.sxpt.module.execution.entity.TaskExecutionContext;

import java.util.List;

/**
 * 任务执行上下文快照服务。
 *
 * 业务功能：
 * 1. 创建学生进入任务时的版本快照。
 * 2. 查询已创建的上下文快照，供 SDK 初始化或排查历史执行使用。
 *
 * 关键流程：
 * 1. 创建时校验最小必填字段，并补齐归档状态、通用状态和软删除字段。
 * 2. 查询时固定租户和软删除边界，可按执行记录、任务或学生进一步过滤。
 */
public interface TaskExecutionContextService {

    /**
     * 创建任务执行上下文快照。
     *
     * @param context 任务执行上下文快照实体。
     * @return 已保存的任务执行上下文快照。
     */
    TaskExecutionContext createContext(TaskExecutionContext context);

    /**
     * 查询任务执行上下文快照。
     *
     * @param tenantId 租户 ID。
     * @param executionId 任务执行 ID，可为空。
     * @param taskId 任务 ID，可为空。
     * @param studentId 学生 ID，可为空。
     * @return 任务执行上下文快照列表。
     */
    List<TaskExecutionContext> listContexts(String tenantId, String executionId, String taskId, String studentId);
}
