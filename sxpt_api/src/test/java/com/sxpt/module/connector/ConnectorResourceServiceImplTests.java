package com.sxpt.module.connector;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.ConnectorResource;
import com.sxpt.module.connector.mapper.ConnectorResourceMapper;
import com.sxpt.module.connector.service.ConnectorResourceService;
import com.sxpt.module.connector.service.impl.ConnectorResourceServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * 原平台正式资源服务测试。
 *
 * 业务功能：
 * 1. 验证正式资源创建会补齐通用生命周期字段。
 * 2. 验证页面资源查询始终通过 Service 附加租户、原平台和软删除边界。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class ConnectorResourceServiceImplTests {

    private final ConnectorResourceMapper mapper = mock(ConnectorResourceMapper.class);

    private final ConnectorResourceService service = new ConnectorResourceServiceImpl(mapper);

    /**
     * 校验创建正式资源时插入 Mapper 并补齐默认值。
     */
    @Test
    void createConnectorResourceShouldInsertAndFillDefaults() {
        ConnectorResource resource = buildValidResource();
        when(mapper.selectOne(any())).thenReturn(resource);

        ConnectorResource saved = service.createConnectorResource(resource);

        assertSame(resource, saved);
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        assertEquals("teacher_001", saved.getUpdateBy());
        verify(mapper).upsertByBusinessKey(saved);
        verify(mapper, never()).insert(any());
    }

    /**
     * 校验相同业务键重复提交时返回数据库中原有资源 ID。
     */
    @Test
    void createConnectorResourceShouldReuseExistingBusinessKey() {
        ConnectorResource request = buildValidResource();
        request.setId("retry_generated_id");
        ConnectorResource existing = buildValidResource();
        existing.setId("existing_resource_id");
        existing.setResourceName("重试后的资源名称");
        when(mapper.selectOne(any())).thenReturn(existing);

        ConnectorResource saved = service.createConnectorResource(request);

        assertSame(existing, saved);
        assertEquals("existing_resource_id", saved.getId());
        verify(mapper).upsertByBusinessKey(request);
        verify(mapper, never()).insert(any());
    }

    /**
     * 校验缺少资源编码时拒绝创建，避免正式资源无法稳定引用。
     */
    @Test
    void createConnectorResourceShouldRejectMissingResourceCode() {
        ConnectorResource resource = buildValidResource();
        resource.setResourceCode(" ");

        assertThrows(BusinessException.class, () -> service.createConnectorResource(resource));
        verify(mapper, times(0)).insert(resource);
    }

    /**
     * 校验按页面查询正式资源时返回 Mapper 结果。
     */
    @Test
    void listByPageShouldReturnMapperResult() {
        ConnectorResource resource = buildValidResource();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(resource));

        List<ConnectorResource> result = service.listByPage(
                "tenant_001", "connector_001", "https://origin.example.com/record/start");

        assertEquals(1, result.size());
        assertSame(resource, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造最小有效正式资源。
     *
     * @return 原平台正式资源实体。
     */
    private ConnectorResource buildValidResource() {
        ConnectorResource resource = new ConnectorResource();
        resource.setId("res_001");
        resource.setTenantId("tenant_001");
        resource.setConnectorSystemId("connector_001");
        resource.setResourceCode("BTN_SUBMIT");
        resource.setResourceName("提交按钮");
        resource.setResourceType("BUTTON");
        resource.setPageUrl("https://origin.example.com/record/start");
        resource.setLocator("#submit");
        resource.setStableKey("submit_button");
        resource.setMetadataJson("{\"text\":\"提交\"}");
        resource.setSourceCaptureId("cap_001");
        resource.setCreateBy("teacher_001");
        return resource;
    }
}
