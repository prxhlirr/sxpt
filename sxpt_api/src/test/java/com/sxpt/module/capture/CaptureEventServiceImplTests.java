package com.sxpt.module.capture;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureEvent;
import com.sxpt.module.capture.mapper.CaptureEventMapper;
import com.sxpt.module.capture.service.CaptureEventService;
import com.sxpt.module.capture.service.impl.CaptureEventServiceImpl;
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
 * SDK 采集事件服务测试。
 *
 * 业务功能：
 * 1. 验证 SDK 事件上报会补齐归档、重试次数和通用生命周期默认值。
 * 2. 验证 clientEventId 幂等规则，避免 SDK 网络重试造成重复入库。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class CaptureEventServiceImplTests {

    private final CaptureEventMapper mapper = mock(CaptureEventMapper.class);

    private final CaptureEventService service = new CaptureEventServiceImpl(mapper);

    /**
     * 校验事件首次上报时插入 Mapper 并补齐默认值。
     */
    @Test
    void reportCaptureEventShouldInsertAndFillDefaults() {
        CaptureEvent captureEvent = buildValidEvent();

        CaptureEvent saved = service.reportCaptureEvent(captureEvent);

        assertSame(captureEvent, saved);
        assertEquals(0L, saved.getRetryCount());
        assertEquals("NONE", saved.getArchiveStatus());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).selectOne(any());
        verify(mapper).insert(saved);
    }

    /**
     * 校验同一个 clientEventId 重试上报时返回既有事件，不重复插入。
     */
    @Test
    void reportCaptureEventShouldReturnExistedClientEvent() {
        CaptureEvent captureEvent = buildValidEvent();
        CaptureEvent existed = buildValidEvent();
        existed.setId("evt_existed");
        when(mapper.selectOne(any())).thenReturn(existed);

        CaptureEvent result = service.reportCaptureEvent(captureEvent);

        assertSame(existed, result);
        verify(mapper).selectOne(any());
        verify(mapper, times(0)).insert(captureEvent);
    }

    /**
     * 校验缺少事件类型时拒绝上报，避免产生无法归类的采集事实。
     */
    @Test
    void reportCaptureEventShouldRejectMissingEventType() {
        CaptureEvent captureEvent = buildValidEvent();
        captureEvent.setEventType(" ");

        assertThrows(BusinessException.class, () -> service.reportCaptureEvent(captureEvent));
        verify(mapper, times(0)).insert(captureEvent);
    }

    /**
     * 校验按采集会话查询事件时返回 Mapper 结果。
     */
    @Test
    void listBySessionShouldReturnMapperResult() {
        CaptureEvent captureEvent = buildValidEvent();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(captureEvent));

        List<CaptureEvent> result = service.listBySession("tenant_001", "cap_001");

        assertEquals(1, result.size());
        assertSame(captureEvent, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造最小有效 SDK 采集事件。
     *
     * @return SDK 采集事件实体。
     */
    private CaptureEvent buildValidEvent() {
        CaptureEvent captureEvent = new CaptureEvent();
        captureEvent.setId("evt_001");
        captureEvent.setTenantId("tenant_001");
        captureEvent.setCaptureSessionId("cap_001");
        captureEvent.setSdkSessionId("sdk_001");
        captureEvent.setClientEventId("client_evt_001");
        captureEvent.setEventType("CLICK");
        captureEvent.setEventTime(LocalDateTime.now());
        captureEvent.setSequenceNo(1L);
        captureEvent.setPageUrl("https://origin.example.com/record/start");
        captureEvent.setTargetText("提交");
        captureEvent.setTargetLocator("#submit");
        captureEvent.setTargetStableKey("submit_button");
        captureEvent.setEventPayloadJson("{\"source\":\"sdk\"}");
        return captureEvent;
    }
}
