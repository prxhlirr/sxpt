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
import com.sxpt.module.connector.service.PlatformCapabilityService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 原平台能力维护服务实现。
 *
 * 业务功能：
 * 1. 维护 platform_capability 表，使原平台是否支持造数、校验、锁定等能力成为显式配置。
 * 2. 支撑模块数据策略启用前的能力校验，避免运行期才发现原平台没有对应接口。
 *
 * 关键流程：
 * 1. 创建时校验原平台存在、能力编码唯一、JSON 字段可解析，并补齐生命周期字段。
 * 2. 更新时不允许改租户、原平台和能力编码，只维护接口、Schema、超时和支持状态等运行配置。
 * 3. 启停只切换通用状态，不删除历史配置，便于审计和回滚。
 */
@Service
@Profile("!test")
public class PlatformCapabilityServiceImpl implements PlatformCapabilityService {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final PlatformCapabilityMapper platformCapabilityMapper;

    private final ConnectorSystemMapper connectorSystemMapper;

    public PlatformCapabilityServiceImpl(PlatformCapabilityMapper platformCapabilityMapper,
                                         ConnectorSystemMapper connectorSystemMapper) {
        this.platformCapabilityMapper = platformCapabilityMapper;
        this.connectorSystemMapper = connectorSystemMapper;
    }

