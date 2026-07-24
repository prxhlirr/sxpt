package com.sxpt.module.capture;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureResourceSnapshot;
import com.sxpt.module.capture.entity.CaptureSession;
import com.sxpt.module.capture.mapper.CaptureResourceSnapshotMapper;
import com.sxpt.module.capture.mapper.CaptureSessionMapper;
import com.sxpt.module.capture.service.CaptureResourcePromoteService;
import com.sxpt.module.capture.service.impl.CaptureResourcePromoteServiceImpl;
import com.sxpt.module.connector.entity.ConnectorResource;
import com.sxpt.module.connector.service.ConnectorResourceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 采集资源快照晋升正式资源服务测试。
 *
 * 业务功能：
 * 1. 验证资源快照可以转换为正式 connector_resource。
 * 2. 验证已有正式资源可被复用，避免重复沉淀同一页面元素。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper 和 ConnectorResourceService，聚焦晋升编排规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class CaptureResourcePromoteServiceImplTests {

    private final CaptureResourceSnapshotMapper snapshotMapper = mock(CaptureResourceSnapshotMapper.class);

    private final CaptureSessionMapper sessionMapper = mock(CaptureSessionMapper.class);

    private final ConnectorResourceService connectorResourceService = mock(ConnectorResourceService.class);

    private final CaptureResourcePromoteService promoteService = new CaptureResourcePromoteServiceImpl(
            snapshotMapper, sessionMapper, connectorResourceService);

    /**
     * 校验快照可创建正式资源，并从采集会话继承原平台 ID。
     */
    @Test
    void promoteSnapshotShouldCreateConnectorResourceFromSnapshot() {
        when(snapshotMapper.selectOne(any())).thenReturn(buildSnapshot());
        when(sessionMapper.selectOne(any())).thenReturn(buildSession());
        when(connectorResourceService.listByPage("tenant_001", "connector_001", "https://origin.example.com/page"))
                .thenReturn(Collections.emptyList());
        when(connectorResourceService.createConnectorResource(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConnectorResource resource = promoteService.promoteSnapshot("tenant_001", "snap_001", "teacher_001");

        assertNotNull(resource.getId());
        assertEquals("tenant_001", resource.getTenantId());
        assertEquals("connector_001", resource.getConnectorSystemId());
        assertEquals("hash_001", resource.getResourceCode());
        assertEquals("提交按钮", resource.getResourceName());
        assertEquals("BUTTON", resource.getResourceType());
        assertEquals("#submit", resource.getLocator());
        assertEquals("hash_001", resource.getStableKey());
        assertEquals("cap_001", resource.getSourceCaptureId());
        assertEquals("teacher_001", resource.getCreateBy());
        assertEquals("teacher_001", resource.getUpdateBy());
        verify(snapshotMapper).updateById(any(CaptureResourceSnapshot.class));
    }

    /**
     * 校验同稳定键已有正式资源时直接复用并回写快照，避免重复创建。
     */
    @Test
    void promoteSnapshotShouldReuseExistingResourceByStableKey() {
        ConnectorResource existed = new ConnectorResource();
        existed.setId("res_001");
        existed.setStableKey("hash_001");
        when(snapshotMapper.selectOne(any())).thenReturn(buildSnapshot());
        when(sessionMapper.selectOne(any())).thenReturn(buildSession());
        when(connectorResourceService.listByPage("tenant_001", "connector_001", "https://origin.example.com/page"))
                .thenReturn(Collections.singletonList(existed));

        ConnectorResource resource = promoteService.promoteSnapshot("tenant_001", "snap_001", "teacher_001");

        assertSame(existed, resource);
        verify(connectorResourceService, times(0)).createConnectorResource(any());
        ArgumentCaptor<CaptureResourceSnapshot> captor = ArgumentCaptor.forClass(CaptureResourceSnapshot.class);
        verify(snapshotMapper).updateById(captor.capture());
        assertEquals("res_001", captor.getValue().getPromotedResourceId());
        assertEquals("teacher_001", captor.getValue().getUpdateBy());
    }

    /**
     * 校验快照缺失时拒绝晋升，避免无来源正式资源污染资源库。
     */
    @Test
    void promoteSnapshotShouldRejectMissingSnapshot() {
        when(snapshotMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> promoteService.promoteSnapshot("tenant_001", "snap_001", "teacher_001"));
    }

    /**
     * 校验快照哈希缺失时使用资源类型和快照 ID 生成资源编码。
     */
    @Test
    void promoteSnapshotShouldFallbackResourceCodeWhenHashMissing() {
        CaptureResourceSnapshot snapshot = buildSnapshot();
        snapshot.setSnapshotHash(" ");
        when(snapshotMapper.selectOne(any())).thenReturn(snapshot);
        when(sessionMapper.selectOne(any())).thenReturn(buildSession());
        when(connectorResourceService.listByPage(any(), any(), any())).thenReturn(Collections.emptyList());
        when(connectorResourceService.createConnectorResource(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        promoteService.promoteSnapshot("tenant_001", "snap_001", "teacher_001");

        ArgumentCaptor<ConnectorResource> captor = ArgumentCaptor.forClass(ConnectorResource.class);
        verify(connectorResourceService).createConnectorResource(captor.capture());
        assertEquals("BUTTON_snap_001", captor.getValue().getResourceCode());
    }

    /**
     * 校验快照已指向同一个正式资源时不重复回写，减少无意义数据库更新。
     */
    @Test
    void promoteSnapshotShouldSkipUpdateWhenPromotedResourceAlreadySame() {
        CaptureResourceSnapshot snapshot = buildSnapshot();
        snapshot.setPromotedResourceId("res_001");
        ConnectorResource existed = new ConnectorResource();
        existed.setId("res_001");
        when(snapshotMapper.selectOne(any())).thenReturn(snapshot);
        when(sessionMapper.selectOne(any())).thenReturn(buildSession());
        when(connectorResourceService.listByPage(any(), any(), any())).thenReturn(Collections.singletonList(existed));

        ConnectorResource resource = promoteService.promoteSnapshot("tenant_001", "snap_001", "teacher_001");

        assertSame(existed, resource);
        verify(snapshotMapper, times(0)).updateById(any(CaptureResourceSnapshot.class));
    }

    /**
     * 构造最小有效采集资源快照。
     *
     * @return 采集资源快照。
     */
    private CaptureResourceSnapshot buildSnapshot() {
        CaptureResourceSnapshot snapshot = new CaptureResourceSnapshot();
        snapshot.setId("snap_001");
        snapshot.setTenantId("tenant_001");
        snapshot.setCaptureSessionId("cap_001");
        snapshot.setPageUrl("https://origin.example.com/page");
        snapshot.setPageTitle("备案页面");
        snapshot.setResourceType("BUTTON");
        snapshot.setResourceName("提交按钮");
        snapshot.setResourceLocator("#submit");
        snapshot.setMetadataJson("{\"text\":\"提交\"}");
        snapshot.setSnapshotHash("hash_001");
        return snapshot;
    }

    /**
     * 构造包含原平台 ID 的采集会话。
     *
     * @return 采集会话。
     */
    private CaptureSession buildSession() {
        CaptureSession session = new CaptureSession();
        session.setId("cap_001");
        session.setTenantId("tenant_001");
        session.setConnectorSystemId("connector_001");
        return session;
    }
}
