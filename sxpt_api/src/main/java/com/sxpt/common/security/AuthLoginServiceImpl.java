package com.sxpt.common.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.config.JwtProperties;
import com.sxpt.controller.AuthLoginRequest;
import com.sxpt.controller.AuthLoginResponse;
import com.sxpt.module.user.entity.TeachRole;
import com.sxpt.module.user.entity.TeachUser;
import com.sxpt.module.user.entity.TeachUserOrg;
import com.sxpt.module.user.entity.TeachUserRole;
import com.sxpt.module.user.mapper.TeachRoleMapper;
import com.sxpt.module.user.mapper.TeachUserMapper;
import com.sxpt.module.user.mapper.TeachUserOrgMapper;
import com.sxpt.module.user.mapper.TeachUserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 正式登录服务实现。
 *
 * 业务功能：
 * 1. 使用 teach_user 作为教学平台正式用户表完成账号密码登录。
 * 2. 登录成功后更新登录审计字段，并签发 JWT。
 *
 * 关键流程：
 * 1. 按 tenantId + username 查询未删除用户。
 * 2. 校验用户状态、密码状态、锁定状态和 PBKDF2 密码哈希。
 * 3. 登录失败增加失败次数，登录成功清零失败次数并更新最近登录时间。
 */
@Service
public class AuthLoginServiceImpl implements AuthLoginService {

    private static final String LOGIN_TYPE_PASSWORD = "PASSWORD";

    private static final String USER_STATUS_ACTIVE = "ACTIVE";

    private static final String PASSWORD_STATUS_NORMAL = "NORMAL";

    private static final int LOGIN_FAILED_CODE = 401;

    private static final String LOGIN_FAILED_MESSAGE = "账号或密码错误";

    private final TeachUserMapper teachUserMapper;

    private final TeachUserRoleMapper teachUserRoleMapper;

    private final TeachRoleMapper teachRoleMapper;

    private final TeachUserOrgMapper teachUserOrgMapper;

    private final PasswordHashService passwordHashService;

    private final JwtService jwtService;

    private final JwtProperties jwtProperties;

