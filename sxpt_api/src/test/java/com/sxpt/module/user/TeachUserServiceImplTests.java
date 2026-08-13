package com.sxpt.module.user;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.PasswordHashService;
import com.sxpt.module.user.entity.TeachUser;
import com.sxpt.module.user.mapper.TeachUserMapper;
import com.sxpt.module.user.service.impl.TeachUserServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 教学平台用户服务测试。
 *
 * 业务功能：
 * 1. 验证创建教学用户时会补齐默认生命周期字段。
 * 2. 验证缺少必填字段时不会写入数据库。
 *
 * 关键流程：
 * 1. 使用 Mockito 模拟 Mapper，避免单元测试依赖真实数据库。
 * 2. 直接调用 Service，聚焦业务规则和持久化调用边界。
 */
class TeachUserServiceImplTests {

    /**
     * 校验创建教学用户时写入 Mapper 并补齐默认值。
     */
    @Test
    void createTeachUserShouldFillDefaultsAndInsert() {
        TeachUserMapper mapper = mock(TeachUserMapper.class);
        TeachUserServiceImpl service = new TeachUserServiceImpl(mapper);
        TeachUser teachUser = buildValidTeachUser();

        TeachUser saved = service.createTeachUser(teachUser);

        assertEquals("ACTIVE", saved.getStatus());
        assertFalse(saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper, times(1)).insert(saved);
    }

    /**
     * 校验缺少必填字段时拒绝创建。
     */
    @Test
    void createTeachUserShouldRejectMissingRequiredField() {
        TeachUserMapper mapper = mock(TeachUserMapper.class);
        TeachUserServiceImpl service = new TeachUserServiceImpl(mapper);
        TeachUser teachUser = buildValidTeachUser();
        teachUser.setUsername(" ");

        assertThrows(BusinessException.class, () -> service.createTeachUser(teachUser));
        verify(mapper, times(0)).insert(teachUser);
    }

    /**
     * 校验编辑用户时只更新基础资料，不修改登录账号。
     */
    @Test
    void updateTeachUserShouldKeepUsernameStable() {
        TeachUserMapper mapper = mock(TeachUserMapper.class);
        TeachUserServiceImpl service = new TeachUserServiceImpl(mapper);
        TeachUser existing = buildValidTeachUser();
        when(mapper.selectOne(any())).thenReturn(existing);
        TeachUser update = new TeachUser();
        update.setId("user_001");
        update.setRealName("教师一改");
        update.setUserType("ADMIN");
        update.setSourceType("LOCAL");
        update.setPhone("13800000000");

        TeachUser saved = service.updateTeachUser("tenant_001", update);

        assertEquals("teacher001", saved.getUsername());
        assertEquals("教师一改", saved.getRealName());
        assertEquals("ADMIN", saved.getUserType());
        assertEquals("13800000000", saved.getPhone());
        assertNotNull(saved.getUpdateTime());
        verify(mapper, times(1)).selectOne(any());
        verify(mapper, times(1)).updateById(saved);
    }

    /**
     * 校验用户状态切换只接受启用和停用状态。
     */
    @Test
    void updateTeachUserStatusShouldRejectUnsupportedStatus() {
        TeachUserMapper mapper = mock(TeachUserMapper.class);
        TeachUserServiceImpl service = new TeachUserServiceImpl(mapper);

        assertThrows(BusinessException.class, () ->
                service.updateTeachUserStatus("tenant_001", "user_001", "LOCKED"));
        verify(mapper, times(0)).selectOne(any());
        verify(mapper, times(0)).updateById(any());
    }

    /**
     * 验证重置密码会生成新的凭据并清理锁定状态。
     */
    @Test
    void resetTeachUserPasswordShouldRefreshCredentialAndUnlockUser() {
        TeachUserMapper mapper = mock(TeachUserMapper.class);
        PasswordHashService passwordHashService = new PasswordHashService();
        TeachUserServiceImpl service = new TeachUserServiceImpl(mapper, passwordHashService);
        TeachUser existing = buildValidTeachUser();
        existing.setPasswordSalt("old-salt");
        existing.setPasswordHash("old-hash");
        existing.setPasswordStatus("LOCKED");
        existing.setFailedLoginCount(5);
        existing.setLockedUntil(java.time.LocalDateTime.now().plusHours(1));
        when(mapper.selectOne(any())).thenReturn(existing);

        TeachUser saved = service.resetTeachUserPassword("tenant_001", "user_001", "Sxpt@123456");

        assertEquals("NORMAL", saved.getPasswordStatus());
        assertEquals(0, saved.getFailedLoginCount());
        assertEquals(PasswordHashService.ALGORITHM, saved.getPasswordAlgorithm());
        assertEquals(PasswordHashService.DEFAULT_ITERATIONS, saved.getPasswordIterations());
        assertNotNull(saved.getPasswordUpdatedTime());
        org.junit.jupiter.api.Assertions.assertNull(saved.getLockedUntil());
        org.junit.jupiter.api.Assertions.assertNotEquals("old-salt", saved.getPasswordSalt());
        org.junit.jupiter.api.Assertions.assertNotEquals("old-hash", saved.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(passwordHashService.matches(
                "Sxpt@123456",
                saved.getPasswordSalt(),
                saved.getPasswordHash(),
                saved.getPasswordIterations()));
        verify(mapper, times(1)).selectOne(any());
        verify(mapper, times(1)).updateById(saved);
    }

    /**
     * 构造最小有效教学平台用户。
     *
     * @return 教学平台用户实体。
     */
    private TeachUser buildValidTeachUser() {
        TeachUser teachUser = new TeachUser();
        teachUser.setId("user_001");
        teachUser.setTenantId("tenant_001");
        teachUser.setUsername("teacher001");
        teachUser.setRealName("教师一");
        teachUser.setUserType("TEACHER");
        teachUser.setSourceType("LOCAL");
        return teachUser;
    }
}
