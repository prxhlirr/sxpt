package com.sxpt.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.security.CurrentUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 当前登录人接口。
 *
 * 业务功能：
 * 1. 向前端返回服务端认证链路确认过的当前用户信息。
 * 2. 为老师工作台、学生任务中心、审计展示提供统一身份上下文入口。
 *
 * 关键流程：
 * 1. 请求先经过 JwtAuthInterceptor 校验 JWT。
 * 2. Controller 从 CurrentUserContext 读取当前用户，不接收前端传入的用户身份。
 * 3. 当前阶段返回 userId、username 和空的租户、角色、组织、身份绑定占位，后续接入用户模块后补齐。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthMeController {

    /**
     * 查询当前登录用户。
     *
     * @return 当前认证用户上下文。
     */
    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("userId", currentUser.getUserId());
        result.put("username", currentUser.getUsername());
        result.put("tenantId", null);
        result.put("roles", Collections.emptyList());
        result.put("orgIds", Collections.emptyList());
        result.put("identityBindings", Collections.emptyList());
        return ApiResult.success(result);
    }
}
