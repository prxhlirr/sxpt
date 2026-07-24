package com.sxpt.module.capture.service;

import com.sxpt.module.capture.entity.CaptureActionDraft;

import java.util.List;

/**
 * 采集事件自动生成动作草稿服务。
 *
 * 业务功能：
 * 1. 将 SDK 上报的低层采集事件整理为老师可确认的动作草稿。
 * 2. 作为老师备案闭环的第一步，只生成 PENDING 草稿，不直接发布教学步骤。
 *
 * 关键流程：
 * 1. 按采集会话读取事件，过滤无效或暂不支持自动生成的事件。
 * 2. 按事件 ID 做幂等校验，避免重复点击生成按钮时产生重复草稿。
 * 3. 使用统一动作类型规则生成动作名称，并委托草稿服务完成默认状态填充和写入。
 */
public interface CaptureActionDraftGenerateService {

    /**
     * 按采集会话自动生成动作草稿。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @param operatorId 触发生成的老师或系统操作人 ID。
     * @return 本次新生成的动作草稿列表；已存在草稿的事件不会重复返回。
     */
    List<CaptureActionDraft> generateDraftsFromSession(String tenantId, String captureSessionId, String operatorId);
}
