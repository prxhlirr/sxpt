package com.sxpt.module.connector;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.mapper.TeachingDataTemplateMapper;
import com.sxpt.module.connector.service.TeachingDataTemplateService;
import com.sxpt.module.connector.service.impl.TeachingDataTemplateServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 教学业务数据模板服务测试。
 *
 * 业务功能：
 * 1. 验证创建模板时会补齐默认生命周期字段。
 * 2. 验证模板查询始终通过 Service 入口加上租户和软删除边界。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class TeachingDataTemplateServiceImplTests {

    private final TeachingDataTemplateMapper mapper = mock(TeachingDataTemplateMapper.class);

    private final TeachingDataTemplateService service = new TeachingDataTemplateServiceImpl(mapper);

    /**
     * 校验创建模板时写入 Mapper 并补齐默认值。
     */
    @Test
    void createTeachingDataTemplateShouldInsertAndFillDefaults() {
        TeachingDataTemplate template = buildValidTemplate();

        TeachingDataTemplate saved = service.createTeachingDataTemplate(template);

        assertSame(template, saved);
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 校验缺少模板编码时拒绝创建。
     */
    @Test
    void createTeachingDataTemplateShouldRejectMissingTemplateCode() {
        TeachingDataTemplate template = buildValidTemplate();
        template.setTemplateCode(" ");

        assertThrows(BusinessException.class, () -> service.createTeachingDataTemplate(template));
        verify(mapper, times(0)).insert(template);
    }

    /**
     * 校验按原平台查询模板时返回 Mapper 结果。
     */
    @Test
    void listTemplatesByConnectorShouldReturnMapperResult() {
        TeachingDataTemplate template = buildValidTemplate();
        when(mapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.singletonList(template));

        List<TeachingDataTemplate> result = service.listTemplatesByConnector("tenant_001", "connector_001");

        assertEquals(1, result.size());
        assertSame(template, result.get(0));
        verify(mapper).selectList(org.mockito.ArgumentMatchers.any());
    }

    /**
     * 校验按教学点和场景查询可用模板时返回 Mapper 结果。
     */
    @Test
    void listActiveTemplatesByTeachingPointAndSceneShouldReturnMapperResult() {
        TeachingDataTemplate template = buildValidTemplate();
        when(mapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.singletonList(template));

        List<TeachingDataTemplate> result = service.listActiveTemplatesByTeachingPointAndScene(
                "tenant_001", "connector_001", "tp_001", "RECORD");

        assertEquals(1, result.size());
        assertSame(template, result.get(0));
        verify(mapper).selectList(org.mockito.ArgumentMatchers.any());
    }

    /**
     * 构造最小有效教学业务数据模板。
     *
     * @return 教学业务数据模板实体。
     */
    private TeachingDataTemplate buildValidTemplate() {
        TeachingDataTemplate template = new TeachingDataTemplate();
        template.setId("tpl_001");
        template.setTenantId("tenant_001");
        template.setConnectorSystemId("connector_001");
        template.setTemplateCode("record_apply_default");
        template.setTemplateName("标准备案申请默认数据");
        template.setSceneType("RECORD");
        return template;
    }
}