    public AuthLoginServiceImpl(
            TeachUserMapper teachUserMapper,
            TeachUserRoleMapper teachUserRoleMapper,
            TeachRoleMapper teachRoleMapper,
            TeachUserOrgMapper teachUserOrgMapper,
            PasswordHashService passwordHashService,
            JwtService jwtService,
            JwtProperties jwtProperties
    ) {
        this.teachUserMapper = teachUserMapper;
        this.teachUserRoleMapper = teachUserRoleMapper;
        this.teachRoleMapper = teachRoleMapper;
        this.teachUserOrgMapper = teachUserOrgMapper;
        this.passwordHashService = passwordHashService;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 执行正式登录。
     *
     * @param request 登录请求。
     * @return 登录成功后的 Token 和用户摘要。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthLoginResponse login(AuthLoginRequest request) {
        validateLoginRequest(request);
        TeachUser teachUser = findActiveUser(request.getTenantId(), request.getUsername());
        validateUserCanLogin(teachUser);
        if (!passwordHashService.matches(
                request.getPassword(),
                teachUser.getPasswordSalt(),
                teachUser.getPasswordHash(),
                teachUser.getPasswordIterations()
        )) {
            recordLoginFailure(teachUser);
            throw loginFailedException();
        }
        recordLoginSuccess(teachUser);
        return buildLoginResponse(teachUser);
    }

    /**
     * 校验登录请求的业务语义。
     *
     * @param request 登录请求。
     */
    private void validateLoginRequest(AuthLoginRequest request) {
        if (request == null
                || !LOGIN_TYPE_PASSWORD.equals(request.getLoginType())
                || !StringUtils.hasText(request.getTenantId())
                || !StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 查询可登录用户。
     *
     * @param tenantId 租户 ID。
     * @param username 用户名。
     * @return 教学平台用户。
     */
    private TeachUser findActiveUser(String tenantId, String username) {
        TeachUser teachUser = teachUserMapper.selectOne(new LambdaQueryWrapper<TeachUser>()
                .eq(TeachUser::getTenantId, tenantId)
                .eq(TeachUser::getUsername, username)
                .eq(TeachUser::getDeleted, Boolean.FALSE));
        if (teachUser == null) {
            throw loginFailedException();
        }
        return teachUser;
    }

    /**
     * 校验用户和密码状态是否允许登录。
     *
     * @param teachUser 教学平台用户。
     */
    private void validateUserCanLogin(TeachUser teachUser) {
        if (!USER_STATUS_ACTIVE.equals(teachUser.getStatus())
                || !PASSWORD_STATUS_NORMAL.equals(teachUser.getPasswordStatus())
                || !StringUtils.hasText(teachUser.getPasswordHash())
                || !StringUtils.hasText(teachUser.getPasswordSalt())
                || teachUser.getPasswordIterations() == null
                || isLocked(teachUser)) {
            throw loginFailedException();
        }
    }

    /**
     * 构造登录失败异常。
     *
     * 业务功能：
     * 1. 将账号不存在、密码错误、密码状态异常统一表达为登录失败。
     * 2. 避免向前端暴露账号是否存在，同时避免误提示为“用户未登录”。
     *
     * 关键流程：
     * 1. 复用 401 业务码表达认证失败。
     * 2. 使用登录场景专属文案，帮助用户定位是账号密码校验未通过。
     */
    private BusinessException loginFailedException() {
        return new BusinessException(LOGIN_FAILED_CODE, LOGIN_FAILED_MESSAGE);
    }

    /**
     * 判断账号是否处于锁定期。
     *
     * @param teachUser 教学平台用户。
     * @return 是否锁定。
     */
    private boolean isLocked(TeachUser teachUser) {
        return teachUser.getLockedUntil() != null && teachUser.getLockedUntil().isAfter(LocalDateTime.now());
    }

    /**
     * 记录登录失败。
     *
     * @param teachUser 教学平台用户。
     */
    private void recordLoginFailure(TeachUser teachUser) {
        Integer failedLoginCount = teachUser.getFailedLoginCount() == null ? 0 : teachUser.getFailedLoginCount();
        teachUser.setFailedLoginCount(failedLoginCount + 1);
        teachUser.setUpdateTime(LocalDateTime.now());
        teachUserMapper.updateById(teachUser);
    }

    /**
     * 记录登录成功。
     *
     * @param teachUser 教学平台用户。
     */
    private void recordLoginSuccess(TeachUser teachUser) {
        LocalDateTime now = LocalDateTime.now();
        teachUser.setFailedLoginCount(0);
        teachUser.setLastLoginTime(now);
        teachUser.setUpdateTime(now);
        teachUserMapper.updateById(teachUser);
    }

    /**
     * 构造登录成功返回。
     *
     * @param teachUser 教学平台用户。
     * @return 登录返回对象。
     */
    private AuthLoginResponse buildLoginResponse(TeachUser teachUser) {
        AuthLoginResponse response = new AuthLoginResponse();
        List<String> roleCodes = new ArrayList<String>(listRoleCodes(teachUser));
        if (StringUtils.hasText(teachUser.getUserType())
                && !containsIgnoreCase(roleCodes, teachUser.getUserType())) {
            roleCodes.add(teachUser.getUserType());
        }

        AuthLoginResponse.UserSummary user = new AuthLoginResponse.UserSummary();
        user.setUserId(teachUser.getId());
        user.setTenantId(teachUser.getTenantId());
        user.setUsername(teachUser.getUsername());
        user.setDisplayName(teachUser.getRealName());
        user.setUserType(teachUser.getUserType());
        user.setStudentNo(teachUser.getStudentNo());
        user.setEmployeeNo(teachUser.getEmployeeNo());
        user.setRoles(roleCodes);
        user.setOrgIds(listOrgIds(teachUser));
        response.setUser(user);
        response.setToken(jwtService.generateToken(
                teachUser.getId(),
                teachUser.getUsername(),
                teachUser.getTenantId(),
                roleCodes
        ));
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtProperties.getExpireSeconds());
        return response;
    }

    private boolean containsIgnoreCase(List<String> values, String expected) {
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(expected)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查询登录用户的教学平台角色编码。
     *
     * 业务功能：
     * 1. 登录成功后返回服务端确认过的角色，而不是让前端自行推断门户权限。
     * 2. 支撑教师、学生、专家和后台入口按真实授权跳转。
     *
     * 关键流程：
     * 1. 先按 tenantId + userId 查询有效用户角色关系。
     * 2. 再按 roleId 查询有效角色定义，只返回 roleCode。
     */
    private List<String> listRoleCodes(TeachUser teachUser) {
        List<TeachUserRole> userRoles = teachUserRoleMapper.selectList(new QueryWrapper<TeachUserRole>()
                .eq("tenant_id", teachUser.getTenantId())
                .eq("user_id", teachUser.getId())
                .eq("status", USER_STATUS_ACTIVE)
                .eq("deleted", Boolean.FALSE));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> roleIds = new ArrayList<>();
        for (TeachUserRole userRole : userRoles) {
            roleIds.add(userRole.getRoleId());
        }
        List<TeachRole> roles = teachRoleMapper.selectList(new QueryWrapper<TeachRole>()
                .eq("tenant_id", teachUser.getTenantId())
                .in("id", roleIds)
                .eq("status", USER_STATUS_ACTIVE)
                .eq("deleted", Boolean.FALSE));
        List<String> roleCodes = new ArrayList<>();
        for (TeachRole role : roles) {
            roleCodes.add(role.getRoleCode());
        }
        return roleCodes;
    }

    /**
     * 查询登录用户所属教学组织。
     *
     * 业务功能：
     * 1. 登录成功后返回班级、课程班或专家组等教学组织范围。
     * 2. 为后续数据准备策略按组织、班级、学生范围生成需求提供前置上下文。
     *
     * 关键流程：
     * 1. 按 tenantId + userId 查询有效用户组织关系。
     * 2. 只返回 orgId，避免登录响应携带过重组织明细。
     */
    private List<String> listOrgIds(TeachUser teachUser) {
        List<TeachUserOrg> userOrgs = teachUserOrgMapper.selectList(new QueryWrapper<TeachUserOrg>()
                .eq("tenant_id", teachUser.getTenantId())
                .eq("user_id", teachUser.getId())
                .eq("status", USER_STATUS_ACTIVE)
                .eq("deleted", Boolean.FALSE));
        List<String> orgIds = new ArrayList<>();
        for (TeachUserOrg userOrg : userOrgs) {
            orgIds.add(userOrg.getOrgId());
        }
        return orgIds;
    }
}
