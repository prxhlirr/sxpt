package com.sxpt.module.capture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureResourceSnapshot;
import com.sxpt.module.capture.entity.CaptureSession;
import com.sxpt.module.capture.mapper.CaptureResourceSnapshotMapper;
import com.sxpt.module.capture.mapper.CaptureSessionMapper;
import com.sxpt.module.capture.service.CaptureResourcePromoteService;
import com.sxpt.module.connector.entity.ConnectorResource;
import com.sxpt.module.connector.service.ConnectorResourceService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * 采集资源快照晋升正式资源服务实现。
 *
 * 业务功能：
 * 1. 将资源快照转换为 connector_resource，支撑动作草稿后续绑定正式资源。
 * 2. 通过已有正式资源匹配避免同一个页面元素被重复沉淀。
 *
 * 关键流程：
 * 1. 校验租户、快照和操作人，按租户读取未删除快照。
 * 2. 读取快照所属采集会话，使用会话上的 connectorSystemId 补齐正式资源归属。
 * 3. 优先复用同页面已有正式资源，未命中时委托 ConnectorResourceService 创建。
 */
@Service
@Profile("!test")
public class CaptureResourcePromoteServiceImpl implements CaptureResourcePromoteService {

    private static final int MAX_RESOURCE_CODE_LENGTH = 128;

    private final CaptureResourceSnapshotMapper snapshotMapper;

    private final CaptureSessionMapper sessionMapper;

    private final ConnectorResourceService connectorResourceService;

    public CaptureResourcePromoteServiceImpl(CaptureResourceSnapshotMapper snapshotMapper,
                                             CaptureSessionMapper sessionMapper,
                                             ConnectorResourceService connectorResourceService) {
        this.snapshotMapper = snapshotMapper;
        this.sessionMapper = sessionMapper;
        this.connectorResourceService = connectorResourceService;
    }

