package com.sxpt.module.capture.service;

import com.sxpt.module.connector.entity.ConnectorResource;

/**
 * 采集资源快照晋升正式资源服务。
 *
 * 业务功能：
 * 1. 将老师备案采集过程中沉淀的资源快照晋升为可被教学步骤、引导和评分引用的正式资源。
 * 2. 保持 capture 模块负责“从采集证据发起晋升”，connector 模块继续负责正式资源自身的创建规则。
 *
 * 关键流程：
 * 1. 按租户和快照 ID 定位采集资源快照，确保晋升动作有明确数据来源。
 * 2. 根据快照所属采集会话补齐原平台 ID，避免 SDK 上报快照时重复携带平台上下文。
 * 3. 构造正式资源并回写快照晋升结果，保证老师备案链路可追溯。
 */
public interface CaptureResourcePromoteService {

    /**
     * 将单个资源快照晋升为正式资源。
     *
     * @param tenantId 租户 ID。
     * @param snapshotId 采集资源快照 ID。
     * @param operatorId 触发晋升的老师或系统操作人 ID。
     * @return 已创建或已存在的正式资源。
     */
    ConnectorResource promoteSnapshot(String tenantId, String snapshotId, String operatorId);
}
