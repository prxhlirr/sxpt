package com.sxpt.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.user.entity.TeachUserRole;
import com.sxpt.module.user.mapper.TeachUserRoleMapper;
import com.sxpt.module.user.service.TeachUserRoleService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教学平台用户角色关系服务实现。
 *
 * 业务功能：
 * 1. 创建教学用户与教学角色之间的授权关系。
 * 2. 统一补齐授权来源、状态、软删除和时间字段，避免 Controller 分散处理默认值。
 *
 * 关键流程：
 * 1. 校验 ID、租户、用户 ID 和角色 ID。
 * 2. 缺省授权来源时按人工授权处理，便于 MVP 后台维护。
 * 3. 调用 TeachUserRoleMapper 写入 teach_user_role 表。
 */
@Service
@Profile("!test")
public class TeachUserRoleServiceImpl implements TeachUserRoleService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final String DISABLED_STATUS = "DISABLED";

    private static final String DEFAULT_GRANT_SOURCE = "MANUAL";

    private final TeachUserRoleMapper teachUserRoleMapper;

    public TeachUserRoleServiceImpl(TeachUserRoleMapper teachUserRoleMapper) {
        this.teachUserRoleMapper = teachUserRoleMapper;
    }

    /**
     * 为教学用户授予教学平台角色。
     *
     * @param teachUserRole 用户角色关系实体，必须包含 ID、租户、用户 ID 和角色 ID。
     * @return 已保存的用户角色关系实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachUserRole grantUserRole(TeachUserRole teachUserRole) {
        validateGrantFields(teachUserRole);
        fillCreateDefaults(teachUserRole);
        teachUserRoleMapper.insert(teachUserRole);
        return teachUserRole;
    }

    /**
     * 切换用户角色授权关系启停用状态。
     *
     * @param tenantId 租户 ID。
     * @param id 授权关系 ID。
     * @param status 目标状态。
     * @return 已更新状态的用户角色关系实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachUserRole updateUserRoleStatus(String tenantId, String id, String status) {
        requireSupportedStatus(status);
        TeachUserRole existing = getExistingUserRole(tenantId, id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        teachUserRoleMapper.updateById(existing);
        return existing;
    }

    /**
     * 查询租户下的用户角色授权关系列表。
     *
     * 业务功能：支撑管理端查看和复核用户角色授权，保证菜单、权限和数据准备角色边界有可追溯来源。
     * 关键流程：校验租户后按软删除边界查询，按创建时间倒序展示最近授权。
     *
     * @param tenantId 租户 ID。
     * @return 用户角色授权关系列表。
     */
    @Override
    public List<TeachUserRole> listUserRolesByTenantId(String tenantId) {
        requireText(tenantId);
        return teachUserRoleMapper.selectList(new QueryWrapper<TeachUserRole>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 校验授权关系所需的最小字段。
     *
     * @param teachUserRole 用户角色关系实体。
     */
    private void validateGrantFields(TeachUserRole teachUserRole) {
        if (teachUserRole == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(teachUserRole.getId());
        requireText(teachUserRole.getTenantId());
        requireText(teachUserRole.getUserId());
        requireText(teachUserRole.getRoleId());
    }

    /**
     * 按租户和关系 ID 读取未删除授权关系，避免跨租户维护绑定。
     *
     * @param tenantId 租户 ID。
     * @param id 授权关系 ID。
     * @return 未删除用户角色关系实体。
     */
    private TeachUserRole getExistingUserRole(String tenantId, String id) {
        requireText(tenantId);
        requireText(id);
        TeachUserRole existing = teachUserRoleMapper.selectOne(new QueryWrapper<TeachUserRole>()
                .eq("tenant_id", tenantId)
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (existing == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return existing;
    }

    /**
     * 限制用户角色关系只允许启用和停用两种状态。
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
     * 补齐创建授权关系时的默认字段。
     *
     * @param teachUserRole 用户角色关系实体。
     */
    private void fillCreateDefaults(TeachUserRole teachUserRole) {
        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(teachUserRole.getGrantSource())) {
            teachUserRole.setGrantSource(DEFAULT_GRANT_SOURCE);
        }
        if (teachUserRole.getCreateTime() == null) {
            teachUserRole.setCreateTime(now);
        }
        if (teachUserRole.getUpdateTime() == null) {
            teachUserRole.setUpdateTime(now);
        }
        if (!StringUtils.hasText(teachUserRole.getStatus())) {
            teachUserRole.setStatus(DEFAULT_STATUS);
        }
        if (teachUserRole.getDeleted() == null) {
            teachUserRole.setDeleted(Boolean.FALSE);
        }
    }
}

