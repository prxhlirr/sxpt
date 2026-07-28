package com.sxpt.module.connector;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.mapper.BusinessModuleMapper;
import com.sxpt.module.connector.service.BusinessModuleService;
import com.sxpt.module.connector.service.impl.BusinessModuleServiceImpl;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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
 * 原平台业务模块管理服务测试。
 *
 * 业务功能：
 * 1. 验证业务模块创建、更新、启停和查询时的租户边界与生命周期字段。
 * 2. 确认业务模块主数据可作为后续数据准备策略和原平台跳转上下文的稳定入口。
 *
 * 关键流程：
 * 1. 使用 Mockito 模拟 Mapper，避免单元测试依赖真实数据库。
 * 2. 直接调用 Service，聚焦必填字段、默认值、状态切换和版本递增规则。
 */
class BusinessModuleServiceImplTests {

    private final BusinessModuleMapper mapper = mock(BusinessModuleMapper.class);

    private final BusinessModuleService service = new BusinessModuleServiceImpl(mapper);

    /**
     * 校验创建业务模块时写入 Mapper 并补齐默认值。
     */
    @Test
    void createBusinessModuleShouldFillDefaultsAndInsert() {
        BusinessModule businessModule = buildValidBusinessModule();

        BusinessModule saved = service.createBusinessModule(businessModule);

        assertSame(businessModule, saved);
        assertEquals(RecordStatus.ACTIVE.getValue(), saved.getStatus());
        assertEquals(0L, saved.getLockVersion());
        assertEquals(Boolean.TRUE, saved.getNeedPreData());
        assertFalse(saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        assertEquals("admin_001", saved.getCreateBy());
        assertEquals("admin_001", saved.getUpdateBy());
        verify(mapper).insert(saved);
    }

    /**
     * 校验缺少支持场景时拒绝创建，避免后续策略无法判断模式范围。
     */
    @Test
    void createBusinessModuleShouldRejectMissingSupportScenes() {
        BusinessModule businessModule = buildValidBusinessModule();
        businessModule.setSupportScenes(" ");

        assertThrows(BusinessException.class, () -> service.createBusinessModule(businessModule));
        verify(mapper, times(0)).insert(businessModule);
    }

    /**
     * 校验更新业务模块时只覆盖可编辑字段并递增版本。
     */
    @Test
    void updateBusinessModuleShouldPatchEditableFieldsAndIncreaseLockVersion() {
        BusinessModule existing = buildValidBusinessModule();
        existing.setLockVersion(3L);
        existing.setCreateTime(LocalDateTime.now().minusDays(1));
        when(mapper.selectOne(any())).thenReturn(existing);

        BusinessModule update = new BusinessModule();
        update.setId("module_001");
        update.setModuleName("备案申请");
        update.setExternalModuleId("origin_record_apply");
        update.setEntryUrl("/record/apply");
        update.setModuleType("BUSINESS");
        update.setSupportScenes("[\"RECORD\",\"PRACTICE\",\"EXAM\"]");
        update.setNeedPreData(Boolean.FALSE);
        update.setDefaultInitialStatus("DRAFT");
        update.setDefaultTargetStatus("SUBMITTED");
        update.setCapabilityCodesJson("[\"CREATE\",\"SUBMIT\"]");
        update.setDefaultTemplateId("tpl_001");
        update.setRemark("用于备案申请教学");
        update.setUpdateBy("admin_001");

        BusinessModule result = service.updateBusinessModule(update);

        assertEquals("tenant_001", result.getTenantId());
        assertEquals("connector_001", result.getConnectorSystemId());
        assertEquals("record_apply", result.getModuleCode());
        assertEquals("备案申请", result.getModuleName());
        assertEquals("origin_record_apply", result.getExternalModuleId());
        assertEquals("/record/apply", result.getEntryUrl());
        assertEquals(Boolean.FALSE, result.getNeedPreData());
        assertEquals(4L, result.getLockVersion());
        assertNotNull(result.getUpdateTime());
        verify(mapper).updateById(result);
    }

    /**
     * 校验禁用业务模块时写入 DISABLED 状态并递增版本。
     */
    @Test
    void disableBusinessModuleShouldSetDisabledStatus() {
        BusinessModule existing = buildValidBusinessModule();
        existing.setStatus(RecordStatus.ACTIVE.getValue());
        existing.setLockVersion(0L);
        when(mapper.selectOne(any())).thenReturn(existing);

        BusinessModule result = service.disableBusinessModule("module_001");

        assertEquals(RecordStatus.DISABLED.getValue(), result.getStatus());
        assertEquals(1L, result.getLockVersion());
        assertNotNull(result.getUpdateTime());
        verify(mapper).updateById(result);
    }

    /**
     * 校验启用业务模块时写入 ACTIVE 状态并递增版本。
     */
    @Test
    void enableBusinessModuleShouldSetActiveStatus() {
        BusinessModule existing = buildValidBusinessModule();
        existing.setStatus(RecordStatus.DISABLED.getValue());
        existing.setLockVersion(2L);
        when(mapper.selectOne(any())).thenReturn(existing);

        BusinessModule result = service.enableBusinessModule("module_001");

        assertEquals(RecordStatus.ACTIVE.getValue(), result.getStatus());
        assertEquals(3L, result.getLockVersion());
        verify(mapper).updateById(result);
    }

    /**
     * 校验根据 ID 查询不到业务模块时返回业务异常。
     */
    @Test
    void getBusinessModuleByIdShouldRejectMissingRecord() {
        when(mapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getBusinessModuleById("module_missing"));
        verify(mapper).selectOne(any());
    }

    /**
     * 校验按租户和原平台查询业务模块时返回 Mapper 结果。
     */
    @Test
    void listBusinessModulesShouldReturnMapperResult() {
        BusinessModule businessModule = buildValidBusinessModule();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(businessModule));

        List<BusinessModule> result = service.listBusinessModules("tenant_001", "connector_001");

        assertEquals(1, result.size());
        assertSame(businessModule, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 校验按租户和原平台查询启用业务模块时返回 Mapper 结果。
     */
    @Test
    void listActiveBusinessModulesShouldReturnMapperResult() {
        BusinessModule businessModule = buildValidBusinessModule();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(businessModule));

        List<BusinessModule> result = service.listActiveBusinessModules("tenant_001", "connector_001");

        assertEquals(1, result.size());
        assertSame(businessModule, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造最小有效业务模块。
     *
     * @return 原平台业务模块实体。
     */
    private BusinessModule buildValidBusinessModule() {
        BusinessModule businessModule = new BusinessModule();
        businessModule.setId("module_001");
        businessModule.setTenantId("tenant_001");
        businessModule.setConnectorSystemId("connector_001");
        businessModule.setModuleCode("record_apply");
        businessModule.setModuleName("备案申请");
        businessModule.setEntryUrl("/record/apply");
        businessModule.setSupportScenes("[\"RECORD\",\"LEARNING\",\"PRACTICE\",\"EXAM\"]");
        businessModule.setUpdateBy("admin_001");
        return businessModule;
    }
}
