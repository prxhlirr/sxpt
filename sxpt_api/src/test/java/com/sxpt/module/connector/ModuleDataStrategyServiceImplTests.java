package com.sxpt.module.connector;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.entity.ModuleDataStrategy;
import com.sxpt.module.connector.entity.PlatformCapability;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.mapper.BusinessModuleMapper;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.connector.mapper.ModuleDataStrategyMapper;
import com.sxpt.module.connector.mapper.PlatformCapabilityMapper;
import com.sxpt.module.connector.mapper.TeachingDataTemplateMapper;
import com.sxpt.module.connector.service.ModuleDataStrategyService;
import com.sxpt.module.connector.service.impl.ModuleDataStrategyServiceImpl;
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
 * 原平台业务模块数据准备策略服务测试。
 *
 * 业务功能：
 * 1. 验证策略创建、更新、启停和查询时的字段边界与生命周期规则。
 * 2. 验证启用策略前必须具备模板、来源、共享、重置、锁定和准备时机。
 *
 * 关键流程：
 * 1. 使用 Mockito 模拟 Mapper，避免单元测试依赖真实数据库。
 * 2. 直接调用 Service，聚焦策略默认停用、版本递增和启用校验。
 */
class ModuleDataStrategyServiceImplTests {

    private final ModuleDataStrategyMapper mapper = mock(ModuleDataStrategyMapper.class);

    private final BusinessModuleMapper businessModuleMapper = mock(BusinessModuleMapper.class);

    private final TeachingDataTemplateMapper teachingDataTemplateMapper = mock(TeachingDataTemplateMapper.class);

    private final PlatformCapabilityMapper platformCapabilityMapper = mock(PlatformCapabilityMapper.class);

    private final ConnectorSystemMapper connectorSystemMapper = mock(ConnectorSystemMapper.class);

    private final ModuleDataStrategyService service = new ModuleDataStrategyServiceImpl(
            mapper,
            businessModuleMapper,
            teachingDataTemplateMapper,
            platformCapabilityMapper,
            connectorSystemMapper);

