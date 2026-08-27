package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.PracticeTaskScene;

import java.util.List;

/**
 * 练习任务业务场景维护服务。
 *
 * 业务功能：
 * 1. 在老师发布练习任务时，把老师备案产物固化为学生练习任务清单。
 * 2. 为原平台 verify launchToken 后返回 businessSceneCode 和来源样本提供数据来源。
 *
 * 关键流程：
 * 1. 发布任务时传入任务 ID、操作人和本次选中的备案产物快照。
 * 2. 服务按任务维度软删除旧清单，避免历史选择继续影响学生练习。
 * 3. 服务插入新的业务场景清单，后续学生练习注册 DataSession 时按该清单校验。
 */
public interface PracticeTaskSceneService {

    /**
     * 全量替换练习任务业务场景清单。
     *
     * @param tenantId 租户 ID。
     * @param taskId 练习任务 ID。
     * @param operatorId 操作人 ID。
     * @param scenes 本次发布后的业务场景清单。
     * @return 已保存的业务场景清单。
     */
    List<PracticeTaskScene> replacePracticeTaskScenes(String tenantId,
                                                      String taskId,
                                                      String operatorId,
                                                      List<PracticeTaskScene> scenes);

    /**
     * 根据老师备案产物 ID 全量替换练习任务业务场景清单。
     *
     * @param tenantId 租户 ID。
     * @param taskId 练习任务 ID。
     * @param operatorId 操作人 ID。
     * @param recordArtifactIds 老师备案产物 ID 列表。
     * @return 已保存的业务场景清单。
     */
    List<PracticeTaskScene> replacePracticeTaskScenesByArtifacts(String tenantId,
                                                                 String taskId,
                                                                 String operatorId,
                                                                 List<String> recordArtifactIds);
}
