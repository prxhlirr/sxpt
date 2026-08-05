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

    private static final String DISABLED_STATUS = "DISABLED";

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
     * 更新教学平台用户基础信息。
     *
     * @param tenantId 租户 ID。
     * @param teachUser 用户实体，必须包含 ID 和可编辑字段。
     * @return 已更新的用户实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachUser updateTeachUser(String tenantId, TeachUser teachUser) {
        requireText(tenantId);
        validateUpdateFields(teachUser);
        TeachUser existing = getExistingUser(tenantId, teachUser.getId());
        existing.setRealName(teachUser.getRealName());
        existing.setPhone(teachUser.getPhone());
        existing.setEmail(teachUser.getEmail());
        existing.setUserType(teachUser.getUserType());
        existing.setSourceType(teachUser.getSourceType());
        existing.setStudentNo(teachUser.getStudentNo());
        existing.setEmployeeNo(teachUser.getEmployeeNo());
        existing.setUpdateTime(LocalDateTime.now());
        teachUserMapper.updateById(existing);
        return existing;
    }

    /**
     * 切换教学平台用户启停用状态。
     *
     * @param tenantId 租户 ID。
     * @param id 用户 ID。
     * @param status 目标状态。
     * @return 已更新状态的用户实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachUser updateTeachUserStatus(String tenantId, String id, String status) {
        requireSupportedStatus(status);
        TeachUser existing = getExistingUser(tenantId, id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        teachUserMapper.updateById(existing);
        return existing;
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
     * 校验更新用户所需字段，用户名不可在此入口修改以保持登录标识稳定。
     *
     * @param teachUser 教学平台用户实体。
     */
    private void validateUpdateFields(TeachUser teachUser) {
        if (teachUser == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(teachUser.getId());
        requireText(teachUser.getRealName());
        requireText(teachUser.getUserType());
        requireText(teachUser.getSourceType());
    }

    /**
     * 按租户和用户 ID 读取未删除用户，保证维护操作不会跨租户修改数据。
     *
     * @param tenantId 租户 ID。
     * @param id 用户 ID。
     * @return 未删除用户实体。
     */
    private TeachUser getExistingUser(String tenantId, String id) {
        requireText(tenantId);
        requireText(id);
        TeachUser existing = teachUserMapper.selectOne(new QueryWrapper<TeachUser>()
                .eq("tenant_id", tenantId)
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (existing == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return existing;
    }

    /**
     * 限制基础主数据只允许启用和停用两种维护状态。
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

