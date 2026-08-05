package com.sxpt.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.user.entity.TeachOrg;
import com.sxpt.module.user.mapper.TeachOrgMapper;
import com.sxpt.module.user.service.TeachOrgService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教学组织服务实现。
 *
 * 业务功能：
 * 1. 创建教学平台班级、课程班和分组，保证后续课程与任务有可引用的教学组织。
 * 2. 在写入前统一补齐基础生命周期字段，避免 Controller 或 Mapper 分散处理默认值。
 *
 * 关键流程：
 * 1. 校验 ID、租户、组织编码、组织名称和组织类型。
 * 2. 补齐创建时间、更新时间、通用状态和软删除标记。
 * 3. 调用 TeachOrgMapper 写入 teach_org 表或查询租户下组织列表。
 */
@Service
@Profile("!test")
public class TeachOrgServiceImpl implements TeachOrgService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final String DISABLED_STATUS = "DISABLED";

    private final TeachOrgMapper teachOrgMapper;

    public TeachOrgServiceImpl(TeachOrgMapper teachOrgMapper) {
        this.teachOrgMapper = teachOrgMapper;
    }

    /**
     * 创建教学组织。
     *
     * @param teachOrg 教学组织实体，必须包含 ID、租户、组织编码、组织名称和组织类型。
     * @return 已保存的教学组织实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachOrg createTeachOrg(TeachOrg teachOrg) {
        validateCreateFields(teachOrg);
        fillCreateDefaults(teachOrg);
        teachOrgMapper.insert(teachOrg);
        return teachOrg;
    }

    /**
     * 更新教学组织基础信息。
     *
     * @param tenantId 租户 ID。
     * @param teachOrg 组织实体，必须包含 ID 和可编辑字段。
     * @return 已更新的组织实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachOrg updateTeachOrg(String tenantId, TeachOrg teachOrg) {
        requireText(tenantId);
        validateUpdateFields(teachOrg);
        TeachOrg existing = getExistingOrg(tenantId, teachOrg.getId());
        existing.setParentId(teachOrg.getParentId());
        existing.setOrgName(teachOrg.getOrgName());
        existing.setOrgType(teachOrg.getOrgType());
        existing.setUpdateTime(LocalDateTime.now());
        teachOrgMapper.updateById(existing);
        return existing;
    }

    /**
     * 切换教学组织启停用状态。
     *
     * @param tenantId 租户 ID。
     * @param id 组织 ID。
     * @param status 目标状态。
     * @return 已更新状态的组织实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachOrg updateTeachOrgStatus(String tenantId, String id, String status) {
        requireSupportedStatus(status);
        TeachOrg existing = getExistingOrg(tenantId, id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        teachOrgMapper.updateById(existing);
        return existing;
    }

    /**
     * 查询指定租户下的教学组织列表。
     *
     * @param tenantId 租户 ID。
     * @return 指定租户下未删除的教学组织列表。
     */
    @Override
    public List<TeachOrg> listTeachOrgsByTenantId(String tenantId) {
        requireText(tenantId);
        return teachOrgMapper.selectList(new QueryWrapper<TeachOrg>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 校验创建教学组织所需的最小字段。
     *
     * @param teachOrg 教学组织实体。
     */
    private void validateCreateFields(TeachOrg teachOrg) {
        if (teachOrg == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(teachOrg.getId());
        requireText(teachOrg.getTenantId());
        requireText(teachOrg.getOrgCode());
        requireText(teachOrg.getOrgName());
        requireText(teachOrg.getOrgType());
    }

    /**
     * 校验组织更新所需字段，组织编码不在编辑入口修改以保持外部引用稳定。
     *
     * @param teachOrg 教学组织实体。
     */
    private void validateUpdateFields(TeachOrg teachOrg) {
        if (teachOrg == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(teachOrg.getId());
        requireText(teachOrg.getOrgName());
        requireText(teachOrg.getOrgType());
    }

    /**
     * 按租户和组织 ID 读取未删除组织，避免跨租户维护单位。
     *
     * @param tenantId 租户 ID。
     * @param id 组织 ID。
     * @return 未删除组织实体。
     */
    private TeachOrg getExistingOrg(String tenantId, String id) {
        requireText(tenantId);
        requireText(id);
        TeachOrg existing = teachOrgMapper.selectOne(new QueryWrapper<TeachOrg>()
                .eq("tenant_id", tenantId)
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (existing == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return existing;
    }

    /**
     * 限制组织维护状态只允许启用和停用。
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
     * @param teachOrg 教学组织实体。
     */
    private void fillCreateDefaults(TeachOrg teachOrg) {
        LocalDateTime now = LocalDateTime.now();
        if (teachOrg.getCreateTime() == null) {
            teachOrg.setCreateTime(now);
        }
        if (teachOrg.getUpdateTime() == null) {
            teachOrg.setUpdateTime(now);
        }
        if (!StringUtils.hasText(teachOrg.getStatus())) {
            teachOrg.setStatus(DEFAULT_STATUS);
        }
        if (teachOrg.getDeleted() == null) {
            teachOrg.setDeleted(Boolean.FALSE);
        }
    }
}

