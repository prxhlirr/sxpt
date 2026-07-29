package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.ConnectorResource;
import com.sxpt.module.connector.mapper.ConnectorResourceMapper;
import com.sxpt.module.connector.service.ConnectorResourceService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 原平台正式资源服务实现。
 *
 * 业务功能：
 * 1. 保存被教学步骤、遮罩或评分引用的原平台正式资源。
 * 2. 按页面查询正式资源，避免 SDK runtime 扫描无教学价值 DOM。
 *
 * 关键流程：
 * 1. 创建资源时校验最小字段，保证资源可被稳定引用。
 * 2. 补齐通用生命周期字段，并按租户、原平台、页面查询未删除资源。
 */
@Service
@Profile("!test")
public class ConnectorResourceServiceImpl implements ConnectorResourceService {

    private final ConnectorResourceMapper connectorResourceMapper;

    public ConnectorResourceServiceImpl(ConnectorResourceMapper connectorResourceMapper) {
        this.connectorResourceMapper = connectorResourceMapper;
    }

    /**
     * 创建正式资源。
     *
     * @param connectorResource 原平台正式资源。
     * @return 已保存的正式资源。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConnectorResource createConnectorResource(ConnectorResource connectorResource) {
        validateCreateFields(connectorResource);
        fillCreateDefaults(connectorResource);
        connectorResourceMapper.upsertByBusinessKey(connectorResource);
        ConnectorResource saved = connectorResourceMapper.selectOne(
                businessKeyQuery(connectorResource));
        if (saved == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return saved;
    }

    /**
     * 按页面查询原平台正式资源。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @param pageUrl 页面地址。
     * @return 页面下的正式资源列表。
     */
    @Override
    public List<ConnectorResource> listByPage(String tenantId, String connectorSystemId, String pageUrl) {
        requireText(tenantId);
        requireText(connectorSystemId);
        requireText(pageUrl);
        return connectorResourceMapper.selectList(new QueryWrapper<ConnectorResource>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("page_url", pageUrl)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("resource_type", "resource_code"));
    }

    /**
     * 校验创建正式资源所需的最小字段。
     *
     * @param connectorResource 原平台正式资源。
     */
    private void validateCreateFields(ConnectorResource connectorResource) {
        if (connectorResource == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(connectorResource.getId());
        requireText(connectorResource.getTenantId());
        requireText(connectorResource.getConnectorSystemId());
        requireText(connectorResource.getResourceCode());
        requireText(connectorResource.getResourceName());
        requireText(connectorResource.getResourceType());
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
     * 补齐创建正式资源时的默认字段。
     *
     * @param connectorResource 原平台正式资源。
     */
    private void fillCreateDefaults(ConnectorResource connectorResource) {
        LocalDateTime now = LocalDateTime.now();
        if (connectorResource.getCreateTime() == null) {
            connectorResource.setCreateTime(now);
        }
        if (connectorResource.getUpdateTime() == null) {
            connectorResource.setUpdateTime(now);
        }
        if (!StringUtils.hasText(connectorResource.getUpdateBy())) {
            connectorResource.setUpdateBy(connectorResource.getCreateBy());
        }
        if (!StringUtils.hasText(connectorResource.getStatus())) {
            connectorResource.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (connectorResource.getDeleted() == null) {
            connectorResource.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 构造与数据库唯一索引一致的资源业务键查询。
     *
     * @param connectorResource 原平台正式资源。
     * @return 业务键查询条件。
     */
    private QueryWrapper<ConnectorResource> businessKeyQuery(
            ConnectorResource connectorResource) {
        return new QueryWrapper<ConnectorResource>()
                .eq("tenant_id", connectorResource.getTenantId())
                .eq("connector_system_id", connectorResource.getConnectorSystemId())
                .eq("resource_code", connectorResource.getResourceCode())
                .eq("deleted", Boolean.FALSE);
    }
}

