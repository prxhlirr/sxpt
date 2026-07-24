package com.sxpt.module.connector;

import com.sxpt.SxptApiApplication;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 原平台最小改造联调探针接口测试。
 *
 * 业务功能：
 * 1. 验证 /teaching-launch 入口会按顺序完成 token 校验和 USED 回写。
 * 2. 验证联调入口返回原平台建立 session 所需的上下文摘要。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用模拟原平台入口。
 * 2. 使用 MockBean 替代 Service，避免联调探针测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.connector.origin-launch-probe-controller.enabled=true")
class OriginPlatformLaunchProbeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlatformLaunchContextService platformLaunchContextService;

    /**
     * 校验原平台联调入口可以完成 verify 和 used 回写。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void teachingLaunchShouldVerifyTokenAndMarkUsed() throws Exception {
        PlatformLaunchContext verified = buildLaunchContext("VERIFIED");
        PlatformLaunchContext used = buildLaunchContext("USED");
        when(platformLaunchContextService.verifyLaunchToken("tenant_001", "ctx_token_001")).thenReturn(verified);
        when(platformLaunchContextService.markLaunchContextUsed("launch_001")).thenReturn(used);

        mockMvc.perform(get("/teaching-launch")
                        .param("tenantId", "tenant_001")
                        .param("token", "ctx_token_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("launch_001")))
                .andExpect(jsonPath("$.result.launchStatus", is("USED")))
                .andExpect(jsonPath("$.result.targetUrl", is("/record/apply")))
                .andExpect(jsonPath("$.result.requiredExternalOrgId", is("org_ext_001")))
                .andExpect(jsonPath("$.result.requiredExternalRoleId", is("role_ext_001")))
                .andExpect(jsonPath("$.result.externalBusinessId", is("biz_001")))
                .andExpect(jsonPath("$.result.sdkMode", is("CAPTURE")))
                .andExpect(jsonPath("$.result.launchToken").doesNotExist())
                .andExpect(jsonPath("$.result.launchTokenHash").doesNotExist());

        InOrder inOrder = inOrder(platformLaunchContextService);
        inOrder.verify(platformLaunchContextService).verifyLaunchToken("tenant_001", "ctx_token_001");
        inOrder.verify(platformLaunchContextService).markLaunchContextUsed("launch_001");
    }

    /**
     * 构造联调探针使用的启动上下文。
     *
     * @param launchStatus 启动状态。
     * @return 原平台启动上下文实体。
     */
    private PlatformLaunchContext buildLaunchContext(String launchStatus) {
        PlatformLaunchContext launchContext = new PlatformLaunchContext();
        launchContext.setId("launch_001");
        launchContext.setTenantId("tenant_001");
        launchContext.setUserId("user_001");
        launchContext.setConnectorSystemId("connector_001");
        launchContext.setTaskId("task_001");
        launchContext.setTeachingPointId("tp_001");
        launchContext.setExecutionId("exec_001");
        launchContext.setDataInstanceId("data_001");
        launchContext.setSceneType("RECORD");
        launchContext.setSdkMode("CAPTURE");
        launchContext.setTargetUrl("/record/apply");
        launchContext.setSegmentNo(1L);
        launchContext.setActorType("APPLICANT");
        launchContext.setRequiredExternalOrgId("org_ext_001");
        launchContext.setRequiredExternalRoleId("role_ext_001");
        launchContext.setExternalBusinessId("biz_001");
        launchContext.setDataScopeJson("{}");
        launchContext.setLaunchStatus(launchStatus);
        launchContext.setVerifiedTime(LocalDateTime.now());
        launchContext.setExpireTime(LocalDateTime.now().plusMinutes(5));
        return launchContext;
    }
}
