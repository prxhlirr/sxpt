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

