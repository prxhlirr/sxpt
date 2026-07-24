package com.sxpt.module.execution.service;

import com.sxpt.module.execution.entity.TaskExecution;

import java.util.List;

/**
 * 学生任务执行主聚合服务。
 *
 * 业务功能：
 * 1. 统一管理学生一次任务执行的创建、查询和提交状态流转。
 * 2. 为后续上下文快照、轨迹上报、自动评分和练习汇总提供唯一执行主入口。
 *
 * 关键流程：
 * 1. 开始执行时校验任务已发布，然后创建 RUNNING 主记录。
 * 2. 提交执行时校验当前状态，只允许 RUNNING 进入 SUBMITTED。
 */
public interface TaskExecutionService {

    /**
     * 开始一次学生任务执行。
     *
     * @param execution 待创建的学生任务执行记录。
     * @return 已保存的学生任务执行记录。
     */
    TaskExecution startExecution(TaskExecution execution);

    /**
     * 提交一次学生任务执行。
     *
     * @param tenantId 租户 ID。
     * @param executionId 执行记录 ID。
     * @param operatorId 操作人 ID。
     * @return 已提交的学生任务执行记录。
     */
    TaskExecution submitExecution(String tenantId, String executionId, String operatorId);

    /**
     * 查询单次学生任务执行。
     *
     * @param tenantId 租户 ID。
     * @param executionId 执行记录 ID。
     * @return 学生任务执行记录。
     */
    TaskExecution getExecution(String tenantId, String executionId);

    /**
     * 按学生和任务查询执行历史。
     *
     * @param tenantId 租户 ID。
     * @param studentId 学生 ID。
     * @param taskId 任务 ID。
     * @return 学生任务执行记录列表。
     */
    List<TaskExecution> listByStudentAndTask(String tenantId, String studentId, String taskId);
}
