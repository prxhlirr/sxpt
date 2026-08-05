package com.sxpt.module.user;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.user.entity.TeachUserOrg;
import com.sxpt.module.user.mapper.TeachUserOrgMapper;
import com.sxpt.module.user.service.impl.TeachUserOrgServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 教学用户组织关系服务测试。
 *
 * 业务功能：
 * 1. 验证添加用户到教学组织时会补齐默认生命周期字段。
 * 2. 验证缺少必填字段时不会写入数据库。
 *
 * 关键流程：
 * 1. 使用 Mockito 模拟 Mapper，避免单元测试依赖真实数据库。
 * 2. 直接调用 Service，聚焦成员关系的业务规则和持久化边界。
 */
class TeachUserOrgServiceImplTests {

    /**
     * 校验添加用户到教学组织时写入 Mapper 并补齐默认值。
     */
    @Test
    void addUserToOrgShouldFillDefaultsAndInsert() {
        TeachUserOrgMapper mapper = mock(TeachUserOrgMapper.class);
        TeachUserOrgServiceImpl service = new TeachUserOrgServiceImpl(mapper);
        TeachUserOrg teachUserOrg = buildValidTeachUserOrg();

        TeachUserOrg saved = service.addUserToOrg(teachUserOrg);

        assertEquals("ACTIVE", saved.getStatus());
        assertFalse(saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper, times(1)).insert(saved);
    }

    /**
     * 校验缺少组织 ID 时拒绝添加成员关系。
     */
    @Test
    void addUserToOrgShouldRejectMissingOrgId() {
        TeachUserOrgMapper mapper = mock(TeachUserOrgMapper.class);
        TeachUserOrgServiceImpl service = new TeachUserOrgServiceImpl(mapper);
        TeachUserOrg teachUserOrg = buildValidTeachUserOrg();
        teachUserOrg.setOrgId(" ");

        assertThrows(BusinessException.class, () -> service.addUserToOrg(teachUserOrg));
        verify(mapper, times(0)).insert(teachUserOrg);
    }

    /**
     * 校验从教学组织移除用户时执行软删除并写回数据库。
     */
    @Test
    void removeUserFromOrgShouldSoftDeleteExistingRelation() {
        TeachUserOrgMapper mapper = mock(TeachUserOrgMapper.class);
        TeachUserOrgServiceImpl service = new TeachUserOrgServiceImpl(mapper);
        TeachUserOrg existing = buildValidTeachUserOrg();
        existing.setDeleted(Boolean.FALSE);
        when(mapper.selectOne(any())).thenReturn(existing);

        TeachUserOrg removed = service.removeUserFromOrg("tenant_001", "org_001", "user_001");

        assertTrue(removed.getDeleted());
        assertEquals("DISABLED", removed.getStatus());
        assertNotNull(removed.getUpdateTime());
        verify(mapper, times(1)).selectOne(any());
        verify(mapper, times(1)).updateById(removed);
    }

    /**
     * 校验移除不存在的成员关系时返回业务异常。
     */
    @Test
    void removeUserFromOrgShouldRejectMissingRelation() {
        TeachUserOrgMapper mapper = mock(TeachUserOrgMapper.class);
        TeachUserOrgServiceImpl service = new TeachUserOrgServiceImpl(mapper);
        when(mapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.removeUserFromOrg("tenant_001", "org_001", "user_001"));
        verify(mapper, times(1)).selectOne(any());
        verify(mapper, times(0)).updateById(any());
    }

    /**
     * 校验用户单位绑定状态切换会写回目标状态。
     */
    @Test
    void updateUserOrgStatusShouldUpdateExistingRelation() {
        TeachUserOrgMapper mapper = mock(TeachUserOrgMapper.class);
        TeachUserOrgServiceImpl service = new TeachUserOrgServiceImpl(mapper);
        TeachUserOrg existing = buildValidTeachUserOrg();
        when(mapper.selectOne(any())).thenReturn(existing);

        TeachUserOrg saved = service.updateUserOrgStatus("tenant_001", "user_org_001", "DISABLED");

        assertEquals("DISABLED", saved.getStatus());
        assertNotNull(saved.getUpdateTime());
        verify(mapper, times(1)).selectOne(any());
        verify(mapper, times(1)).updateById(saved);
    }

    /**
     * 构造最小有效教学用户组织关系。
     *
     * @return 用户组织关系实体。
     */
    private TeachUserOrg buildValidTeachUserOrg() {
        TeachUserOrg teachUserOrg = new TeachUserOrg();
        teachUserOrg.setId("user_org_001");
        teachUserOrg.setTenantId("tenant_001");
        teachUserOrg.setUserId("user_001");
        teachUserOrg.setOrgId("org_001");
        teachUserOrg.setRelationType("STUDENT");
        return teachUserOrg;
    }
}
