package com.sxpt.module.capture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureEvent;
import com.sxpt.module.capture.mapper.CaptureEventMapper;
import com.sxpt.module.capture.service.CaptureEventService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SDK 采集事件服务实现。
 *
 * 业务功能：
 * 1. 保存 SDK 上报的页面操作事件，为后续动作草稿和教学步骤沉淀提供事实来源。
 * 2. 基于 clientEventId 处理 SDK 重试幂等，避免重复点击或网络重试造成重复事件。
 *
 * 关键流程：
 * 1. 先校验事件所属租户、采集会话、事件类型、事件时间和顺序号。
 * 2. 当 clientEventId 已存在时直接返回原事件；不存在时补齐默认生命周期字段后插入。
 */
@Service
@Profile("!test")
public class CaptureEventServiceImpl implements CaptureEventService {

    private static final String DEFAULT_ARCHIVE_STATUS = "NONE";

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final CaptureEventMapper captureEventMapper;

    public CaptureEventServiceImpl(CaptureEventMapper captureEventMapper) {
        this.captureEventMapper = captureEventMapper;
    }

    /**
     * 上报单条 SDK 采集事件。
     *
     * @param captureEvent SDK 采集事件。
     * @return 已保存或幂等命中的 SDK 采集事件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaptureEvent reportCaptureEvent(CaptureEvent captureEvent) {
        validateReportFields(captureEvent);
        CaptureEvent existed = findExistedClientEvent(captureEvent);
        if (existed != null) {
            return existed;
        }
        fillCreateDefaults(captureEvent);
        captureEventMapper.insert(captureEvent);
        return captureEvent;
    }

    /**
     * 查询指定采集会话下的 SDK 事件。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @return 按事件顺序排序的 SDK 事件列表。
     */
    @Override
    public List<CaptureEvent> listBySession(String tenantId, String captureSessionId) {
        requireText(tenantId);
        requireText(captureSessionId);
        return captureEventMapper.selectList(new QueryWrapper<CaptureEvent>()
                .eq("tenant_id", tenantId)
                .eq("capture_session_id", captureSessionId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("sequence_no", "event_time"));
    }

    /**
     * 校验 SDK 事件上报所需的最小字段。
     *
     * @param captureEvent SDK 采集事件。
     */
    private void validateReportFields(CaptureEvent captureEvent) {
        if (captureEvent == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(captureEvent.getId());
        requireText(captureEvent.getTenantId());
        requireText(captureEvent.getCaptureSessionId());
        requireText(captureEvent.getEventType());
        if (captureEvent.getEventTime() == null || captureEvent.getSequenceNo() == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 按客户端事件 ID 查找已入库事件。
     *
     * @param captureEvent SDK 采集事件。
     * @return 已存在事件；无 clientEventId 或未命中时返回 null。
     */
    private CaptureEvent findExistedClientEvent(CaptureEvent captureEvent) {
        if (!StringUtils.hasText(captureEvent.getClientEventId())) {
            return null;
        }
        return captureEventMapper.selectOne(new QueryWrapper<CaptureEvent>()
                .eq("tenant_id", captureEvent.getTenantId())
                .eq("capture_session_id", captureEvent.getCaptureSessionId())
                .eq("client_event_id", captureEvent.getClientEventId())
                .eq("deleted", Boolean.FALSE));
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
     * 补齐事件创建时的默认字段。
     *
     * @param captureEvent SDK 采集事件。
     */
    private void fillCreateDefaults(CaptureEvent captureEvent) {
        LocalDateTime now = LocalDateTime.now();
        if (captureEvent.getRetryCount() == null) {
            captureEvent.setRetryCount(0L);
        }
        if (!StringUtils.hasText(captureEvent.getArchiveStatus())) {
            captureEvent.setArchiveStatus(DEFAULT_ARCHIVE_STATUS);
        }
        if (captureEvent.getCreateTime() == null) {
            captureEvent.setCreateTime(now);
        }
        if (captureEvent.getUpdateTime() == null) {
            captureEvent.setUpdateTime(now);
        }
        if (!StringUtils.hasText(captureEvent.getStatus())) {
            captureEvent.setStatus(DEFAULT_STATUS);
        }
        if (captureEvent.getDeleted() == null) {
            captureEvent.setDeleted(Boolean.FALSE);
        }
    }
}

