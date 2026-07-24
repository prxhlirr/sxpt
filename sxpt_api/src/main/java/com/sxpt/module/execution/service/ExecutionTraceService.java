package com.sxpt.module.execution.service;

import com.sxpt.module.execution.entity.ExecutionTrace;

import java.util.List;

/**
 * 学生执行轨迹服务。
 *
 * 业务功能：
 * 1. 接收 SDK 上报的学生执行轨迹，并按 clientTraceId 做幂等去重。
 * 2. 支持按任务执行 ID 查询轨迹，为练习过程分和自动评分提供事实来源。
 *
 * 关键流程：
 * 1. 上报时校验租户、任务执行、轨迹类型、轨迹时间和顺序号。
 * 2. clientTraceId 已存在时返回既有轨迹，避免 SDK 重试造成重复入库。
 */
public interface ExecutionTraceService {

    /**
     * 上报单条学生执行轨迹。
     *
     * @param executionTrace 学生执行轨迹。
     * @return 已保存或幂等命中的学生执行轨迹。
     */
    ExecutionTrace reportExecutionTrace(ExecutionTrace executionTrace);

    /**
     * 查询指定任务执行下的学生执行轨迹。
     *
     * @param tenantId 租户 ID。
     * @param executionId 任务执行 ID。
     * @return 按轨迹顺序排序的学生执行轨迹列表。
     */
    List<ExecutionTrace> listByExecution(String tenantId, String executionId);
}
