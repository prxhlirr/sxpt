package com.sxpt.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.user.entity.TeachUser;
import com.sxpt.module.user.mapper.TeachUserMapper;
import com.sxpt.module.user.service.TeachUserService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教学平台用户服务实现。
 *
 * 业务功能：
 * 1. 创建教学平台用户，保证教师、学生、管理员在教学平台内有稳定身份镜像。
 * 2. 在写入前统一补齐基础生命周期字段，避免 Controller 或 Mapper 分散处理默认值。
 *
 * 关键流程：
 * 1. 校验 ID、租户、账号、姓名、用户类型和来源类型。
 * 2. 补齐创建时间、更新时间、通用状态和软删除标记。
 * 3. 调用 TeachUserMapper 写入 teach_user 表。
 */
@Service
@Profile("!test")
public class TeachUserServiceImpl implements TeachUserService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final TeachUserMapper teachUserMapper;

    public TeachUserServiceImpl(TeachUserMapper teachUserMapper) {
        this.teachUserMapper = teachUserMapper;
    }

    /**
     * 创建教学平台用户。
     *
     * @param teachUser 教学平台用户实体，必须包含 ID、租户、账号、姓名、用户类型和来源类型。
     * @return 已保存的教学平台用户实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachUser createTeachUser(TeachUser teachUser) {
        validateCreateFields(teachUser);
        fillCreateDefaults(teachUser);
        teachUserMapper.insert(teachUser);
        return teachUser;
    }

    /**
     * 查询租户下的教学平台用户列表。
     *
     * 业务功能：支撑后台用户管理页面展示真实用户主数据，补齐数据准备依赖的教师、学生和管理员来源。
     * 关键流程：先校验租户，再按软删除边界查询，按创建时间倒序方便管理员看到最近维护的数据。
     *
     * @param tenantId 租户 ID。
     * @return 教学平台用户列表。
     */
    @Override
    public List<TeachUser> listTeachUsersByTenantId(String tenantId) {
        requireText(tenantId);
        return teachUserMapper.selectList(new QueryWrapper<TeachUser>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 校验创建教学用户所需的最小字段。
     *
     * @param teachUser 教学平台用户实体。
     */
    private void validateCreateFields(TeachUser teachUser) {
        if (teachUser == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(teachUser.getId());
        requireText(teachUser.getTenantId());
        requireText(teachUser.getUsername());
        requireText(teachUser.getRealName());
        requireText(teachUser.getUserType());
        requireText(teachUser.getSourceType());
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
     * @param teachUser 教学平台用户实体。
     */
    private void fillCreateDefaults(TeachUser teachUser) {
        LocalDateTime now = LocalDateTime.now();
        if (teachUser.getCreateTime() == null) {
            teachUser.setCreateTime(now);
        }
        if (teachUser.getUpdateTime() == null) {
            teachUser.setUpdateTime(now);
        }
        if (!StringUtils.hasText(teachUser.getStatus())) {
            teachUser.setStatus(DEFAULT_STATUS);
        }
        if (teachUser.getDeleted() == null) {
            teachUser.setDeleted(Boolean.FALSE);
        }
    }
}

