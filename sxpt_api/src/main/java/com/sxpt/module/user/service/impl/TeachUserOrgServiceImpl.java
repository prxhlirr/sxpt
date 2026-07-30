package com.sxpt.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.user.entity.TeachUserOrg;
import com.sxpt.module.user.mapper.TeachUserOrgMapper;
import com.sxpt.module.user.service.TeachUserOrgService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教学用户组织关系服务实现。
 *
 * 业务功能：
 * 1. 创建教学用户与班级、课程班或分组之间的关系。
 * 2. 统一补齐状态、软删除和时间字段，避免 Controller 分散处理默认值。
 *
 * 关键流程：
 * 1. 校验 ID、租户、用户 ID、组织 ID 和关系类型。
 * 2. 补齐创建时间、更新时间、通用状态和软删除标记。
 * 3. 调用 TeachUserOrgMapper 写入 teach_user_org 表。
 */
@Service
@Profile("!test")
public class TeachUserOrgServiceImpl implements TeachUserOrgService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final String DISABLED_STATUS = "DISABLED";

    private final TeachUserOrgMapper teachUserOrgMapper;

    public TeachUserOrgServiceImpl(TeachUserOrgMapper teachUserOrgMapper) {
        this.teachUserOrgMapper = teachUserOrgMapper;
    }

    /**
     * 添加用户到教学组织。
     *
     * @param teachUserOrg 用户组织关系实体，必须包含 ID、租户、用户 ID、组织 ID 和关系类型。
     * @return 已保存的用户组织关系实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachUserOrg addUserToOrg(TeachUserOrg teachUserOrg) {
        validateAddFields(teachUserOrg);
        fillCreateDefaults(teachUserOrg);
        teachUserOrgMapper.insert(teachUserOrg);
        return teachUserOrg;
    }

    /**
     * 从教学组织中移除用户。
     *
     * @param tenantId 租户 ID。
     * @param orgId 教学组织 ID。
     * @param userId 用户 ID。
     * @return 已软删除的用户组织关系实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachUserOrg removeUserFromOrg(String tenantId, String orgId, String userId) {
        requireText(tenantId);
        requireText(orgId);
        requireText(userId);
        TeachUserOrg existing = teachUserOrgMapper.selectOne(new QueryWrapper<TeachUserOrg>()
                .eq("tenant_id", tenantId)
                .eq("org_id", orgId)
                .eq("user_id", userId)
                .eq("deleted", Boolean.FALSE));
        if (existing == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        existing.setDeleted(Boolean.TRUE);
        existing.setStatus(DISABLED_STATUS);
        existing.setUpdateTime(LocalDateTime.now());
        teachUserOrgMapper.updateById(existing);
        return existing;
    }

    /**
     * 查询租户下的用户教学组织关系列表。
     *
     * 业务功能：支撑管理端查看用户与班级、课程班、分组的绑定关系，保证任务发布和数据准备学生范围可追溯。
     * 关键流程：校验租户后只读取未软删除关系，按创建时间倒序展示最近维护结果。
     *
     * @param tenantId 租户 ID。
     * @return 用户教学组织关系列表。
     */
    @Override
    public List<TeachUserOrg> listUserOrgsByTenantId(String tenantId) {
        requireText(tenantId);
        return teachUserOrgMapper.selectList(new QueryWrapper<TeachUserOrg>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 校验添加用户到组织所需的最小字段。
     *
     * @param teachUserOrg 用户组织关系实体。
     */
    private void validateAddFields(TeachUserOrg teachUserOrg) {
        if (teachUserOrg == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(teachUserOrg.getId());
        requireText(teachUserOrg.getTenantId());
        requireText(teachUserOrg.getUserId());
        requireText(teachUserOrg.getOrgId());
        requireText(teachUserOrg.getRelationType());
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
     * 补齐创建关系时的默认字段。
     *
     * @param teachUserOrg 用户组织关系实体。
     */
    private void fillCreateDefaults(TeachUserOrg teachUserOrg) {
        LocalDateTime now = LocalDateTime.now();
        if (teachUserOrg.getCreateTime() == null) {
            teachUserOrg.setCreateTime(now);
        }
        if (teachUserOrg.getUpdateTime() == null) {
            teachUserOrg.setUpdateTime(now);
        }
        if (!StringUtils.hasText(teachUserOrg.getStatus())) {
            teachUserOrg.setStatus(DEFAULT_STATUS);
        }
        if (teachUserOrg.getDeleted() == null) {
            teachUserOrg.setDeleted(Boolean.FALSE);
        }
    }
}