    /**
     * 创建原平台能力声明。
     *
     * @param capability 原平台能力实体。
     * @return 已保存的原平台能力实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformCapability createPlatformCapability(PlatformCapability capability) {
        validateCreateFields(capability);
        validateConnectorSystemReference(capability);
        validateJsonFields(capability);
        ensureCapabilityCodeUnique(capability);
        fillCreateDefaults(capability);
        platformCapabilityMapper.insert(capability);
        return capability;
    }

    /**
     * 更新原平台能力声明。
     *
     * @param capability 原平台能力实体。
     * @return 已更新的原平台能力实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformCapability updatePlatformCapability(PlatformCapability capability) {
        validateUpdateFields(capability);
        validateJsonFields(capability);
        PlatformCapability existing = getPlatformCapabilityById(capability.getId());
        existing.setCapabilityName(capability.getCapabilityName());
        existing.setCapabilityType(capability.getCapabilityType());
        existing.setSupportFlag(capability.getSupportFlag());
        existing.setEndpointUrl(capability.getEndpointUrl());
        existing.setMethod(capability.getMethod());
        existing.setRequestSchemaJson(capability.getRequestSchemaJson());
        existing.setResponseSchemaJson(capability.getResponseSchemaJson());
        existing.setTimeoutMs(capability.getTimeoutMs());
        existing.setRetryPolicyJson(capability.getRetryPolicyJson());
        existing.setUpdateBy(capability.getUpdateBy());
        existing.setUpdateTime(LocalDateTime.now());
        platformCapabilityMapper.updateById(existing);
        return existing;
    }

    /**
     * 启用原平台能力声明。
     *
     * @param id 能力 ID。
     * @return 已启用的原平台能力实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformCapability enablePlatformCapability(String id) {
        return changeStatus(id, RecordStatus.ACTIVE.getValue());
    }

    /**
     * 停用原平台能力声明。
     *
     * @param id 能力 ID。
     * @return 已停用的原平台能力实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformCapability disablePlatformCapability(String id) {
        return changeStatus(id, RecordStatus.DISABLED.getValue());
    }

    /**
     * 查询能力详情。
     *
     * @param id 能力 ID。
     * @return 未删除的原平台能力实体。
     */
    @Override
    public PlatformCapability getPlatformCapabilityById(String id) {
        requireText(id);
        PlatformCapability capability = platformCapabilityMapper.selectOne(new QueryWrapper<PlatformCapability>()
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (capability == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return capability;
    }

    /**
     * 查询某个原平台下的全部能力声明。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @return 能力声明列表。
     */
    @Override
    public List<PlatformCapability> listPlatformCapabilities(String tenantId, String connectorSystemId) {
        requireText(tenantId);
        requireText(connectorSystemId);
        return platformCapabilityMapper.selectList(new QueryWrapper<PlatformCapability>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("capability_code"));
    }

    /**
     * 切换能力声明状态。
     *
     * @param id 能力 ID。
     * @param status 目标状态。
     * @return 已切换状态的能力实体。
     */
    private PlatformCapability changeStatus(String id, String status) {
        PlatformCapability existing = getPlatformCapabilityById(id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        platformCapabilityMapper.updateById(existing);
        return existing;
    }

    /**
     * 校验创建能力声明所需的最小字段。
     *
     * @param capability 原平台能力实体。
     */
    private void validateCreateFields(PlatformCapability capability) {
        if (capability == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(capability.getId());
        requireText(capability.getTenantId());
        requireText(capability.getConnectorSystemId());
        requireText(capability.getCapabilityCode());
        validateEditableFields(capability);
    }

    /**
     * 校验更新能力声明所需的最小字段。
     *
     * @param capability 原平台能力实体。
     */
    private void validateUpdateFields(PlatformCapability capability) {
        if (capability == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(capability.getId());
        validateEditableFields(capability);
    }

    /**
     * 校验能力可编辑字段，确保策略启用时读取到的是可调用的运行配置。
     *
     * @param capability 原平台能力实体。
     */
    private void validateEditableFields(PlatformCapability capability) {
        requireText(capability.getCapabilityName());
        requireText(capability.getCapabilityType());
        if (capability.getSupportFlag() == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(capability.getEndpointUrl());
        requireText(capability.getMethod());
        if (capability.getTimeoutMs() != null && capability.getTimeoutMs() < 0) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验能力绑定的原平台存在，避免孤立能力声明污染策略启用判断。
     *
     * @param capability 原平台能力实体。
     */
    private void validateConnectorSystemReference(PlatformCapability capability) {
        ConnectorSystem connectorSystem = connectorSystemMapper.selectOne(new QueryWrapper<ConnectorSystem>()
                .eq("id", capability.getConnectorSystemId())
                .eq("tenant_id", capability.getTenantId())
                .eq("deleted", Boolean.FALSE));
        if (connectorSystem == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 校验同一原平台下能力编码唯一，避免策略启用时读到多条冲突能力声明。
     *
     * @param capability 原平台能力实体。
     */
    private void ensureCapabilityCodeUnique(PlatformCapability capability) {
        Integer count = platformCapabilityMapper.selectCount(new QueryWrapper<PlatformCapability>()
                .eq("tenant_id", capability.getTenantId())
                .eq("connector_system_id", capability.getConnectorSystemId())
                .eq("capability_code", capability.getCapabilityCode())
                .eq("deleted", Boolean.FALSE));
        if (count != null && count > 0) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验 JSON 字段必须是对象结构，避免接口 Schema 和重试策略在运行期不可解析。
     *
     * @param capability 原平台能力实体。
     */
    private void validateJsonFields(PlatformCapability capability) {
        parseJsonObject(capability.getRequestSchemaJson());
        parseJsonObject(capability.getResponseSchemaJson());
        parseJsonObject(capability.getRetryPolicyJson());
    }

    /**
     * 解析 JSON 对象文本。
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
     * 补齐创建能力声明时的生命周期字段。
     *
     * @param capability 原平台能力实体。
     */
    private void fillCreateDefaults(PlatformCapability capability) {
        LocalDateTime now = LocalDateTime.now();
        if (capability.getCreateTime() == null) {
            capability.setCreateTime(now);
        }
        if (capability.getUpdateTime() == null) {
            capability.setUpdateTime(now);
        }
        if (!StringUtils.hasText(capability.getCreateBy())) {
            capability.setCreateBy("system");
        }
        if (!StringUtils.hasText(capability.getUpdateBy())) {
            capability.setUpdateBy(capability.getCreateBy());
        }
        if (!StringUtils.hasText(capability.getStatus())) {
            capability.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (capability.getDeleted() == null) {
            capability.setDeleted(Boolean.FALSE);
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
}
