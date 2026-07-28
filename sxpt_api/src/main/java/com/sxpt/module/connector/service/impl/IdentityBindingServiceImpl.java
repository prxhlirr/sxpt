package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.IdentityBinding;
import com.sxpt.module.connector.mapper.IdentityBindingMapper;
import com.sxpt.module.connector.service.IdentityBindingService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 原平台身份绑定服务实现。
 *
 * 业务功能：
 * 1. 创建教学用户与原平台账号之间的绑定关系。
 * 2. 查询教学用户在指定原平台下的有效绑定身份，供后续 launchToken 生成使用。
 *
 * 关键流程：
 * 1. 校验 ID、租户、教学用户、原平台、外部用户 ID 和绑定类型。
 * 2. 补齐创建时间、更新时间、通用状态和软删除标记。
 * 3. 调用 IdentityBindingMapper 写入或查询 identity_binding 表。
 */
@Service
@Profile("!test")
public class IdentityBindingServiceImpl implements IdentityBindingService {

    private final IdentityBindingMapper identityBindingMapper;

    public IdentityBindingServiceImpl(IdentityBindingMapper identityBindingMapper) {
        this.identityBindingMapper = identityBindingMapper;
    }

    /**
     * 创建原平台身份绑定。
     *
     * @param identityBinding 原平台身份绑定实体，必须包含 ID、租户、教学用户、原平台和外部用户 ID。
     * @return 已保存的原平台身份绑定实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public IdentityBinding createIdentityBinding(IdentityBinding identityBinding) {
        validateCreateFields(identityBinding);
        fillCreateDefaults(identityBinding);
        identityBindingMapper.insert(identityBinding);
        return identityBinding;
    }

    /**
     * 查询教学用户在指定原平台下的有效身份绑定。
     *
     * @param tenantId 租户 ID。
     * @param userId 教学用户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @return 未删除的原平台身份绑定。
     */
    @Override
    public IdentityBinding getBindingByUserAndConnector(String tenantId, String userId, String connectorSystemId) {
        requireText(tenantId);
        requireText(userId);
        requireText(connectorSystemId);
        IdentityBinding identityBinding = identityBindingMapper.selectOne(new QueryWrapper<IdentityBinding>()
                .eq("tenant_id", tenantId)
                .eq("user_id", userId)
                .eq("connector_system_id", connectorSystemId)
                .eq("deleted", Boolean.FALSE));
        if (identityBinding == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return identityBinding;
    }

    /**
     * 校验创建身份绑定所需的最小字段。
     *
     * @param identityBinding 原平台身份绑定实体。
     */
    private void validateCreateFields(IdentityBinding identityBinding) {
        if (identityBinding == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(identityBinding.getId());
        requireText(identityBinding.getTenantId());
        requireText(identityBinding.getUserId());
        requireText(identityBinding.getConnectorSystemId());
        requireText(identityBinding.getExternalUserId());
        requireText(identityBinding.getBindingType());
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
     * @param identityBinding 原平台身份绑定实体。
     */
    private void fillCreateDefaults(IdentityBinding identityBinding) {
        LocalDateTime now = LocalDateTime.now();
        if (identityBinding.getCreateTime() == null) {
            identityBinding.setCreateTime(now);
        }
        if (identityBinding.getUpdateTime() == null) {
            identityBinding.setUpdateTime(now);
        }
        if (!StringUtils.hasText(identityBinding.getStatus())) {
            identityBinding.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (identityBinding.getDeleted() == null) {
            identityBinding.setDeleted(Boolean.FALSE);
        }
    }
}

