package com.sxpt.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.idempotent.Idempotent;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 安全能力探测接口。
 *
 * 业务功能：
 * 1. 为测试和开发环境验证 JWT 与幂等能力。
 * 2. 避免在真实业务模块未建立前，为测试公共组件而污染业务接口。
 *
 * 关键流程：
 * 1. 受 JWT 拦截器保护。
 * 2. 写接口通过 @Idempotent 启用幂等控制。
 */
@Profile({"dev", "test"})
@RestController
@RequestMapping("/api/v1/security-probe")
public class SecurityProbeController {

    /**
     * 查询受保护接口状态。
     *
     * @return 认证通过后的状态信息。
     */
    @PostMapping("/protected")
    public ApiResult<Map<String, Object>> protectedApi() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("authenticated", true);
        return ApiResult.success(result);
    }

    /**
     * 验证幂等写接口。
     *
     * @return 写操作结果。
     */
    @Idempotent(expireSeconds = 60L)
    @PostMapping("/idempotent")
    public ApiResult<Map<String, Object>> idempotentApi() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("accepted", true);
        return ApiResult.success(result);
    }
}
