package com.sxpt.module.capture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureResourceSnapshot;
import com.sxpt.module.capture.mapper.CaptureResourceSnapshotMapper;
import com.sxpt.module.capture.service.CaptureResourceSnapshotService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集资源快照服务实现。
 *
 * 业务功能：
 * 1. 保存 SDK 上报的页面摘要和关键元素摘要，为后续动作草稿生成提供资源证据。
 * 2. 拦截默认全量 DOM 保存，只允许 DEBUG_FULL_DOM 在短期过期策略下临时留存。
 *
 * 关键流程：
 * 1. 校验租户、采集会话、页面地址、资源类型、快照范围和摘要 JSON。
 * 2. DEBUG_FULL_DOM 必须设置 30 天内过期时间，避免排障数据长期膨胀。
 */
@Service
@Profile("!test")
public class CaptureResourceSnapshotServiceImpl implements CaptureResourceSnapshotService {

    private static final String DEBUG_FULL_DOM_SCOPE = "DEBUG_FULL_DOM";

    private static final String DEFAULT_ARCHIVE_STATUS = "NONE";

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final long MAX_DEBUG_FULL_DOM_RETENTION_DAYS = 30L;

    private final CaptureResourceSnapshotMapper snapshotMapper;

    public CaptureResourceSnapshotServiceImpl(CaptureResourceSnapshotMapper snapshotMapper) {
        this.snapshotMapper = snapshotMapper;
    }

    /**
     * 上报单条采集资源快照。
     *
     * @param snapshot 采集资源快照。
     * @return 已保存的采集资源快照。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaptureResourceSnapshot reportSnapshot(CaptureResourceSnapshot snapshot) {
        validateReportFields(snapshot);
        fillCreateDefaults(snapshot);
        snapshotMapper.insert(snapshot);
        return snapshot;
    }

    /**
     * 查询指定采集会话下的资源快照。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @return 按创建时间倒序排列的资源快照列表。
     */
    @Override
    public List<CaptureResourceSnapshot> listBySession(String tenantId, String captureSessionId) {
        requireText(tenantId);
        requireText(captureSessionId);
        return snapshotMapper.selectList(new QueryWrapper<CaptureResourceSnapshot>()
                .eq("tenant_id", tenantId)
                .eq("capture_session_id", captureSessionId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 校验资源快照上报所需的最小字段和 DEBUG 全量 DOM 规则。
     *
     * @param snapshot 采集资源快照。
     */
    private void validateReportFields(CaptureResourceSnapshot snapshot) {
        if (snapshot == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(snapshot.getId());
        requireText(snapshot.getTenantId());
        requireText(snapshot.getCaptureSessionId());
        requireText(snapshot.getPageUrl());
        requireText(snapshot.getResourceType());
        requireText(snapshot.getSnapshotScope());
        requireText(snapshot.getElementSnapshotJson());
        validateDebugFullDom(snapshot);
    }

    /**
     * 校验 DEBUG_FULL_DOM 只能短期保留。
     *
     * @param snapshot 采集资源快照。
     */
    private void validateDebugFullDom(CaptureResourceSnapshot snapshot) {
        if (!DEBUG_FULL_DOM_SCOPE.equals(snapshot.getSnapshotScope())) {
            return;
        }
        LocalDateTime expireTime = snapshot.getExpireTime();
        LocalDateTime latestExpireTime = LocalDateTime.now().plusDays(MAX_DEBUG_FULL_DOM_RETENTION_DAYS);
        if (expireTime == null || expireTime.isAfter(latestExpireTime)) {
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
     * 补齐快照创建时的默认字段。
     *
     * @param snapshot 采集资源快照。
     */
    private void fillCreateDefaults(CaptureResourceSnapshot snapshot) {
        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(snapshot.getArchiveStatus())) {
            snapshot.setArchiveStatus(DEFAULT_ARCHIVE_STATUS);
        }
        if (snapshot.getCreateTime() == null) {
            snapshot.setCreateTime(now);
        }
        if (snapshot.getUpdateTime() == null) {
            snapshot.setUpdateTime(now);
        }
        if (!StringUtils.hasText(snapshot.getStatus())) {
            snapshot.setStatus(DEFAULT_STATUS);
        }
        if (snapshot.getDeleted() == null) {
            snapshot.setDeleted(Boolean.FALSE);
        }
    }
}

