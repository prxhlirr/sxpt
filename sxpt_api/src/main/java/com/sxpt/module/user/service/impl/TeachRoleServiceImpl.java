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

