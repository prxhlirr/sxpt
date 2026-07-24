package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.mapper.TeachingDataInstanceMapper;
import com.sxpt.module.connector.service.TeachingDataInstanceService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教学业务数据实例服务实现。
 *
 * 业务功能：
 * 1. 创建 teaching_data_instance 记录，保存原平台已创建或已复用的业务数据引用。
 * 2. 按外部业务 ID、使用人或任务维度查询教学数据实例，为后续跳转和任务运行提供上下文。
 *
 * 关键流程：
 * 1. 校验创建实例所需的最小字段，避免产生无法回溯到原平台的数据。
 * 2. 补齐实例状态、重置次数、通用状态和软删除默认值。
 * 3. 查询时统一追加租户和软删除条件，保证教学数据实例不会跨租户串读。
 */
@Service
@Profile("!test")
public class TeachingDataInstanceServiceImpl implements TeachingDataInstanceService {

    private static final String DEFAULT_INSTANCE_STATUS = "CREATED";

    private static final String INSTANCE_STATUS_LOCKED = "LOCKED";

    private static final String INSTANCE_STATUS_DISCARDED = "DISCARDED";

    private static final String INSTANCE_STATUS_RESET = "RESET";

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final String DISABLED_STATUS = "DISABLED";

    private static final long DEFAULT_RESET_COUNT = 0L;

    private final TeachingDataInstanceMapper teachingDataInstanceMapper;

    public TeachingDataInstanceServiceImpl(TeachingDataInstanceMapper teachingDataInstanceMapper) {
        this.teachingDataInstanceMapper = teachingDataInstanceMapper;
    }

    /**
     * 创建教学业务数据实例。
     *
     * @param instance 教学业务数据实例实体。
     * @return 已保存的教学业务数据实例。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingDataInstance createTeachingDataInstance(TeachingDataInstance instance) {
        validateCreateFields(instance);
        fillCreateDefaults(instance);
        teachingDataInstanceMapper.insert(instance);
        return instance;
    }

    /**
     * 按原平台业务数据 ID 查询教学业务数据实例。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param externalBusinessId 原平台业务数据 ID。
     * @return 教学业务数据实例；不存在时返回 null。
     */
    @Override
    public TeachingDataInstance getByExternalBusiness(String tenantId,
                                                       String connectorSystemId,
                                                       String externalBusinessId) {
        requireText(tenantId);
        requireText(connectorSystemId);
        requireText(externalBusinessId);
        return teachingDataInstanceMapper.selectOne(new QueryWrapper<TeachingDataInstance>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("external_business_id", externalBusinessId)
                .eq("deleted", Boolean.FALSE));
    }

