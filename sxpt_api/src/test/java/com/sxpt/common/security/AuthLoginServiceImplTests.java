package com.sxpt.common.security;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.config.JwtProperties;
import com.sxpt.controller.AuthLoginRequest;
import com.sxpt.controller.AuthLoginResponse;
import com.sxpt.module.user.entity.TeachUser;
import com.sxpt.module.user.mapper.TeachUserMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 正式登录服务测试。
 *
 * 业务功能：
 * 1. 验证账号密码登录使用 teach_user 的 PBKDF2 密码字段。
 * 2. 验证密码错误不会签发 Token，并会记录失败次数。
 *
 * 关键流程：
 * 1. 使用 Mock Mapper 提供用户数据。
 * 2. 使用真实 PasswordHashService 和 JwtService 校验登录结果。
 */
class AuthLoginServiceImplTests {

    private final PasswordHashService passwordHashService = new PasswordHashService();

    /**
     * 校验密码正确时签发 Token 并返回用户摘要。
     */
    @Test
    void loginShouldReturnTokenWhenPasswordIsCorrect() {
        TeachUserMapper mapper = mock(TeachUserMapper.class);
        TeachUser teachUser = buildLoginUser("StrongPassword123");
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(teachUser);
        AuthLoginServiceImpl service = buildService(mapper);

        AuthLoginResponse response = service.login(buildRequest("StrongPassword123"));

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getUserId()).isEqualTo("user_001");
        assertThat(response.getUser().getEmployeeNo()).isEqualTo("T001");
        assertThat(teachUser.getFailedLoginCount()).isEqualTo(0);
        assertThat(teachUser.getLastLoginTime()).isNotNull();
        verify(mapper, times(1)).updateById(teachUser);
    }

    /**
     * 校验密码错误时拒绝登录并记录失败次数。
     */
    @Test
    void loginShouldRejectWrongPasswordAndRecordFailure() {
        TeachUserMapper mapper = mock(TeachUserMapper.class);
        TeachUser teachUser = buildLoginUser("StrongPassword123");
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(teachUser);
        AuthLoginServiceImpl service = buildService(mapper);

        assertThatThrownBy(() -> service.login(buildRequest("WrongPassword123")))
                .isInstanceOf(BusinessException.class);
        assertThat(teachUser.getFailedLoginCount()).isEqualTo(1);
        verify(mapper, times(1)).updateById(teachUser);
    }

    /**
     * 校验非 PASSWORD 登录类型暂不开放，避免接口假装支持未完成的 SSO。
     */
    @Test
    void loginShouldRejectUnsupportedLoginType() {
        TeachUserMapper mapper = mock(TeachUserMapper.class);
        AuthLoginServiceImpl service = buildService(mapper);
        AuthLoginRequest request = buildRequest("StrongPassword123");
        request.setLoginType("SSO_TICKET");

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(BusinessException.class);
        verify(mapper, times(0)).selectOne(any(Wrapper.class));
    }

    private AuthLoginServiceImpl buildService(TeachUserMapper mapper) {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("sxpt_test_jwt_secret");
        jwtProperties.setExpireSeconds(7200L);
        return new AuthLoginServiceImpl(
                mapper,
                passwordHashService,
                new JwtService(jwtProperties),
                jwtProperties
        );
    }

    private AuthLoginRequest buildRequest(String password) {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setLoginType("PASSWORD");
        request.setTenantId("tenant_001");
        request.setUsername("teacher001");
        request.setPassword(password);
        return request;
    }

    private TeachUser buildLoginUser(String rawPassword) {
        String salt = passwordHashService.generateSalt();
        TeachUser teachUser = new TeachUser();
        teachUser.setId("user_001");
        teachUser.setTenantId("tenant_001");
        teachUser.setUsername("teacher001");
        teachUser.setRealName("教师一");
        teachUser.setUserType("TEACHER");
        teachUser.setEmployeeNo("T001");
        teachUser.setStatus("ACTIVE");
        teachUser.setDeleted(Boolean.FALSE);
        teachUser.setPasswordStatus("NORMAL");
        teachUser.setPasswordSalt(salt);
        teachUser.setPasswordAlgorithm(PasswordHashService.ALGORITHM);
        teachUser.setPasswordIterations(PasswordHashService.DEFAULT_ITERATIONS);
        teachUser.setPasswordHash(passwordHashService.hash(rawPassword, salt));
        teachUser.setFailedLoginCount(0);
        teachUser.setUpdateTime(LocalDateTime.now());
        return teachUser;
    }
}
