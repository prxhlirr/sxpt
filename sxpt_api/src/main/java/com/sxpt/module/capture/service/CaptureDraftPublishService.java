package com.sxpt.module.capture.service;

import com.sxpt.module.teaching.entity.TaskStep;

/**
 * 动作草稿发布教学步骤服务。
 *
 * 业务功能：
 * 1. 将教师已确认的动作草稿发布为学生学习和练习可见的任务步骤。
 * 2. 保持草稿确认、资源晋升、教学步骤发布三段职责分离，避免确认动作时直接改写教学编排。
 *
 * 关键流程：
 * 1. 按租户和草稿 ID 校验草稿存在且已确认。
 * 2. 使用调用方传入的任务 ID 和教学点 ID 生成 task_step，并写入 sourceActionDraftId 形成追溯关系。
 * 3. 后续评分项生成由独立阶段接入，不在本接口中提前混入评分配置。
 */
public interface CaptureDraftPublishService {

    /**
     * 将单个已确认动作草稿发布为教学任务步骤。
     *
     * @param tenantId 租户 ID。
     * @param actionDraftId 动作草稿 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param operatorId 发布操作人 ID。
     * @return 已创建或已存在的任务步骤。
     */
    TaskStep publishDraftToTaskStep(String tenantId,
                                    String actionDraftId,
                                    String taskId,
                                    String teachingPointId,
                                    String operatorId);

    /**
     * 灏嗗崟涓凡纭鍔ㄤ綔鑽夌鍙戝竷涓烘暀瀛︿换鍔℃楠わ紝骞剁敓鎴?MVP 鍩虹璇勫垎椤广€?     *
     * @param tenantId 绉熸埛 ID銆?     * @param actionDraftId 鍔ㄤ綔鑽夌 ID銆?     * @param taskId 浠诲姟 ID銆?     * @param teachingPointId 鏁欏鐐?ID銆?     * @param evaluationRuleId 璇勪环瑙勫垯 ID銆?     * @param operatorId 鍙戝竷鎿嶄綔浜?ID銆?     * @return 宸插垱寤烘垨宸插瓨鍦ㄧ殑浠诲姟姝ラ銆?     */
    TaskStep publishDraftToTaskStep(String tenantId,
                                    String actionDraftId,
                                    String taskId,
                                    String teachingPointId,
                                    String evaluationRuleId,
                                    String operatorId);
}
