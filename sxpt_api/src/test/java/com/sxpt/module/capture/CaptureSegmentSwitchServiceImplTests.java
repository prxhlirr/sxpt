package com.sxpt.module.capture;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.dto.ConfirmCaptureSegmentSwitchRequest;
import com.sxpt.module.capture.service.CaptureSegmentSwitchService;
import com.sxpt.module.capture.service.impl.CaptureSegmentSwitchServiceImpl;
import com.sxpt.module.capture.vo.CaptureSegmentSwitchVO;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 备案片段切换服务测试。
 *
 * 业务功能：
 * 1. 验证教师确认下一片段后会创建 RECORD/CAPTURE 启动上下文。
 * 2. 验证同片段切换会被拒绝，避免无意义的角色切换。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 launchToken 服务，聚焦片段切换编排规则。
 * 2. 捕获创建的 PlatformLaunchContext，确认下一片段身份上下文正确。
 */
class CaptureSegmentSwitchServiceImplTests {

    private final PlatformLaunchContextService platformLaunchContextService = mock(PlatformLaunchContextService.class);

    private final CaptureSegmentSwitchService service = new CaptureSegmentSwitchServiceImpl(platformLaunchContextService);

    /**
     * 校验确认切换后创建下一片段 launchToken。
     */
    @Test
    void confirmSwitchShouldCreateNextSegmentLaunchContext() {
        PlatformLaunchContext saved = buildSavedLaunchContext();
        when(platformLaunchContextService.createLaunchContext(any(PlatformLaunchContext.class)))
                .thenReturn(new PlatformLaunchContextService.CreatedLaunchContext(saved, "ctx_token_002"));

        CaptureSegmentSwitchVO result = service.confirmSwitch(buildValidRequest());

        assertEquals("launch_002", result.getLaunchContextId());
        assertEquals("ctx_token_002", result.getLaunchToken());
        assertEquals(Long.valueOf(1), result.getCurrentSegmentNo());
        assertEquals(Long.valueOf(2), result.getNextSegmentNo());
        assertEquals(Boolean.TRUE, result.getSwitchConfirmRequired());
        assertEquals("TEACHER_PATH", result.getSwitchDecisionSource());
        assertEquals("切换到审核角色", result.getSwitchReason());

        ArgumentCaptor<PlatformLaunchContext> captor = ArgumentCaptor.forClass(PlatformLaunchContext.class);
        verify(platformLaunchContextService).createLaunchContext(captor.capture());
        PlatformLaunchContext launchContext = captor.getValue();
        assertNotNull(launchContext.getId());
        assertEquals("tenant_001", launchContext.getTenantId());
        assertEquals("teacher_001", launchContext.getUserId());
        assertEquals("connector_001", launchContext.getConnectorSystemId());
        assertEquals("data_001", launchContext.getDataInstanceId());
        assertEquals("RECORD", launchContext.getSceneType());
        assertEquals("CAPTURE", launchContext.getSdkMode());
        assertEquals(Long.valueOf(2), launchContext.getSegmentNo());
        assertEquals("AUDITOR", launchContext.getActorType());
        assertEquals("org_ext_002", launchContext.getRequiredExternalOrgId());
        assertEquals("role_ext_002", launchContext.getRequiredExternalRoleId());
        assertEquals("https://origin.example.com/record/audit", launchContext.getTargetUrl());
        assertEquals("teacher_001", launchContext.getCreateBy());
    }

    /**
     * 校验当前片段和下一片段相同时拒绝切换。
     */
    @Test
    void confirmSwitchShouldRejectSameSegment() {
        ConfirmCaptureSegmentSwitchRequest request = buildValidRequest();
        request.setNextSegmentNo(1L);

        assertThrows(BusinessException.class, () -> service.confirmSwitch(request));
    }

    /**
     * 构造最小有效确认切换请求。
     *
     * @return 确认备案片段切换请求。
     */
    private ConfirmCaptureSegmentSwitchRequest buildValidRequest() {
        ConfirmCaptureSegmentSwitchRequest request = new ConfirmCaptureSegmentSwitchRequest();
        request.setTenantId("tenant_001");
        request.setTeacherId("teacher_001");
        request.setConnectorSystemId("connector_001");
        request.setCaptureSessionId("cap_001");
        request.setDataInstanceId("data_001");
        request.setCurrentSegmentNo(1L);
        request.setNextSegmentNo(2L);
        request.setActorType("AUDITOR");
        request.setRequiredExternalOrgId("org_ext_002");
        request.setRequiredExternalRoleId("role_ext_002");
        request.setTargetUrl("https://origin.example.com/record/audit");
        request.setSwitchReason("切换到审核角色");
        return request;
    }

    /**
     * 构造 launchToken 服务返回的已保存启动上下文。
     *
     * @return 原平台启动上下文。
     */
    private PlatformLaunchContext buildSavedLaunchContext() {
        PlatformLaunchContext launchContext = new PlatformLaunchContext();
        launchContext.setId("launch_002");
        launchContext.setTenantId("tenant_001");
        launchContext.setUserId("teacher_001");
        launchContext.setConnectorSystemId("connector_001");
        launchContext.setSegmentNo(2L);
        launchContext.setActorType("AUDITOR");
        launchContext.setRequiredExternalOrgId("org_ext_002");
        launchContext.setRequiredExternalRoleId("role_ext_002");
        launchContext.setTargetUrl("https://origin.example.com/record/audit");
        launchContext.setExpireTime(LocalDateTime.now().plusMinutes(5));
        return launchContext;
    }
}
