package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.connector.service.ConnectorSystemService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

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

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final String DISABLED_STATUS = "DISABLED";

    private final ConnectorSystemMapper connectorSystemMapper;

    public ConnectorSystemServiceImpl(ConnectorSystemMapper connectorSystemMapper) {
        this.connectorSystemMapper = connectorSystemMapper;
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
        fillCreateDefaults(connectorSystem);
        connectorSystemMapper.insert(connectorSystem);
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
        existing.setSystemName(connectorSystem.getSystemName());
        existing.setSystemType(connectorSystem.getSystemType());
        existing.setBaseUrl(connectorSystem.getBaseUrl());
        existing.setAuthType(connectorSystem.getAuthType());
        existing.setConfigJson(connectorSystem.getConfigJson());
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
        return changeStatus(id, DEFAULT_STATUS);
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
        return changeStatus(id, DISABLED_STATUS);
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
            connectorSystem.setStatus(DEFAULT_STATUS);
        }
        if (connectorSystem.getDeleted() == null) {
            connectorSystem.setDeleted(Boolean.FALSE);
        }
    }
}

