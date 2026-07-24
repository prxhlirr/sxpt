package com.sxpt.module.capture.service;

import com.sxpt.module.capture.dto.ConfirmCaptureSegmentSwitchRequest;
import com.sxpt.module.capture.vo.CaptureSegmentSwitchVO;

/**
 * 备案片段切换服务。
 *
 * 业务功能：
 * 1. 支持教师完成当前备案片段后，确认进入下一角色片段。
 * 2. 复用原平台启动上下文能力，为下一片段生成 RECORD/CAPTURE launchToken。
 *
 * 关键流程：
 * 1. 接收教师确认后的下一片段单位、角色和目标地址。
 * 2. 创建下一片段启动上下文，明确 segmentNo、actorType 和原平台执行身份。
 */
public interface CaptureSegmentSwitchService {

    /**
     * 确认并创建下一备案片段的启动上下文。
     *
     * @param request 确认备案片段切换请求。
     * @return 下一片段切换结果。
     */
    CaptureSegmentSwitchVO confirmSwitch(ConfirmCaptureSegmentSwitchRequest request);
}
