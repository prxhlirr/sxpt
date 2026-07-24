package com.sxpt.module.user;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.user.entity.TeachUserRole;
import com.sxpt.module.user.mapper.TeachUserRoleMapper;
import com.sxpt.module.user.service.impl.TeachUserRoleServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 教学平台用户角色关系服务测试。
 *
 * 业务功能：
 * 1. 验证授予教学角色时会补齐默认生命周期字段。
 * 2. 验证缺少必填字段时不会写入数据库。
 *
 * 关键流程：
 * 1. 使用 Mockito 模拟 Mapper，避免单元测试依赖真实数据库。
 * 2. 直接调用 Service，聚焦授权关系的业务规则和持久化边界。
 */
class TeachUserRoleServiceImplTests {

    /**
     * 校验授予教学角色时写入 Mapper 并补齐默认值。
     */
    @Test
    void grantUserRoleShouldFillDefaultsAndInsert() {
        TeachUserRoleMapper mapper = mock(TeachUserRoleMapper.class);
        TeachUserRoleServiceImpl service = new TeachUserRoleServiceImpl(mapper);
        TeachUserRole teachUserRole = buildValidTeachUserRole();

        TeachUserRole saved = service.grantUserRole(teachUserRole);

        assertEquals("MANUAL", saved.getGrantSource());
        assertEquals("ACTIVE", saved.getStatus());
        assertFalse(saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper, times(1)).insert(saved);
    }

    /**
     * 校验缺少用户 ID 时拒绝授权。
     */
    @Test
    void grantUserRoleShouldRejectMissingUserId() {
        TeachUserRoleMapper mapper = mock(TeachUserRoleMapper.class);
        TeachUserRoleServiceImpl service = new TeachUserRoleServiceImpl(mapper);
        TeachUserRole teachUserRole = buildValidTeachUserRole();
        teachUserRole.setUserId(" ");

        assertThrows(BusinessException.class, () -> service.grantUserRole(teachUserRole));
        verify(mapper, times(0)).insert(teachUserRole);
    }

    /**
     * 构造最小有效教学平台用户角色关系。
     *
     * @return 用户角色关系实体。
     */
    private TeachUserRole buildValidTeachUserRole() {
        TeachUserRole teachUserRole = new TeachUserRole();
        teachUserRole.setId("user_role_001");
        teachUserRole.setTenantId("tenant_001");
        teachUserRole.setUserId("user_001");
        teachUserRole.setRoleId("role_001");
        return teachUserRole;
    }
}
