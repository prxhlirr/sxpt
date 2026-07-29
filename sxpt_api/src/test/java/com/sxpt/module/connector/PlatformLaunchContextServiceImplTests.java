package com.sxpt.module.connector;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.mapper.PlatformLaunchContextMapper;
import com.sxpt.module.connector.mapper.TeachingDataInstanceMapper;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.connector.service.impl.PlatformLaunchContextServiceImpl;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataInstanceStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.LaunchStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.ValidationStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 原平台启动上下文服务测试。
 *
 * 业务功能：
 * 1. 验证创建启动上下文时会生成明文 token 并只保存 hash。
 * 2. 验证缺少关键启动字段时不会写入数据库。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接计算返回 token 的 SHA-256，确认实体中保存的是 hash 而不是明文。
 */
class PlatformLaunchContextServiceImplTests {

    private final PlatformLaunchContextMapper mapper = mock(PlatformLaunchContextMapper.class);

    private final TeachingDataInstanceMapper teachingDataInstanceMapper = mock(TeachingDataInstanceMapper.class);

    private final PlatformLaunchContextService service = new PlatformLaunchContextServiceImpl(
            mapper, teachingDataInstanceMapper);

    /**
     * 校验创建启动上下文时生成 token、保存 hash 并补齐默认字段。
     *
     * @throws Exception SHA-256 计算异常由测试框架处理。
     */
    @Test
    void createLaunchContextShouldGenerateTokenHashAndDefaults() throws Exception {
        PlatformLaunchContext launchContext = buildValidLaunchContext();
        when(teachingDataInstanceMapper.selectById("instance_001")).thenReturn(buildReadyPassedInstance());

        PlatformLaunchContextService.CreatedLaunchContext created = service.createLaunchContext(launchContext);

        assertSame(launchContext, created.getLaunchContext());
        assertNotNull(created.getLaunchToken());
        assertTrue(created.getLaunchToken().startsWith("ctx_"));
        assertFalse(created.getLaunchToken().equals(launchContext.getLaunchTokenHash()));
        assertEquals(sha256(created.getLaunchToken()), launchContext.getLaunchTokenHash());
        assertEquals(64, launchContext.getLaunchTokenHash().length());
        assertEquals(LaunchStatus.CREATED.getValue(), launchContext.getLaunchStatus());
        assertEquals(RecordStatus.ACTIVE.getValue(), launchContext.getStatus());
        assertEquals(Boolean.FALSE, launchContext.getDeleted());
        assertNotNull(launchContext.getCreateTime());
        assertNotNull(launchContext.getUpdateTime());
        assertNotNull(launchContext.getExpireTime());
        assertTrue(launchContext.getExpireTime().isAfter(LocalDateTime.now()));
        assertEquals("biz_001", launchContext.getExternalBusinessId());
        assertEquals("org_required", launchContext.getRequiredExternalOrgId());
        assertEquals("role_required", launchContext.getRequiredExternalRoleId());
        assertNotNull(launchContext.getSdkConfigSnapshotJson());
        assertTrue(launchContext.getSdkConfigSnapshotJson().contains("\"sdkMode\":\"CAPTURE\""));
        assertTrue(launchContext.getSdkConfigSnapshotJson().contains("\"dataInstanceId\":\"instance_001\""));
        assertNotNull(launchContext.getDataInstanceValidationSnapshotJson());
        assertTrue(launchContext.getDataInstanceValidationSnapshotJson().contains("\"validationStatus\":\"PASSED\""));
        assertTrue(launchContext.getDataInstanceValidationSnapshotJson().contains("\"externalBusinessId\":\"biz_001\""));

        ArgumentCaptor<PlatformLaunchContext> captor = ArgumentCaptor.forClass(PlatformLaunchContext.class);
        verify(mapper).insert(captor.capture());
        assertSame(launchContext, captor.getValue());
    }

    /**
     * 校验绑定的数据实例未通过原平台校验时拒绝创建 launchToken。
     */
    @Test
    void createLaunchContextShouldRejectUnvalidatedDataInstance() {
        PlatformLaunchContext launchContext = buildValidLaunchContext();
        TeachingDataInstance instance = buildReadyPassedInstance();
        instance.setValidationStatus(ValidationStatus.FAILED.getValue());
        when(teachingDataInstanceMapper.selectById("instance_001")).thenReturn(instance);

        assertThrows(BusinessException.class, () -> service.createLaunchContext(launchContext));
        verify(mapper, times(0)).insert(launchContext);
    }

