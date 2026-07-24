package com.sxpt.module.capture.service;

import com.sxpt.module.capture.vo.TeacherFilingOrchestrationResultVO;

/**
 * 老师备案编排服务。
 *
 * 业务功能：
 * 1. 以一次采集会话为入口，串联动作草稿生成、资源准备、教学步骤发布、评分项补齐和发布前校验。
 * 2. 将多个底层服务收敛为教师端可调用的一条备案准备链路，避免前端手工拼接内部调用顺序。
 *
 * 关键流程：
 * 1. 根据采集会话自动生成待确认动作草稿。
 * 2. 对已确认草稿执行教学步骤发布，并补齐基础评分项。
 * 3. 调用教学资产发布前完整性校验，返回是否已具备发布条件。
 */
public interface TeacherFilingOrchestrationService {

    /**
     * 准备指定采集会话对应的教学资产。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @param operatorId 操作人 ID。
     * @return 老师备案编排结果。
     */
    TeacherFilingOrchestrationResultVO prepareTeachingAssets(String tenantId,
                                                             String captureSessionId,
                                                             String taskId,
                                                             String teachingPointId,
                                                             String evaluationRuleId,
                                                             String operatorId);
}
