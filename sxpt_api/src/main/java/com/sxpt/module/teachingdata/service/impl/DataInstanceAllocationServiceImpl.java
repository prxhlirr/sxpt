package com.sxpt.module.teachingdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.mapper.TeachingDataInstanceMapper;
import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;
import com.sxpt.module.teachingdata.entity.TeachingDataPool;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.AllocationStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataInstanceStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataPoolStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.ValidationStatus;
import com.sxpt.module.teachingdata.mapper.DataInstanceAllocationMapper;
import com.sxpt.module.teachingdata.mapper.TeachingDataPoolMapper;
import com.sxpt.module.teachingdata.service.DataInstanceAllocationService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据实例分配服务实现。
 *
 * 业务功能：
 * 1. 创建 data_instance_allocation 记录，保存学生领取原平台数据时的上下文。
 * 2. 查询学生或数据实例维度的分配记录，支撑练习重置、考试追溯和后台审计。
 *
 * 关键流程：
 * 1. 创建时校验租户、数据池、数据实例、任务、学生、场景、单位和角色。
 * 2. 补齐分配状态、领取时间、审计时间和软删除默认值。
 * 3. 查询时统一追加租户和软删除条件，避免跨租户读取分配记录。
 */
@Service
@Profile("!test")
public class DataInstanceAllocationServiceImpl implements DataInstanceAllocationService {

    private final DataInstanceAllocationMapper dataInstanceAllocationMapper;

    private final TeachingDataPoolMapper teachingDataPoolMapper;

    private final TeachingDataInstanceMapper teachingDataInstanceMapper;

    public DataInstanceAllocationServiceImpl(DataInstanceAllocationMapper dataInstanceAllocationMapper,
                                             TeachingDataPoolMapper teachingDataPoolMapper,
                                             TeachingDataInstanceMapper teachingDataInstanceMapper) {
        this.dataInstanceAllocationMapper = dataInstanceAllocationMapper;
        this.teachingDataPoolMapper = teachingDataPoolMapper;
        this.teachingDataInstanceMapper = teachingDataInstanceMapper;
    }

