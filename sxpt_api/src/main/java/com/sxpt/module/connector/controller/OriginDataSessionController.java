package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.RegisterOriginDataSessionRequest;
import com.sxpt.module.connector.service.OriginDataSessionService;
import com.sxpt.module.connector.vo.OriginDataSessionVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 原平台数据会话接口。
 *
 * 业务功能：
 * 1. 提供原平台自造数或复制数据后的 DataSession 注册入口。
 * 2. 返回遮罩层采集所需的 dataSessionId 和 dataInstanceId。
 *
 * 关键流程：
 * 1. 原平台在页面命中业务场景后准备业务数据。
 * 2. 原平台调用注册接口提交业务场景和业务数据引用。
 * 3. 教学平台幂等创建教学数据实例和 DataSession，并返回绑定上下文。
 */
@RestController
@RequestMapping("/api/v1/origin-runtime/data-sessions")
@ConditionalOnProperty(name = "sxpt.connector.origin-data-session-controller.enabled", havingValue = "true", matchIfMissing = true)
public class OriginDataSessionController {

    private final OriginDataSessionService originDataSessionService;

    public OriginDataSessionController(OriginDataSessionService originDataSessionService) {
        this.originDataSessionService = originDataSessionService;
    }

    /**
     * 注册原平台数据会话。
     *
     * @param request 原平台数据会话注册请求。
     * @return 原平台数据会话返回对象。
     */
    @PostMapping("/register")
    public ApiResult<OriginDataSessionVO> register(@Valid @RequestBody RegisterOriginDataSessionRequest request) {
        return ApiResult.success(originDataSessionService.register(request));
    }
}
