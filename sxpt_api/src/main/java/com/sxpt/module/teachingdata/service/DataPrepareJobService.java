package com.sxpt.module.teachingdata.service;

import com.sxpt.module.teachingdata.entity.DataPrepareJob;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据准备任务服务。
 *
 * 业务功能：
 * 1. 创建数据准备任务，记录一次调用原平台准备、校验或重试数据的执行边界。
 * 2. 查询任务状态和待重试任务，为后台管理、失败补偿和过程审计提供入口。
 *
 * 关键流程：
 * 1. 数据需求明细生成后创建 DataPrepareJob，并写入幂等键和请求批次号。
 * 2. 后续编排服务按任务调用原平台 Adapter，并回写外部请求 ID、成功数、失败数和结果。
 * 3. 失败任务通过 retryCount 和 nextRetryTime 控制重试节奏。
 */
public interface DataPrepareJobService {

    /**
     * 创建数据准备任务。
     *
     * @param job 数据准备任务实体。
     * @return 已保存的数据准备任务。
     */
    DataPrepareJob createDataPrepareJob(DataPrepareJob job);

    /**
     * 按幂等键查询数据准备任务。
     *
     * @param tenantId 租户 ID。
     * @param idempotencyKey 幂等键。
     * @return 命中的数据准备任务；不存在时返回 null。
     */
    DataPrepareJob getByIdempotencyKey(String tenantId, String idempotencyKey);

    /**
     * 查询指定任务和场景下的数据准备任务。
     *
     * @param tenantId 租户 ID。
     * @param taskId 教学任务 ID。
     * @param sceneType 教学场景。
     * @return 数据准备任务列表。
     */
    List<DataPrepareJob> listByTaskAndScene(String tenantId, String taskId, String sceneType);

    /**
     * 查询到达重试时间的数据准备任务。
     *
     * @param tenantId 租户 ID。
     * @param now 当前时间。
     * @return 待重试数据准备任务列表。
     */
    List<DataPrepareJob> listRetryableJobs(String tenantId, LocalDateTime now);
}