    /**
     * 创建数据实例分配记录。
     *
     * @param allocation 数据实例分配实体。
     * @return 已保存的数据实例分配记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataInstanceAllocation createDataInstanceAllocation(DataInstanceAllocation allocation) {
        validateCreateFields(allocation);
        fillCreateDefaults(allocation);
        dataInstanceAllocationMapper.insert(allocation);
        return allocation;
    }

    /**
     * 从 READY 数据池领取一个校验通过且尚未分配的数据实例。
     *
     * @param request 数据领取请求。
     * @return 已创建的分配记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataInstanceAllocation acquireReadyInstance(AcquireReadyInstanceRequest request) {
        validateAcquireRequest(request);
        TeachingDataPool pool = getReadyPool(request);
        TeachingDataInstance instance = acquireFirstReadyInstance(pool, request);
        try {
            DataInstanceAllocation allocation = buildAllocation(request, pool, instance);
            createDataInstanceAllocation(allocation);
            refreshPoolCounters(pool);
            return allocation;
        } catch (RuntimeException ex) {
            releaseInstanceAfterAllocationFailure(instance, request);
            throw ex;
        }
    }

    /**
     * 查询指定学生在任务和场景下的数据分配记录。
     *
     * @param tenantId 租户 ID。
     * @param taskId 教学任务 ID。
     * @param allocationScene 分配场景。
     * @param ownerUserId 领取数据的用户 ID。
     * @return 数据实例分配记录列表。
     */
    @Override
    public List<DataInstanceAllocation> listByOwnerAndScene(String tenantId,
                                                            String taskId,
                                                            String allocationScene,
                                                            String ownerUserId) {
        requireText(tenantId);
        requireText(taskId);
        requireText(allocationScene);
        requireText(ownerUserId);
        return dataInstanceAllocationMapper.selectList(new QueryWrapper<DataInstanceAllocation>()
                .eq("tenant_id", tenantId)
                .eq("task_id", taskId)
                .eq("allocation_scene", allocationScene)
                .eq("owner_user_id", ownerUserId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("question_attempt_id")
                .orderByAsc("segment_no")
                .orderByDesc("allocate_time"));
    }

    /**
     * 查询指定数据实例的有效分配记录。
     *
     * @param tenantId 租户 ID。
     * @param dataInstanceId 教学数据实例 ID。
     * @return 数据实例分配记录列表。
     */
    @Override
    public List<DataInstanceAllocation> listByDataInstance(String tenantId, String dataInstanceId) {
        requireText(tenantId);
        requireText(dataInstanceId);
        return dataInstanceAllocationMapper.selectList(new QueryWrapper<DataInstanceAllocation>()
                .eq("tenant_id", tenantId)
                .eq("data_instance_id", dataInstanceId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("allocate_time"));
    }

    /**
     * 校验创建数据实例分配记录所需的最小字段。
     *
     * @param allocation 数据实例分配实体。
     */
    private void validateCreateFields(DataInstanceAllocation allocation) {
        if (allocation == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(allocation.getId());
        requireText(allocation.getTenantId());
        requireText(allocation.getPoolId());
        requireText(allocation.getDataInstanceId());
        requireText(allocation.getTaskId());
        requireText(allocation.getOwnerUserId());
        requireText(allocation.getAllocationScene());
        requireText(allocation.getRequiredExternalOrgId());
        requireText(allocation.getRequiredExternalRoleId());
        requireText(allocation.getActorType());
    }

    /**
     * 校验领取请求的最小业务字段，避免创建缺少领取人或池 ID 的悬空分配记录。
     *
     * @param request 数据领取请求。
     */
    private void validateAcquireRequest(AcquireReadyInstanceRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getTenantId());
        requireText(request.getPoolId());
        requireText(request.getOwnerUserId());
        requireText(request.getTaskId());
        requireText(request.getAllocationScene());
        requireText(request.getCreateBy());
        requireText(request.getUpdateBy());
    }

    /**
     * 读取可领取数据池，只有 READY 且 readyCount 大于 0 的池允许分配。
     *
     * @param request 数据领取请求。
     * @return 数据池。
     */
    private TeachingDataPool getReadyPool(AcquireReadyInstanceRequest request) {
        TeachingDataPool pool = teachingDataPoolMapper.selectOne(new QueryWrapper<TeachingDataPool>()
                .eq("id", request.getPoolId())
                .eq("tenant_id", request.getTenantId())
                .eq("task_id", request.getTaskId())
                .eq("pool_status", DataPoolStatus.READY.getValue())
                .gt("ready_count", 0L)
                .eq("deleted", Boolean.FALSE));
        if (pool == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return pool;
    }

    /**
     * 从池中选择第一条校验通过且未分配的数据实例。
     *
     * @param pool 数据池。
     * @return 数据实例。
     */
    private TeachingDataInstance acquireFirstReadyInstance(TeachingDataPool pool, AcquireReadyInstanceRequest request) {
        List<TeachingDataInstance> candidates = teachingDataInstanceMapper.selectList(
                new QueryWrapper<TeachingDataInstance>()
                        .eq("tenant_id", pool.getTenantId())
                        .eq("pool_id", pool.getId())
                        .eq("instance_status", DataInstanceStatus.READY.getValue())
                        .eq("validation_status", ValidationStatus.PASSED.getValue())
                        .eq("deleted", Boolean.FALSE)
                        .orderByAsc("create_time"));
        for (TeachingDataInstance candidate : candidates) {
            Integer allocatedCount = dataInstanceAllocationMapper.selectCount(new QueryWrapper<DataInstanceAllocation>()
                    .eq("tenant_id", pool.getTenantId())
                    .eq("data_instance_id", candidate.getId())
                    .eq("allocation_status", AllocationStatus.ALLOCATED.getValue())
                    .eq("deleted", Boolean.FALSE));
            if ((allocatedCount == null || allocatedCount == 0) && markInstanceAllocated(candidate, request)) {
                return candidate;
            }
        }
        throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
    }

    /**
     * 根据实例快照构造分配记录，保证领取时的组织、角色、参与身份可审计。
     *
     * @param request 数据领取请求。
     * @param pool 数据池。
     * @param instance 数据实例。
     * @return 分配记录。
     */
    private DataInstanceAllocation buildAllocation(AcquireReadyInstanceRequest request,
                                                   TeachingDataPool pool,
                                                   TeachingDataInstance instance) {
        DataInstanceAllocation allocation = new DataInstanceAllocation();
        allocation.setId(generateId());
        allocation.setTenantId(request.getTenantId());
        allocation.setPoolId(pool.getId());
        allocation.setDataInstanceId(instance.getId());
        allocation.setTaskId(request.getTaskId());
        allocation.setExecutionId(instance.getExecutionId());
        allocation.setAttemptId(request.getAttemptId());
        allocation.setQuestionAttemptId(request.getQuestionAttemptId());
        allocation.setOwnerUserId(request.getOwnerUserId());
        allocation.setAllocationScene(request.getAllocationScene());
        allocation.setRequiredExternalOrgId(instance.getRequiredExternalOrgId());
        allocation.setRequiredExternalOrgName(instance.getRequiredExternalOrgName());
        allocation.setRequiredExternalRoleId(instance.getRequiredExternalRoleId());
        allocation.setRequiredExternalRoleName(instance.getRequiredExternalRoleName());
        allocation.setActorType(instance.getActorType());
        allocation.setRequirementSnapshotJson(instance.getRequirementSnapshotJson());
        allocation.setCreateBy(request.getCreateBy());
        allocation.setUpdateBy(request.getUpdateBy());
        return allocation;
    }

    /**
     * 将实例推进为已分配，防止下一次领取再次选中同一条实例。
     *
     * @param instance 数据实例。
     * @param request 数据领取请求。
     */
    private boolean markInstanceAllocated(TeachingDataInstance instance, AcquireReadyInstanceRequest request) {
        LocalDateTime now = LocalDateTime.now();
        int updated = teachingDataInstanceMapper.update(null, new UpdateWrapper<TeachingDataInstance>()
                .eq("id", instance.getId())
                .eq("tenant_id", request.getTenantId())
                .eq("pool_id", instance.getPoolId())
                .eq("instance_status", DataInstanceStatus.READY.getValue())
                .eq("validation_status", ValidationStatus.PASSED.getValue())
                .eq("deleted", Boolean.FALSE)
                .set("owner_user_id", request.getOwnerUserId())
                .set("attempt_id", request.getAttemptId())
                .set("instance_status", DataInstanceStatus.ALLOCATED.getValue())
                .set("update_by", request.getUpdateBy())
                .set("update_time", now));
        if (updated != 1) {
            return false;
        }
        instance.setOwnerUserId(request.getOwnerUserId());
        instance.setAttemptId(request.getAttemptId());
        instance.setInstanceStatus(DataInstanceStatus.ALLOCATED.getValue());
        instance.setUpdateBy(request.getUpdateBy());
        instance.setUpdateTime(now);
        return true;
    }

    /**
     * 分配记录创建失败时释放刚抢占的实例。
     * <p>
     * 条件更新会先把实例从 READY 推进到 ALLOCATED，若后续插入分配记录失败，事务通常会整体回滚；这里显式释放
     * 是为了兼容非事务代理调用和未来拆分远程写入时的补偿路径。
     *
     * @param instance 已抢占的数据实例。
     * @param request 数据领取请求。
     */
    private void releaseInstanceAfterAllocationFailure(TeachingDataInstance instance, AcquireReadyInstanceRequest request) {
        teachingDataInstanceMapper.update(null, new UpdateWrapper<TeachingDataInstance>()
                .eq("id", instance.getId())
                .eq("tenant_id", request.getTenantId())
                .eq("instance_status", DataInstanceStatus.ALLOCATED.getValue())
                .eq("owner_user_id", request.getOwnerUserId())
                .eq("deleted", Boolean.FALSE)
                .set("owner_user_id", null)
                .set("attempt_id", null)
                .set("instance_status", DataInstanceStatus.READY.getValue())
                .set("update_by", request.getUpdateBy())
                .set("update_time", LocalDateTime.now()));
    }

    /**
     * 重新统计池的可用和已分配数量，保证后台库存数字与实例状态一致。
     *
     * @param pool 数据池。
     */
    private void refreshPoolCounters(TeachingDataPool pool) {
        Integer readyCount = teachingDataInstanceMapper.selectCount(new QueryWrapper<TeachingDataInstance>()
                .eq("tenant_id", pool.getTenantId())
                .eq("pool_id", pool.getId())
                .eq("instance_status", DataInstanceStatus.READY.getValue())
                .eq("validation_status", ValidationStatus.PASSED.getValue())
                .eq("deleted", Boolean.FALSE));
        Integer allocatedCount = teachingDataInstanceMapper.selectCount(new QueryWrapper<TeachingDataInstance>()
                .eq("tenant_id", pool.getTenantId())
                .eq("pool_id", pool.getId())
                .eq("instance_status", DataInstanceStatus.ALLOCATED.getValue())
                .eq("deleted", Boolean.FALSE));
        pool.setReadyCount(readyCount == null ? 0L : readyCount.longValue());
        pool.setAllocatedCount(allocatedCount == null ? 0L : allocatedCount.longValue());
        pool.setPoolStatus(pool.getReadyCount() != null && pool.getReadyCount() > 0
                ? DataPoolStatus.READY.getValue()
                : DataPoolStatus.EXHAUSTED.getValue());
        pool.setUpdateTime(LocalDateTime.now());
        teachingDataPoolMapper.updateById(pool);
    }

    /**
     * 生成应用层主键，避免领取接口依赖数据库自增能力。
     *
     * @return 无横线 UUID。
     */
    private String generateId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
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
     * @param allocation 数据实例分配实体。
     */
    private void fillCreateDefaults(DataInstanceAllocation allocation) {
        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(allocation.getAllocationStatus())) {
            allocation.setAllocationStatus(AllocationStatus.ALLOCATED.getValue());
        }
        if (allocation.getAllocateTime() == null) {
            allocation.setAllocateTime(now);
        }
        if (allocation.getCreateTime() == null) {
            allocation.setCreateTime(now);
        }
        if (allocation.getUpdateTime() == null) {
            allocation.setUpdateTime(now);
        }
        if (!StringUtils.hasText(allocation.getStatus())) {
            allocation.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (allocation.getDeleted() == null) {
            allocation.setDeleted(Boolean.FALSE);
        }
    }
}
