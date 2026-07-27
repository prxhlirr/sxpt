package com.sxpt.common.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.config.JwtProperties;
import com.sxpt.controller.AuthLoginRequest;
import com.sxpt.controller.AuthLoginResponse;
import com.sxpt.module.user.entity.TeachUser;
import com.sxpt.module.user.mapper.TeachUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

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

    private final TeachUserMapper teachUserMapper;

    private final PasswordHashService passwordHashService;

    private final JwtService jwtService;

    private final JwtProperties jwtProperties;

    public AuthLoginServiceImpl(
            TeachUserMapper teachUserMapper,
            PasswordHashService passwordHashService,
            JwtService jwtService,
            JwtProperties jwtProperties
    ) {
        this.teachUserMapper = teachUserMapper;
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
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
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
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
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
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
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
        response.setToken(jwtService.generateToken(teachUser.getId(), teachUser.getUsername()));
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtProperties.getExpireSeconds());

        AuthLoginResponse.UserSummary user = new AuthLoginResponse.UserSummary();
        user.setUserId(teachUser.getId());
        user.setTenantId(teachUser.getTenantId());
        user.setUsername(teachUser.getUsername());
        user.setDisplayName(teachUser.getRealName());
        user.setUserType(teachUser.getUserType());
        user.setStudentNo(teachUser.getStudentNo());
        user.setEmployeeNo(teachUser.getEmployeeNo());
        response.setUser(user);
        return response;
    }
}
