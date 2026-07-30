package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.entity.ModuleDataStrategy;
import com.sxpt.module.connector.entity.PlatformCapability;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.mapper.BusinessModuleMapper;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.connector.mapper.ModuleDataStrategyMapper;
import com.sxpt.module.connector.mapper.PlatformCapabilityMapper;
import com.sxpt.module.connector.mapper.TeachingDataTemplateMapper;
import com.sxpt.module.connector.service.ModuleDataStrategyService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.StrategyLockPolicy;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.StrategyPrepareTiming;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.StrategyRegeneratePolicy;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.StrategySceneType;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.StrategySharePolicy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 原平台业务模块数据准备策略服务实现。
 *
 * 业务功能：
 * 1. 为后台按业务模块和场景配置数据准备策略提供统一业务入口。
 * 2. 为运行态按模块和场景读取启用策略提供稳定查询能力。
 *
 * 关键流程：
 * 1. 创建时校验策略身份字段和最小规则字段，补齐版本、状态、软删除和时间字段。
 * 2. 更新时先读取现有策略，再覆盖规则字段并递增版本，防止策略身份边界被误改。
 * 3. 启用时强校验运行必备策略字段，避免不完整策略参与数据准备。
 */
@Service
@Profile("!test")
public class ModuleDataStrategyServiceImpl implements ModuleDataStrategyService {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final String CAPABILITY_DATA_CREATE = "DATA_CREATE";

    private static final String CAPABILITY_DATA_LOCK = "DATA_LOCK";

    private static final String CAPABILITY_RESULT_CHECK = "RESULT_CHECK";

    private static final String CAPABILITY_DATA_ARCHIVE = "DATA_ARCHIVE";

    private static final String LOCAL_DEV_SYSTEM_TYPE = "LOCAL_DEV";

    private final ModuleDataStrategyMapper moduleDataStrategyMapper;

    private final BusinessModuleMapper businessModuleMapper;

    private final TeachingDataTemplateMapper teachingDataTemplateMapper;

    private final PlatformCapabilityMapper platformCapabilityMapper;

    private final ConnectorSystemMapper connectorSystemMapper;

    public ModuleDataStrategyServiceImpl(ModuleDataStrategyMapper moduleDataStrategyMapper,
                                         BusinessModuleMapper businessModuleMapper,
                                         TeachingDataTemplateMapper teachingDataTemplateMapper,
                                         PlatformCapabilityMapper platformCapabilityMapper,
                                         ConnectorSystemMapper connectorSystemMapper) {
        this.moduleDataStrategyMapper = moduleDataStrategyMapper;
        this.businessModuleMapper = businessModuleMapper;
        this.teachingDataTemplateMapper = teachingDataTemplateMapper;
        this.platformCapabilityMapper = platformCapabilityMapper;
        this.connectorSystemMapper = connectorSystemMapper;
    }

