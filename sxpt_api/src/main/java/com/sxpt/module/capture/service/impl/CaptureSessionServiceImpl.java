package com.sxpt.module.capture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureSession;
import com.sxpt.module.capture.mapper.CaptureSessionMapper;
import com.sxpt.module.capture.service.CaptureSessionService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 备案采集会话服务实现。
 *
 * 业务功能：
 * 1. 创建 capture_session 记录，为教师备案跳转和后续 SDK 采集建立会话上下文。
 * 2. 查询和结束教师采集会话，支撑备案入口页的会话列表和结束采集操作。
 *
 * 关键流程：
 * 1. 校验创建会话所需的最小字段。
 * 2. 补齐采集模式、会话状态和通用生命周期默认值。
 * 3. 查询时按租户、教师和软删除边界过滤，避免跨教师读取备案会话。
 */
@Service
@Profile("!test")
public class CaptureSessionServiceImpl implements CaptureSessionService {

    private static final String DEFAULT_CAPTURE_MODE = "STANDARD";

    private static final String SESSION_STATUS_RUNNING = "RUNNING";

    private static final String SESSION_STATUS_FINISHED = "FINISHED";

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final CaptureSessionMapper captureSessionMapper;

    public CaptureSessionServiceImpl(CaptureSessionMapper captureSessionMapper) {
        this.captureSessionMapper = captureSessionMapper;
    }

    /**
     * 创建备案采集会话。
     *
     * @param captureSession 备案采集会话实体。
     * @return 已保存的备案采集会话。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaptureSession createCaptureSession(CaptureSession captureSession) {
        validateCreateFields(captureSession);
        fillCreateDefaults(captureSession);
        captureSessionMapper.insert(captureSession);
        return captureSession;
    }

    /**
     * 查询教师的备案采集会话。
     *
     * @param tenantId 租户 ID。
     * @param teacherId 教师用户 ID。
     * @return 备案采集会话列表。
     */
    @Override
    public List<CaptureSession> listByTeacher(String tenantId, String teacherId) {
        requireText(tenantId);
        requireText(teacherId);
        return captureSessionMapper.selectList(new QueryWrapper<CaptureSession>()
                .eq("tenant_id", tenantId)
                .eq("teacher_id", teacherId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 结束备案采集会话。
     *
     * @param id 备案采集会话 ID。
     * @return 已结束的备案采集会话。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaptureSession finishCaptureSession(String id) {
        requireText(id);
        CaptureSession captureSession = captureSessionMapper.selectById(id);
        if (captureSession == null || Boolean.TRUE.equals(captureSession.getDeleted())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        if (SESSION_STATUS_FINISHED.equals(captureSession.getSessionStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        LocalDateTime now = LocalDateTime.now();
        captureSession.setSessionStatus(SESSION_STATUS_FINISHED);
        captureSession.setEndTime(now);
        captureSession.setUpdateTime(now);
        captureSessionMapper.updateById(captureSession);
        return captureSession;
    }

    /**
     * 校验创建会话所需的最小字段。
     *
     * @param captureSession 备案采集会话实体。
     */
    private void validateCreateFields(CaptureSession captureSession) {
        if (captureSession == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(captureSession.getId());
        requireText(captureSession.getTenantId());
        requireText(captureSession.getConnectorSystemId());
        requireText(captureSession.getTeacherId());
        requireText(captureSession.getSessionName());
        requireText(captureSession.getStartUrl());
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
     * @param captureSession 备案采集会话实体。
     */
    private void fillCreateDefaults(CaptureSession captureSession) {
        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(captureSession.getCaptureMode())) {
            captureSession.setCaptureMode(DEFAULT_CAPTURE_MODE);
        }
        if (!StringUtils.hasText(captureSession.getSessionStatus())) {
            captureSession.setSessionStatus(SESSION_STATUS_RUNNING);
        }
        if (captureSession.getStartTime() == null) {
            captureSession.setStartTime(now);
        }
        if (captureSession.getCreateTime() == null) {
            captureSession.setCreateTime(now);
        }
        if (captureSession.getUpdateTime() == null) {
            captureSession.setUpdateTime(now);
        }
        if (!StringUtils.hasText(captureSession.getStatus())) {
            captureSession.setStatus(DEFAULT_STATUS);
        }
        if (captureSession.getDeleted() == null) {
            captureSession.setDeleted(Boolean.FALSE);
        }
    }
}

