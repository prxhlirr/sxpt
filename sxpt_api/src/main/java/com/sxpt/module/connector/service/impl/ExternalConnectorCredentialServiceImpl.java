package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.ConnectorExternalCredential;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.mapper.ConnectorExternalCredentialMapper;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.connector.service.ExternalConnectorCredentialService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
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
import java.util.UUID;

/**
 * 第三方原平台系统级凭证服务实现。
 *
 * 业务功能：
 * 1. 为教学平台中的原平台正式环境生成外部调用 API Key。
 * 2. 使用 API Key 反查正式环境平台，建立第三方系统级调用身份。
 *
 * 关键流程：
 * 1. 生成时只返回一次 API Key 明文，数据库保存 SHA-256 哈希和前缀。
 * 2. 认证时先校验凭证状态，再校验绑定平台必须是启用的 PROD 正式环境。
 * 3. 认证通过后自动定位同环境组 LEARNING 学习环境，供外部接口限制资源边界。
 */
@Service
@Profile("!test")
public class ExternalConnectorCredentialServiceImpl implements ExternalConnectorCredentialService {

    private static final String ENVIRONMENT_TYPE_PROD = "PROD";

    private static final String ENVIRONMENT_TYPE_LEARNING = "LEARNING";

    private static final String HASH_ALGORITHM = "SHA-256";

    private static final String API_KEY_PREFIX = "sk_live_";

    private static final int RANDOM_KEY_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    private final ConnectorExternalCredentialMapper credentialMapper;

    private final ConnectorSystemMapper connectorSystemMapper;

    public ExternalConnectorCredentialServiceImpl(ConnectorExternalCredentialMapper credentialMapper,
                                                  ConnectorSystemMapper connectorSystemMapper) {
        this.credentialMapper = credentialMapper;
        this.connectorSystemMapper = connectorSystemMapper;
    }

    /**
     * 为原平台正式环境生成第三方调用 API Key。
     *
     * @param connectorSystemId 教学平台生成的原平台正式环境 ID。
     * @param operator 操作人。
     * @return 凭证实体和一次性 API Key 明文。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneratedCredential generateApiKey(String connectorSystemId, String operator) {
        ConnectorSystem sourceSystem = getConnectorSystem(connectorSystemId);
        if (!ENVIRONMENT_TYPE_PROD.equals(sourceSystem.getEnvironmentType())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (!StringUtils.hasText(sourceSystem.getEnvironmentGroupCode())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (getApiKeyByConnectorSystemId(sourceSystem.getId()) != null) {
            throw new BusinessException(409, "该系统已存在第三方对接 Key，不允许重复生成");
        }
        String apiKey = generatePlainApiKey(sourceSystem);
        String hash = hash(apiKey);
        LocalDateTime now = LocalDateTime.now();

        ConnectorExternalCredential credential = new ConnectorExternalCredential();
        credential.setId(UUID.randomUUID().toString().replace("-", ""));
        credential.setTenantId(sourceSystem.getTenantId());
        credential.setConnectorSystemId(sourceSystem.getId());
        credential.setCredentialName(sourceSystem.getSystemName() + "外部调用密钥");
        credential.setApiKeyPrefix(apiKey.substring(0, Math.min(apiKey.length(), 24)));
        credential.setApiKeyHash(hash);
        credential.setHashAlgorithm(HASH_ALGORITHM);
        credential.setCreateBy(firstText(operator, "system"));
        credential.setCreateTime(now);
        credential.setUpdateBy(firstText(operator, "system"));
        credential.setUpdateTime(now);
        credential.setStatus(RecordStatus.ACTIVE.getValue());
        credential.setDeleted(Boolean.FALSE);
        credentialMapper.insert(credential);
        return new GeneratedCredential(credential, apiKey);
    }

    /**
     * 查询某个原平台正式环境绑定的第三方对接 Key 摘要。
     *
     * @param connectorSystemId 教学平台生成的原平台系统 ID。
     * @return 已绑定凭证；未生成时返回 null。
     */
    @Override
    public ConnectorExternalCredential getApiKeyByConnectorSystemId(String connectorSystemId) {
        if (!StringUtils.hasText(connectorSystemId)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        return credentialMapper.selectOne(new QueryWrapper<ConnectorExternalCredential>()
                .eq("connector_system_id", connectorSystemId)
                .eq("deleted", Boolean.FALSE)
                .last("limit 1"));
    }

    /**
     * 禁用第三方调用 API Key。
     *
     * @param credentialId 凭证 ID。
     * @param operator 操作人。
     * @return 已禁用凭证。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConnectorExternalCredential disableApiKey(String credentialId, String operator) {
        throw new BusinessException(403, "第三方对接 Key 与系统一一对应，不允许停用或修改");
    }

    /**
     * 校验第三方 API Key 并返回绑定的正式环境身份。
     *
     * @param apiKey 第三方请求头传入的 API Key。
     * @return 已认证的第三方调用身份。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthenticatedExternalConnector authenticate(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
        ConnectorExternalCredential credential = credentialMapper.selectOne(new QueryWrapper<ConnectorExternalCredential>()
                .eq("api_key_hash", hash(apiKey.trim()))
                .eq("deleted", Boolean.FALSE));
        if (credential == null || !RecordStatus.ACTIVE.getValue().equals(credential.getStatus())) {
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
        if (credential.getExpireTime() != null && credential.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
        ConnectorSystem sourceSystem = getConnectorSystem(credential.getConnectorSystemId());
        if (!credential.getTenantId().equals(sourceSystem.getTenantId())
                || !ENVIRONMENT_TYPE_PROD.equals(sourceSystem.getEnvironmentType())
                || !RecordStatus.ACTIVE.getValue().equals(sourceSystem.getStatus())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        ConnectorSystem learningSystem = getLearningSystem(sourceSystem);
        credential.setLastUsedTime(LocalDateTime.now());
        credential.setUpdateTime(LocalDateTime.now());
        credentialMapper.updateById(credential);
        return new AuthenticatedExternalConnector(credential, sourceSystem, learningSystem);
    }

    private ConnectorExternalCredential getCredentialById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        ConnectorExternalCredential credential = credentialMapper.selectOne(new QueryWrapper<ConnectorExternalCredential>()
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (credential == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return credential;
    }

    private ConnectorSystem getConnectorSystem(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        ConnectorSystem system = connectorSystemMapper.selectOne(new QueryWrapper<ConnectorSystem>()
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (system == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return system;
    }

    private ConnectorSystem getLearningSystem(ConnectorSystem sourceSystem) {
        if (!StringUtils.hasText(sourceSystem.getEnvironmentGroupCode())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        ConnectorSystem learningSystem = connectorSystemMapper.selectOne(new QueryWrapper<ConnectorSystem>()
                .eq("tenant_id", sourceSystem.getTenantId())
                .eq("environment_group_code", sourceSystem.getEnvironmentGroupCode())
                .eq("environment_type", ENVIRONMENT_TYPE_LEARNING)
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE)
                .last("limit 1"));
        if (learningSystem == null) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        return learningSystem;
    }

    private String generatePlainApiKey(ConnectorSystem sourceSystem) {
        byte[] randomBytes = new byte[RANDOM_KEY_BYTES];
        secureRandom.nextBytes(randomBytes);
        return API_KEY_PREFIX
                + sourceSystem.getId()
                + "_"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
