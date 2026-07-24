package com.sxpt.module.execution;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.execution.dto.IdentitySwitchRunRequest;
import com.sxpt.module.execution.service.IdentitySwitchRunService;
import com.sxpt.module.execution.service.RuntimeContextService;
import com.sxpt.module.execution.vo.IdentitySwitchRunVO;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 身份切换运行接口测试。
 *
 * 业务功能：
 * 1. 验证 SDK 可通过 HTTP 接口创建下一步骤身份切换 launchToken。
 * 2. 验证缺少下一步骤 ID 时由 Web 层拦截无效请求。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用真实路由和参数绑定。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "sxpt.execution.identity-switch-controller.enabled=true",
        "sxpt.capture.event-controller.enabled=false",
        "sxpt.capture.resource-snapshot-controller.enabled=false",
        "sxpt.capture.action-draft-controller.enabled=false"
})
class IdentitySwitchRunControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private IdentitySwitchRunService identitySwitchRunService;

    @MockBean
    private RuntimeContextService runtimeContextService;

    /**
     * 校验成功创建下一步骤身份切换运行上下文。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void runShouldReturnNextStepLaunchToken() throws Exception {
        when(identitySwitchRunService.runSwitch(any(IdentitySwitchRunRequest.class))).thenReturn(buildRunVO());

        mockMvc.perform(post("/api/v1/sdk/identity-switches/run")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"studentId\":\"student_001\",\"connectorSystemId\":\"connector_001\",\"taskId\":\"task_001\",\"teachingPointId\":\"tp_001\",\"executionId\":\"execution_001\",\"currentStepId\":\"step_001\",\"nextStepId\":\"step_002\",\"dataInstanceId\":\"data_001\",\"sdkMode\":\"PRACTICE\",\"targetUrl\":\"https://origin.example.com/practice/audit\",\"externalBusinessId\":\"biz_001\",\"externalBusinessNo\":\"NO_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.launchContextId", is("launch_002")))
                .andExpect(jsonPath("$.result.launchToken", is("ctx_token_002")))
                .andExpect(jsonPath("$.result.nextStepId", is("step_002")))
                .andExpect(jsonPath("$.result.nextSegmentNo", is(2)))
                .andExpect(jsonPath("$.result.requiredExternalRoleId", is("role_ext_002")))
                .andExpect(jsonPath("$.result.switchDecisionSource", is("TASK_STEP")))
                .andExpect(jsonPath("$.result.launchTokenHash").doesNotExist())
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<IdentitySwitchRunRequest> captor =
                ArgumentCaptor.forClass(IdentitySwitchRunRequest.class);
        verify(identitySwitchRunService).runSwitch(captor.capture());
        IdentitySwitchRunRequest request = captor.getValue();
        assertEquals("tenant_001", request.getTenantId());
        assertEquals("student_001", request.getStudentId());
        assertEquals("step_002", request.getNextStepId());
        assertEquals("PRACTICE", request.getSdkMode());
    }

    /**
     * 校验缺少下一步骤 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void runShouldRejectMissingNextStepId() throws Exception {
        mockMvc.perform(post("/api/v1/sdk/identity-switches/run")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"studentId\":\"student_001\",\"connectorSystemId\":\"connector_001\",\"taskId\":\"task_001\",\"teachingPointId\":\"tp_001\",\"executionId\":\"execution_001\",\"sdkMode\":\"PRACTICE\",\"targetUrl\":\"https://origin.example.com/practice/audit\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)));
    }

    /**
     * 构造身份切换运行返回对象。
     *
     * @return 身份切换运行返回对象。
     */
    private IdentitySwitchRunVO buildRunVO() {
        IdentitySwitchRunVO vo = new IdentitySwitchRunVO();
        vo.setLaunchContextId("launch_002");
        vo.setLaunchToken("ctx_token_002");
        vo.setTenantId("tenant_001");
        vo.setStudentId("student_001");
        vo.setConnectorSystemId("connector_001");
        vo.setTaskId("task_001");
        vo.setTeachingPointId("tp_001");
        vo.setExecutionId("execution_001");
        vo.setCurrentStepId("step_001");
        vo.setNextStepId("step_002");
        vo.setCurrentSegmentNo(1L);
        vo.setNextSegmentNo(2L);
        vo.setSdkMode("PRACTICE");
        vo.setSceneType("PRACTICE");
        vo.setActorType("AUDITOR");
        vo.setRequiredExternalOrgId("org_ext_002");
        vo.setRequiredExternalOrgName("审核单位");
        vo.setRequiredExternalRoleId("role_ext_002");
        vo.setRequiredExternalRoleName("审核人");
        vo.setTargetUrl("https://origin.example.com/practice/audit");
        vo.setSwitchConfirmRequired(Boolean.TRUE);
        vo.setSwitchDecisionSource("TASK_STEP");
        vo.setSwitchStrategy("LAUNCH_TOKEN");
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
