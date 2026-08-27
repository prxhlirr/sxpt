package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.CreateOriginRuntimeLaunchRequest;
import com.sxpt.module.connector.service.OriginRuntimeLaunchService;
import com.sxpt.module.connector.vo.OriginRuntimeLaunchVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 原平台运行时启动接口。
 *
 * 业务功能：
 * 1. 提供 v2 方案下老师/学生进入原平台首页的产品级接口。
 * 2. 不接收原平台模块路径，避免教学平台继续维护原平台页面结构。
 *
 * 关键流程：
 * 1. 接收老师备案、学生练习或考试的启动请求。
 * 2. 调用运行时启动服务创建平台首页 launchToken。
 * 3. 返回可直接跳转的原平台 `/teaching/launch` 地址。
 */
@RestController
@RequestMapping("/api/v1/origin-runtime/launches")
@ConditionalOnProperty(name = "sxpt.connector.origin-runtime-launch-controller.enabled", havingValue = "true", matchIfMissing = true)
public class OriginRuntimeLaunchController {

    private final OriginRuntimeLaunchService originRuntimeLaunchService;

    public OriginRuntimeLaunchController(OriginRuntimeLaunchService originRuntimeLaunchService) {
        this.originRuntimeLaunchService = originRuntimeLaunchService;
    }

    /**
     * 创建进入原平台首页的启动地址。
     *
     * @param request 创建运行时启动请求。
     * @return 原平台运行时启动返回对象。
     */
    @PostMapping("/create")
    public ApiResult<OriginRuntimeLaunchVO> create(@Valid @RequestBody CreateOriginRuntimeLaunchRequest request) {
        return ApiResult.success(originRuntimeLaunchService.createLaunch(request));
    }
}
