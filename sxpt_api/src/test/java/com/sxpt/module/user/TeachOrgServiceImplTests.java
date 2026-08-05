package com.sxpt.module.user;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.user.entity.TeachOrg;
import com.sxpt.module.user.mapper.TeachOrgMapper;
import com.sxpt.module.user.service.impl.TeachOrgServiceImpl;
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
 * 教学组织服务测试。
 *
 * 业务功能：
 * 1. 验证创建教学组织时会补齐默认生命周期字段。
 * 2. 验证缺少必填字段时不会写入数据库。
 *
 * 关键流程：
 * 1. 使用 Mockito 模拟 Mapper，避免单元测试依赖真实数据库。
 * 2. 直接调用 Service，聚焦组织维护的业务规则和持久化边界。
 */
class TeachOrgServiceImplTests {

    /**
     * 校验创建教学组织时写入 Mapper 并补齐默认值。
     */
    @Test
    void createTeachOrgShouldFillDefaultsAndInsert() {
        TeachOrgMapper mapper = mock(TeachOrgMapper.class);
        TeachOrgServiceImpl service = new TeachOrgServiceImpl(mapper);
        TeachOrg teachOrg = buildValidTeachOrg();

        TeachOrg saved = service.createTeachOrg(teachOrg);

        assertEquals("ACTIVE", saved.getStatus());
        assertFalse(saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper, times(1)).insert(saved);
    }

    /**
     * 校验缺少组织编码时拒绝创建。
     */
    @Test
    void createTeachOrgShouldRejectMissingRequiredField() {
        TeachOrgMapper mapper = mock(TeachOrgMapper.class);
        TeachOrgServiceImpl service = new TeachOrgServiceImpl(mapper);
        TeachOrg teachOrg = buildValidTeachOrg();
        teachOrg.setOrgCode(" ");

        assertThrows(BusinessException.class, () -> service.createTeachOrg(teachOrg));
        verify(mapper, times(0)).insert(teachOrg);
    }

    /**
     * 校验按租户查询教学组织列表时委托 Mapper 并返回查询结果。
     */
    @Test
    void listTeachOrgsByTenantIdShouldReturnMapperResult() {
        TeachOrgMapper mapper = mock(TeachOrgMapper.class);
        TeachOrgServiceImpl service = new TeachOrgServiceImpl(mapper);
        List<TeachOrg> orgs = Collections.singletonList(buildValidTeachOrg());
        when(mapper.selectList(any())).thenReturn(orgs);

        List<TeachOrg> result = service.listTeachOrgsByTenantId("tenant_001");

        assertSame(orgs, result);
        verify(mapper, times(1)).selectList(any());
    }

    /**
     * 校验缺少租户 ID 时拒绝查询教学组织列表。
     */
    @Test
    void listTeachOrgsByTenantIdShouldRejectMissingTenantId() {
        TeachOrgMapper mapper = mock(TeachOrgMapper.class);
        TeachOrgServiceImpl service = new TeachOrgServiceImpl(mapper);

        assertThrows(BusinessException.class, () -> service.listTeachOrgsByTenantId(" "));
        verify(mapper, times(0)).selectList(any());
    }

    /**
     * 校验编辑单位时只更新名称、类型和上级单位，不修改单位编码。
     */
    @Test
    void updateTeachOrgShouldKeepOrgCodeStable() {
        TeachOrgMapper mapper = mock(TeachOrgMapper.class);
        TeachOrgServiceImpl service = new TeachOrgServiceImpl(mapper);
        TeachOrg existing = buildValidTeachOrg();
        when(mapper.selectOne(any())).thenReturn(existing);
        TeachOrg update = new TeachOrg();
        update.setId("org_001");
        update.setParentId("org_parent");
        update.setOrgName("2026级实训一班");
        update.setOrgType("CLASS");

        TeachOrg saved = service.updateTeachOrg("tenant_001", update);

        assertEquals("class_2026_01", saved.getOrgCode());
        assertEquals("org_parent", saved.getParentId());
        assertEquals("2026级实训一班", saved.getOrgName());
        assertEquals("CLASS", saved.getOrgType());
        assertNotNull(saved.getUpdateTime());
        verify(mapper, times(1)).selectOne(any());
        verify(mapper, times(1)).updateById(saved);
    }

    /**
     * 校验单位状态切换会写回目标状态。
     */
    @Test
    void updateTeachOrgStatusShouldUpdateExistingOrg() {
        TeachOrgMapper mapper = mock(TeachOrgMapper.class);
        TeachOrgServiceImpl service = new TeachOrgServiceImpl(mapper);
        TeachOrg existing = buildValidTeachOrg();
        when(mapper.selectOne(any())).thenReturn(existing);

        TeachOrg saved = service.updateTeachOrgStatus("tenant_001", "org_001", "DISABLED");

        assertEquals("DISABLED", saved.getStatus());
        assertNotNull(saved.getUpdateTime());
        verify(mapper, times(1)).selectOne(any());
        verify(mapper, times(1)).updateById(saved);
    }

    /**
     * 构造最小有效教学组织。
     *
     * @return 教学组织实体。
     */
    private TeachOrg buildValidTeachOrg() {
        TeachOrg teachOrg = new TeachOrg();
        teachOrg.setId("org_001");
        teachOrg.setTenantId("tenant_001");
        teachOrg.setOrgCode("class_2026_01");
        teachOrg.setOrgName("2026级实训1班");
        teachOrg.setOrgType("CLASS");
        return teachOrg;
    }
}
