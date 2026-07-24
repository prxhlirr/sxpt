package com.sxpt.module.practice.service;

import com.sxpt.module.practice.entity.PracticeAttempt;

import java.util.List;

/**
 * 练习次数服务。
 *
 * 业务功能：
 * 1. 创建学生每一次练习记录，并由服务端计算 attemptNo。
 * 2. 完成练习时回写状态、耗时、得分和过程统计。
 *
 * 关键流程：
 * 1. 开始练习时按租户、学生、任务查询历史最大 attemptNo。
 * 2. 完成练习时校验当前记录仍处于 RUNNING，避免重复结束或覆盖历史结果。
 */
public interface PracticeAttemptService {

    /**
     * 开始一次练习。
     *
     * @param attempt 待创建的练习次数记录。
     * @return 已创建的练习次数记录。
     */
    PracticeAttempt startAttempt(PracticeAttempt attempt);

    /**
     * 完成一次练习。
     *
     * @param attempt 需要回写的练习结果。
     * @return 已完成的练习次数记录。
     */
    PracticeAttempt finishAttempt(PracticeAttempt attempt);

    /**
     * 按学生和任务查询练习次数。
     *
     * @param tenantId 租户 ID。
     * @param studentId 学生 ID。
     * @param taskId 任务 ID。
     * @return 练习次数列表。
     */
    List<PracticeAttempt> listByStudentAndTask(String tenantId, String studentId, String taskId);
}
