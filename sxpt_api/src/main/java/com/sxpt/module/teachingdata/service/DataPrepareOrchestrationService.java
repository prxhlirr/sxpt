package com.sxpt.module.teachingdata.service;

import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;

/**
 * 数据准备编排服务。
 *
 * 业务功能：
 * 1. 串联数据准备任务、需求明细、原平台数据创建和教学数据实例落库。
 * 2. 确保原平台保存真实业务数据，教学平台只保存可追踪的数据引用和校验摘要。
 *
 * 关键流程：
 * 1. 根据 data_prepare_job 读取同一 requestBatchId 下的需求明细。
 * 2. 调用 OriginDataPrepareAdapter 批量创建原平台业务数据。
 * 3. 按 requestItemId 回填明细结果，并为成功结果创建 teaching_data_instance 引用。
 */
public interface DataPrepareOrchestrationService {

    /**
     * 执行批量创建型数据准备任务。
     *
     * @param jobId 数据准备任务 ID。
     * @return 已完成状态更新的数据准备任务。
     */
    DataPrepareJob executeCreateJob(String jobId);

    /**
     * 校验教学数据实例是否满足原平台业务约束。
     *
     * @param instanceId 教学数据实例 ID。
     * @return 已更新校验结果的教学数据实例。
     */
    TeachingDataInstance validatePreparedInstance(String instanceId);
}
