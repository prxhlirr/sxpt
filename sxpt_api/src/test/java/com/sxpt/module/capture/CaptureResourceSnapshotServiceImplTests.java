package com.sxpt.module.capture;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureResourceSnapshot;
import com.sxpt.module.capture.mapper.CaptureResourceSnapshotMapper;
import com.sxpt.module.capture.service.CaptureResourceSnapshotService;
import com.sxpt.module.capture.service.impl.CaptureResourceSnapshotServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
 * 采集资源快照服务测试。
 *
 * 业务功能：
 * 1. 验证关键元素摘要上报会补齐默认生命周期字段。
 * 2. 验证 DEBUG_FULL_DOM 必须短期过期，避免默认保存全量 DOM。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class CaptureResourceSnapshotServiceImplTests {

    private final CaptureResourceSnapshotMapper mapper = mock(CaptureResourceSnapshotMapper.class);

    private final CaptureResourceSnapshotService service = new CaptureResourceSnapshotServiceImpl(mapper);

    /**
     * 校验关键元素摘要快照首次上报时插入 Mapper 并补齐默认值。
     */
    @Test
    void reportSnapshotShouldInsertAndFillDefaults() {
        CaptureResourceSnapshot snapshot = buildValidSnapshot();

        CaptureResourceSnapshot saved = service.reportSnapshot(snapshot);

        assertSame(snapshot, saved);
        assertEquals("NONE", saved.getArchiveStatus());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 校验缺少元素摘要 JSON 时拒绝上报，避免保存无证据快照。
     */
    @Test
    void reportSnapshotShouldRejectMissingElementSnapshotJson() {
        CaptureResourceSnapshot snapshot = buildValidSnapshot();
        snapshot.setElementSnapshotJson(" ");

        assertThrows(BusinessException.class, () -> service.reportSnapshot(snapshot));
        verify(mapper, times(0)).insert(snapshot);
    }

    /**
     * 校验 DEBUG_FULL_DOM 没有过期时间时拒绝上报。
     */
    @Test
    void reportSnapshotShouldRejectDebugFullDomWithoutExpireTime() {
        CaptureResourceSnapshot snapshot = buildValidSnapshot();
        snapshot.setSnapshotScope("DEBUG_FULL_DOM");

        assertThrows(BusinessException.class, () -> service.reportSnapshot(snapshot));
        verify(mapper, times(0)).insert(snapshot);
    }

    /**
     * 校验 DEBUG_FULL_DOM 超过 30 天保留期时拒绝上报。
     */
    @Test
    void reportSnapshotShouldRejectTooLongDebugFullDomRetention() {
        CaptureResourceSnapshot snapshot = buildValidSnapshot();
        snapshot.setSnapshotScope("DEBUG_FULL_DOM");
        snapshot.setExpireTime(LocalDateTime.now().plusDays(31));

        assertThrows(BusinessException.class, () -> service.reportSnapshot(snapshot));
        verify(mapper, times(0)).insert(snapshot);
    }

    /**
     * 校验按采集会话查询资源快照时返回 Mapper 结果。
     */
    @Test
    void listBySessionShouldReturnMapperResult() {
        CaptureResourceSnapshot snapshot = buildValidSnapshot();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(snapshot));

        List<CaptureResourceSnapshot> result = service.listBySession("tenant_001", "cap_001");

        assertEquals(1, result.size());
        assertSame(snapshot, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造最小有效采集资源快照。
     *
     * @return 采集资源快照实体。
     */
    private CaptureResourceSnapshot buildValidSnapshot() {
        CaptureResourceSnapshot snapshot = new CaptureResourceSnapshot();
        snapshot.setId("snap_001");
        snapshot.setTenantId("tenant_001");
        snapshot.setCaptureSessionId("cap_001");
        snapshot.setPageUrl("https://origin.example.com/record/start");
        snapshot.setPageTitle("备案申请");
        snapshot.setResourceType("BUTTON");
        snapshot.setSnapshotScope("TARGET_ELEMENT");
        snapshot.setResourceName("提交按钮");
        snapshot.setResourceLocator("#submit");
        snapshot.setElementSnapshotJson("{\"text\":\"提交\",\"role\":\"button\"}");
        snapshot.setSnapshotHash("hash_001");
        return snapshot;
    }
}
