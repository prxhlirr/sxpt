package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.entity.OriginOrg;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.connector.mapper.OriginOrgMapper;
import com.sxpt.module.connector.service.OriginOrgService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 原平台组织字典维护服务实现。
 *
 * 业务功能：
 * 1. 维护 origin_org 表，让模块流程参与方配置从手工输入组织改为选择已登记组织。
 * 2. 为数据准备生成参数时提供可追溯、可启停的原平台组织事实来源。
 *
 * 关键流程：
 * 1. 创建时校验最小字段、原平台引用和组织编码唯一性，再补齐生命周期字段。
 * 2. 更新时保留租户、原平台和组织编码不变，只更新组织名称、原平台组织标识、层级、分类、备注和更新人。
 * 3. 查询列表时始终按租户和原平台隔离，避免跨系统误选组织。
 */
@Service
@Profile("!test")
public class OriginOrgServiceImpl implements OriginOrgService {

    private final OriginOrgMapper originOrgMapper;

    private final ConnectorSystemMapper connectorSystemMapper;

    public OriginOrgServiceImpl(OriginOrgMapper originOrgMapper,
                                ConnectorSystemMapper connectorSystemMapper) {
        this.originOrgMapper = originOrgMapper;
        this.connectorSystemMapper = connectorSystemMapper;
    }

    /**
     * 创建原平台组织字典。
     *
     * @param originOrg 原平台组织字典实体。
     * @return 已保存的原平台组织字典实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OriginOrg createOriginOrg(OriginOrg originOrg) {
        validateCreateFields(originOrg);
        validateConnectorSystemReference(originOrg);
        ensureOrgCodeUnique(originOrg);
        fillCreateDefaults(originOrg);
        originOrgMapper.insert(originOrg);
        return originOrg;
    }

    /**
     * 更新原平台组织字典。
     *
     * @param originOrg 原平台组织字典实体。
     * @return 已更新的原平台组织字典实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OriginOrg updateOriginOrg(OriginOrg originOrg) {
        validateUpdateFields(originOrg);
        OriginOrg existing = getOriginOrgById(originOrg.getId());
        existing.setOrgName(originOrg.getOrgName());
        existing.setExternalOrgId(originOrg.getExternalOrgId());
        existing.setParentExternalOrgId(originOrg.getParentExternalOrgId());
        existing.setOrgType(originOrg.getOrgType());
        existing.setRemark(originOrg.getRemark());
        existing.setUpdateBy(originOrg.getUpdateBy());
        existing.setUpdateTime(LocalDateTime.now());
        originOrgMapper.updateById(existing);
        return existing;
    }

    /**
     * 启用原平台组织字典。
     *
     * @param id 组织字典 ID。
     * @return 已启用的原平台组织字典实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OriginOrg enableOriginOrg(String id) {
        return changeStatus(id, RecordStatus.ACTIVE.getValue());
    }

    /**
     * 停用原平台组织字典。
     *
     * @param id 组织字典 ID。
     * @return 已停用的原平台组织字典实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OriginOrg disableOriginOrg(String id) {
        return changeStatus(id, RecordStatus.DISABLED.getValue());
    }

    /**
     * 查询原平台组织字典详情。
     *
     * @param id 组织字典 ID。
     * @return 未删除的原平台组织字典实体。
     */
    @Override
    public OriginOrg getOriginOrgById(String id) {
        requireText(id);
        OriginOrg originOrg = originOrgMapper.selectOne(new QueryWrapper<OriginOrg>()
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (originOrg == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return originOrg;
    }

    /**
     * 查询某个原平台下的组织字典列表。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @param activeOnly 是否只返回启用组织。
     * @return 原平台组织字典列表。
     */
    @Override
    public List<OriginOrg> listOriginOrgs(String tenantId, String connectorSystemId, boolean activeOnly) {
        requireText(tenantId);
        requireText(connectorSystemId);
        QueryWrapper<OriginOrg> queryWrapper = new QueryWrapper<OriginOrg>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("org_code");
        if (activeOnly) {
            queryWrapper.eq("status", RecordStatus.ACTIVE.getValue());
        }
        return originOrgMapper.selectList(queryWrapper);
    }

    /**
     * 切换原平台组织字典状态。
     *
     * @param id 组织字典 ID。
     * @param status 目标状态。
     * @return 已切换状态的组织字典实体。
     */
    private OriginOrg changeStatus(String id, String status) {
        OriginOrg existing = getOriginOrgById(id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        originOrgMapper.updateById(existing);
        return existing;
    }

    /**
     * 校验创建组织字典所需的最小字段。
     *
     * @param originOrg 原平台组织字典实体。
     */
    private void validateCreateFields(OriginOrg originOrg) {
        if (originOrg == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(originOrg.getId());
        requireText(originOrg.getTenantId());
        requireText(originOrg.getConnectorSystemId());
        requireText(originOrg.getOrgCode());
        validateEditableFields(originOrg);
    }

    /**
     * 校验更新组织字典所需的最小字段。
     *
     * @param originOrg 原平台组织字典实体。
     */
    private void validateUpdateFields(OriginOrg originOrg) {
        if (originOrg == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(originOrg.getId());
        validateEditableFields(originOrg);
    }

    /**
     * 校验可编辑字段，确保下拉项有稳定可读的展示名称。
     *
     * @param originOrg 原平台组织字典实体。
     */
    private void validateEditableFields(OriginOrg originOrg) {
        requireText(originOrg.getOrgName());
    }

    /**
     * 校验组织绑定的原平台存在，避免孤立组织污染模块流程配置。
     *
     * @param originOrg 原平台组织字典实体。
     */
    private void validateConnectorSystemReference(OriginOrg originOrg) {
        ConnectorSystem connectorSystem = connectorSystemMapper.selectOne(new QueryWrapper<ConnectorSystem>()
                .eq("id", originOrg.getConnectorSystemId())
                .eq("tenant_id", originOrg.getTenantId())
                .eq("deleted", Boolean.FALSE));
        if (connectorSystem == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 校验同一原平台下组织编码唯一，避免模块参与方配置时同一个编码指向多个组织。
     *
     * @param originOrg 原平台组织字典实体。
     */
    private void ensureOrgCodeUnique(OriginOrg originOrg) {
        Integer count = originOrgMapper.selectCount(new QueryWrapper<OriginOrg>()
                .eq("tenant_id", originOrg.getTenantId())
                .eq("connector_system_id", originOrg.getConnectorSystemId())
                .eq("org_code", originOrg.getOrgCode())
                .eq("deleted", Boolean.FALSE));
        if (count != null && count > 0) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 补齐创建组织字典时的生命周期字段。
     *
     * @param originOrg 原平台组织字典实体。
     */
    private void fillCreateDefaults(OriginOrg originOrg) {
        LocalDateTime now = LocalDateTime.now();
        if (originOrg.getCreateTime() == null) {
            originOrg.setCreateTime(now);
        }
        if (originOrg.getUpdateTime() == null) {
            originOrg.setUpdateTime(now);
        }
        if (!StringUtils.hasText(originOrg.getCreateBy())) {
            originOrg.setCreateBy("system");
        }
        if (!StringUtils.hasText(originOrg.getUpdateBy())) {
            originOrg.setUpdateBy(originOrg.getCreateBy());
        }
        if (!StringUtils.hasText(originOrg.getStatus())) {
            originOrg.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (originOrg.getDeleted() == null) {
            originOrg.setDeleted(Boolean.FALSE);
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
