package com.sxpt.module.capture.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.capture.dto.ConfirmCaptureSegmentSwitchRequest;
import com.sxpt.module.capture.service.CaptureSegmentSwitchService;
import com.sxpt.module.capture.vo.CaptureSegmentSwitchVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 备案片段切换接口。
 *
 * 业务功能：
 * 1. 提供教师确认下一备案片段并生成下一段 launchToken 的入口。
 * 2. 明确片段切换来自教师确认，不由系统根据页面 DOM 或原平台状态自动强切。
 *
 * 关键流程：
 * 1. 接收教师确认的下一片段单位、角色和目标地址。
 * 2. 调用 Service 创建下一片段启动上下文。
 * 3. 返回明文 launchToken，前端据此跳转原平台进入下一角色片段。
 */
@RestController
@RequestMapping("/api/v1/capture/segment-switches")
@ConditionalOnProperty(name = "sxpt.capture.segment-switch-controller.enabled", havingValue = "true", matchIfMissing = true)
public class CaptureSegmentSwitchController {

    private final CaptureSegmentSwitchService captureSegmentSwitchService;

    public CaptureSegmentSwitchController(CaptureSegmentSwitchService captureSegmentSwitchService) {
        this.captureSegmentSwitchService = captureSegmentSwitchService;
    }

    /**
     * 确认并创建下一备案片段的启动上下文。
     *
     * @param request 确认备案片段切换请求。
     * @return 下一片段切换结果。
     */
    @PostMapping("/confirm")
    public ApiResult<CaptureSegmentSwitchVO> confirm(@Valid @RequestBody ConfirmCaptureSegmentSwitchRequest request) {
        return ApiResult.success(captureSegmentSwitchService.confirmSwitch(request));
    }
}
