package com.sxpt.common.security;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

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
        AuthLoginServiceImpl service = buildService(mapper, buildUserRoleMapper(), buildRoleMapper(), buildUserOrgMapper());

        AuthLoginResponse response = service.login(buildRequest("StrongPassword123"));

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getUserId()).isEqualTo("user_001");
        assertThat(response.getUser().getEmployeeNo()).isEqualTo("T001");
        assertThat(response.getUser().getRoles()).containsExactly("teacher");
        assertThat(response.getUser().getOrgIds()).containsExactly("org-class-001");
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
        AuthLoginServiceImpl service = buildService(mapper, buildUserRoleMapper(), buildRoleMapper(), buildUserOrgMapper());

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
        AuthLoginServiceImpl service = buildService(mapper, buildUserRoleMapper(), buildRoleMapper(), buildUserOrgMapper());
        AuthLoginRequest request = buildRequest("StrongPassword123");
        request.setLoginType("SSO_TICKET");

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(BusinessException.class);
        verify(mapper, times(0)).selectOne(any(Wrapper.class));
    }

    private AuthLoginServiceImpl buildService(
            TeachUserMapper mapper,
            TeachUserRoleMapper userRoleMapper,
            TeachRoleMapper roleMapper,
            TeachUserOrgMapper userOrgMapper
    ) {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("sxpt_test_jwt_secret");
        jwtProperties.setExpireSeconds(7200L);
        return new AuthLoginServiceImpl(
                mapper,
                userRoleMapper,
                roleMapper,
                userOrgMapper,
                passwordHashService,
                new JwtService(jwtProperties),
                jwtProperties
        );
    }

    private TeachUserRoleMapper buildUserRoleMapper() {
        TeachUserRoleMapper mapper = mock(TeachUserRoleMapper.class);
        TeachUserRole userRole = new TeachUserRole();
        userRole.setTenantId("tenant_001");
        userRole.setUserId("user_001");
        userRole.setRoleId("role_teacher");
        when(mapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(userRole));
        return mapper;
    }

    private TeachRoleMapper buildRoleMapper() {
        TeachRoleMapper mapper = mock(TeachRoleMapper.class);
        TeachRole role = new TeachRole();
        role.setId("role_teacher");
        role.setRoleCode("teacher");
        role.setStatus("ACTIVE");
        role.setDeleted(Boolean.FALSE);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(role));
        return mapper;
    }

    private TeachUserOrgMapper buildUserOrgMapper() {
        TeachUserOrgMapper mapper = mock(TeachUserOrgMapper.class);
        TeachUserOrg userOrg = new TeachUserOrg();
        userOrg.setTenantId("tenant_001");
        userOrg.setUserId("user_001");
        userOrg.setOrgId("org-class-001");
        when(mapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(userOrg));
        return mapper;
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
