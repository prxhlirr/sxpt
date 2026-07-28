package com.sxpt.module.teachingdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.teachingdata.entity.TeachingDataPool;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataPoolStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.mapper.TeachingDataPoolMapper;
import com.sxpt.module.teachingdata.service.TeachingDataPoolService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教学数据池服务实现。
 *
 * 业务功能：
 * 1. 创建 teaching_data_pool 记录，保存数据准备后的库存聚合视图。
 * 2. 查询任务场景下的数据池和可领取的 READY 数据池。
 *
 * 关键流程：
 * 1. 创建时校验租户、原平台、业务模块、任务、场景、需求批次和幂等键。
 * 2. 补齐池状态、统计计数、乐观锁、审计时间和软删除默认值。
 * 3. 查询时统一追加租户和软删除条件，避免跨租户读取数据池。
 */
@Service
@Profile("!test")
public class TeachingDataPoolServiceImpl implements TeachingDataPoolService {

    private final TeachingDataPoolMapper teachingDataPoolMapper;

    public TeachingDataPoolServiceImpl(TeachingDataPoolMapper teachingDataPoolMapper) {
        this.teachingDataPoolMapper = teachingDataPoolMapper;
    }

    /**
     * 创建教学数据池。
     *
     * @param pool 教学数据池实体。
     * @return 已保存的教学数据池。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingDataPool createTeachingDataPool(TeachingDataPool pool) {
        validateCreateFields(pool);
        fillCreateDefaults(pool);
        teachingDataPoolMapper.insert(pool);
        return pool;
    }

    /**
     * 查询指定任务和场景下的数据池。
     *
     * @param tenantId 租户 ID。
     * @param taskId 教学任务 ID。
     * @param sceneType 教学场景。
     * @return 教学数据池列表。
     */
    @Override
    public List<TeachingDataPool> listByTaskAndScene(String tenantId, String taskId, String sceneType) {
        requireText(tenantId);
        requireText(taskId);
        requireText(sceneType);
        return teachingDataPoolMapper.selectList(new QueryWrapper<TeachingDataPool>()
                .eq("tenant_id", tenantId)
                .eq("task_id", taskId)
                .eq("scene_type", sceneType)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 查询指定批次下的数据池，保证后台能直接核验本次准备的数据是否形成库存。
     *
     * @param tenantId 租户 ID。
     * @param requirementId 数据需求批次 ID。
     * @return 数据池列表。
     */
    @Override
    public List<TeachingDataPool> listByRequirement(String tenantId, String requirementId) {
        requireText(tenantId);
        requireText(requirementId);
        return teachingDataPoolMapper.selectList(new QueryWrapper<TeachingDataPool>()
                .eq("tenant_id", tenantId)
                .eq("requirement_id", requirementId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("question_id")
                .orderByDesc("create_time"));
    }

    /**
     * 查询指定任务、场景和题目下已就绪的数据池。
     *
     * @param tenantId 租户 ID。
     * @param taskId 教学任务 ID。
     * @param sceneType 教学场景。
     * @param questionId 题目 ID。
     * @return 已就绪教学数据池列表。
     */
    @Override
    public List<TeachingDataPool> listReadyPools(String tenantId, String taskId, String sceneType, String questionId) {
        requireText(tenantId);
        requireText(taskId);
        requireText(sceneType);
        requireText(questionId);
        return teachingDataPoolMapper.selectList(new QueryWrapper<TeachingDataPool>()
                .eq("tenant_id", tenantId)
                .eq("task_id", taskId)
                .eq("scene_type", sceneType)
                .eq("question_id", questionId)
                .eq("pool_status", DataPoolStatus.READY.getValue())
                .gt("ready_count", 0L)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("create_time"));
    }

    /**
     * 校验创建教学数据池所需的最小字段。
     *
     * @param pool 教学数据池实体。
     */
    private void validateCreateFields(TeachingDataPool pool) {
        if (pool == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(pool.getId());
        requireText(pool.getTenantId());
        requireText(pool.getConnectorSystemId());
        requireText(pool.getModuleCode());
        requireText(pool.getTaskId());
        requireText(pool.getSceneType());
        requireText(pool.getRequirementId());
        requireText(pool.getIdempotencyKey());
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
     * @param pool 教学数据池实体。
     */
    private void fillCreateDefaults(TeachingDataPool pool) {
        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(pool.getPoolStatus())) {
            pool.setPoolStatus(DataPoolStatus.CREATED.getValue());
        }
        if (pool.getTotalCount() == null) {
            pool.setTotalCount(0L);
        }
        if (pool.getReadyCount() == null) {
            pool.setReadyCount(0L);
        }
        if (pool.getAllocatedCount() == null) {
            pool.setAllocatedCount(0L);
        }
        if (pool.getFailedCount() == null) {
            pool.setFailedCount(0L);
        }
        if (pool.getLockVersion() == null) {
            pool.setLockVersion(0L);
        }
        if (pool.getCreateTime() == null) {
            pool.setCreateTime(now);
        }
        if (pool.getUpdateTime() == null) {
            pool.setUpdateTime(now);
        }
        if (!StringUtils.hasText(pool.getStatus())) {
            pool.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (pool.getDeleted() == null) {
            pool.setDeleted(Boolean.FALSE);
        }
    }
}