    /**
     * 查询指定使用人和场景下的教学业务数据实例。
     *
     * @param tenantId 租户 ID。
     * @param ownerUserId 使用人 ID。
     * @param sceneType 场景类型。
     * @return 教学业务数据实例列表。
     */
    @Override
    public List<TeachingDataInstance> listByOwnerAndScene(String tenantId, String ownerUserId, String sceneType) {
        requireText(tenantId);
        requireText(ownerUserId);
        requireText(sceneType);
        return teachingDataInstanceMapper.selectList(new QueryWrapper<TeachingDataInstance>()
                .eq("tenant_id", tenantId)
                .eq("owner_user_id", ownerUserId)
                .eq("scene_type", sceneType)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 查询指定任务和场景下的教学业务数据实例。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param sceneType 场景类型。
     * @return 教学业务数据实例列表。
     */
    @Override
    public List<TeachingDataInstance> listByTaskAndScene(String tenantId, String taskId, String sceneType) {
        requireText(tenantId);
        requireText(taskId);
        requireText(sceneType);
        return teachingDataInstanceMapper.selectList(new QueryWrapper<TeachingDataInstance>()
                .eq("tenant_id", tenantId)
                .eq("task_id", taskId)
                .eq("scene_type", sceneType)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 校验创建实例所需的最小字段。
     *
     * @param instance 教学业务数据实例实体。
     */
    /**
     * 锁定教学业务数据实例。
     *
     * @param id 教学业务数据实例 ID。
     * @return 已锁定的教学业务数据实例。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingDataInstance lockTeachingDataInstance(String id) {
        TeachingDataInstance instance = getExistingInstance(id);
        ensureNotDiscarded(instance);
        LocalDateTime now = LocalDateTime.now();
        instance.setInstanceStatus(INSTANCE_STATUS_LOCKED);
        instance.setLockTime(now);
        instance.setUpdateTime(now);
        teachingDataInstanceMapper.updateById(instance);
        return instance;
    }

    /**
     * 废弃教学业务数据实例。
     *
     * @param id 教学业务数据实例 ID。
     * @return 已废弃的教学业务数据实例。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingDataInstance discardTeachingDataInstance(String id) {
        TeachingDataInstance instance = getExistingInstance(id);
        ensureNotLocked(instance);
        LocalDateTime now = LocalDateTime.now();
        instance.setInstanceStatus(INSTANCE_STATUS_DISCARDED);
        instance.setStatus(DISABLED_STATUS);
        instance.setUpdateTime(now);
        teachingDataInstanceMapper.updateById(instance);
        return instance;
    }

    /**
     * 重置教学业务数据实例。
     *
     * @param id 教学业务数据实例 ID。
     * @param externalBusinessId 新的原平台业务数据 ID。
     * @param externalBusinessNo 新的原平台业务单据号。
     * @param externalStatus 新的原平台业务状态。
     * @param metadataJson 新的原平台业务数据摘要。
     * @return 已重置的教学业务数据实例。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingDataInstance resetTeachingDataInstance(String id,
                                                          String externalBusinessId,
                                                          String externalBusinessNo,
                                                          String externalStatus,
                                                          String metadataJson) {
        requireText(externalBusinessId);
        TeachingDataInstance instance = getExistingInstance(id);
        ensureNotLocked(instance);
        ensureNotDiscarded(instance);
        LocalDateTime now = LocalDateTime.now();
        Long resetCount = instance.getResetCount() == null ? DEFAULT_RESET_COUNT : instance.getResetCount();
        instance.setExternalBusinessId(externalBusinessId);
        instance.setExternalBusinessNo(externalBusinessNo);
        instance.setExternalStatus(externalStatus);
        instance.setMetadataJson(metadataJson);
        instance.setInstanceStatus(INSTANCE_STATUS_RESET);
        instance.setResetCount(resetCount + 1L);
        instance.setUpdateTime(now);
        teachingDataInstanceMapper.updateById(instance);
        return instance;
    }

    private void validateCreateFields(TeachingDataInstance instance) {
        if (instance == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(instance.getId());
        requireText(instance.getTenantId());
        requireText(instance.getTemplateId());
        requireText(instance.getConnectorSystemId());
        requireText(instance.getSceneType());
        requireText(instance.getExternalBusinessId());
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
     * @param instance 教学业务数据实例实体。
     */
    /**
     * 按主键读取未软删除的教学业务数据实例。
     *
     * @param id 教学业务数据实例 ID。
     * @return 教学业务数据实例。
     */
    private TeachingDataInstance getExistingInstance(String id) {
        requireText(id);
        TeachingDataInstance instance = teachingDataInstanceMapper.selectById(id);
        if (instance == null || Boolean.TRUE.equals(instance.getDeleted())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return instance;
    }

    /**
     * 校验实例未锁定。
     *
     * @param instance 教学业务数据实例。
     */
    private void ensureNotLocked(TeachingDataInstance instance) {
        if (INSTANCE_STATUS_LOCKED.equals(instance.getInstanceStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
    }

    /**
     * 校验实例未废弃。
     *
     * @param instance 教学业务数据实例。
     */
    private void ensureNotDiscarded(TeachingDataInstance instance) {
        if (INSTANCE_STATUS_DISCARDED.equals(instance.getInstanceStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
    }

    private void fillCreateDefaults(TeachingDataInstance instance) {
        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(instance.getInstanceStatus())) {
            instance.setInstanceStatus(DEFAULT_INSTANCE_STATUS);
        }
        if (instance.getResetCount() == null) {
            instance.setResetCount(DEFAULT_RESET_COUNT);
        }
        if (instance.getCreateTime() == null) {
            instance.setCreateTime(now);
        }
        if (instance.getUpdateTime() == null) {
            instance.setUpdateTime(now);
        }
        if (!StringUtils.hasText(instance.getStatus())) {
            instance.setStatus(DEFAULT_STATUS);
        }
        if (instance.getDeleted() == null) {
            instance.setDeleted(Boolean.FALSE);
        }
    }
}

