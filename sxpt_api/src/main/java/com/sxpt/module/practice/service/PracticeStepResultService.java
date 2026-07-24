package com.sxpt.module.practice.service;

import com.sxpt.module.practice.entity.PracticeStepResult;

import java.util.List;

/**
 * 练习步骤结果服务。
 *
 * 业务功能：
 * 1. 支撑 SDK 或评分流程上报某次练习中的关键步骤结果。
 * 2. 支撑前端按练习次数查询步骤结果，定位学生在哪一步失败、跳过或耗时过长。
 *
 * 关键流程：
 * 1. 上报时按 tenantId、attemptId 和 taskStepId 做幂等写入。
 * 2. 查询时按 sequenceNo 升序返回，保持和 task_step 编排顺序一致。
 */
public interface PracticeStepResultService {

    /**
     * 上报单个练习步骤结果。
     *
     * @param stepResult 练习步骤结果。
     * @return 已保存或已更新的练习步骤结果。
     */
    PracticeStepResult reportStepResult(PracticeStepResult stepResult);

    /**
     * 按练习次数查询步骤结果。
     *
     * @param tenantId 租户 ID。
     * @param attemptId 练习次数 ID。
     * @return 步骤结果列表。
     */
    List<PracticeStepResult> listByAttempt(String tenantId, String attemptId);
}
