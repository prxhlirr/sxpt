package com.sxpt.module.capture.service;

import com.sxpt.module.capture.entity.CaptureSession;

import java.util.List;

/**
 * 备案采集会话服务。
 *
 * 业务功能：
 * 1. 支持教师从教学平台发起一次备案采集会话。
 * 2. 支持教师查看自己的备案采集会话并结束当前采集。
 *
 * 关键流程：
 * 1. 创建会话时校验租户、原平台、教师、会话名称和入口地址。
 * 2. 会话创建后进入 RUNNING 状态，后续 SDK 事件和资源快照都关联该会话。
 */
public interface CaptureSessionService {

    /**
     * 创建备案采集会话。
     *
     * @param captureSession 备案采集会话实体。
     * @return 已保存的备案采集会话。
     */
    CaptureSession createCaptureSession(CaptureSession captureSession);

    /**
     * 查询教师的备案采集会话。
     *
     * @param tenantId 租户 ID。
     * @param teacherId 教师用户 ID。
     * @return 备案采集会话列表。
     */
    List<CaptureSession> listByTeacher(String tenantId, String teacherId);

    /**
     * 结束备案采集会话。
     *
     * @param id 备案采集会话 ID。
     * @return 已结束的备案采集会话。
     */
    CaptureSession finishCaptureSession(String id);
}
