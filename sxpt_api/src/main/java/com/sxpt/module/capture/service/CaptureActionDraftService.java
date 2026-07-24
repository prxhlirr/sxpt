package com.sxpt.module.capture.service;

import com.sxpt.module.capture.entity.CaptureActionDraft;

import java.util.List;

/**
 * 页面动作草稿服务。
 *
 * 业务功能：
 * 1. 根据采集事件事实生成教师可确认的页面动作草稿。
 * 2. 支持按采集会话查询草稿，为后续教师确认动作草稿页面提供数据。
 *
 * 关键流程：
 * 1. 创建草稿时校验租户、采集会话、动作名称、动作类型和顺序号。
 * 2. 默认生成 PENDING 状态，等待教师确认或丢弃。
 */
public interface CaptureActionDraftService {

    /**
     * 创建单条页面动作草稿。
     *
     * @param actionDraft 页面动作草稿。
     * @return 已保存的页面动作草稿。
     */
    CaptureActionDraft createActionDraft(CaptureActionDraft actionDraft);

    /**
     * 查询指定采集会话下的页面动作草稿。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @return 按顺序号排列的页面动作草稿列表。
     */
    List<CaptureActionDraft> listBySession(String tenantId, String captureSessionId);

    /**
     * 教师确认页面动作草稿。
     *
     * @param id 页面动作草稿 ID。
     * @param confirmedOperationName 教师确认业务操作名称。
     * @param confirmedStepName 教师确认教学步骤名称。
     * @param guideContent 学习模式提示内容。
     * @param practiceHint 练习模式提示内容。
     * @param updateBy 更新人 ID。
     * @return 已确认的页面动作草稿。
     */
    CaptureActionDraft confirmActionDraft(String id,
                                          String confirmedOperationName,
                                          String confirmedStepName,
                                          String guideContent,
                                          String practiceHint,
                                          String updateBy);

    /**
     * 教师确认页面动作草稿并绑定正式资源。
     *
     * @param id 页面动作草稿 ID。
     * @param confirmedOperationName 教师确认业务操作名称。
     * @param confirmedStepName 教师确认教学步骤名称。
     * @param guideContent 学习模式提示内容。
     * @param practiceHint 练习模式提示内容。
     * @param connectorResourceId 正式资源 ID，允许为空表示该动作暂不绑定资源。
     * @param updateBy 更新人 ID。
     * @return 已确认的页面动作草稿。
     */
    CaptureActionDraft confirmActionDraft(String id,
                                          String confirmedOperationName,
                                          String confirmedStepName,
                                          String guideContent,
                                          String practiceHint,
                                          String connectorResourceId,
                                          String updateBy);

    /**
     * 教师丢弃页面动作草稿。
     *
     * @param id 页面动作草稿 ID。
     * @param updateBy 更新人 ID。
     * @return 已丢弃的页面动作草稿。
     */
    CaptureActionDraft discardActionDraft(String id, String updateBy);
}
