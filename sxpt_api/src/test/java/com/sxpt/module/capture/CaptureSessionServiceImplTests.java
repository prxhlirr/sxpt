package com.sxpt.module.capture;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureSession;
import com.sxpt.module.capture.mapper.CaptureSessionMapper;
import com.sxpt.module.capture.service.CaptureSessionService;
import com.sxpt.module.capture.service.impl.CaptureSessionServiceImpl;
import org.junit.jupiter.api.Test;

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
 * 备案采集会话服务测试。
 *
 * 业务功能：
 * 1. 验证教师开始备案时会创建 RUNNING 会话。
 * 2. 验证教师会话查询和结束采集遵守 Service 层状态规则。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class CaptureSessionServiceImplTests {

    private final CaptureSessionMapper mapper = mock(CaptureSessionMapper.class);

    private final CaptureSessionService service = new CaptureSessionServiceImpl(mapper);

    /**
     * 校验创建采集会话时写入 Mapper 并补齐默认值。
     */
    @Test
    void createCaptureSessionShouldInsertAndFillDefaults() {
        CaptureSession captureSession = buildValidSession();

        CaptureSession saved = service.createCaptureSession(captureSession);

        assertSame(captureSession, saved);
        assertEquals("STANDARD", saved.getCaptureMode());
        assertEquals("RUNNING", saved.getSessionStatus());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getStartTime());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 校验缺少开始地址时拒绝创建，避免教师无法跳转原平台。
     */
    @Test
    void createCaptureSessionShouldRejectMissingStartUrl() {
        CaptureSession captureSession = buildValidSession();
        captureSession.setStartUrl(" ");

        assertThrows(BusinessException.class, () -> service.createCaptureSession(captureSession));
        verify(mapper, times(0)).insert(captureSession);
    }

    /**
     * 校验按教师查询采集会话时返回 Mapper 结果。
     */
    @Test
    void listByTeacherShouldReturnMapperResult() {
        CaptureSession captureSession = buildValidSession();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(captureSession));

        List<CaptureSession> result = service.listByTeacher("tenant_001", "teacher_001");

        assertEquals(1, result.size());
        assertSame(captureSession, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 校验结束采集会话时写入结束状态和结束时间。
     */
    @Test
    void finishCaptureSessionShouldMarkFinished() {
        CaptureSession captureSession = buildValidSession();
        when(mapper.selectById("cap_001")).thenReturn(captureSession);

        CaptureSession result = service.finishCaptureSession("cap_001");

        assertSame(captureSession, result);
        assertEquals("FINISHED", result.getSessionStatus());
        assertNotNull(result.getEndTime());
        assertNotNull(result.getUpdateTime());
        verify(mapper).updateById(captureSession);
    }

    /**
     * 校验已结束会话不能重复结束。
     */
    @Test
    void finishCaptureSessionShouldRejectFinishedSession() {
        CaptureSession captureSession = buildValidSession();
        captureSession.setSessionStatus("FINISHED");
        when(mapper.selectById("cap_001")).thenReturn(captureSession);

        assertThrows(BusinessException.class, () -> service.finishCaptureSession("cap_001"));
        verify(mapper, times(0)).updateById(captureSession);
    }

    /**
     * 构造最小有效备案采集会话。
     *
     * @return 备案采集会话实体。
     */
    private CaptureSession buildValidSession() {
        CaptureSession captureSession = new CaptureSession();
        captureSession.setId("cap_001");
        captureSession.setTenantId("tenant_001");
        captureSession.setConnectorSystemId("connector_001");
        captureSession.setTeacherId("teacher_001");
        captureSession.setSessionName("标准备案申请采集");
        captureSession.setBusinessName("标准备案申请");
        captureSession.setStartUrl("https://origin.example.com/record/start");
        return captureSession;
    }
}
