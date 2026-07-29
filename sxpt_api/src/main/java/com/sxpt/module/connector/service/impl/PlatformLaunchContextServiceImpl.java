package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.mapper.PlatformLaunchContextMapper;
import com.sxpt.module.connector.mapper.TeachingDataInstanceMapper;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataInstanceStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.LaunchStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.ValidationStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 原平台启动上下文服务实现。
 *
 * 业务功能：
 * 1. 创建 platform_launch_context 记录，支撑教学平台跳转原平台。
 * 2. 生成短有效期一次性 launchToken，并只持久化 SHA-256 hash。
 *
 * 关键流程：
 * 1. 校验启动上下文最小必填字段。
 * 2. 生成高熵随机 token 并计算 hash。
 * 3. 补齐 CREATED、ACTIVE、未删除、创建时间、更新时间和默认过期时间。
 * 4. 调用 Mapper 写入数据库，并把明文 token 仅随返回对象交给上层。
 */
@Service
@Profile("!test")
public class PlatformLaunchContextServiceImpl implements PlatformLaunchContextService {

    private static final int TOKEN_RANDOM_BYTES = 32;

    private static final int DEFAULT_EXPIRE_MINUTES = 5;

    private static final String VERIFY_REQUEST_PREFIX = "verify_";

    private final PlatformLaunchContextMapper platformLaunchContextMapper;

    private final TeachingDataInstanceMapper teachingDataInstanceMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    public PlatformLaunchContextServiceImpl(PlatformLaunchContextMapper platformLaunchContextMapper,
                                            TeachingDataInstanceMapper teachingDataInstanceMapper) {
        this.platformLaunchContextMapper = platformLaunchContextMapper;
        this.teachingDataInstanceMapper = teachingDataInstanceMapper;
    }

