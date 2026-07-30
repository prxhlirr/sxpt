package com.sxpt.module.teachingdata;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.entity.ModuleDataStrategy;
import com.sxpt.module.connector.mapper.BusinessModuleMapper;
import com.sxpt.module.connector.service.ModuleDataStrategyService;
import com.sxpt.module.teachingdata.entity.DataRequirement;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RequirementStatus;
import com.sxpt.module.teachingdata.mapper.DataRequirementMapper;
import com.sxpt.module.teachingdata.service.DataRequirementService;
import com.sxpt.module.teachingdata.service.impl.DataRequirementServiceImpl;
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
import static org.mockito.Mockito.when;

/**
 * 数据需求批次服务测试。
 *
 * 业务功能：
 * 1. 验证创建数据需求批次时会补齐状态、计数、软删除和乐观锁默认值。
 * 2. 验证任务场景查询通过 Service 入口追加租户和软删除边界。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class DataRequirementServiceImplTests {

    private final DataRequirementMapper mapper = mock(DataRequirementMapper.class);

    private final BusinessModuleMapper businessModuleMapper = mock(BusinessModuleMapper.class);

    private final ModuleDataStrategyService moduleDataStrategyService = mock(ModuleDataStrategyService.class);

    private final DataRequirementService service = new DataRequirementServiceImpl(
            mapper, businessModuleMapper, moduleDataStrategyService);

    /**
     * 校验创建数据需求批次时写入 Mapper 并补齐默认值。
     */
    @Test
    void createDataRequirementShouldInsertAndFillDefaults() {
        DataRequirement requirement = buildValidRequirement();
        when(businessModuleMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(buildBusinessModule());
        when(moduleDataStrategyService.getActiveStrategyByModuleCodeAndScene(
                "tenant_001", "connector_001", "BUSINESS_APPLY", "PRACTICE"))
                .thenReturn(buildStrategy());

        DataRequirement saved = service.createDataRequirement(requirement);

        assertSame(requirement, saved);
        assertEquals(RequirementStatus.CREATED.getValue(), saved.getRequirementStatus());
        assertEquals(0L, saved.getExpectedCount());
        assertEquals(0L, saved.getSuccessCount());
        assertEquals(0L, saved.getFailedCount());
        assertEquals(0L, saved.getLockVersion());
        assertEquals(RecordStatus.ACTIVE.getValue(), saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 校验缺少场景时拒绝创建，避免数据需求无法匹配备案、练习或考试策略。
     */
    @Test
    void createDataRequirementShouldRejectMissingSceneType() {
        DataRequirement requirement = buildValidRequirement();
        requirement.setSceneType(" ");

        assertThrows(BusinessException.class, () -> service.createDataRequirement(requirement));
        verify(mapper, times(0)).insert(requirement);
    }

    /**
     * 校验按任务和场景查询数据需求批次时返回 Mapper 结果。
     */
    @Test
    void listByTaskAndSceneShouldReturnMapperResult() {
        DataRequirement requirement = buildValidRequirement();
        when(mapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.singletonList(requirement));

        List<DataRequirement> result = service.listByTaskAndScene("tenant_001", "task_001", "PRACTICE");

        assertEquals(1, result.size());
        assertSame(requirement, result.get(0));
        verify(mapper).selectList(org.mockito.ArgumentMatchers.any());
    }

    /**
     * 验证批次策略快照为空时会写入发布快照，确保 job 重试或证据查看不依赖单个执行任务。
     */
    @Test
    void freezeRequirementPolicySnapshotShouldFillEmptySnapshot() {
        DataRequirement requirement = buildValidRequirement();
        when(mapper.selectById("req_001")).thenReturn(requirement);

        DataRequirement result = service.freezeRequirementPolicySnapshot(
                "tenant_001", "req_001", "{\"snapshot\":\"published\"}");

        assertSame(requirement, result);
        assertEquals("{\"snapshot\":\"published\"}", result.getRequirementPolicyJson());
        verify(mapper).updateById(requirement);
    }

    /**
     * 验证已经冻结的批次策略不会被后续触发覆盖，避免历史证据被新配置污染。
     */
    @Test
    void freezeRequirementPolicySnapshotShouldKeepExistingSnapshot() {
        DataRequirement requirement = buildValidRequirement();
        requirement.setRequirementPolicyJson("{\"snapshot\":\"origin\"}");
        when(mapper.selectById("req_001")).thenReturn(requirement);

        DataRequirement result = service.freezeRequirementPolicySnapshot(
                "tenant_001", "req_001", "{\"snapshot\":\"new\"}");

        assertSame(requirement, result);
        assertEquals("{\"snapshot\":\"origin\"}", result.getRequirementPolicyJson());
        verify(mapper, times(0)).updateById(any(DataRequirement.class));
    }

    /**
     * 构造最小有效数据需求批次。
     *
     * @return 数据需求批次实体。
     */
    private DataRequirement buildValidRequirement() {
        DataRequirement requirement = new DataRequirement();
        requirement.setId("req_001");
        requirement.setTenantId("tenant_001");
        requirement.setRequirementCode("REQ-PRACTICE-001");
        requirement.setConnectorSystemId("connector_001");
        requirement.setBusinessModuleId("module_001");
        requirement.setModuleCode("BUSINESS_APPLY");
        requirement.setSceneType("PRACTICE");
        requirement.setTaskId("task_001");
        return requirement;
    }

    /**
     * 构造已启用业务模块，保证需求批次来源于明确的模块边界。
     *
     * @return 业务模块。
     */
    private BusinessModule buildBusinessModule() {
        BusinessModule businessModule = new BusinessModule();
        businessModule.setId("module_001");
        businessModule.setModuleCode("BUSINESS_APPLY");
        return businessModule;
    }

    /**
     * 构造已启用策略，保证需求批次能沉淀模板和校验策略来源。
     *
     * @return 数据准备策略。
     */
    private ModuleDataStrategy buildStrategy() {
        ModuleDataStrategy strategy = new ModuleDataStrategy();
        strategy.setId("strategy_001");
        strategy.setBusinessModuleId("module_001");
        strategy.setTemplateId("template_001");
        strategy.setValidationPolicyJson("{\"required\":true}");
        return strategy;
    }
}
