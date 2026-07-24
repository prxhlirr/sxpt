package com.sxpt.module.capture;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.capture.entity.CaptureResourceSnapshot;
import com.sxpt.module.capture.service.CaptureResourceSnapshotService;
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
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 采集资源快照接口测试。
 *
 * 业务功能：
 * 1. 验证资源快照上报接口遵循统一响应结构。
 * 2. 验证按采集会话查询资源快照接口可用，且不返回内部软删除字段。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.capture.resource-snapshot-controller.enabled=true")
class CaptureResourceSnapshotControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private CaptureResourceSnapshotService snapshotService;

    /**
     * 校验资源快照上报成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void reportShouldReturnCaptureResourceSnapshotVo() throws Exception {
        CaptureResourceSnapshot saved = buildSavedSnapshot();
        when(snapshotService.reportSnapshot(any(CaptureResourceSnapshot.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/capture/resource-snapshots/report")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"captureSessionId\":\"cap_001\",\"pageUrl\":\"https://origin.example.com/record/start\",\"pageTitle\":\"备案申请\",\"resourceType\":\"BUTTON\",\"snapshotScope\":\"TARGET_ELEMENT\",\"resourceName\":\"提交按钮\",\"resourceLocator\":\"#submit\",\"elementSnapshotJson\":\"{\\\"text\\\":\\\"提交\\\",\\\"role\\\":\\\"button\\\"}\",\"snapshotHash\":\"hash_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("snap_001")))
                .andExpect(jsonPath("$.result.snapshotScope", is("TARGET_ELEMENT")))
                .andExpect(jsonPath("$.result.resourceType", is("BUTTON")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<CaptureResourceSnapshot> captor =
                ArgumentCaptor.forClass(CaptureResourceSnapshot.class);
        verify(snapshotService).reportSnapshot(captor.capture());
        CaptureResourceSnapshot requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("cap_001", requestEntity.getCaptureSessionId());
        org.junit.jupiter.api.Assertions.assertEquals("TARGET_ELEMENT", requestEntity.getSnapshotScope());
        org.junit.jupiter.api.Assertions.assertEquals("{\"text\":\"提交\",\"role\":\"button\"}",
                requestEntity.getElementSnapshotJson());
    }

    /**
     * 校验 DEBUG_FULL_DOM 缺少过期时间时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void reportShouldRejectDebugFullDomWithoutExpireTime() throws Exception {
        mockMvc.perform(post("/api/v1/capture/resource-snapshots/report")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"captureSessionId\":\"cap_001\",\"pageUrl\":\"https://origin.example.com/record/start\",\"resourceType\":\"STATE\",\"snapshotScope\":\"DEBUG_FULL_DOM\",\"elementSnapshotJson\":\"{\\\"compressed\\\":true}\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验按采集会话查询资源快照列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listBySessionShouldReturnCaptureResourceSnapshotVos() throws Exception {
        CaptureResourceSnapshot saved = buildSavedSnapshot();
        when(snapshotService.listBySession("tenant_001", "cap_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/capture/resource-snapshots")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("captureSessionId", "cap_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("snap_001")))
                .andExpect(jsonPath("$.result[0].captureSessionId", is("cap_001")))
                .andExpect(jsonPath("$.result[0].snapshotHash", is("hash_001")));

        verify(snapshotService).listBySession("tenant_001", "cap_001");
    }

    /**
     * 构造 Service 返回的已保存资源快照。
     *
     * @return 采集资源快照实体。
     */
    private CaptureResourceSnapshot buildSavedSnapshot() {
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
        snapshot.setArchiveStatus("NONE");
        snapshot.setStatus("ACTIVE");
        snapshot.setCreateTime(LocalDateTime.now());
        return snapshot;
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
