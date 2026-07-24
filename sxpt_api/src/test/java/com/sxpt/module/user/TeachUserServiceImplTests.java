package com.sxpt.module.user;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.user.entity.TeachUser;
import com.sxpt.module.user.mapper.TeachUserMapper;
import com.sxpt.module.user.service.impl.TeachUserServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
