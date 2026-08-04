package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.entity.OriginRole;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.connector.mapper.OriginRoleMapper;
import com.sxpt.module.connector.service.OriginRoleService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 原平台角色字典维护服务实现。
 *
 * 业务功能：
 * 1. 维护 origin_role 表，让模块流程参与方配置从手工输入角色改为选择已登记角色。
 * 2. 为数据准备生成参数时提供可追溯、可启停的原平台角色事实来源。
 *
 * 关键流程：
 * 1. 创建时校验最小字段、原平台引用和角色编码唯一性，再补齐生命周期字段。
 * 2. 更新时保留租户、原平台和角色编码不变，只更新角色名称、原平台角色标识、分类、备注和更新人。
 * 3. 查询列表时始终按租户和原平台隔离，避免跨系统误选角色。
 */
@Service
@Profile("!test")
public class OriginRoleServiceImpl implements OriginRoleService {

    private final OriginRoleMapper originRoleMapper;

    private final ConnectorSystemMapper connectorSystemMapper;

    public OriginRoleServiceImpl(OriginRoleMapper originRoleMapper,
                                 ConnectorSystemMapper connectorSystemMapper) {
        this.originRoleMapper = originRoleMapper;
        this.connectorSystemMapper = connectorSystemMapper;
    }

    /**
     * 创建原平台角色字典。
     *
     * @param originRole 原平台角色字典实体。
     * @return 已保存的原平台角色字典实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OriginRole createOriginRole(OriginRole originRole) {
        validateCreateFields(originRole);
        validateConnectorSystemReference(originRole);
        ensureRoleCodeUnique(originRole);
        fillCreateDefaults(originRole);
        originRoleMapper.insert(originRole);
        return originRole;
    }

    /**
     * 更新原平台角色字典。
     *
     * @param originRole 原平台角色字典实体。
     * @return 已更新的原平台角色字典实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OriginRole updateOriginRole(OriginRole originRole) {
        validateUpdateFields(originRole);
        OriginRole existing = getOriginRoleById(originRole.getId());
        existing.setRoleName(originRole.getRoleName());
        existing.setExternalRoleId(originRole.getExternalRoleId());
        existing.setRoleType(originRole.getRoleType());
        existing.setRemark(originRole.getRemark());
        existing.setUpdateBy(originRole.getUpdateBy());
        existing.setUpdateTime(LocalDateTime.now());
        originRoleMapper.updateById(existing);
        return existing;
    }

    /**
     * 启用原平台角色字典。
     *
     * @param id 角色字典 ID。
     * @return 已启用的原平台角色字典实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OriginRole enableOriginRole(String id) {
        return changeStatus(id, RecordStatus.ACTIVE.getValue());
    }

    /**
     * 停用原平台角色字典。
     *
     * @param id 角色字典 ID。
     * @return 已停用的原平台角色字典实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OriginRole disableOriginRole(String id) {
        return changeStatus(id, RecordStatus.DISABLED.getValue());
    }

    /**
     * 查询原平台角色字典详情。
     *
     * @param id 角色字典 ID。
     * @return 未删除的原平台角色字典实体。
     */
    @Override
    public OriginRole getOriginRoleById(String id) {
        requireText(id);
        OriginRole originRole = originRoleMapper.selectOne(new QueryWrapper<OriginRole>()
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (originRole == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return originRole;
    }

    /**
     * 查询某个原平台下的角色字典列表。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @param activeOnly 是否只返回启用角色。
     * @return 原平台角色字典列表。
     */
    @Override
    public List<OriginRole> listOriginRoles(String tenantId, String connectorSystemId, boolean activeOnly) {
        requireText(tenantId);
        requireText(connectorSystemId);
        QueryWrapper<OriginRole> queryWrapper = new QueryWrapper<OriginRole>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("role_code");
        if (activeOnly) {
            queryWrapper.eq("status", RecordStatus.ACTIVE.getValue());
        }
        return originRoleMapper.selectList(queryWrapper);
    }

    /**
     * 切换原平台角色字典状态。
     *
     * @param id 角色字典 ID。
     * @param status 目标状态。
     * @return 已切换状态的角色字典实体。
     */
    private OriginRole changeStatus(String id, String status) {
        OriginRole existing = getOriginRoleById(id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        originRoleMapper.updateById(existing);
        return existing;
    }

    /**
     * 校验创建角色字典所需的最小字段。
     *
     * @param originRole 原平台角色字典实体。
     */
    private void validateCreateFields(OriginRole originRole) {
        if (originRole == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(originRole.getId());
        requireText(originRole.getTenantId());
        requireText(originRole.getConnectorSystemId());
        requireText(originRole.getRoleCode());
        validateEditableFields(originRole);
    }

    /**
     * 校验更新角色字典所需的最小字段。
     *
     * @param originRole 原平台角色字典实体。
     */
    private void validateUpdateFields(OriginRole originRole) {
        if (originRole == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(originRole.getId());
        validateEditableFields(originRole);
    }

    /**
     * 校验可编辑字段，确保下拉项有稳定可读的展示名称。
     *
     * @param originRole 原平台角色字典实体。
     */
    private void validateEditableFields(OriginRole originRole) {
        requireText(originRole.getRoleName());
    }

    /**
     * 校验角色绑定的原平台存在，避免孤立角色污染模块流程配置。
     *
     * @param originRole 原平台角色字典实体。
     */
    private void validateConnectorSystemReference(OriginRole originRole) {
        ConnectorSystem connectorSystem = connectorSystemMapper.selectOne(new QueryWrapper<ConnectorSystem>()
                .eq("id", originRole.getConnectorSystemId())
                .eq("tenant_id", originRole.getTenantId())
                .eq("deleted", Boolean.FALSE));
        if (connectorSystem == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 校验同一原平台下角色编码唯一，避免模块参与方配置时同一个编码指向多个角色。
     *
     * @param originRole 原平台角色字典实体。
     */
    private void ensureRoleCodeUnique(OriginRole originRole) {
        Integer count = originRoleMapper.selectCount(new QueryWrapper<OriginRole>()
                .eq("tenant_id", originRole.getTenantId())
                .eq("connector_system_id", originRole.getConnectorSystemId())
                .eq("role_code", originRole.getRoleCode())
                .eq("deleted", Boolean.FALSE));
        if (count != null && count > 0) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 补齐创建角色字典时的生命周期字段。
     *
     * @param originRole 原平台角色字典实体。
     */
    private void fillCreateDefaults(OriginRole originRole) {
        LocalDateTime now = LocalDateTime.now();
        if (originRole.getCreateTime() == null) {
            originRole.setCreateTime(now);
        }
        if (originRole.getUpdateTime() == null) {
            originRole.setUpdateTime(now);
        }
        if (!StringUtils.hasText(originRole.getCreateBy())) {
            originRole.setCreateBy("system");
        }
        if (!StringUtils.hasText(originRole.getUpdateBy())) {
            originRole.setUpdateBy(originRole.getCreateBy());
        }
        if (!StringUtils.hasText(originRole.getStatus())) {
            originRole.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (originRole.getDeleted() == null) {
            originRole.setDeleted(Boolean.FALSE);
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
