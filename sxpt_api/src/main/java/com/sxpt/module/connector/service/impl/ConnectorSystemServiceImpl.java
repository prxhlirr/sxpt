package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.entity.PlatformCapability;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.connector.mapper.PlatformCapabilityMapper;
import com.sxpt.module.connector.service.ConnectorSystemService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 原业务平台配置服务实现。
 *
 * 业务功能：
 * 1. 创建原平台接入配置，保证后续 launchToken、SDK 和采集流程有可引用的平台来源。
 * 2. 在写入前统一补齐基础生命周期字段，避免 Controller 或 Mapper 分散处理默认值。
 *
 * 关键流程：
 * 1. 校验租户、平台编码、名称、类型、基础地址和认证方式。
 * 2. 补齐创建时间、更新时间、通用状态和软删除标记。
 * 3. 调用 ConnectorSystemMapper 写入 connector_system 表。
 */
@Service
@Profile("!test")
public class ConnectorSystemServiceImpl implements ConnectorSystemService {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final String LOCAL_DEV_SYSTEM_TYPE = "LOCAL_DEV";

    private static final String AUTH_TYPE_API_KEY = "API_KEY";

    private static final String CONFIG_API_KEY_FIELD = "apiKey";

    private static final String ENVIRONMENT_TYPE_PROD = "PROD";

    private static final String ENVIRONMENT_TYPE_LEARNING = "LEARNING";

    private static final String[] LOCAL_CAPABILITY_CODES = {
            "DATA_CREATE"
    };

    private final ConnectorSystemMapper connectorSystemMapper;

    private final PlatformCapabilityMapper platformCapabilityMapper;

    public ConnectorSystemServiceImpl(ConnectorSystemMapper connectorSystemMapper,
                                      PlatformCapabilityMapper platformCapabilityMapper) {
        this.connectorSystemMapper = connectorSystemMapper;
        this.platformCapabilityMapper = platformCapabilityMapper;
    }