    /**
     * 校验创建策略时写入 Mapper，并默认停用半成品策略。
     */
    @Test
    void createModuleDataStrategyShouldFillDefaultsAndInsert() {
        ModuleDataStrategy strategy = buildValidStrategy();

        ModuleDataStrategy saved = service.createModuleDataStrategy(strategy);

        assertSame(strategy, saved);
        assertEquals(RecordStatus.DISABLED.getValue(), saved.getStatus());
        assertEquals(Boolean.TRUE, saved.getNeedPreData());
        assertEquals(1L, saved.getStrategyVersion());
        assertEquals(0L, saved.getLockVersion());
        assertFalse(saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 校验缺少场景时拒绝创建，避免策略无法绑定备案、学习、练习或考试模式。
     */
    @Test
    void createModuleDataStrategyShouldRejectMissingSceneType() {
        ModuleDataStrategy strategy = buildValidStrategy();
        strategy.setSceneType(" ");

        assertThrows(BusinessException.class, () -> service.createModuleDataStrategy(strategy));
        verify(mapper, times(0)).insert(strategy);
    }

    /**
     * 校验共享策略不在系统枚举中时拒绝创建，避免运行时出现未知隔离策略。
     */
    @Test
    void createModuleDataStrategyShouldRejectUnknownSharePolicy() {
        ModuleDataStrategy strategy = buildValidStrategy();
        strategy.setSharePolicy("TENANT_SHARED");

        assertThrows(BusinessException.class, () -> service.createModuleDataStrategy(strategy));
        verify(mapper, times(0)).insert(strategy);
    }

    /**
     * 校验策略 JSON 配置不可解析时拒绝创建，避免策略启用后才暴露配置错误。
     */
    @Test
    void createModuleDataStrategyShouldRejectInvalidPolicyJson() {
        ModuleDataStrategy strategy = buildValidStrategy();
        strategy.setDefaultOrgRolePolicyJson("{invalid");

        assertThrows(BusinessException.class, () -> service.createModuleDataStrategy(strategy));
        verify(mapper, times(0)).insert(strategy);
    }

    /**
     * 校验更新策略时只覆盖规则字段并递增策略版本和乐观锁版本。
     */
    @Test
    void updateModuleDataStrategyShouldPatchEditableFieldsAndIncreaseVersions() {
        ModuleDataStrategy existing = buildValidStrategy();
        existing.setStrategyVersion(2L);
        existing.setLockVersion(5L);
        existing.setCreateTime(LocalDateTime.now().minusDays(1));
        when(mapper.selectOne(any())).thenReturn(existing);

        ModuleDataStrategy update = buildValidStrategy();
        update.setId("strategy_001");
        update.setModuleName("备案申请更新");
        update.setNeedPreData(Boolean.FALSE);
        update.setTemplateId("tpl_002");
        update.setPrepareTiming("ON_DEMAND");
        update.setSharePolicy("QUESTION_EXCLUSIVE");
        update.setUpdateBy("admin_002");

        ModuleDataStrategy result = service.updateModuleDataStrategy(update);

        assertEquals("tenant_001", result.getTenantId());
        assertEquals("connector_001", result.getConnectorSystemId());
        assertEquals("module_001", result.getBusinessModuleId());
        assertEquals("record_apply", result.getModuleCode());
        assertEquals("RECORD", result.getSceneType());
        assertEquals("备案申请更新", result.getModuleName());
        assertEquals(Boolean.FALSE, result.getNeedPreData());
        assertEquals("tpl_002", result.getTemplateId());
        assertEquals("ON_DEMAND", result.getPrepareTiming());
        assertEquals("QUESTION_EXCLUSIVE", result.getSharePolicy());
        assertEquals(3L, result.getStrategyVersion());
        assertEquals(6L, result.getLockVersion());
        verify(mapper).updateById(result);
    }

    /**
     * 校验缺少模板时拒绝启用策略，避免运行态无法创建原平台数据。
     */
    @Test
    void enableModuleDataStrategyShouldRejectMissingTemplateId() {
        ModuleDataStrategy existing = buildValidStrategy();
        existing.setTemplateId(" ");
        when(mapper.selectOne(any())).thenReturn(existing);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.enableModuleDataStrategy("strategy_001"));

        assertEquals(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(), exception.getCode());
        verify(mapper, times(0)).updateById(existing);
    }

    /**
     * 校验准备时机不在系统枚举中时拒绝启用，避免策略进入未知触发边界。
     */
    @Test
    void enableModuleDataStrategyShouldRejectUnknownPrepareTiming() {
        ModuleDataStrategy existing = buildValidStrategy();
        existing.setPrepareTiming("AFTER_FINISH");
        when(mapper.selectOne(any())).thenReturn(existing);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.enableModuleDataStrategy("strategy_001"));

        assertEquals(ApiResultCode.PARAM_ERROR.getCode(), exception.getCode());
        verify(mapper, times(0)).updateById(existing);
    }

    /**
     * 校验启用策略时必须具备默认单位角色策略，保证学生数据实例能映射到原平台办理身份。
     */
    @Test
    void enableModuleDataStrategyShouldRejectMissingDefaultOrgRolePolicy() {
        ModuleDataStrategy existing = buildValidStrategy();
        existing.setDefaultOrgRolePolicyJson(" ");
        when(mapper.selectOne(any())).thenReturn(existing);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.enableModuleDataStrategy("strategy_001"));

