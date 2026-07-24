package com.sxpt.module.capture.service;

import com.sxpt.module.capture.entity.CaptureEvent;

import java.util.List;

/**
 * SDK 采集事件服务。
 *
 * 业务功能：
 * 1. 接收 SDK 上报的页面操作事件，并按 clientEventId 做幂等去重。
 * 2. 支持按采集会话查询事件，为后续动作草稿生成提供事实来源。
 *
 * 关键流程：
 * 1. 上报时校验租户、采集会话、事件类型、事件时间和顺序号。
 * 2. clientEventId 已存在时返回既有事件，避免 SDK 重试造成重复入库。
 */
public interface CaptureEventService {

    /**
     * 上报单条 SDK 采集事件。
     *
     * @param captureEvent SDK 采集事件。
     * @return 已保存或幂等命中的 SDK 采集事件。
     */
    CaptureEvent reportCaptureEvent(CaptureEvent captureEvent);

    /**
     * 查询指定采集会话下的 SDK 事件。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @return 按事件顺序排序的 SDK 事件列表。
     */
    List<CaptureEvent> listBySession(String tenantId, String captureSessionId);
}
