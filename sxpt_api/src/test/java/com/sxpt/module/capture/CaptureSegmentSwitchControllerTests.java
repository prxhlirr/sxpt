package com.sxpt.module.capture;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.capture.dto.ConfirmCaptureSegmentSwitchRequest;
import com.sxpt.module.capture.service.CaptureSegmentSwitchService;
import com.sxpt.module.capture.vo.CaptureSegmentSwitchVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 备案片段切换接口测试。
 *
 * 业务功能：
 * 1. 验证教师确认下一片段接口遵循统一响应结构。
 * 2. 验证接口不会返回 launchTokenHash 等内部安全字段。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.capture.segment-switch-controller.enabled=true")
class CaptureSegmentSwitchControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private CaptureSegmentSwitchService captureSegmentSwitchService;

    /**
     * 校验确认片段切换成功返回下一片段 launchToken。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void confirmShouldReturnNextSegmentLaunchToken() throws Exception {
        when(captureSegmentSwitchService.confirmSwitch(any(ConfirmCaptureSegmentSwitchRequest.class)))
                .thenReturn(buildSwitchVO());

        mockMvc.perform(post("/api/v1/capture/segment-switches/confirm")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"teacherId\":\"teacher_001\",\"connectorSystemId\":\"connector_001\",\"captureSessionId\":\"cap_001\",\"dataInstanceId\":\"data_001\",\"currentSegmentNo\":1,\"nextSegmentNo\":2,\"actorType\":\"AUDITOR\",\"requiredExternalOrgId\":\"org_ext_002\",\"requiredExternalOrgName\":\"审核单位\",\"requiredExternalRoleId\":\"role_ext_002\",\"requiredExternalRoleName\":\"备案审核人\",\"targetUrl\":\"https://origin.example.com/record/audit\",\"switchReason\":\"切换到审核角色\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.launchContextId", is("launch_002")))
                .andExpect(jsonPath("$.result.launchToken", is("ctx_token_002")))
                .andExpect(jsonPath("$.result.currentSegmentNo", is(1)))
                .andExpect(jsonPath("$.result.nextSegmentNo", is(2)))
                .andExpect(jsonPath("$.result.switchConfirmRequired", is(true)))
                .andExpect(jsonPath("$.result.switchDecisionSource", is("TEACHER_PATH")))
                .andExpect(jsonPath("$.result.launchTokenHash").doesNotExist());

        ArgumentCaptor<ConfirmCaptureSegmentSwitchRequest> captor =
                ArgumentCaptor.forClass(ConfirmCaptureSegmentSwitchRequest.class);
        verify(captureSegmentSwitchService).confirmSwitch(captor.capture());
        ConfirmCaptureSegmentSwitchRequest request = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", request.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("teacher_001", request.getTeacherId());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1), request.getCurrentSegmentNo());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(2), request.getNextSegmentNo());
        org.junit.jupiter.api.Assertions.assertEquals("role_ext_002", request.getRequiredExternalRoleId());
    }

    /**
     * 校验缺少下一片段编号时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void confirmShouldRejectMissingNextSegmentNo() throws Exception {
        mockMvc.perform(post("/api/v1/capture/segment-switches/confirm")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"teacherId\":\"teacher_001\",\"connectorSystemId\":\"connector_001\",\"currentSegmentNo\":1,\"requiredExternalOrgId\":\"org_ext_002\",\"requiredExternalRoleId\":\"role_ext_002\",\"targetUrl\":\"https://origin.example.com/record/audit\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 构造 Service 返回的片段切换结果。
     *
     * @return 备案片段切换返回对象。
     */
    private CaptureSegmentSwitchVO buildSwitchVO() {
        CaptureSegmentSwitchVO vo = new CaptureSegmentSwitchVO();
        vo.setLaunchContextId("launch_002");
        vo.setLaunchToken("ctx_token_002");
        vo.setTenantId("tenant_001");
        vo.setTeacherId("teacher_001");
        vo.setConnectorSystemId("connector_001");
        vo.setCurrentSegmentNo(1L);
        vo.setNextSegmentNo(2L);
        vo.setActorType("AUDITOR");
        vo.setRequiredExternalOrgId("org_ext_002");
        vo.setRequiredExternalRoleId("role_ext_002");
        vo.setTargetUrl("https://origin.example.com/record/audit");
        vo.setSwitchConfirmRequired(Boolean.TRUE);
        vo.setSwitchDecisionSource("TEACHER_PATH");
        vo.setSwitchReason("切换到审核角色");
        vo.setExpireTime(LocalDateTime.now().plusMinutes(5));
        return vo;
    }

    /**
     * 构造通过 JWT 拦截器所需的认证请求头。
     *
     * @return Bearer Token 请求头值。
     */
    private String bearerToken() {
        return "Bearer " + jwtService.generateToken("admin_001", "admin");
    }
}