        assertEquals(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(), exception.getCode());
        verify(mapper, times(0)).updateById(existing);
    }

    /**
     * 校验策略池水位上下限，避免最小准备量大于最大准备量导致补数任务永远不可满足。
     */
    @Test
    void enableModuleDataStrategyShouldRejectInvalidPoolSizePolicy() {
        ModuleDataStrategy existing = buildValidStrategy();
        existing.setPoolSizePolicyJson("{\"minReadyCount\":20,\"maxReadyCount\":5}");
        when(mapper.selectOne(any())).thenReturn(existing);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.enableModuleDataStrategy("strategy_001"));

        assertEquals(ApiResultCode.PARAM_ERROR.getCode(), exception.getCode());
        verify(mapper, times(0)).updateById(existing);
    }

    /**
     * 校验业务模块不存在或未启用时拒绝启用策略，避免策略绑定到不可用业务入口。
     */
    @Test
    void enableModuleDataStrategyShouldRejectMissingBusinessModule() {
        ModuleDataStrategy existing = buildValidStrategy();
        when(mapper.selectOne(any())).thenReturn(existing);
        when(businessModuleMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.enableModuleDataStrategy("strategy_001"));

        assertEquals(ApiResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
        verify(mapper, times(0)).updateById(existing);
    }

    /**
     * 校验模板不存在或未启用时拒绝启用策略，避免运行态缺少造数请求结构。
     */
    @Test
    void enableModuleDataStrategyShouldRejectMissingTemplate() {
        ModuleDataStrategy existing = buildValidStrategy();
        when(mapper.selectOne(any())).thenReturn(existing);
        when(businessModuleMapper.selectOne(any())).thenReturn(buildActiveBusinessModule());
        when(teachingDataTemplateMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.enableModuleDataStrategy("strategy_001"));

        assertEquals(ApiResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
        verify(mapper, times(0)).updateById(existing);
    }

    /**
     * 校验考试策略不是题目独占时拒绝启用，避免考试题目串用原平台数据。
     */
    @Test
    void enableExamStrategyShouldRejectNonQuestionExclusiveSharePolicy() {
        ModuleDataStrategy existing = buildValidExamStrategy();
        existing.setSharePolicy("ATTEMPT_EXCLUSIVE");
        when(mapper.selectOne(any())).thenReturn(existing);
        when(businessModuleMapper.selectOne(any())).thenReturn(buildActiveBusinessModule());
        when(teachingDataTemplateMapper.selectOne(any())).thenReturn(buildActiveTemplate("EXAM"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.enableModuleDataStrategy("strategy_001"));

        assertEquals(ApiResultCode.PARAM_ERROR.getCode(), exception.getCode());
        verify(mapper, times(0)).updateById(existing);
    }

    /**
     * 校验考试策略不是开考锁定时拒绝启用，避免考试期间原平台数据被继续修改。
     */
    @Test
    void enableExamStrategyShouldRejectNonExamStartLockPolicy() {
        ModuleDataStrategy existing = buildValidExamStrategy();
        existing.setLockPolicy("ON_ALLOCATE");
        when(mapper.selectOne(any())).thenReturn(existing);
        when(businessModuleMapper.selectOne(any())).thenReturn(buildActiveBusinessModule());
        when(teachingDataTemplateMapper.selectOne(any())).thenReturn(buildActiveTemplate("EXAM"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.enableModuleDataStrategy("strategy_001"));

        assertEquals(ApiResultCode.PARAM_ERROR.getCode(), exception.getCode());
        verify(mapper, times(0)).updateById(existing);
    }

    /**
     * 校验考试策略缺少结果校验策略时拒绝启用，避免考试得分缺少原平台结果核验依据。
     */
    @Test
    void enableExamStrategyShouldRejectMissingResultCheckPolicy() {
        ModuleDataStrategy existing = buildValidExamStrategy();
        existing.setResultCheckPolicyJson(" ");
        when(mapper.selectOne(any())).thenReturn(existing);
        when(businessModuleMapper.selectOne(any())).thenReturn(buildActiveBusinessModule());
        when(teachingDataTemplateMapper.selectOne(any())).thenReturn(buildActiveTemplate());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.enableModuleDataStrategy("strategy_001"));

        assertEquals(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(), exception.getCode());
        verify(mapper, times(0)).updateById(existing);
    }

    /**
     * 校验原平台未注册数据创建能力时拒绝启用策略，避免运行时才发现无法生成业务数据。
     */
    @Test
    void enableModuleDataStrategyShouldRejectMissingDataCreateCapability() {
        ModuleDataStrategy existing = buildValidStrategy();
        when(mapper.selectOne(any())).thenReturn(existing);
        when(businessModuleMapper.selectOne(any())).thenReturn(buildActiveBusinessModule());
        when(teachingDataTemplateMapper.selectOne(any())).thenReturn(buildActiveTemplate());
        when(platformCapabilityMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.enableModuleDataStrategy("strategy_001"));

        assertEquals(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(), exception.getCode());
        verify(mapper, times(0)).updateById(existing);
    }

    /**
     * 校验锁定策略依赖原平台锁定能力，避免考试或强约束练习数据被并发修改。
     */
    @Test
    void enableModuleDataStrategyShouldRejectMissingLockCapability() {
        ModuleDataStrategy existing = buildValidStrategy();
        existing.setLockPolicy("ON_ALLOCATE");
        when(mapper.selectOne(any())).thenReturn(existing);
        when(businessModuleMapper.selectOne(any())).thenReturn(buildActiveBusinessModule());
        when(teachingDataTemplateMapper.selectOne(any())).thenReturn(buildActiveTemplate());
        when(platformCapabilityMapper.selectOne(any()))
                .thenReturn(buildSupportedCapability("DATA_CREATE"))
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.enableModuleDataStrategy("strategy_001"));

        assertEquals(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(), exception.getCode());
        verify(mapper, times(0)).updateById(existing);
    }

    /**
     * 校验结果校验策略依赖原平台结果校验能力，避免考试评分缺少原平台侧依据。
     */
    @Test
    void enableExamStrategyShouldRejectMissingResultCheckCapability() {
        ModuleDataStrategy existing = buildValidExamStrategy();
        when(mapper.selectOne(any())).thenReturn(existing);
        when(businessModuleMapper.selectOne(any())).thenReturn(buildActiveBusinessModule());
        when(teachingDataTemplateMapper.selectOne(any())).thenReturn(buildActiveTemplate("EXAM"));
        when(platformCapabilityMapper.selectOne(any()))
                .thenReturn(buildSupportedCapability("DATA_CREATE"))
                .thenReturn(buildSupportedCapability("DATA_LOCK"))
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.enableModuleDataStrategy("strategy_001"));

        assertEquals(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(), exception.getCode());
        verify(mapper, times(0)).updateById(existing);
    }

    /**
     * 校验考试策略满足题目独占、开考锁定和结果校验时允许启用。
     */
    @Test
    void enableExamStrategyShouldSetActiveStatusWhenPolicyComplete() {
        ModuleDataStrategy existing = buildValidExamStrategy();
        existing.setStatus(RecordStatus.DISABLED.getValue());
        existing.setLockVersion(1L);
        when(mapper.selectOne(any())).thenReturn(existing);
        when(businessModuleMapper.selectOne(any())).thenReturn(buildActiveBusinessModule());
        when(teachingDataTemplateMapper.selectOne(any())).thenReturn(buildActiveTemplate("EXAM"));
        when(platformCapabilityMapper.selectOne(any()))
                .thenReturn(buildSupportedCapability("DATA_CREATE"))
                .thenReturn(buildSupportedCapability("DATA_LOCK"))
                .thenReturn(buildSupportedCapability("RESULT_CHECK"));

        ModuleDataStrategy result = service.enableModuleDataStrategy("strategy_001");

        assertEquals(RecordStatus.ACTIVE.getValue(), result.getStatus());
        assertEquals(2L, result.getLockVersion());
        verify(mapper).updateById(result);
    }

    /**
     * 校验策略完整时可启用并递增乐观锁版本。
     */
    @Test
    void enableModuleDataStrategyShouldSetActiveStatus() {
        ModuleDataStrategy existing = buildValidStrategy();
        existing.setStatus(RecordStatus.DISABLED.getValue());
        existing.setLockVersion(0L);
        when(mapper.selectOne(any())).thenReturn(existing);
        when(businessModuleMapper.selectOne(any())).thenReturn(buildActiveBusinessModule());
        when(teachingDataTemplateMapper.selectOne(any())).thenReturn(buildActiveTemplate());
        when(platformCapabilityMapper.selectOne(any())).thenReturn(buildSupportedCapability("DATA_CREATE"));

        ModuleDataStrategy result = service.enableModuleDataStrategy("strategy_001");

        assertEquals(RecordStatus.ACTIVE.getValue(), result.getStatus());
        assertEquals(1L, result.getLockVersion());
        verify(mapper).updateById(result);
    }

    /**
     * 校验禁用策略时写入 DISABLED 状态。
     */
    @Test
    void disableModuleDataStrategyShouldSetDisabledStatus() {
        ModuleDataStrategy existing = buildValidStrategy();
        existing.setStatus(RecordStatus.ACTIVE.getValue());
        existing.setLockVersion(2L);
        when(mapper.selectOne(any())).thenReturn(existing);

        ModuleDataStrategy result = service.disableModuleDataStrategy("strategy_001");

        assertEquals(RecordStatus.DISABLED.getValue(), result.getStatus());
        assertEquals(3L, result.getLockVersion());
        verify(mapper).updateById(result);
    }

    /**
     * 校验按业务模块查询策略列表时返回 Mapper 结果。
     */
    @Test
    void listStrategiesByBusinessModuleShouldReturnMapperResult() {
        ModuleDataStrategy strategy = buildValidStrategy();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(strategy));

        List<ModuleDataStrategy> result = service.listStrategiesByBusinessModule(
                "tenant_001", "connector_001", "module_001");

        assertEquals(1, result.size());
        assertSame(strategy, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 校验按模块编码和场景查询启用策略时返回单条策略。
     */
    @Test
    void getActiveStrategyByModuleCodeAndSceneShouldReturnMapperResult() {
        ModuleDataStrategy strategy = buildValidStrategy();
        when(mapper.selectOne(any())).thenReturn(strategy);

        ModuleDataStrategy result = service.getActiveStrategyByModuleCodeAndScene(
                "tenant_001", "connector_001", "record_apply", "RECORD");

        assertSame(strategy, result);
        verify(mapper).selectOne(any());
    }

    /**
     * 构造最小有效数据准备策略。
     *
     * @return 原平台业务模块数据准备策略实体。
     */
    private ModuleDataStrategy buildValidStrategy() {
        ModuleDataStrategy strategy = new ModuleDataStrategy();
        strategy.setId("strategy_001");
        strategy.setTenantId("tenant_001");
        strategy.setConnectorSystemId("connector_001");
        strategy.setBusinessModuleId("module_001");
        strategy.setModuleCode("record_apply");
        strategy.setModuleName("备案申请");
        strategy.setSceneType("RECORD");
        strategy.setDataSourceStrategy("MOCK_GENERATE");
        strategy.setSharePolicy("ATTEMPT_EXCLUSIVE");
        strategy.setRegeneratePolicy("ON_ATTEMPT");
        strategy.setLockPolicy("NONE");
        strategy.setTemplateId("tpl_001");
        strategy.setPrepareTiming("ON_PUBLISH");
        strategy.setDefaultOrgRolePolicyJson("{\"org\":\"required\",\"role\":\"required\"}");
        strategy.setPoolSizePolicyJson("{\"minReadyCount\":1,\"maxReadyCount\":50}");
        strategy.setValidationPolicyJson("{\"requiredStatus\":\"DRAFT\"}");
        strategy.setCreateBy("admin_001");
        strategy.setUpdateBy("admin_001");
        return strategy;
    }

    /**
     * 构造最小有效考试数据准备策略。
     *
     * @return 原平台业务模块考试数据准备策略实体。
     */
    private ModuleDataStrategy buildValidExamStrategy() {
        ModuleDataStrategy strategy = buildValidStrategy();
        strategy.setSceneType("EXAM");
        strategy.setSharePolicy("QUESTION_EXCLUSIVE");
        strategy.setLockPolicy("ON_EXAM_START");
        strategy.setResultCheckPolicyJson("{\"required\":true}");
        return strategy;
    }

    /**
     * 构造启用状态的业务模块。
     *
     * @return 原平台业务模块实体。
     */
    private BusinessModule buildActiveBusinessModule() {
        BusinessModule businessModule = new BusinessModule();
        businessModule.setId("module_001");
        businessModule.setTenantId("tenant_001");
        businessModule.setConnectorSystemId("connector_001");
        businessModule.setModuleCode("record_apply");
        businessModule.setStatus(RecordStatus.ACTIVE.getValue());
        businessModule.setDeleted(Boolean.FALSE);
        return businessModule;
    }

    /**
     * 构造备案场景启用状态的数据模板。
     *
     * @return 教学业务数据模板实体。
     */
    private TeachingDataTemplate buildActiveTemplate() {
        return buildActiveTemplate("RECORD");
    }

    /**
     * 构造指定场景启用状态的数据模板。
     *
     * @param sceneType 场景类型。
     * @return 教学业务数据模板实体。
     */
    private TeachingDataTemplate buildActiveTemplate(String sceneType) {
        TeachingDataTemplate template = new TeachingDataTemplate();
        template.setId("tpl_001");
        template.setTenantId("tenant_001");
        template.setConnectorSystemId("connector_001");
        template.setSceneType(sceneType);
        template.setStatus(RecordStatus.ACTIVE.getValue());
        template.setDeleted(Boolean.FALSE);
        return template;
    }

    /**
     * 构造已启用且声明支持的原平台能力。
     *
     * @param capabilityCode 原平台能力编码。
     * @return 原平台能力注册实体。
     */
    private PlatformCapability buildSupportedCapability(String capabilityCode) {
        PlatformCapability capability = new PlatformCapability();
        capability.setId("capability_" + capabilityCode);
        capability.setTenantId("tenant_001");
        capability.setConnectorSystemId("connector_001");
        capability.setCapabilityCode(capabilityCode);
        capability.setSupportFlag(Boolean.TRUE);
        capability.setStatus(RecordStatus.ACTIVE.getValue());
        capability.setDeleted(Boolean.FALSE);
        return capability;
    }
}
