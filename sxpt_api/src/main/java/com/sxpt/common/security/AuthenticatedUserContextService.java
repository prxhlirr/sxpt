package com.sxpt.common.security;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.user.service.SystemConfigService;
import com.sxpt.module.user.vo.RuntimeUserContextVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 当前认证用户上下文加载服务。
 *
 * 业务功能：
 * 1. 以 JWT 中的最小用户标识为入口，反查服务端可信的用户、租户、角色和组织上下文。
 * 2. 将运行时上下文转换为 CurrentUserContext 使用的不可变请求身份对象。
 *
 * 关键流程：
 * 1. JwtAuthInterceptor 完成 Token 认证后调用本服务。
 * 2. 本服务通过用户模块反查真实用户上下文，避免业务接口继续信任前端传入的租户和操作者。
 * 3. 缺少租户或用户摘要时直接拒绝请求，因为生产链路必须有明确的数据边界。
 */
@Service
public class AuthenticatedUserContextService {

    private final SystemConfigService systemConfigService;

    @Autowired
    public AuthenticatedUserContextService(ObjectProvider<SystemConfigService> systemConfigServiceProvider) {
        this.systemConfigService = systemConfigServiceProvider.getIfAvailable();
    }

    AuthenticatedUserContextService(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * 根据 JWT 主体加载完整当前用户。
     *
     * @param principal JWT 解析得到的最小认证主体。
     * @return 可写入 CurrentUserContext 的完整当前用户。
     */
    public CurrentUserContext.CurrentUser loadCurrentUser(JwtPrincipal principal) {
        if (principal == null || !StringUtils.hasText(principal.getUserId())) {
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
        if (systemConfigService == null) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
        RuntimeUserContextVO context = systemConfigService.getRuntimeContextForUser(principal.getUserId());
        RuntimeUserContextVO.UserSummary user = context == null ? null : context.getUser();
        if (user == null || !StringUtils.hasText(user.getUserId())) {
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
        if (!StringUtils.hasText(user.getTenantId())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        return new CurrentUserContext.CurrentUser(
                user.getUserId(),
                user.getTenantId(),
                firstText(user.getUsername(), principal.getUsername()),
                user.getDisplayName(),
                user.getUserType(),
                user.getStudentNo(),
                user.getEmployeeNo(),
                toRoleCodes(context.getRoles()),
                toOrgIds(context.getOrgs()),
                Collections.emptyList());
    }

    private List<String> toRoleCodes(List<RuntimeUserContextVO.RoleSummary> roles) {
        List<String> roleCodes = new ArrayList<String>();
        if (roles == null) {
            return roleCodes;
        }
        for (RuntimeUserContextVO.RoleSummary role : roles) {
            if (role != null && StringUtils.hasText(role.getRoleCode())) {
                roleCodes.add(role.getRoleCode());
            }
        }
        return roleCodes;
    }

    private List<String> toOrgIds(List<RuntimeUserContextVO.OrgSummary> orgs) {
        List<String> orgIds = new ArrayList<String>();
        if (orgs == null) {
            return orgIds;
        }
        for (RuntimeUserContextVO.OrgSummary org : orgs) {
            if (org != null && StringUtils.hasText(org.getId())) {
                orgIds.add(org.getId());
            }
        }
        return orgIds;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
