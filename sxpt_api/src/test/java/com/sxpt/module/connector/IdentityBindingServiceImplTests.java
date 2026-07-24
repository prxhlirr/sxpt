package com.sxpt.module.connector;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.IdentityBinding;
import com.sxpt.module.connector.mapper.IdentityBindingMapper;
import com.sxpt.module.connector.service.impl.IdentityBindingServiceImpl;
import org.junit.jupiter.api.Test;

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
 * 原平台身份绑定服务测试。
 *
 * 业务功能：
 * 1. 验证创建身份绑定时会补齐默认生命周期字段。
 * 2. 验证查询指定教学用户和原平台的有效绑定。
 *
 * 关键流程：
 * 1. 使用 Mockito 模拟 Mapper，避免单元测试依赖真实数据库。
 * 2. 直接调用 Service，聚焦身份绑定的业务规则和持久化边界。
 */
class IdentityBindingServiceImplTests {

    /**
     * 校验创建身份绑定时写入 Mapper 并补齐默认值。
     */
    @Test
    void createIdentityBindingShouldFillDefaultsAndInsert() {
        IdentityBindingMapper mapper = mock(IdentityBindingMapper.class);
        IdentityBindingServiceImpl service = new IdentityBindingServiceImpl(mapper);
        IdentityBinding identityBinding = buildValidIdentityBinding();

        IdentityBinding saved = service.createIdentityBinding(identityBinding);

        assertEquals("ACTIVE", saved.getStatus());
        assertFalse(saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper, times(1)).insert(saved);
    }

    /**
     * 校验缺少外部用户 ID 时拒绝创建。
     */
    @Test
    void createIdentityBindingShouldRejectMissingExternalUserId() {
        IdentityBindingMapper mapper = mock(IdentityBindingMapper.class);
        IdentityBindingServiceImpl service = new IdentityBindingServiceImpl(mapper);
        IdentityBinding identityBinding = buildValidIdentityBinding();
        identityBinding.setExternalUserId(" ");

        assertThrows(BusinessException.class, () -> service.createIdentityBinding(identityBinding));
        verify(mapper, times(0)).insert(identityBinding);
    }

    /**
     * 校验按教学用户和原平台查询身份绑定时返回 Mapper 结果。
     */
    @Test
    void getBindingByUserAndConnectorShouldReturnExistingBinding() {
        IdentityBindingMapper mapper = mock(IdentityBindingMapper.class);
        IdentityBindingServiceImpl service = new IdentityBindingServiceImpl(mapper);
        IdentityBinding identityBinding = buildValidIdentityBinding();
        when(mapper.selectOne(any())).thenReturn(identityBinding);

        IdentityBinding result = service.getBindingByUserAndConnector("tenant_001", "user_001", "connector_001");

        assertSame(identityBinding, result);
        verify(mapper, times(1)).selectOne(any());
    }

    /**
     * 校验查询不到身份绑定时返回业务异常。
     */
    @Test
    void getBindingByUserAndConnectorShouldRejectMissingBinding() {
        IdentityBindingMapper mapper = mock(IdentityBindingMapper.class);
        IdentityBindingServiceImpl service = new IdentityBindingServiceImpl(mapper);
        when(mapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> service.getBindingByUserAndConnector("tenant_001", "user_001", "connector_001"));
        verify(mapper, times(1)).selectOne(any());
    }

    /**
     * 构造最小有效原平台身份绑定。
     *
     * @return 原平台身份绑定实体。
     */
    private IdentityBinding buildValidIdentityBinding() {
        IdentityBinding identityBinding = new IdentityBinding();
        identityBinding.setId("binding_001");
        identityBinding.setTenantId("tenant_001");
        identityBinding.setUserId("user_001");
        identityBinding.setConnectorSystemId("connector_001");
        identityBinding.setExternalUserId("origin_user_001");
        identityBinding.setBindingType("TRAINING");
        return identityBinding;
    }
}