    /**
     * 创建原平台启动上下文并返回本次明文 launchToken。
     *
     * @param launchContext 原平台启动上下文，必须包含 ID、租户、用户、原平台、场景、SDK 模式和目标地址。
     * @return 已保存上下文与本次明文 launchToken。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatedLaunchContext createLaunchContext(PlatformLaunchContext launchContext) {
        validateCreateFields(launchContext);
        validateLaunchDataInstance(launchContext);
        String launchToken = generateLaunchToken();
        launchContext.setLaunchTokenHash(hashToken(launchToken));
        fillCreateDefaults(launchContext);
        platformLaunchContextMapper.insert(launchContext);
        return new CreatedLaunchContext(launchContext, launchToken);
    }

    /**
     * 校验原平台提交的明文 launchToken 并返回启动上下文。
     *
     * @param tenantId 租户 ID，用于隔离不同租户下的 token。
     * @param launchToken 原平台提交的明文 launchToken。
     * @return 已校验的启动上下文。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformLaunchContext verifyLaunchToken(String tenantId, String launchToken) {
        requireText(tenantId);
        requireText(launchToken);
        PlatformLaunchContext launchContext = platformLaunchContextMapper.selectOne(new QueryWrapper<PlatformLaunchContext>()
                .eq("tenant_id", tenantId)
                .eq("launch_token_hash", hashToken(launchToken))
                .eq("deleted", Boolean.FALSE));
        if (launchContext == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        ensureLaunchContextCanBeVerified(launchContext);
        validateLaunchDataInstance(launchContext);
        LocalDateTime now = LocalDateTime.now();
        launchContext.setLaunchStatus(LaunchStatus.VERIFIED.getValue());
        launchContext.setVerifiedTime(now);
        launchContext.setVerifyTime(now);
        launchContext.setVerifyRequestId(generateVerifyRequestId());
        launchContext.setUpdateTime(now);
        platformLaunchContextMapper.updateById(launchContext);
        return launchContext;
    }

    /**
     * 标记原平台已完成 session 建立。
     *
     * @param id 启动上下文 ID。
     * @return 已标记使用的启动上下文。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformLaunchContext markLaunchContextUsed(String id) {
        PlatformLaunchContext launchContext = getActiveLaunchContextById(id);
        if (!LaunchStatus.VERIFIED.getValue().equals(launchContext.getLaunchStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        LocalDateTime now = LocalDateTime.now();
        launchContext.setLaunchStatus(LaunchStatus.USED.getValue());
        launchContext.setUsedTime(now);
        launchContext.setUpdateTime(now);
        platformLaunchContextMapper.updateById(launchContext);
        return launchContext;
    }

    /**
     * 标记原平台启动失败并记录失败原因。
     *
     * @param id 启动上下文 ID。
     * @param errorMessage 启动失败原因。
     * @return 已标记失败的启动上下文。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformLaunchContext markLaunchContextFailed(String id, String errorMessage) {
        requireText(errorMessage);
        PlatformLaunchContext launchContext = getActiveLaunchContextById(id);
        LocalDateTime now = LocalDateTime.now();
        launchContext.setLaunchStatus(LaunchStatus.FAILED.getValue());
        launchContext.setErrorMessage(errorMessage);
        launchContext.setUpdateTime(now);
        platformLaunchContextMapper.updateById(launchContext);
        return launchContext;
    }

    /**
     * 校验创建启动上下文所需的最小字段。
     *
     * @param launchContext 原平台启动上下文实体。
     */
    private void validateCreateFields(PlatformLaunchContext launchContext) {
        if (launchContext == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(launchContext.getId());
        requireText(launchContext.getTenantId());
        requireText(launchContext.getUserId());
        requireText(launchContext.getConnectorSystemId());
        requireText(launchContext.getDataInstanceId());
        requireText(launchContext.getSceneType());
        requireText(launchContext.getSdkMode());
        requireText(launchContext.getTargetUrl());
    }

    /**
     * 校验启动上下文绑定的数据实例已经完成原平台校验。
     *
     * @param launchContext 原平台启动上下文实体。
     */
    private void validateLaunchDataInstance(PlatformLaunchContext launchContext) {
        TeachingDataInstance instance = teachingDataInstanceMapper.selectById(launchContext.getDataInstanceId());
        if (instance == null || Boolean.TRUE.equals(instance.getDeleted())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        if (!equalsText(launchContext.getTenantId(), instance.getTenantId())
                || !equalsText(launchContext.getConnectorSystemId(), instance.getConnectorSystemId())
                || !equalsText(launchContext.getSceneType(), instance.getSceneType())
                || !equalsText(launchContext.getUserId(), instance.getOwnerUserId())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        if (!isLaunchableInstanceStatus(instance.getInstanceStatus())
                || !ValidationStatus.PASSED.getValue().equals(instance.getValidationStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        if (!StringUtils.hasText(launchContext.getExternalBusinessId())) {
            launchContext.setExternalBusinessId(instance.getExternalBusinessId());
        }
        if (!StringUtils.hasText(launchContext.getExternalBusinessNo())) {
            launchContext.setExternalBusinessNo(instance.getExternalBusinessNo());
        }
        if (!StringUtils.hasText(launchContext.getRequiredExternalOrgId())) {
            launchContext.setRequiredExternalOrgId(instance.getRequiredExternalOrgId());
        }
        if (!StringUtils.hasText(launchContext.getRequiredExternalRoleId())) {
            launchContext.setRequiredExternalRoleId(instance.getRequiredExternalRoleId());
        }
        if (!StringUtils.hasText(launchContext.getDataScopeJson())) {
            launchContext.setDataScopeJson(instance.getRequirementSnapshotJson());
        }
        launchContext.setSdkConfigSnapshotJson(buildSdkConfigSnapshot(launchContext));
        launchContext.setDataInstanceValidationSnapshotJson(buildDataInstanceValidationSnapshot(instance));
    }

    /**
     * 比较两个文本字段是否完全一致。
     *
     * @param left 左侧文本。
     * @param right 右侧文本。
     * @return true 表示两个文本一致。
     */
    private boolean equalsText(String left, String right) {
        return left != null && left.equals(right);
    }

    /**
     * 判断数据实例是否允许生成进入原平台的启动上下文。
     * <p>
     * 数据准备完成后实例处于 READY；学生领取后实例会推进为 ALLOCATED。
     * 学生真正进入原平台发生在领取之后，因此这里必须允许已分配给当前学生的 ALLOCATED 实例，
     * 上方 ownerUserId 校验会继续保证学生只能启动自己的那条数据。
     *
     * @param instanceStatus 数据实例当前状态。
     * @return true 表示该状态允许创建或校验启动上下文。
     */
    private boolean isLaunchableInstanceStatus(String instanceStatus) {
        return DataInstanceStatus.READY.getValue().equals(instanceStatus)
                || DataInstanceStatus.ALLOCATED.getValue().equals(instanceStatus);
    }

    /**
     * 校验文本字段非空。
     *
     * @param value 待校验字段值。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 补齐创建时默认字段。
     *
     * @param launchContext 原平台启动上下文实体。
     */
    private void fillCreateDefaults(PlatformLaunchContext launchContext) {
        LocalDateTime now = LocalDateTime.now();
        if (launchContext.getCreateTime() == null) {
            launchContext.setCreateTime(now);
        }
        if (launchContext.getUpdateTime() == null) {
            launchContext.setUpdateTime(now);
        }
        if (launchContext.getExpireTime() == null) {
            launchContext.setExpireTime(now.plusMinutes(DEFAULT_EXPIRE_MINUTES));
        }
        if (!StringUtils.hasText(launchContext.getLaunchStatus())) {
            launchContext.setLaunchStatus(LaunchStatus.CREATED.getValue());
        }
        if (!StringUtils.hasText(launchContext.getStatus())) {
            launchContext.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (launchContext.getDeleted() == null) {
            launchContext.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 校验启动上下文是否仍可被原平台校验。
     *
     * @param launchContext 原平台启动上下文实体。
     */
    private void ensureLaunchContextCanBeVerified(PlatformLaunchContext launchContext) {
        LocalDateTime now = LocalDateTime.now();
        if (launchContext.getExpireTime() == null || !launchContext.getExpireTime().isAfter(now)) {
            launchContext.setLaunchStatus(LaunchStatus.EXPIRED.getValue());
            launchContext.setUpdateTime(now);
            platformLaunchContextMapper.updateById(launchContext);
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        if (!LaunchStatus.CREATED.getValue().equals(launchContext.getLaunchStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
    }

    /**
     * 按 ID 查询未删除启动上下文。
     *
     * @param id 启动上下文 ID。
     * @return 未删除启动上下文。
     */
    private PlatformLaunchContext getActiveLaunchContextById(String id) {
        requireText(id);
        PlatformLaunchContext launchContext = platformLaunchContextMapper.selectOne(new QueryWrapper<PlatformLaunchContext>()
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (launchContext == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return launchContext;
    }

    /**
     * 生成高熵短 token。
     *
     * @return URL 安全的明文 launchToken。
     */
    private String generateLaunchToken() {
        byte[] randomBytes = new byte[TOKEN_RANDOM_BYTES];
        secureRandom.nextBytes(randomBytes);
        return "ctx_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * 计算 launchToken 的 SHA-256 hash。
     *
     * @param launchToken 明文 launchToken。
     * @return 64 位十六进制 hash。
     */
    /**
     * 生成原平台 verify 请求审计 ID。
     *
     * @return verify 请求 ID。
     */
    private String generateVerifyRequestId() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return VERIFY_REQUEST_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * 构造下发给遮罩 SDK 的启动配置快照。
     *
     * @param launchContext 原平台启动上下文实体。
     * @return JSON 快照。
     */
    private String buildSdkConfigSnapshot(PlatformLaunchContext launchContext) {
        return "{"
                + "\"sdkMode\":" + jsonValue(launchContext.getSdkMode()) + ","
                + "\"sceneType\":" + jsonValue(launchContext.getSceneType()) + ","
                + "\"taskId\":" + jsonValue(launchContext.getTaskId()) + ","
                + "\"executionId\":" + jsonValue(launchContext.getExecutionId()) + ","
                + "\"dataInstanceId\":" + jsonValue(launchContext.getDataInstanceId()) + ","
                + "\"actorType\":" + jsonValue(launchContext.getActorType()) + ","
                + "\"requiredExternalOrgId\":" + jsonValue(launchContext.getRequiredExternalOrgId()) + ","
                + "\"requiredExternalRoleId\":" + jsonValue(launchContext.getRequiredExternalRoleId())
                + "}";
    }

    /**
     * 构造绑定数据实例的校验快照。
     *
     * @param instance 教学数据实例。
     * @return JSON 快照。
     */
    private String buildDataInstanceValidationSnapshot(TeachingDataInstance instance) {
        return "{"
                + "\"dataInstanceId\":" + jsonValue(instance.getId()) + ","
                + "\"externalBusinessId\":" + jsonValue(instance.getExternalBusinessId()) + ","
                + "\"externalBusinessNo\":" + jsonValue(instance.getExternalBusinessNo()) + ","
                + "\"externalStatus\":" + jsonValue(instance.getExternalStatus()) + ","
                + "\"instanceStatus\":" + jsonValue(instance.getInstanceStatus()) + ","
                + "\"validationStatus\":" + jsonValue(instance.getValidationStatus()) + ","
                + "\"validationTime\":" + jsonValue(instance.getValidationTime() == null
                ? null : instance.getValidationTime().toString()) + ","
                + "\"validationResultJson\":" + jsonValue(instance.getValidationResultJson())
                + "}";
    }

    /**
     * 对 JSON 字符串值做最小转义。
     *
     * @param value 原始文本。
     * @return JSON 字符串值或 null。
     */
    private String jsonValue(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n") + "\"";
    }

    private String hashToken(String launchToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(launchToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
    }
}