    /**
     * 创建数据准备策略。
     *
     * @param strategy 数据准备策略实体。
     * @return 已保存的策略实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModuleDataStrategy createModuleDataStrategy(ModuleDataStrategy strategy) {
        validateCreateFields(strategy);
        validateStrategyJsonFields(strategy, false);
        fillCreateDefaults(strategy);
        moduleDataStrategyMapper.insert(strategy);
        return strategy;
    }

    /**
     * 更新数据准备策略。
     *
     * @param strategy 数据准备策略实体。
     * @return 已更新的策略实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModuleDataStrategy updateModuleDataStrategy(ModuleDataStrategy strategy) {
        validateUpdateFields(strategy);
        validateStrategyJsonFields(strategy, false);
        ModuleDataStrategy existing = getModuleDataStrategyById(strategy.getId());
        existing.setModuleName(strategy.getModuleName());
        existing.setNeedPreData(strategy.getNeedPreData());
        existing.setDataSourceStrategy(strategy.getDataSourceStrategy());
        existing.setInitExternalStatus(strategy.getInitExternalStatus());
        existing.setTargetExternalStatus(strategy.getTargetExternalStatus());
        existing.setDefaultOrgRolePolicyJson(strategy.getDefaultOrgRolePolicyJson());
        existing.setSharePolicy(strategy.getSharePolicy());
        existing.setRegeneratePolicy(strategy.getRegeneratePolicy());
        existing.setLockPolicy(strategy.getLockPolicy());
        existing.setExpirePolicyJson(strategy.getExpirePolicyJson());
        existing.setResultCheckPolicyJson(strategy.getResultCheckPolicyJson());
        existing.setStrategyCode(strategy.getStrategyCode());
        existing.setTemplateId(strategy.getTemplateId());
        existing.setPrepareTiming(strategy.getPrepareTiming());
        existing.setPoolSizePolicyJson(strategy.getPoolSizePolicyJson());
        existing.setValidationPolicyJson(strategy.getValidationPolicyJson());
        existing.setArchivePolicyJson(strategy.getArchivePolicyJson());
        existing.setUpdateBy(strategy.getUpdateBy());
        existing.setUpdateTime(LocalDateTime.now());
        existing.setStrategyVersion(nextVersion(existing.getStrategyVersion()));
        existing.setLockVersion(nextVersion(existing.getLockVersion()));
        moduleDataStrategyMapper.updateById(existing);
        return existing;
    }

    /**
     * 启用数据准备策略。
     *
     * @param id 策略 ID。
     * @return 已启用的策略实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModuleDataStrategy enableModuleDataStrategy(String id) {
        ModuleDataStrategy existing = getModuleDataStrategyById(id);
        validateEnableFields(existing);
        existing.setStatus(RecordStatus.ACTIVE.getValue());
        existing.setUpdateTime(LocalDateTime.now());
        existing.setLockVersion(nextVersion(existing.getLockVersion()));
        moduleDataStrategyMapper.updateById(existing);
        return existing;
    }

    /**
     * 禁用数据准备策略。
     *
     * @param id 策略 ID。
     * @return 已禁用的策略实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModuleDataStrategy disableModuleDataStrategy(String id) {
        ModuleDataStrategy existing = getModuleDataStrategyById(id);
        existing.setStatus(RecordStatus.DISABLED.getValue());
        existing.setUpdateTime(LocalDateTime.now());
        existing.setLockVersion(nextVersion(existing.getLockVersion()));
        moduleDataStrategyMapper.updateById(existing);
        return existing;
    }

    /**
     * 查询数据准备策略详情。
     *
     * @param id 策略 ID。
     * @return 未删除的策略实体。
     */
    @Override
    public ModuleDataStrategy getModuleDataStrategyById(String id) {
        requireText(id);
        ModuleDataStrategy strategy = moduleDataStrategyMapper.selectOne(new QueryWrapper<ModuleDataStrategy>()
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (strategy == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return strategy;
    }

    /**
     * 查询某业务模块下的全部未删除策略。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param businessModuleId 业务模块 ID。
     * @return 策略列表。
     */
    @Override
    public List<ModuleDataStrategy> listStrategiesByBusinessModule(String tenantId,
                                                                   String connectorSystemId,
                                                                   String businessModuleId) {
        requireText(tenantId);
        requireText(connectorSystemId);
        requireText(businessModuleId);
        return moduleDataStrategyMapper.selectList(new QueryWrapper<ModuleDataStrategy>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("business_module_id", businessModuleId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 查询某业务模块下的启用策略。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param businessModuleId 业务模块 ID。
     * @return 启用策略列表。
     */
    @Override
    public List<ModuleDataStrategy> listActiveStrategiesByBusinessModule(String tenantId,
                                                                         String connectorSystemId,
                                                                         String businessModuleId) {
        requireText(tenantId);
        requireText(connectorSystemId);
        requireText(businessModuleId);
        return moduleDataStrategyMapper.selectList(new QueryWrapper<ModuleDataStrategy>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("business_module_id", businessModuleId)
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 查询某模块编码和场景对应的启用策略。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param moduleCode 业务模块编码。
     * @param sceneType 教学场景。
     * @return 启用策略实体。
     */
    @Override
    public ModuleDataStrategy getActiveStrategyByModuleCodeAndScene(String tenantId,
                                                                    String connectorSystemId,
                                                                    String moduleCode,
                                                                    String sceneType) {
        requireText(tenantId);
        requireText(connectorSystemId);
        requireText(moduleCode);
        requireText(sceneType);
        ModuleDataStrategy strategy = moduleDataStrategyMapper.selectOne(new QueryWrapper<ModuleDataStrategy>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("module_code", moduleCode)
                .eq("scene_type", sceneType)
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE));
        if (strategy == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return strategy;
    }

    /**
     * 校验创建数据策略所需的最小字段。
     *
     * @param strategy 数据准备策略实体。
     */
    private void validateCreateFields(ModuleDataStrategy strategy) {
        if (strategy == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(strategy.getId());
        requireText(strategy.getTenantId());
        requireText(strategy.getConnectorSystemId());
        requireText(strategy.getBusinessModuleId());
        requireText(strategy.getModuleCode());
        requireText(strategy.getModuleName());
        requireText(strategy.getSceneType());
        requireText(strategy.getDataSourceStrategy());
        requireText(strategy.getSharePolicy());
        requireText(strategy.getRegeneratePolicy());
        requireText(strategy.getLockPolicy());
        requireText(strategy.getCreateBy());
        requireText(strategy.getUpdateBy());
        requireSceneType(strategy.getSceneType());
        requireSharePolicy(strategy.getSharePolicy());
        requireRegeneratePolicy(strategy.getRegeneratePolicy());
        requireLockPolicy(strategy.getLockPolicy());
        if (StringUtils.hasText(strategy.getPrepareTiming())) {
            requirePrepareTiming(strategy.getPrepareTiming());
        }
    }

    /**
     * 校验更新数据策略所需的最小字段。
     *
     * @param strategy 数据准备策略实体。
     */
    private void validateUpdateFields(ModuleDataStrategy strategy) {
        if (strategy == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(strategy.getId());
        requireText(strategy.getModuleName());
        requireText(strategy.getDataSourceStrategy());
        requireText(strategy.getSharePolicy());
        requireText(strategy.getRegeneratePolicy());
        requireText(strategy.getLockPolicy());
        requireText(strategy.getUpdateBy());
        requireSharePolicy(strategy.getSharePolicy());
        requireRegeneratePolicy(strategy.getRegeneratePolicy());
        requireLockPolicy(strategy.getLockPolicy());
        if (StringUtils.hasText(strategy.getPrepareTiming())) {
            requirePrepareTiming(strategy.getPrepareTiming());
        }
    }

    /**
     * 校验策略启用前的运行必备字段。
     *
     * @param strategy 数据准备策略实体。
     */
    private void validateEnableFields(ModuleDataStrategy strategy) {
        requireRuntimeText(strategy.getTemplateId());
        requireText(strategy.getDataSourceStrategy());
        requireText(strategy.getSharePolicy());
        requireText(strategy.getRegeneratePolicy());
        requireText(strategy.getLockPolicy());
        requireText(strategy.getPrepareTiming());
        requireSceneType(strategy.getSceneType());
        requireSharePolicy(strategy.getSharePolicy());
        requireRegeneratePolicy(strategy.getRegeneratePolicy());
        requireLockPolicy(strategy.getLockPolicy());
        requirePrepareTiming(strategy.getPrepareTiming());
        validateStrategyJsonFields(strategy, true);
        validateBusinessModuleReference(strategy);
        validateTemplateReference(strategy);
        validateExamStrategy(strategy);
        validatePlatformCapabilities(strategy);
    }

    /**
     * 校验策略引用的业务模块存在且启用。
     *
     * @param strategy 数据准备策略实体。
     */
    private void validateBusinessModuleReference(ModuleDataStrategy strategy) {
        BusinessModule businessModule = businessModuleMapper.selectOne(new QueryWrapper<BusinessModule>()
                .eq("id", strategy.getBusinessModuleId())
                .eq("tenant_id", strategy.getTenantId())
                .eq("connector_system_id", strategy.getConnectorSystemId())
                .eq("module_code", strategy.getModuleCode())
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE));
        if (businessModule == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 校验策略引用的数据模板存在且启用。
     *
     * @param strategy 数据准备策略实体。
     */
    private void validateTemplateReference(ModuleDataStrategy strategy) {
        TeachingDataTemplate template = teachingDataTemplateMapper.selectOne(new QueryWrapper<TeachingDataTemplate>()
                .eq("id", strategy.getTemplateId())
                .eq("tenant_id", strategy.getTenantId())
                .eq("connector_system_id", strategy.getConnectorSystemId())
                .eq("scene_type", strategy.getSceneType())
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE));
        if (template == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 校验考试场景的强约束策略。
     *
     * @param strategy 数据准备策略实体。
     */
    private void validateExamStrategy(ModuleDataStrategy strategy) {
        if (!StrategySceneType.EXAM.getValue().equals(strategy.getSceneType())) {
            return;
        }
        if (!StrategySharePolicy.QUESTION_EXCLUSIVE.getValue().equals(strategy.getSharePolicy())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (!StrategyLockPolicy.ON_EXAM_START.getValue().equals(strategy.getLockPolicy())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireRuntimeText(strategy.getResultCheckPolicyJson());
    }

    /**
     * 校验策略 JSON 配置，保存时保证结构可解析，启用时保证运行期能生成学生数据分配关系。
     *
     * @param strategy 鏁版嵁鍑嗗绛栫暐瀹炰綋銆?
     * @param requireRuntimeFields true 表示启用场景，需要校验造数运行必需策略。
     */
    private void validateStrategyJsonFields(ModuleDataStrategy strategy, boolean requireRuntimeFields) {
        JsonNode defaultOrgRolePolicy = parseJsonObject(strategy.getDefaultOrgRolePolicyJson());
        JsonNode poolSizePolicy = parseJsonObject(strategy.getPoolSizePolicyJson());
        JsonNode validationPolicy = parseJsonObject(strategy.getValidationPolicyJson());
        parseJsonObject(strategy.getExpirePolicyJson());
        parseJsonObject(strategy.getResultCheckPolicyJson());
        parseJsonObject(strategy.getArchivePolicyJson());

        if (poolSizePolicy != null) {
            validatePoolSizePolicy(poolSizePolicy);
        }
        if (!requireRuntimeFields || Boolean.FALSE.equals(strategy.getNeedPreData())) {
            return;
        }
        requireJsonObject(defaultOrgRolePolicy);
        requireJsonText(defaultOrgRolePolicy, "org");
        requireJsonText(defaultOrgRolePolicy, "role");
        requireJsonObject(validationPolicy);
        requireJsonText(validationPolicy, "requiredStatus");
    }

    /**
     * 校验策略池水位，避免出现最小准备量大于最大准备量这种永远无法满足的策略。
     *
     * @param poolSizePolicy 策略池 JSON 对象。
     */
    private void validatePoolSizePolicy(JsonNode poolSizePolicy) {
        JsonNode minReadyCountNode = poolSizePolicy.get("minReadyCount");
        JsonNode maxReadyCountNode = poolSizePolicy.get("maxReadyCount");
        if (minReadyCountNode == null && maxReadyCountNode == null) {
            return;
        }
        if (minReadyCountNode == null || maxReadyCountNode == null
                || !minReadyCountNode.canConvertToInt()
                || !maxReadyCountNode.canConvertToInt()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        int minReadyCount = minReadyCountNode.asInt();
        int maxReadyCount = maxReadyCountNode.asInt();
        if (minReadyCount < 0 || maxReadyCount < 0 || minReadyCount > maxReadyCount) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 将前端维护的 JSON 文本解析为对象节点；策略配置只接受对象以保持字段语义稳定。
     *
     * @param json JSON 文本。
     * @return 空文本返回 null，非空文本返回 JSON 对象节点。
     */
    private JsonNode parseJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(json);
            if (node == null || !node.isObject()) {
                throw new BusinessException(ApiResultCode.PARAM_ERROR);
            }
            return node;
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验启用态必须存在 JSON 对象，避免策略缺少单位角色或校验策略仍被教师发布任务使用。
     *
     * @param node JSON 对象节点。
     */
    private void requireJsonObject(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
    }

    /**
     * 校验 JSON 中的必填文本字段，字段值承担原平台造数和学生分配的业务约束。
     *
     * @param node JSON 对象节点。
     * @param fieldName 字段名。
     */
    private void requireJsonText(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || !StringUtils.hasText(node.get(fieldName).asText())) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
    }

    /**
     * 校验策略依赖的原平台能力已注册且启用。
     *
     * @param strategy 数据准备策略实体。
     */
    private void validatePlatformCapabilities(ModuleDataStrategy strategy) {
        requirePlatformCapability(strategy, CAPABILITY_DATA_CREATE);
        if (!StrategyLockPolicy.NONE.getValue().equals(strategy.getLockPolicy())) {
            requirePlatformCapability(strategy, CAPABILITY_DATA_LOCK);
        }
        if (StringUtils.hasText(strategy.getResultCheckPolicyJson())) {
            requirePlatformCapability(strategy, CAPABILITY_RESULT_CHECK);
        }
        if (StringUtils.hasText(strategy.getArchivePolicyJson())) {
            requirePlatformCapability(strategy, CAPABILITY_DATA_ARCHIVE);
        }
    }

    /**
     * 校验单项原平台能力存在、启用且被声明支持。
     *
     * @param strategy 数据准备策略实体。
     * @param capabilityCode 原平台能力编码。
     */
    private void requirePlatformCapability(ModuleDataStrategy strategy, String capabilityCode) {
        PlatformCapability capability = platformCapabilityMapper.selectOne(new QueryWrapper<PlatformCapability>()
                .eq("tenant_id", strategy.getTenantId())
                .eq("connector_system_id", strategy.getConnectorSystemId())
                .eq("capability_code", capabilityCode)
                .eq("support_flag", Boolean.TRUE)
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE));
        if (capability == null) {
            if (isLocalDevConnector(strategy)) {
                return;
            }
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
    }

    /**
     * 校验启用策略时必须具备的文本配置；这里返回配置不完整，方便前端引导管理员回到维护页面补齐。
     *
     * @param value 运行态必填配置值。
     */
    private void requireRuntimeText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
    }

    /**
     * 本地联调平台由 LocalOriginDataPrepareAdapter 承担能力，允许历史本地平台在未补能力表时启用策略。
     *
     * @param strategy 数据准备策略。
     * @return true 表示本地联调平台。
     */
    private boolean isLocalDevConnector(ModuleDataStrategy strategy) {
        ConnectorSystem connectorSystem = connectorSystemMapper.selectOne(new QueryWrapper<ConnectorSystem>()
                .eq("id", strategy.getConnectorSystemId())
                .eq("tenant_id", strategy.getTenantId())
                .eq("system_type", LOCAL_DEV_SYSTEM_TYPE)
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE));
        return connectorSystem != null;
    }

    /**
     * 校验策略场景命中系统枚举。
     *
     * @param value 策略场景。
     */
    private void requireSceneType(String value) {
        if (StrategySceneType.fromValue(value) == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验准备时机命中系统枚举。
     *
     * @param value 准备时机。
     */
    private void requirePrepareTiming(String value) {
        if (StrategyPrepareTiming.fromValue(value) == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验共享策略命中系统枚举。
     *
     * @param value 共享策略。
     */
    private void requireSharePolicy(String value) {
        if (StrategySharePolicy.fromValue(value) == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验重生成策略命中系统枚举。
     *
     * @param value 重生成策略。
     */
    private void requireRegeneratePolicy(String value) {
        if (StrategyRegeneratePolicy.fromValue(value) == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验锁定策略命中系统枚举。
     *
     * @param value 锁定策略。
     */
    private void requireLockPolicy(String value) {
        if (StrategyLockPolicy.fromValue(value) == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验文本字段非空。
     *
     * @param value 待校验字段值。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 补齐创建时默认字段。
     *
     * @param strategy 数据准备策略实体。
     */
    private void fillCreateDefaults(ModuleDataStrategy strategy) {
        LocalDateTime now = LocalDateTime.now();
        if (strategy.getCreateTime() == null) {
            strategy.setCreateTime(now);
        }
        if (strategy.getUpdateTime() == null) {
            strategy.setUpdateTime(now);
        }
        if (strategy.getNeedPreData() == null) {
            strategy.setNeedPreData(Boolean.TRUE);
        }
        if (strategy.getStrategyVersion() == null) {
            strategy.setStrategyVersion(1L);
        }
        if (strategy.getLockVersion() == null) {
            strategy.setLockVersion(0L);
        }
        if (!StringUtils.hasText(strategy.getStatus())) {
            strategy.setStatus(RecordStatus.DISABLED.getValue());
        }
        if (strategy.getDeleted() == null) {
            strategy.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 递增版本号。
     *
     * @param currentVersion 当前版本，历史数据为空时按 0 处理。
     * @return 下一版本号。
     */
    private Long nextVersion(Long currentVersion) {
        if (currentVersion == null) {
            return 1L;
        }
        return currentVersion + 1;
    }
}
