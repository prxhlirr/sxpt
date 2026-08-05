package com.sxpt.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.user.entity.TeachRole;
import com.sxpt.module.user.mapper.TeachRoleMapper;
import com.sxpt.module.user.service.TeachRoleService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教学平台角色服务实现。
 *
 * 业务功能：
 * 1. 创建教学平台角色，保证教师、学生、管理员等角色在租户内有稳定定义。
 * 2. 在写入前统一补齐基础生命周期字段，避免 Controller 或 Mapper 分散处理默认值。
 *
 * 关键流程：
 * 1. 校验 ID、租户、角色编码和角色名称。
 * 2. 补齐创建时间、更新时间、通用状态和软删除标记。
 * 3. 调用 TeachRoleMapper 写入 teach_role 表。
 */
@Service
@Profile("!test")
public class TeachRoleServiceImpl implements TeachRoleService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final String DISABLED_STATUS = "DISABLED";

    private final TeachRoleMapper teachRoleMapper;

    public TeachRoleServiceImpl(TeachRoleMapper teachRoleMapper) {
        this.teachRoleMapper = teachRoleMapper;
    }

    /**
     * 创建教学平台角色。
     *
     * @param teachRole 教学平台角色实体，必须包含 ID、租户、角色编码和角色名称。
     * @return 已保存的教学平台角色实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachRole createTeachRole(TeachRole teachRole) {
        validateCreateFields(teachRole);
        fillCreateDefaults(teachRole);
        teachRoleMapper.insert(teachRole);
        return teachRole;
    }

    /**
     * 更新教学平台角色基础信息。
     *
     * @param tenantId 租户 ID。
     * @param teachRole 角色实体，必须包含 ID 和可编辑字段。
     * @return 已更新的角色实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachRole updateTeachRole(String tenantId, TeachRole teachRole) {
        requireText(tenantId);
        validateUpdateFields(teachRole);
        TeachRole existing = getExistingRole(tenantId, teachRole.getId());
        existing.setRoleName(teachRole.getRoleName());
        existing.setDescription(teachRole.getDescription());
        existing.setUpdateTime(LocalDateTime.now());
        teachRoleMapper.updateById(existing);
        return existing;
    }

    /**
     * 切换教学平台角色启停用状态。
     *
     * @param tenantId 租户 ID。
     * @param id 角色 ID。
     * @param status 目标状态。
     * @return 已更新状态的角色实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachRole updateTeachRoleStatus(String tenantId, String id, String status) {
        requireSupportedStatus(status);
        TeachRole existing = getExistingRole(tenantId, id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        teachRoleMapper.updateById(existing);
        return existing;
    }

    /**
     * 查询指定租户下的教学平台角色列表。
     *
     * @param tenantId 租户 ID。
     * @return 指定租户下未删除的教学平台角色列表。
     */
    @Override
    public List<TeachRole> listTeachRolesByTenantId(String tenantId) {
        requireText(tenantId);
        return teachRoleMapper.selectList(new QueryWrapper<TeachRole>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 校验创建教学角色所需的最小字段。
     *
     * @param teachRole 教学平台角色实体。
     */
    private void validateCreateFields(TeachRole teachRole) {
        if (teachRole == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(teachRole.getId());
        requireText(teachRole.getTenantId());
        requireText(teachRole.getRoleCode());
        requireText(teachRole.getRoleName());
    }

    /**
     * 校验角色更新所需字段，角色编码不在编辑入口修改以保持授权引用稳定。
     *
     * @param teachRole 教学平台角色实体。
     */
    private void validateUpdateFields(TeachRole teachRole) {
        if (teachRole == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(teachRole.getId());
        requireText(teachRole.getRoleName());
    }

    /**
     * 按租户和角色 ID 读取未删除角色，避免跨租户维护角色。
     *
     * @param tenantId 租户 ID。
     * @param id 角色 ID。
     * @return 未删除角色实体。
     */
    private TeachRole getExistingRole(String tenantId, String id) {
        requireText(tenantId);
        requireText(id);
        TeachRole existing = teachRoleMapper.selectOne(new QueryWrapper<TeachRole>()
                .eq("tenant_id", tenantId)
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (existing == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return existing;
    }

    /**
     * 限制角色维护状态只允许启用和停用。
     *
     * @param status 目标状态。
     */
    private void requireSupportedStatus(String status) {
        requireText(status);
        if (!DEFAULT_STATUS.equals(status) && !DISABLED_STATUS.equals(status)) {
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
     * @param teachRole 教学平台角色实体。
     */
    private void fillCreateDefaults(TeachRole teachRole) {
        LocalDateTime now = LocalDateTime.now();
        if (teachRole.getCreateTime() == null) {
            teachRole.setCreateTime(now);
        }
        if (teachRole.getUpdateTime() == null) {
            teachRole.setUpdateTime(now);
        }
        if (!StringUtils.hasText(teachRole.getStatus())) {
            teachRole.setStatus(DEFAULT_STATUS);
        }
        if (teachRole.getDeleted() == null) {
            teachRole.setDeleted(Boolean.FALSE);
        }
    }
}