    /**
     * 创建原业务平台配置。
     *
     * @param connectorSystem 原平台配置实体，必须包含租户、编码、名称、类型、基础地址和认证方式。
     * @return 已保存的原平台配置实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConnectorSystem createConnectorSystem(ConnectorSystem connectorSystem) {
        validateRequiredFields(connectorSystem);
        normalizeAndValidateAuthConfig(connectorSystem);
        normalizeAndValidateEnvironment(connectorSystem);
        fillCreateDefaults(connectorSystem);
        connectorSystemMapper.insert(connectorSystem);
        createLocalCapabilitiesIfNeeded(connectorSystem);
        return connectorSystem;
    }

    /**
     * 更新原业务平台配置。
     *
     * @param connectorSystem 原平台配置实体，必须包含 ID、名称、类型、基础地址和认证方式。
     * @return 已更新的原平台配置实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConnectorSystem updateConnectorSystem(ConnectorSystem connectorSystem) {
        validateUpdateFields(connectorSystem);
        ConnectorSystem existing = getConnectorSystemById(connectorSystem.getId());
        ConnectorSystem authCandidate = new ConnectorSystem();
        authCandidate.setAuthType(connectorSystem.getAuthType());
        authCandidate.setConfigJson(connectorSystem.getConfigJson() == null
                ? existing.getConfigJson()
                : connectorSystem.getConfigJson());
        normalizeAndValidateAuthConfig(authCandidate);
        existing.setSystemName(connectorSystem.getSystemName());
        existing.setSystemType(connectorSystem.getSystemType());
        if (connectorSystem.getEnvironmentType() != null) {
            existing.setEnvironmentType(connectorSystem.getEnvironmentType());
        }
        if (connectorSystem.getEnvironmentGroupCode() != null) {
            existing.setEnvironmentGroupCode(connectorSystem.getEnvironmentGroupCode());
        }
        normalizeAndValidateEnvironment(existing);
        existing.setBaseUrl(connectorSystem.getBaseUrl());
        existing.setAuthType(authCandidate.getAuthType());
        if (connectorSystem.getConfigJson() != null) {
            existing.setConfigJson(connectorSystem.getConfigJson());
        }
        existing.setUpdateBy(connectorSystem.getUpdateBy());
        existing.setUpdateTime(LocalDateTime.now());
        connectorSystemMapper.updateById(existing);
        return existing;
    }

    /**
     * 启用原业务平台配置。
     *
     * @param id 原平台配置 ID。
     * @return 已启用的原平台配置实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConnectorSystem enableConnectorSystem(String id) {
        return changeStatus(id, RecordStatus.ACTIVE.getValue());
    }

    /**
     * 禁用原业务平台配置。
     *
     * @param id 原平台配置 ID。
     * @return 已禁用的原平台配置实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConnectorSystem disableConnectorSystem(String id) {
        return changeStatus(id, RecordStatus.DISABLED.getValue());
    }

    /**
     * 查询原业务平台配置详情。
     *
     * @param id 原平台配置 ID。
     * @return 未删除的原平台配置实体。
     */
    @Override
    public ConnectorSystem getConnectorSystemById(String id) {
        requireText(id);
        ConnectorSystem connectorSystem = connectorSystemMapper.selectOne(new QueryWrapper<ConnectorSystem>()
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (connectorSystem == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return connectorSystem;
    }

    /**
     * 查询指定租户下的原业务平台配置列表。
     *
     * @param tenantId 租户 ID。
     * @return 指定租户下未删除的原平台配置列表。
     */
    @Override
    public List<ConnectorSystem> listConnectorSystemsByTenantId(String tenantId) {
        requireText(tenantId);
        return connectorSystemMapper.selectList(new QueryWrapper<ConnectorSystem>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 切换原平台配置通用状态。
     *
     * @param id 原平台配置 ID。
     * @param status 目标状态。
     * @return 已切换状态的原平台配置实体。
     */
    private ConnectorSystem changeStatus(String id, String status) {
        requireText(id);
        ConnectorSystem existing = getConnectorSystemById(id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        connectorSystemMapper.updateById(existing);
        return existing;
    }

    /**
     * 校验创建原平台配置所需的最小字段。
     *
     * @param connectorSystem 原平台配置实体。
     */
    private void validateRequiredFields(ConnectorSystem connectorSystem) {
        if (connectorSystem == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(connectorSystem.getId());
        requireText(connectorSystem.getTenantId());
        requireText(connectorSystem.getSystemCode());
        requireText(connectorSystem.getSystemName());
        requireText(connectorSystem.getSystemType());
        requireText(connectorSystem.getBaseUrl());
        requireText(connectorSystem.getAuthType());
    }

    /**
     * 校验更新原平台配置所需的最小字段。
     *
     * @param connectorSystem 原平台配置实体。
     */
    private void validateUpdateFields(ConnectorSystem connectorSystem) {
        if (connectorSystem == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(connectorSystem.getId());
        requireText(connectorSystem.getSystemName());
        requireText(connectorSystem.getSystemType());
        requireText(connectorSystem.getBaseUrl());
        requireText(connectorSystem.getAuthType());
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
     * 校验原平台首期只允许 API_KEY 认证，并确保 HTTP 适配器运行时能拿到真实密钥。
     *
     * @param connectorSystem 原平台配置实体。
     */
    private void normalizeAndValidateAuthConfig(ConnectorSystem connectorSystem) {
        String authType = connectorSystem.getAuthType().trim().toUpperCase(Locale.ROOT);
        if (!AUTH_TYPE_API_KEY.equals(authType)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        connectorSystem.setAuthType(authType);
        JsonNode config = parseConfigJson(connectorSystem.getConfigJson());
        JsonNode apiKeyNode = config.get(CONFIG_API_KEY_FIELD);
        if (apiKeyNode == null || !StringUtils.hasText(apiKeyNode.asText())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 规范化原平台环境字段；经典案例依赖 PROD/LEARNING 和环境组建立正式环境到学习环境的安全映射。
     *
     * @param connectorSystem 原平台配置实体。
     */
    private void normalizeAndValidateEnvironment(ConnectorSystem connectorSystem) {
        if (StringUtils.hasText(connectorSystem.getEnvironmentType())) {
            String environmentType = connectorSystem.getEnvironmentType().trim().toUpperCase(Locale.ROOT);
            if (!ENVIRONMENT_TYPE_PROD.equals(environmentType) && !ENVIRONMENT_TYPE_LEARNING.equals(environmentType)) {
                throw new BusinessException(ApiResultCode.PARAM_ERROR);
            }
            connectorSystem.setEnvironmentType(environmentType);
        } else {
            connectorSystem.setEnvironmentType(null);
        }
        if (StringUtils.hasText(connectorSystem.getEnvironmentGroupCode())) {
            connectorSystem.setEnvironmentGroupCode(connectorSystem.getEnvironmentGroupCode().trim());
        } else {
            connectorSystem.setEnvironmentGroupCode(null);
        }
    }

    /**
     * 解析认证配置 JSON；认证配置是调用原平台的运行时凭据，必须保持对象结构便于后续扩展 headerName 等参数。
     *
     * @param configJson 认证配置 JSON 文本。
     * @return JSON 对象节点。
     */
    private JsonNode parseConfigJson(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(configJson);
            if (node == null || !node.isObject()) {
                throw new BusinessException(ApiResultCode.PARAM_ERROR);
            }
            return node;
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 补齐创建时默认字段。
     *
     * @param connectorSystem 原平台配置实体。
     */
    private void fillCreateDefaults(ConnectorSystem connectorSystem) {
        LocalDateTime now = LocalDateTime.now();
        if (connectorSystem.getCreateTime() == null) {
            connectorSystem.setCreateTime(now);
        }
        if (connectorSystem.getUpdateTime() == null) {
            connectorSystem.setUpdateTime(now);
        }
        if (!StringUtils.hasText(connectorSystem.getStatus())) {
            connectorSystem.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (!StringUtils.hasText(connectorSystem.getCreateBy())) {
            connectorSystem.setCreateBy("system");
        }
        if (!StringUtils.hasText(connectorSystem.getUpdateBy())) {
            connectorSystem.setUpdateBy(connectorSystem.getCreateBy());
        }
        if (connectorSystem.getDeleted() == null) {
            connectorSystem.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 本地联调平台默认注册完整数据准备能力，保证后续模块策略可以直接启用。
     *
     * @param connectorSystem 原平台配置。
     */
    private void createLocalCapabilitiesIfNeeded(ConnectorSystem connectorSystem) {
        if (!LOCAL_DEV_SYSTEM_TYPE.equals(connectorSystem.getSystemType())) {
            return;
        }
        for (String capabilityCode : LOCAL_CAPABILITY_CODES) {
            if (hasCapability(connectorSystem, capabilityCode)) {
                continue;
            }
            platformCapabilityMapper.insert(buildLocalCapability(connectorSystem, capabilityCode));
        }
    }

    /**
     * 判断能力是否已存在，避免重复创建本地联调能力声明。
     *
     * @param connectorSystem 原平台配置。
     * @param capabilityCode 能力编码。
     * @return true 表示已存在。
     */
    private boolean hasCapability(ConnectorSystem connectorSystem, String capabilityCode) {
        Integer count = platformCapabilityMapper.selectCount(new QueryWrapper<PlatformCapability>()
                .eq("tenant_id", connectorSystem.getTenantId())
                .eq("connector_system_id", connectorSystem.getId())
                .eq("capability_code", capabilityCode)
                .eq("deleted", Boolean.FALSE));
        return count != null && count > 0;
    }

    /**
     * 构造本地联调能力记录，使策略启用前的能力校验有明确依据。
     *
     * @param connectorSystem 原平台配置。
     * @param capabilityCode 能力编码。
     * @return 平台能力记录。
     */
    private PlatformCapability buildLocalCapability(ConnectorSystem connectorSystem, String capabilityCode) {
        LocalDateTime now = LocalDateTime.now();
        String operator = resolveOperator(connectorSystem);
        PlatformCapability capability = new PlatformCapability();
        capability.setId(UUID.randomUUID().toString().replace("-", ""));
        capability.setTenantId(connectorSystem.getTenantId());
        capability.setConnectorSystemId(connectorSystem.getId());
        capability.setCapabilityCode(capabilityCode);
        capability.setCapabilityName(capabilityCode);
        capability.setCapabilityType(capabilityCode);
        capability.setSupportFlag(Boolean.TRUE);
        capability.setEndpointUrl(connectorSystem.getBaseUrl());
        capability.setMethod("POST");
        // 平台能力是系统自动补齐的基础配置，必须显式写入审计字段以满足数据库非空约束。
        capability.setCreateBy(operator);
        capability.setCreateTime(now);
        capability.setUpdateBy(operator);
        capability.setUpdateTime(now);
        capability.setStatus(RecordStatus.ACTIVE.getValue());
        capability.setDeleted(Boolean.FALSE);
        return capability;
    }

    /**
     * 解析平台能力自动注册时使用的操作人。
     *
     * @param connectorSystem 原平台配置。
     * @return 优先使用平台创建人，缺失时使用系统操作人。
     */
    private String resolveOperator(ConnectorSystem connectorSystem) {
        if (StringUtils.hasText(connectorSystem.getCreateBy())) {
            return connectorSystem.getCreateBy();
        }
        if (StringUtils.hasText(connectorSystem.getUpdateBy())) {
            return connectorSystem.getUpdateBy();
        }
        return "system";
    }
}

