package com.sxpt.module.practice.service;

import com.sxpt.module.practice.entity.PracticeScoreSummary;

/**
 * 练习过程分汇总服务。
 *
 * 业务功能：
 * 1. 根据练习次数和步骤结果生成学生维度的练习过程分。
 * 2. 支持教师端查询学生在指定任务教学点下的最新汇总。
 *
 * 关键流程：
 * 1. 生成时读取 practice_attempt 作为分数和完成率来源。
 * 2. 再读取 practice_step_result 生成薄弱步骤摘要，最后幂等写入 practice_score_summary。
 */
public interface PracticeScoreSummaryService {

    /**
     * 生成或重算练习过程分汇总。
     *
     * @param scope 汇总范围。
     * @return 已保存的练习过程分汇总。
     */
    PracticeScoreSummary generateSummary(PracticeScoreSummary scope);

    /**
     * 查询练习过程分汇总。
     *
     * @param tenantId 租户 ID。
     * @param studentId 学生 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @return 练习过程分汇总。
     */
    PracticeScoreSummary getSummary(String tenantId, String studentId, String taskId, String teachingPointId);
}