    /**
     * 校验缺少目标地址时拒绝创建启动上下文。
     */
    @Test
    void createLaunchContextShouldRejectMissingTargetUrl() {
        PlatformLaunchContext launchContext = buildValidLaunchContext();
        launchContext.setTargetUrl(" ");

        assertThrows(BusinessException.class, () -> service.createLaunchContext(launchContext));
        verify(mapper, times(0)).insert(launchContext);
    }

    /**
     * 校验有效 token 会返回启动上下文并标记为 VERIFIED。
     *
     * @throws Exception SHA-256 计算异常由测试框架处理。
     */
    @Test
    void verifyLaunchTokenShouldMarkContextVerified() throws Exception {
        String launchToken = "ctx_token_001";
        PlatformLaunchContext launchContext = buildValidLaunchContext();
        launchContext.setLaunchTokenHash(sha256(launchToken));
        launchContext.setLaunchStatus(LaunchStatus.CREATED.getValue());
        launchContext.setExpireTime(LocalDateTime.now().plusMinutes(5));
        when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(launchContext);
        when(teachingDataInstanceMapper.selectById("instance_001")).thenReturn(buildReadyPassedInstance());

        PlatformLaunchContext verified = service.verifyLaunchToken("tenant_001", launchToken);

        assertSame(launchContext, verified);
        assertEquals(LaunchStatus.VERIFIED.getValue(), verified.getLaunchStatus());
        assertNotNull(verified.getVerifiedTime());
        assertNotNull(verified.getVerifyTime());
        assertNotNull(verified.getVerifyRequestId());
        assertTrue(verified.getVerifyRequestId().startsWith("verify_"));
        assertNotNull(verified.getUpdateTime());
        verify(mapper).selectOne(org.mockito.ArgumentMatchers.any());
        verify(mapper).updateById(launchContext);
    }

    /**
     * 校验 verify 时会再次确认数据实例仍然可用，防止 token 创建后实例被废弃或校验失败。
     */
    @Test
    void verifyLaunchTokenShouldRejectFailedDataInstance() {
        PlatformLaunchContext launchContext = buildValidLaunchContext();
        launchContext.setLaunchStatus(LaunchStatus.CREATED.getValue());
        launchContext.setExpireTime(LocalDateTime.now().plusMinutes(5));
        TeachingDataInstance instance = buildReadyPassedInstance();
        instance.setInstanceStatus(DataInstanceStatus.FAILED.getValue());
        when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(launchContext);
        when(teachingDataInstanceMapper.selectById("instance_001")).thenReturn(instance);

        assertThrows(BusinessException.class, () -> service.verifyLaunchToken("tenant_001", "ctx_failed_instance"));
        verify(mapper, times(0)).updateById(launchContext);
    }

