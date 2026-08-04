package com.sxpt.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.security.CurrentUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        result.put("tenantId", currentUser.getTenantId());
        result.put("username", currentUser.getUsername());
        result.put("displayName", currentUser.getDisplayName());
        result.put("userType", currentUser.getUserType());
        result.put("studentNo", currentUser.getStudentNo());
        result.put("employeeNo", currentUser.getEmployeeNo());
        result.put("roles", currentUser.getRoleCodes());
        result.put("orgIds", currentUser.getOrgIds());
        result.put("identityBindings", toIdentityBindingResults(currentUser.getIdentityBindings()));
        return ApiResult.success(result);
    }

    private List<Map<String, Object>> toIdentityBindingResults(
            List<CurrentUserContext.IdentityBindingSummary> identityBindings) {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        for (CurrentUserContext.IdentityBindingSummary identityBinding : identityBindings) {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("connectorSystemId", identityBinding.getConnectorSystemId());
            result.put("externalUserId", identityBinding.getExternalUserId());
            result.put("externalUsername", identityBinding.getExternalUsername());
            results.add(result);
        }
        return results;
    }
}
