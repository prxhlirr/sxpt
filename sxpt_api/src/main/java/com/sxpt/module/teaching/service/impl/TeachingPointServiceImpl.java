package com.sxpt.module.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.teaching.entity.TeachingPoint;
import com.sxpt.module.teaching.mapper.TeachingPointMapper;
import com.sxpt.module.teaching.service.TeachingPointService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教学点服务实现。
 *
 * 业务功能：
 * 1. 保存教师发布后的教学点，形成后续步骤、评分项和任务引用的核心资产。
 * 2. 按原平台查询教学点，支撑任务发布和教学点管理列表。
 *
 * 关键流程：
 * 1. 创建时校验最小发布字段，避免生成无法被引用的教学点。
 * 2. 补齐默认版本、发布状态、通用状态和软删除字段。
 */
@Service
@Profile("!test")
public class TeachingPointServiceImpl implements TeachingPointService {

    private static final long DEFAULT_VERSION_NO = 1L;

    private static final String DEFAULT_POINT_STATUS = "PUBLISHED";

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final TeachingPointMapper teachingPointMapper;

    public TeachingPointServiceImpl(TeachingPointMapper teachingPointMapper) {
        this.teachingPointMapper = teachingPointMapper;
    }

    /**
     * 发布教学点。
     *
     * @param teachingPoint 教学点实体。
     * @return 已发布的教学点。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingPoint createTeachingPoint(TeachingPoint teachingPoint) {
        validateCreateFields(teachingPoint);
        fillCreateDefaults(teachingPoint);
        teachingPointMapper.insert(teachingPoint);
        return teachingPoint;
    }

    /**
     * 按原平台查询教学点。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @return 教学点列表。
     */
    @Override
    public List<TeachingPoint> listByConnector(String tenantId, String connectorSystemId) {
        requireText(tenantId);
        requireText(connectorSystemId);
        return teachingPointMapper.selectList(new QueryWrapper<TeachingPoint>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("point_code", "version_no"));
    }

    /**
     * 校验创建教学点所需的最小字段。
     *
     * @param teachingPoint 教学点实体。
     */
    private void validateCreateFields(TeachingPoint teachingPoint) {
        if (teachingPoint == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(teachingPoint.getId());
        requireText(teachingPoint.getTenantId());
        requireText(teachingPoint.getConnectorSystemId());
        requireText(teachingPoint.getPointCode());
        requireText(teachingPoint.getPointName());
        requireText(teachingPoint.getPointType());
        requireText(teachingPoint.getExecutionStrategy());
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
     * 补齐创建教学点时的默认字段。
     *
     * @param teachingPoint 教学点实体。
     */
    private void fillCreateDefaults(TeachingPoint teachingPoint) {
        LocalDateTime now = LocalDateTime.now();
        if (teachingPoint.getVersionNo() == null) {
            teachingPoint.setVersionNo(DEFAULT_VERSION_NO);
        }
        if (teachingPoint.getCreateTime() == null) {
            teachingPoint.setCreateTime(now);
        }
        if (teachingPoint.getUpdateTime() == null) {
            teachingPoint.setUpdateTime(now);
        }
        if (!StringUtils.hasText(teachingPoint.getPointStatus())) {
            teachingPoint.setPointStatus(DEFAULT_POINT_STATUS);
        }
        if (!StringUtils.hasText(teachingPoint.getStatus())) {
            teachingPoint.setStatus(DEFAULT_STATUS);
        }
        if (teachingPoint.getDeleted() == null) {
            teachingPoint.setDeleted(Boolean.FALSE);
        }
    }
}

