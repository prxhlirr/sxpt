package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.mapper.PlatformLaunchContextMapper;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
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

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final String DEFAULT_LAUNCH_STATUS = "CREATED";

    private static final String VERIFIED_LAUNCH_STATUS = "VERIFIED";

    private static final String EXPIRED_LAUNCH_STATUS = "EXPIRED";

    private static final String USED_LAUNCH_STATUS = "USED";

    private static final String FAILED_LAUNCH_STATUS = "FAILED";

    private static final int TOKEN_RANDOM_BYTES = 32;

    private static final int DEFAULT_EXPIRE_MINUTES = 5;

    private final PlatformLaunchContextMapper platformLaunchContextMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    public PlatformLaunchContextServiceImpl(PlatformLaunchContextMapper platformLaunchContextMapper) {
        this.platformLaunchContextMapper = platformLaunchContextMapper;
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
        LocalDateTime now = LocalDateTime.now();
        launchContext.setLaunchStatus(VERIFIED_LAUNCH_STATUS);
        launchContext.setVerifiedTime(now);
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
        if (!VERIFIED_LAUNCH_STATUS.equals(launchContext.getLaunchStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        LocalDateTime now = LocalDateTime.now();
        launchContext.setLaunchStatus(USED_LAUNCH_STATUS);
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
        launchContext.setLaunchStatus(FAILED_LAUNCH_STATUS);
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
        requireText(launchContext.getSceneType());
        requireText(launchContext.getSdkMode());
        requireText(launchContext.getTargetUrl());
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
            launchContext.setLaunchStatus(DEFAULT_LAUNCH_STATUS);
        }
        if (!StringUtils.hasText(launchContext.getStatus())) {
            launchContext.setStatus(DEFAULT_STATUS);
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
            launchContext.setLaunchStatus(EXPIRED_LAUNCH_STATUS);
            launchContext.setUpdateTime(now);
            platformLaunchContextMapper.updateById(launchContext);
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        if (!DEFAULT_LAUNCH_STATUS.equals(launchContext.getLaunchStatus())) {
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

