package com.sxpt.module.teachingdata.service;

import com.sxpt.module.teachingdata.entity.TeachingDataPool;

import java.util.List;

/**
 * 教学数据池服务。
 *
 * 业务功能：
 * 1. 创建教学数据池，记录某个任务、场景、题目或模板下可领取的数据库存。
 * 2. 查询数据池状态，为发布预生成、学生练习重置和考试进入前的容量判断提供入口。
 *
 * 关键流程：
 * 1. 数据需求批次生成后创建 TeachingDataPool，并绑定 requirementId 和 idempotencyKey。
 * 2. 数据准备任务回写 ready、allocated、failed 等统计信息。
 * 3. 后续数据分配服务基于 READY 数据池领取具体数据实例。
 */
public interface TeachingDataPoolService {

    /**
     * 创建教学数据池。
     *
     * @param pool 教学数据池实体。
     * @return 已保存的教学数据池。
     */
    TeachingDataPool createTeachingDataPool(TeachingDataPool pool);

    /**
     * 查询指定任务和场景下的数据池。
     *
     * @param tenantId 租户 ID。
     * @param taskId 教学任务 ID。
     * @param sceneType 教学场景。
     * @return 教学数据池列表。
     */
    List<TeachingDataPool> listByTaskAndScene(String tenantId, String taskId, String sceneType);

    /**
     * 查询指定数据需求批次下的数据池，支撑后台从批次视角验证入池结果。
     *
     * @param tenantId 租户 ID。
     * @param requirementId 数据需求批次 ID。
     * @return 数据池列表。
     */
    List<TeachingDataPool> listByRequirement(String tenantId, String requirementId);

    /**
     * 查询指定任务、场景和题目下已就绪的数据池。
     *
     * @param tenantId 租户 ID。
     * @param taskId 教学任务 ID。
     * @param sceneType 教学场景。
     * @param questionId 题目 ID。
     * @return 已就绪教学数据池列表。
     */
    List<TeachingDataPool> listReadyPools(String tenantId, String taskId, String sceneType, String questionId);
}
