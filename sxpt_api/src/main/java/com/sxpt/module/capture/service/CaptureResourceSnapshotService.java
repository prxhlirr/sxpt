package com.sxpt.module.capture.service;

import com.sxpt.module.capture.entity.CaptureResourceSnapshot;

import java.util.List;

/**
 * 采集资源快照服务。
 *
 * 业务功能：
 * 1. 接收 SDK 上报的目标元素摘要和页面摘要。
 * 2. 支持按采集会话查询资源快照，为后续动作草稿和正式资源沉淀提供证据。
 *
 * 关键流程：
 * 1. 上报时校验页面地址、资源类型、快照范围和元素摘要 JSON。
 * 2. DEBUG_FULL_DOM 必须显式携带短期过期时间，避免全量 DOM 长期沉积。
 */
public interface CaptureResourceSnapshotService {

    /**
     * 上报单条采集资源快照。
     *
     * @param snapshot 采集资源快照。
     * @return 已保存的采集资源快照。
     */
    CaptureResourceSnapshot reportSnapshot(CaptureResourceSnapshot snapshot);

    /**
     * 查询指定采集会话下的资源快照。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @return 按创建时间倒序排列的资源快照列表。
     */
    List<CaptureResourceSnapshot> listBySession(String tenantId, String captureSessionId);
}
