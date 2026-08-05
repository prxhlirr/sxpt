package com.sxpt.module.user;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.user.entity.TeachRole;
import com.sxpt.module.user.mapper.TeachRoleMapper;
import com.sxpt.module.user.service.impl.TeachRoleServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 教学平台角色服务测试。
 *
 * 业务功能：
 * 1. 验证创建教学角色时会补齐默认生命周期字段。
 * 2. 验证缺少必填字段时不会写入数据库。
 *
 * 关键流程：
 * 1. 使用 Mockito 模拟 Mapper，避免单元测试依赖真实数据库。
 * 2. 直接调用 Service，聚焦业务规则和持久化调用边界。
 */
class TeachRoleServiceImplTests {

    /**
     * 校验创建教学角色时写入 Mapper 并补齐默认值。
     */
    @Test
    void createTeachRoleShouldFillDefaultsAndInsert() {
        TeachRoleMapper mapper = mock(TeachRoleMapper.class);
        TeachRoleServiceImpl service = new TeachRoleServiceImpl(mapper);
        TeachRole teachRole = buildValidTeachRole();

        TeachRole saved = service.createTeachRole(teachRole);

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
    void createTeachRoleShouldRejectMissingRequiredField() {
        TeachRoleMapper mapper = mock(TeachRoleMapper.class);
        TeachRoleServiceImpl service = new TeachRoleServiceImpl(mapper);
        TeachRole teachRole = buildValidTeachRole();
        teachRole.setRoleCode(" ");

        assertThrows(BusinessException.class, () -> service.createTeachRole(teachRole));
        verify(mapper, times(0)).insert(teachRole);
    }

    /**
     * 校验按租户查询教学角色列表时委托 Mapper 并返回查询结果。
     */
    @Test
    void listTeachRolesByTenantIdShouldReturnMapperResult() {
        TeachRoleMapper mapper = mock(TeachRoleMapper.class);
        TeachRoleServiceImpl service = new TeachRoleServiceImpl(mapper);
        List<TeachRole> roles = Collections.singletonList(buildValidTeachRole());
        when(mapper.selectList(any())).thenReturn(roles);

        List<TeachRole> result = service.listTeachRolesByTenantId("tenant_001");

        assertSame(roles, result);
        verify(mapper, times(1)).selectList(any());
    }

    /**
     * 校验缺少租户 ID 时拒绝查询角色列表。
     */
    @Test
    void listTeachRolesByTenantIdShouldRejectMissingTenantId() {
        TeachRoleMapper mapper = mock(TeachRoleMapper.class);
        TeachRoleServiceImpl service = new TeachRoleServiceImpl(mapper);

        assertThrows(BusinessException.class, () -> service.listTeachRolesByTenantId(" "));
        verify(mapper, times(0)).selectList(any());
    }

    /**
     * 校验编辑角色时只更新名称和说明，不修改角色编码。
     */
    @Test
    void updateTeachRoleShouldKeepRoleCodeStable() {
        TeachRoleMapper mapper = mock(TeachRoleMapper.class);
        TeachRoleServiceImpl service = new TeachRoleServiceImpl(mapper);
        TeachRole existing = buildValidTeachRole();
        when(mapper.selectOne(any())).thenReturn(existing);
        TeachRole update = new TeachRole();
        update.setId("role_001");
        update.setRoleName("实训教师");
        update.setDescription("负责实训任务");

        TeachRole saved = service.updateTeachRole("tenant_001", update);

        assertEquals("TEACHER", saved.getRoleCode());
        assertEquals("实训教师", saved.getRoleName());
        assertEquals("负责实训任务", saved.getDescription());
        assertNotNull(saved.getUpdateTime());
        verify(mapper, times(1)).selectOne(any());
        verify(mapper, times(1)).updateById(saved);
    }

    /**
     * 校验角色状态切换会写回目标状态。
     */
    @Test
    void updateTeachRoleStatusShouldUpdateExistingRole() {
        TeachRoleMapper mapper = mock(TeachRoleMapper.class);
        TeachRoleServiceImpl service = new TeachRoleServiceImpl(mapper);
        TeachRole existing = buildValidTeachRole();
        when(mapper.selectOne(any())).thenReturn(existing);

        TeachRole saved = service.updateTeachRoleStatus("tenant_001", "role_001", "DISABLED");

        assertEquals("DISABLED", saved.getStatus());
        assertNotNull(saved.getUpdateTime());
        verify(mapper, times(1)).selectOne(any());
        verify(mapper, times(1)).updateById(saved);
    }

    /**
     * 构造最小有效教学平台角色。
     *
     * @return 教学平台角色实体。
     */
    private TeachRole buildValidTeachRole() {
        TeachRole teachRole = new TeachRole();
        teachRole.setId("role_001");
        teachRole.setTenantId("tenant_001");
        teachRole.setRoleCode("TEACHER");
        teachRole.setRoleName("教师");
        return teachRole;
    }
}