    /**
     * 校验不存在的 token 返回业务异常。
     */
    @Test
    void verifyLaunchTokenShouldRejectMissingContext() {
        when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.verifyLaunchToken("tenant_001", "ctx_missing"));
        verify(mapper).selectOne(org.mockito.ArgumentMatchers.any());
        verify(mapper, times(0)).updateById(org.mockito.ArgumentMatchers.any());
    }

    /**
     * 校验过期 token 会标记为 EXPIRED 并拒绝继续校验。
     */
    @Test
    void verifyLaunchTokenShouldRejectExpiredContext() {
        PlatformLaunchContext launchContext = buildValidLaunchContext();
        launchContext.setLaunchStatus(LaunchStatus.CREATED.getValue());
        launchContext.setExpireTime(LocalDateTime.now().minusMinutes(1));
        when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(launchContext);

        assertThrows(BusinessException.class, () -> service.verifyLaunchToken("tenant_001", "ctx_expired"));
        assertEquals(LaunchStatus.EXPIRED.getValue(), launchContext.getLaunchStatus());
        verify(mapper).updateById(launchContext);
    }

    /**
     * 校验已被校验过的 token 不能重复校验。
     */
    @Test
    void verifyLaunchTokenShouldRejectAlreadyVerifiedContext() {
        PlatformLaunchContext launchContext = buildValidLaunchContext();
        launchContext.setLaunchStatus(LaunchStatus.VERIFIED.getValue());
        launchContext.setExpireTime(LocalDateTime.now().plusMinutes(5));
        when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(launchContext);

        assertThrows(BusinessException.class, () -> service.verifyLaunchToken("tenant_001", "ctx_verified"));
        verify(mapper, times(0)).updateById(launchContext);
    }

    /**
     * 构造最小有效原平台启动上下文。
     *
     * @return 原平台启动上下文实体。
     */
    /**
     * 校验 VERIFIED 状态可以回写为 USED。
     */
    @Test
    void markLaunchContextUsedShouldUpdateVerifiedContext() {
        PlatformLaunchContext launchContext = buildValidLaunchContext();
        launchContext.setLaunchStatus(LaunchStatus.VERIFIED.getValue());
        when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(launchContext);

        PlatformLaunchContext used = service.markLaunchContextUsed("launch_001");

        assertSame(launchContext, used);
        assertEquals(LaunchStatus.USED.getValue(), used.getLaunchStatus());
        assertNotNull(used.getUsedTime());
        assertNotNull(used.getUpdateTime());
        verify(mapper).updateById(launchContext);
    }

    /**
     * 校验未完成 verify 的启动上下文不能直接标记 USED。
     */
    @Test
    void markLaunchContextUsedShouldRejectCreatedContext() {
        PlatformLaunchContext launchContext = buildValidLaunchContext();
        launchContext.setLaunchStatus(LaunchStatus.CREATED.getValue());
        when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(launchContext);

        assertThrows(BusinessException.class, () -> service.markLaunchContextUsed("launch_001"));
        verify(mapper, times(0)).updateById(launchContext);
    }

    /**
     * 校验启动失败可记录失败状态和原因。
     */
    @Test
    void markLaunchContextFailedShouldRecordErrorMessage() {
        PlatformLaunchContext launchContext = buildValidLaunchContext();
        launchContext.setLaunchStatus(LaunchStatus.VERIFIED.getValue());
        when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(launchContext);

        PlatformLaunchContext failed = service.markLaunchContextFailed("launch_001", "原平台 session 建立失败");

        assertSame(launchContext, failed);
        assertEquals(LaunchStatus.FAILED.getValue(), failed.getLaunchStatus());
        assertEquals("原平台 session 建立失败", failed.getErrorMessage());
        assertNotNull(failed.getUpdateTime());
        verify(mapper).updateById(launchContext);
    }

    /**
     * 校验启动失败原因不能为空。
     */
    @Test
    void markLaunchContextFailedShouldRejectBlankErrorMessage() {
        assertThrows(BusinessException.class, () -> service.markLaunchContextFailed("launch_001", " "));
        verify(mapper, times(0)).selectOne(org.mockito.ArgumentMatchers.any());
        verify(mapper, times(0)).updateById(org.mockito.ArgumentMatchers.any());
    }

    private PlatformLaunchContext buildValidLaunchContext() {
        PlatformLaunchContext launchContext = new PlatformLaunchContext();
        launchContext.setId("launch_001");
        launchContext.setTenantId("tenant_001");
        launchContext.setUserId("user_001");
        launchContext.setConnectorSystemId("connector_001");
        launchContext.setDataInstanceId("instance_001");
        launchContext.setSceneType("RECORD");
        launchContext.setSdkMode("CAPTURE");
        launchContext.setTargetUrl("/record/apply");
        return launchContext;
    }

    /**
     * 构造已通过原平台校验的数据实例。
     *
     * @return 教学数据实例。
     */
    private TeachingDataInstance buildReadyPassedInstance() {
        TeachingDataInstance instance = new TeachingDataInstance();
        instance.setId("instance_001");
        instance.setTenantId("tenant_001");
        instance.setConnectorSystemId("connector_001");
        instance.setOwnerUserId("user_001");
        instance.setSceneType("RECORD");
        instance.setExternalBusinessId("biz_001");
        instance.setExternalBusinessNo("NO-biz_001");
        instance.setExternalStatus("DRAFT");
        instance.setRequiredExternalOrgId("org_required");
        instance.setRequiredExternalRoleId("role_required");
        instance.setRequirementSnapshotJson("{\"scope\":\"demo\"}");
        instance.setInstanceStatus(DataInstanceStatus.READY.getValue());
        instance.setValidationStatus(ValidationStatus.PASSED.getValue());
        instance.setValidationTime(LocalDateTime.now().minusMinutes(1));
        instance.setValidationResultJson("{\"passed\":true}");
        instance.setDeleted(Boolean.FALSE);
        return instance;
    }

    /**
     * 测试侧计算 SHA-256，用于验证 Service 没有保存明文 token。
     *
     * @param value 待计算文本。
     * @return 64 位十六进制 hash。
     * @throws Exception SHA-256 算法不可用时抛出。
     */
    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        for (byte item : bytes) {
            builder.append(String.format("%02x", item));
        }
        return builder.toString();
    }
}