    /**
     * 将单个资源快照晋升为正式资源。
     *
     * @param tenantId 租户 ID。
     * @param snapshotId 采集资源快照 ID。
     * @param operatorId 触发晋升的老师或系统操作人 ID。
     * @return 已创建或已存在的正式资源。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConnectorResource promoteSnapshot(String tenantId, String snapshotId, String operatorId) {
        requireText(tenantId);
        requireText(snapshotId);
        requireText(operatorId);
        CaptureResourceSnapshot snapshot = loadSnapshot(tenantId, snapshotId);
        CaptureSession session = loadSession(tenantId, snapshot.getCaptureSessionId());
        ConnectorResource existed = findExistingResource(snapshot, session);
        if (existed != null) {
            writePromotedResource(snapshot, existed.getId(), operatorId);
            return existed;
        }
        ConnectorResource created = connectorResourceService.createConnectorResource(
                buildConnectorResource(snapshot, session, operatorId));
        writePromotedResource(snapshot, created.getId(), operatorId);
        return created;
    }

    /**
     * 读取待晋升的采集资源快照。
     *
     * @param tenantId 租户 ID。
     * @param snapshotId 快照 ID。
     * @return 采集资源快照。
     */
    private CaptureResourceSnapshot loadSnapshot(String tenantId, String snapshotId) {
        CaptureResourceSnapshot snapshot = snapshotMapper.selectOne(new QueryWrapper<CaptureResourceSnapshot>()
                .eq("tenant_id", tenantId)
                .eq("id", snapshotId)
                .eq("deleted", Boolean.FALSE));
        if (snapshot == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return snapshot;
    }

    /**
     * 读取快照所属采集会话。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @return 采集会话。
     */
    private CaptureSession loadSession(String tenantId, String captureSessionId) {
        CaptureSession session = sessionMapper.selectOne(new QueryWrapper<CaptureSession>()
                .eq("tenant_id", tenantId)
                .eq("id", captureSessionId)
                .eq("deleted", Boolean.FALSE));
        if (session == null || !StringUtils.hasText(session.getConnectorSystemId())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        return session;
    }

    /**
     * 从同页面正式资源中查找可复用资源。
     *
     * @param snapshot 采集资源快照。
     * @param session 采集会话。
     * @return 已存在的正式资源；没有命中时返回 null。
     */
    private ConnectorResource findExistingResource(CaptureResourceSnapshot snapshot, CaptureSession session) {
        List<ConnectorResource> resources = connectorResourceService.listByPage(
                snapshot.getTenantId(), session.getConnectorSystemId(), snapshot.getPageUrl());
        for (ConnectorResource resource : resources) {
            if (samePromotedResource(snapshot, resource)
                    || sameStableKey(snapshot, resource)
                    || sameLocator(snapshot, resource)
                    || samePageResource(snapshot, resource)) {
                return resource;
            }
        }
        return null;
    }

    /**
     * 判断快照是否已经记录过正式资源 ID。
     *
     * @param snapshot 采集资源快照。
     * @param resource 正式资源。
     * @return true 表示资源就是快照已晋升结果。
     */
    private boolean samePromotedResource(CaptureResourceSnapshot snapshot, ConnectorResource resource) {
        return StringUtils.hasText(snapshot.getPromotedResourceId())
                && snapshot.getPromotedResourceId().equals(resource.getId());
    }

    /**
     * 判断快照哈希是否能匹配正式资源稳定键。
     *
     * @param snapshot 采集资源快照。
     * @param resource 正式资源。
     * @return true 表示稳定键一致。
     */
    private boolean sameStableKey(CaptureResourceSnapshot snapshot, ConnectorResource resource) {
        return StringUtils.hasText(snapshot.getSnapshotHash())
                && snapshot.getSnapshotHash().equals(resource.getStableKey());
    }

    /**
     * 判断元素定位表达式是否一致。
     *
     * @param snapshot 采集资源快照。
     * @param resource 正式资源。
     * @return true 表示定位一致。
     */
    private boolean sameLocator(CaptureResourceSnapshot snapshot, ConnectorResource resource) {
        return StringUtils.hasText(snapshot.getResourceLocator())
                && snapshot.getResourceLocator().equals(resource.getLocator());
    }

    /**
     * 判断同页面、同类型、同名称资源是否一致。
     *
     * @param snapshot 采集资源快照。
     * @param resource 正式资源。
     * @return true 表示可作为兜底匹配。
     */
    private boolean samePageResource(CaptureResourceSnapshot snapshot, ConnectorResource resource) {
        return textEquals(snapshot.getPageUrl(), resource.getPageUrl())
                && textEquals(snapshot.getResourceType(), resource.getResourceType())
                && StringUtils.hasText(snapshot.getResourceName())
                && snapshot.getResourceName().equals(resource.getResourceName());
    }

    /**
     * 根据快照构造正式资源。
     *
     * @param snapshot 采集资源快照。
     * @param session 采集会话。
     * @param operatorId 操作人 ID。
     * @return 待创建的正式资源。
     */
    private ConnectorResource buildConnectorResource(CaptureResourceSnapshot snapshot, CaptureSession session,
                                                     String operatorId) {
        ConnectorResource resource = new ConnectorResource();
        resource.setId(generateId());
        resource.setTenantId(snapshot.getTenantId());
        resource.setConnectorSystemId(session.getConnectorSystemId());
        resource.setResourceCode(buildResourceCode(snapshot));
        resource.setResourceName(buildResourceName(snapshot));
        resource.setResourceType(snapshot.getResourceType());
        resource.setPageUrl(snapshot.getPageUrl());
        resource.setLocator(snapshot.getResourceLocator());
        resource.setStableKey(snapshot.getSnapshotHash());
        resource.setMetadataJson(snapshot.getMetadataJson());
        resource.setSourceCaptureId(snapshot.getCaptureSessionId());
        resource.setCreateBy(operatorId);
        resource.setUpdateBy(operatorId);
        return resource;
    }

    /**
     * 回写快照晋升结果。
     *
     * @param snapshot 采集资源快照。
     * @param resourceId 正式资源 ID。
     * @param operatorId 操作人 ID。
     */
    private void writePromotedResource(CaptureResourceSnapshot snapshot, String resourceId, String operatorId) {
        if (!StringUtils.hasText(resourceId) || resourceId.equals(snapshot.getPromotedResourceId())) {
            return;
        }
        snapshot.setPromotedResourceId(resourceId);
        snapshot.setUpdateBy(operatorId);
        snapshotMapper.updateById(snapshot);
    }

    /**
     * 生成正式资源编码。
     *
     * @param snapshot 采集资源快照。
     * @return 不超过数据库长度限制的资源编码。
     */
    private String buildResourceCode(CaptureResourceSnapshot snapshot) {
        String codeSource = StringUtils.hasText(snapshot.getSnapshotHash())
                ? snapshot.getSnapshotHash()
                : snapshot.getResourceType() + "_" + snapshot.getId();
        return trimToMaxLength(codeSource.replaceAll("[^A-Za-z0-9_\\-]", "_"), MAX_RESOURCE_CODE_LENGTH);
    }

    /**
     * 生成正式资源名称。
     *
     * @param snapshot 采集资源快照。
     * @return 可读资源名称。
     */
    private String buildResourceName(CaptureResourceSnapshot snapshot) {
        if (StringUtils.hasText(snapshot.getResourceName())) {
            return snapshot.getResourceName().trim();
        }
        if (StringUtils.hasText(snapshot.getPageTitle())) {
            return snapshot.getPageTitle().trim();
        }
        return snapshot.getPageUrl().trim();
    }

    /**
     * 判断两个文本是否相等。
     *
     * @param left 左侧文本。
     * @param right 右侧文本。
     * @return true 表示相等。
     */
    private boolean textEquals(String left, String right) {
        return StringUtils.hasText(left) && left.equals(right);
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
     * 按最大长度截断文本。
     *
     * @param value 原始文本。
     * @param maxLength 最大长度。
     * @return 截断后的文本。
     */
    private String trimToMaxLength(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 生成正式资源主键。
     *
     * @return 32 位无横线 UUID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
