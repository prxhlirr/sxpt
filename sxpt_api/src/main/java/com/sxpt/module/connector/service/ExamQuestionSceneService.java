package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.ExamQuestionScene;

import java.util.List;

/**
 * 考试题目业务场景维护服务。
 *
 * 业务功能：
 * 1. 在考试发布或组卷时，把题目与原平台业务场景编码固化下来。
 * 2. 为原平台 verify launchToken 后返回考试题目清单提供数据来源。
 *
 * 关键流程：
 * 1. 发布或组卷阶段传入 examAttemptId 和题目业务场景清单。
 * 2. 服务按考试尝试维度软删除旧清单。
 * 3. 服务插入新清单，学生考试造数时按 questionAttemptId 校验。
 */
public interface ExamQuestionSceneService {

    /**
     * 全量替换考试题目业务场景清单。
     *
     * @param tenantId 租户 ID。
     * @param taskId 考试任务 ID。
     * @param executionId 任务执行 ID。
     * @param examAttemptId 考试尝试 ID。
     * @param operatorId 操作人 ID。
     * @param scenes 本次考试题目业务场景清单。
     * @return 已保存的考试题目业务场景清单。
     */
    List<ExamQuestionScene> replaceExamQuestionScenes(String tenantId,
                                                      String taskId,
                                                      String executionId,
                                                      String examAttemptId,
                                                      String operatorId,
                                                      List<ExamQuestionScene> scenes);
}
