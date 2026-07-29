package com.sxpt.module.teachingdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.mapper.TeachingDataInstanceMapper;
import com.sxpt.module.connector.service.OriginDataPrepareAdapter;
import com.sxpt.module.connector.service.BusinessModuleProcessSnapshotService;
import com.sxpt.module.connector.service.BusinessModuleProcessSnapshotService.SnapshotResult;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import com.sxpt.module.teachingdata.entity.DataRequirement;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;
import com.sxpt.module.teachingdata.entity.TeachingDataPool;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataInstanceStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataPoolStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.PrepareJobStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RequirementItemStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RequirementStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.ValidationStatus;
import com.sxpt.module.teachingdata.mapper.DataPrepareJobMapper;
import com.sxpt.module.teachingdata.mapper.DataRequirementMapper;
import com.sxpt.module.teachingdata.mapper.DataRequirementItemMapper;
import com.sxpt.module.teachingdata.mapper.TeachingDataPoolMapper;
import com.sxpt.module.teachingdata.service.DataPrepareOrchestrationService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据准备编排服务实现。
 *
 * 业务功能：
 * 1. 负责执行创建型数据准备任务，把需求明细提交给原平台并接收逐条结果。
 * 2. 将原平台已落库的业务数据转换为 teaching_data_instance 引用，避免教学平台接管原业务数据。
 *
 * 关键流程：
 * 1. 读取任务和同批次需求明细，任务进入 RUNNING。
 * 2. 调用 OriginDataPrepareAdapter.createTeachingData 创建原平台数据。
 * 3. 逐条按 requestItemId 回填成功或失败结果，成功项生成 READY 教学数据实例。
 * 4. 根据成功和失败数量更新任务终态。
 */
@Service
@Profile("!test")
public class DataPrepareOrchestrationServiceImpl implements DataPrepareOrchestrationService {

    private static final String ADAPTER_SUCCESS_STATUS = "SUCCESS";

    private static final long INITIAL_RESET_COUNT = 0L;

    private static final long INITIAL_LOCK_VERSION = 0L;

    private final DataPrepareJobMapper dataPrepareJobMapper;

    private final DataRequirementItemMapper dataRequirementItemMapper;

    private final DataRequirementMapper dataRequirementMapper;

    private final TeachingDataPoolMapper teachingDataPoolMapper;

    private final TeachingDataInstanceMapper teachingDataInstanceMapper;

    private final OriginDataPrepareAdapter originDataPrepareAdapter;

    private final BusinessModuleProcessSnapshotService processSnapshotService;

    public DataPrepareOrchestrationServiceImpl(DataPrepareJobMapper dataPrepareJobMapper,
                                               DataRequirementItemMapper dataRequirementItemMapper,
                                               DataRequirementMapper dataRequirementMapper,
                                               TeachingDataPoolMapper teachingDataPoolMapper,
                                               TeachingDataInstanceMapper teachingDataInstanceMapper,
                                               OriginDataPrepareAdapter originDataPrepareAdapter,
                                               BusinessModuleProcessSnapshotService processSnapshotService) {
        this.dataPrepareJobMapper = dataPrepareJobMapper;
        this.dataRequirementItemMapper = dataRequirementItemMapper;
        this.dataRequirementMapper = dataRequirementMapper;
        this.teachingDataPoolMapper = teachingDataPoolMapper;
        this.teachingDataInstanceMapper = teachingDataInstanceMapper;
        this.originDataPrepareAdapter = originDataPrepareAdapter;
        this.processSnapshotService = processSnapshotService;
    }

    /**
     * 执行创建型数据准备任务。
     *
     * @param jobId 数据准备任务 ID。
     * @return 已完成状态更新的数据准备任务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataPrepareJob executeCreateJob(String jobId) {
        requireText(jobId);
        DataPrepareJob job = getExistingJob(jobId);
        List<DataRequirementItem> items = listRequirementItems(job);
        ensureHasItems(items);
        markJobRunning(job, items.size());
        OriginDataPrepareAdapter.BatchCreateResponse response = originDataPrepareAdapter.createTeachingData(
                buildBatchCreateRequest(job, items));
        return applyBatchCreateResponse(job, items, response);
    }

    /**
     * 校验教学数据实例是否满足原平台业务约束。
     *
     * @param instanceId 教学数据实例 ID。
     * @return 已更新校验结果的教学数据实例。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingDataInstance validatePreparedInstance(String instanceId) {
        requireText(instanceId);
        TeachingDataInstance instance = getExistingInstance(instanceId);
        OriginDataPrepareAdapter.ValidationResponse response = originDataPrepareAdapter.validateTeachingData(
                buildValidationRequest(instance));
        applyValidationResponse(instance, response);
        updateRequirementItemValidation(instance, response);
        return instance;
    }

    /**
     * 读取未软删除的数据准备任务。
     *
     * @param jobId 数据准备任务 ID。
     * @return 数据准备任务实体。
     */
    private DataPrepareJob getExistingJob(String jobId) {
        DataPrepareJob job = dataPrepareJobMapper.selectById(jobId);
        if (job == null || Boolean.TRUE.equals(job.getDeleted())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return job;
    }

    /**
     * 读取未软删除的教学数据实例。
     *
     * @param instanceId 教学数据实例 ID。
     * @return 教学数据实例。
     */
    private TeachingDataInstance getExistingInstance(String instanceId) {
        TeachingDataInstance instance = teachingDataInstanceMapper.selectById(instanceId);
        if (instance == null || Boolean.TRUE.equals(instance.getDeleted())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return instance;
    }

    /**
     * 查询任务对应批次下仍有效的需求明细。
     *
     * @param job 数据准备任务。
     * @return 需求明细列表。
     */
    private List<DataRequirementItem> listRequirementItems(DataPrepareJob job) {
        return dataRequirementItemMapper.selectList(new QueryWrapper<DataRequirementItem>()
                .eq("tenant_id", job.getTenantId())
                .eq("request_batch_id", job.getRequestBatchId())
                .eq("connector_system_id", job.getConnectorSystemId())
                .eq("module_code", job.getModuleCode())
                .eq("scene_type", job.getSceneType())
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("request_item_id"));
    }

    /**
     * 校验批次内存在待处理需求，避免空批次调用原平台。
     *
     * @param items 需求明细列表。
     */
    private void ensureHasItems(List<DataRequirementItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 将任务标记为运行中并记录预期数量。
     *
     * @param job 数据准备任务。
     * @param expectedCount 预期处理数量。
     */
    private void markJobRunning(DataPrepareJob job, int expectedCount) {
        LocalDateTime now = LocalDateTime.now();
        job.setJobStatus(PrepareJobStatus.RUNNING.getValue());
        job.setExpectedCount((long) expectedCount);
        job.setStartTime(now);
        job.setUpdateTime(now);
        dataPrepareJobMapper.updateById(job);
        updateRequirementPreparing(job);
    }

    /**
     * 构造提交给原平台的批量创建请求。
     *
     * @param job 数据准备任务。
     * @param items 需求明细列表。
     * @return 原平台批量创建请求。
     */
    private OriginDataPrepareAdapter.BatchCreateRequest buildBatchCreateRequest(DataPrepareJob job,
                                                                                List<DataRequirementItem> items) {
        OriginDataPrepareAdapter.BatchCreateRequest request = new OriginDataPrepareAdapter.BatchCreateRequest();
        request.setTenantId(job.getTenantId());
        request.setConnectorSystemId(job.getConnectorSystemId());
        request.setModuleCode(job.getModuleCode());
        request.setSceneType(job.getSceneType());
        request.setRequestBatchId(job.getRequestBatchId());
        request.setIdempotencyKey(job.getIdempotencyKey());
        request.setRequestJson(job.getRequestJson());
        request.setItems(items.stream().map(this::buildRequestItem).collect(Collectors.toList()));
        return request;
    }

    /**
     * 构造单条原平台创建请求，保留学生、单位、角色和动作约束。
     *
     * @param item 数据需求明细。
     * @return 原平台逐条创建请求。
     */
    private OriginDataPrepareAdapter.RequestItem buildRequestItem(DataRequirementItem item) {
        OriginDataPrepareAdapter.RequestItem requestItem = new OriginDataPrepareAdapter.RequestItem();
        requestItem.setRequestItemId(item.getRequestItemId());
        requestItem.setStudentId(item.getStudentId());
        requestItem.setQuestionId(item.getQuestionId());
        requestItem.setRequiredExternalOrgId(item.getRequiredExternalOrgId());
        requestItem.setRequiredExternalRoleId(item.getRequiredExternalRoleId());
        requestItem.setActorType(item.getActorType());
        requestItem.setDataScopeJson(item.getDataScopeJson());
        requestItem.setRequiredActionsJson(item.getRequiredActionsJson());
        return requestItem;
    }

    /**
     * 应用原平台批量创建响应，完成明细回填、实例落库和任务终态更新。
     *
     * @param job 数据准备任务。
     * @param items 原始需求明细。
     * @param response 原平台批量创建响应。
     * @return 已更新的数据准备任务。
     */
    private DataPrepareJob applyBatchCreateResponse(DataPrepareJob job,
                                                    List<DataRequirementItem> items,
                                                    OriginDataPrepareAdapter.BatchCreateResponse response) {
        if (response == null) {
            markJobFailed(job, "原平台未返回数据准备结果");
            return job;
        }
        Map<String, OriginDataPrepareAdapter.ResponseItem> responseItemMap = mapResponseItems(response.getItems());
        Map<String, TeachingDataPool> poolMap = createPoolsByQuestion(job, items);
        long successCount = 0L;
        long failedCount = 0L;
        for (DataRequirementItem item : items) {
            OriginDataPrepareAdapter.ResponseItem responseItem = responseItemMap.get(item.getRequestItemId());
            if (isSuccessfulResponseItem(responseItem)) {
                applySuccessItem(job, item, responseItem, resolvePool(poolMap, item));
                successCount++;
            } else {
                applyFailedItem(item, responseItem);
                failedCount++;
            }
        }
        refreshPoolCounters(poolMap);
        markJobFinished(job, response, successCount, failedCount);
        return job;
    }

    /**
     * 将原平台逐条响应按 requestItemId 建索引。
     *
     * @param responseItems 原平台逐条响应。
     * @return 响应索引。
     */
    private Map<String, OriginDataPrepareAdapter.ResponseItem> mapResponseItems(
            List<OriginDataPrepareAdapter.ResponseItem> responseItems) {
        if (responseItems == null) {
            return Collections.emptyMap();
        }
        return responseItems.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getRequestItemId()))
                .collect(Collectors.toMap(
                        OriginDataPrepareAdapter.ResponseItem::getRequestItemId,
                        Function.identity(),
                        (first, second) -> first));
    }

    /**
     * 判断原平台逐条响应是否代表可用业务数据。
     *
     * @param responseItem 原平台逐条响应。
     * @return true 表示创建成功且返回外部业务 ID。
     */
    private boolean isSuccessfulResponseItem(OriginDataPrepareAdapter.ResponseItem responseItem) {
        return responseItem != null
                && StringUtils.hasText(responseItem.getExternalBusinessId())
                && ADAPTER_SUCCESS_STATUS.equals(responseItem.getItemStatus());
    }

    /**
     * 回填成功明细并创建教学数据实例引用。
     *
     * @param job 数据准备任务。
     * @param item 需求明细。
     * @param responseItem 原平台逐条响应。
     */
    private void applySuccessItem(DataPrepareJob job,
                                  DataRequirementItem item,
                                  OriginDataPrepareAdapter.ResponseItem responseItem,
                                  TeachingDataPool pool) {
        LocalDateTime now = LocalDateTime.now();
        item.setExternalBusinessId(responseItem.getExternalBusinessId());
        item.setExternalBusinessNo(responseItem.getExternalBusinessNo());
        item.setExternalBusinessName(firstText(responseItem.getExternalBusinessName(), responseItem.getExternalBusinessNo()));
        item.setExternalStatus(responseItem.getExternalStatus());
        item.setTargetUrl(responseItem.getTargetUrl());
        applyResponseProcessContext(item, responseItem);
        item.setItemStatus(RequirementItemStatus.READY.getValue());
        item.setValidationStatus(ValidationStatus.NOT_CHECKED.getValue());
        item.setFailureReason(null);
        item.setUpdateTime(now);
        dataRequirementItemMapper.updateById(item);
        TeachingDataInstance instance = buildTeachingDataInstance(job, item, responseItem, pool, now);
        teachingDataInstanceMapper.insert(instance);
        validatePreparedInstance(instance.getId());
    }

    /**
     * 使用原平台返回的当前办理上下文或业务模块标准办理链补齐实例链路快照。
     * <p>
     * 原平台返回完整办理链时直接保存真实链路；只返回当前步骤或当前参与方时，先保存原平台返回值，再使用
     * 标准办理链生成完整快照。这样真实原平台可以逐步接入，不必一次实现完整链路协议。
     *
     * @param item 数据需求明细。
     * @param responseItem 原平台逐条响应。
     */
    private void applyResponseProcessContext(DataRequirementItem item, OriginDataPrepareAdapter.ResponseItem responseItem) {
        item.setCurrentStepCode(responseItem.getCurrentStepCode());
        item.setCurrentActorNo(responseItem.getCurrentActorNo());
        item.setCurrentOrgId(responseItem.getCurrentOrgId());
        item.setCurrentOrgName(responseItem.getCurrentOrgName());
        item.setCurrentRoleId(responseItem.getCurrentRoleId());
        item.setCurrentRoleName(responseItem.getCurrentRoleName());
        if (StringUtils.hasText(responseItem.getProcessChainJson())) {
            item.setProcessChainSnapshotJson(responseItem.getProcessChainJson());
            applyCurrentIdentityFromResponse(item);
            return;
        }
        if (!StringUtils.hasText(item.getBusinessModuleId())) {
            applyCurrentIdentityFromResponse(item);
            return;
        }
        SnapshotResult snapshot = processSnapshotService.generateStandardSnapshot(
                item.getTenantId(),
                item.getBusinessModuleId(),
                item.getCurrentStepCode(),
                item.getCurrentActorNo());
        item.setProcessChainSnapshotJson(snapshot.getSnapshotJson());
        item.setCurrentStepCode(snapshot.getCurrentStepCode());
        item.setCurrentActorNo(snapshot.getCurrentActorNo());
        item.setCurrentOrgId(snapshot.getCurrentOrgId());
        item.setCurrentOrgName(snapshot.getCurrentOrgName());
        item.setCurrentRoleId(snapshot.getCurrentRoleId());
        item.setCurrentRoleName(snapshot.getCurrentRoleName());
        item.setRequiredExternalOrgId(snapshot.getCurrentOrgId());
        item.setRequiredExternalOrgName(snapshot.getCurrentOrgName());
        item.setRequiredExternalRoleId(snapshot.getCurrentRoleId());
        item.setRequiredExternalRoleName(snapshot.getCurrentRoleName());
    }

    /**
     * 原平台已经返回当前单位和角色时，直接同步到校验字段。
     * <p>
     * 该方法只在完整真实链路或无标准链兜底时使用，避免覆盖后续标准链推导出的默认身份。
     *
     * @param item 数据需求明细。
     */
    private void applyCurrentIdentityFromResponse(DataRequirementItem item) {
        if (StringUtils.hasText(item.getCurrentOrgId())) {
            item.setRequiredExternalOrgId(item.getCurrentOrgId());
            item.setRequiredExternalOrgName(item.getCurrentOrgName());
        }
        if (StringUtils.hasText(item.getCurrentRoleId())) {
            item.setRequiredExternalRoleId(item.getCurrentRoleId());
            item.setRequiredExternalRoleName(item.getCurrentRoleName());
        }
    }

    /**
     * 回填失败明细，保留原平台错误原因便于后续重试。
     *
     * @param item 需求明细。
     * @param responseItem 原平台逐条响应，可能为空。
     */
    private void applyFailedItem(DataRequirementItem item, OriginDataPrepareAdapter.ResponseItem responseItem) {
        item.setItemStatus(RequirementItemStatus.FAILED.getValue());
        item.setValidationStatus(ValidationStatus.NOT_CHECKED.getValue());
        item.setFailureReason(responseItem == null ? "原平台未返回该需求项结果" : responseItem.getErrorMessage());
        item.setUpdateTime(LocalDateTime.now());
        dataRequirementItemMapper.updateById(item);
    }

    /**
     * 构造教学平台保存的原平台业务数据引用实例。
     *
     * @param job 数据准备任务。
     * @param item 需求明细。
     * @param responseItem 原平台逐条响应。
     * @param now 当前时间。
     * @return 教学数据实例。
     */
    private TeachingDataInstance buildTeachingDataInstance(DataPrepareJob job,
                                                           DataRequirementItem item,
                                                           OriginDataPrepareAdapter.ResponseItem responseItem,
                                                           TeachingDataPool pool,
                                                           LocalDateTime now) {
        TeachingDataInstance instance = new TeachingDataInstance();
        instance.setId(UUID.randomUUID().toString());
        instance.setTenantId(item.getTenantId());
        instance.setTemplateId(item.getTemplateId());
        instance.setConnectorSystemId(item.getConnectorSystemId());
        instance.setPoolId(pool == null ? null : pool.getId());
        instance.setOwnerUserId(item.getStudentId());
        instance.setTaskId(item.getTaskId());
        instance.setExecutionId(item.getExecutionId());
        instance.setSceneType(item.getSceneType());
        instance.setModuleCode(item.getModuleCode());
        instance.setRequirementId(item.getRequirementId());
        instance.setRequirementItemId(item.getId());
        instance.setPrepareJobId(job.getId());
        instance.setRequestBatchId(item.getRequestBatchId());
        instance.setRequestItemId(item.getRequestItemId());
        instance.setExternalBusinessId(responseItem.getExternalBusinessId());
        instance.setExternalBusinessNo(responseItem.getExternalBusinessNo());
        instance.setExternalStatus(responseItem.getExternalStatus());
        instance.setOwnerExternalOrgId(item.getOwnerExternalOrgId());
        instance.setOwnerExternalOrgName(item.getOwnerExternalOrgName());
        instance.setRequiredExternalOrgId(item.getRequiredExternalOrgId());
        instance.setRequiredExternalOrgName(item.getRequiredExternalOrgName());
        instance.setRequiredExternalRoleId(item.getRequiredExternalRoleId());
        instance.setRequiredExternalRoleName(item.getRequiredExternalRoleName());
        instance.setActorType(item.getActorType());
        instance.setTargetUrl(responseItem.getTargetUrl());
        instance.setRequirementSnapshotJson(item.getDataScopeJson());
        instance.setValidationStatus(ValidationStatus.NOT_CHECKED.getValue());
        instance.setInstanceStatus(DataInstanceStatus.READY.getValue());
        instance.setResetCount(INITIAL_RESET_COUNT);
        instance.setLockVersion(INITIAL_LOCK_VERSION);
        instance.setCreateBy(job.getCreateBy());
        instance.setUpdateBy(job.getUpdateBy());
        instance.setCreateTime(now);
        instance.setUpdateTime(now);
        instance.setStatus(RecordStatus.ACTIVE.getValue());
        instance.setDeleted(Boolean.FALSE);
        return instance;
    }

    /**
     * 按题目维度创建或复用数据池。
     * <p>
     * 一个教学任务通常包含多个题目，题目之间的数据约束、角色、初始化状态可能不同，因此数据池必须先按
     * questionId 分组，后续学生领取时才能从对应题目的可用池中取数，避免跨题目复用错误数据。
     *
     * @param job 数据准备任务。
     * @param items 本次批次生成出的需求明细。
     * @return 以题目分组键为 key 的数据池映射。
     */
    private Map<String, TeachingDataPool> createPoolsByQuestion(DataPrepareJob job, List<DataRequirementItem> items) {
        Map<String, TeachingDataPool> result = new HashMap<>();
        for (DataRequirementItem item : items) {
            String key = poolKey(item);
            if (!result.containsKey(key)) {
                result.put(key, getOrCreatePool(job, item, key));
            }
        }
        return result;
    }

    /**
     * 获取或创建单个数据池。
     * <p>
     * 数据准备任务可能因为页面重复触发、接口重试或任务补偿被多次执行，所以这里使用业务幂等键保证同一个
     * requirement + batch + question 只生成一个池，避免同一批数据被重复纳入可领取范围。
     *
     * @param job 数据准备任务。
     * @param item 用于抽取平台、模块、场景、题目等池归属信息的需求明细。
     * @param key 题目分组键。
     * @return 已存在或新创建的数据池。
     */
    private TeachingDataPool getOrCreatePool(DataPrepareJob job, DataRequirementItem item, String key) {
        String idempotencyKey = buildPoolIdempotencyKey(item, key);
        TeachingDataPool existing = teachingDataPoolMapper.selectOne(new QueryWrapper<TeachingDataPool>()
                .eq("tenant_id", item.getTenantId())
                .eq("idempotency_key", idempotencyKey)
                .eq("deleted", Boolean.FALSE));
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        TeachingDataPool pool = new TeachingDataPool();
        pool.setId(UUID.randomUUID().toString().replace("-", ""));
        pool.setTenantId(item.getTenantId());
        pool.setConnectorSystemId(item.getConnectorSystemId());
        pool.setModuleCode(item.getModuleCode());
        pool.setTaskId(item.getTaskId());
        pool.setSceneType(item.getSceneType());
        pool.setQuestionId(item.getQuestionId());
        pool.setTemplateId(item.getTemplateId());
        pool.setStrategyId(getRequirementStrategyId(item.getRequirementId()));
        pool.setRequirementId(item.getRequirementId());
        pool.setPoolStatus(DataPoolStatus.PREPARING.getValue());
        pool.setTotalCount(0L);
        pool.setReadyCount(0L);
        pool.setAllocatedCount(0L);
        pool.setFailedCount(0L);
        pool.setIdempotencyKey(idempotencyKey);
        pool.setCreateBy(job.getCreateBy());
        pool.setUpdateBy(job.getUpdateBy());
        pool.setCreateTime(now);
        pool.setUpdateTime(now);
        pool.setStatus(RecordStatus.ACTIVE.getValue());
        pool.setDeleted(Boolean.FALSE);
        teachingDataPoolMapper.insert(pool);
        return pool;
    }

    /**
     * 根据实例真实状态回算数据池统计。
     * <p>
     * 池统计不直接信任外部平台返回数量，而是以本系统落库后的实例状态为准，这样验证失败、重复回调、
     * 部分成功等场景都能得到可追溯的池状态。
     *
     * @param poolMap 本次准备任务涉及的数据池。
     */
    private void refreshPoolCounters(Map<String, TeachingDataPool> poolMap) {
        for (TeachingDataPool pool : poolMap.values()) {
            Integer totalCount = teachingDataInstanceMapper.selectCount(new QueryWrapper<TeachingDataInstance>()
                    .eq("tenant_id", pool.getTenantId())
                    .eq("pool_id", pool.getId())
                    .eq("deleted", Boolean.FALSE));
            Integer readyCount = teachingDataInstanceMapper.selectCount(new QueryWrapper<TeachingDataInstance>()
                    .eq("tenant_id", pool.getTenantId())
                    .eq("pool_id", pool.getId())
                    .eq("instance_status", DataInstanceStatus.READY.getValue())
                    .eq("validation_status", ValidationStatus.PASSED.getValue())
                    .eq("deleted", Boolean.FALSE));
            Integer failedCount = teachingDataInstanceMapper.selectCount(new QueryWrapper<TeachingDataInstance>()
                    .eq("tenant_id", pool.getTenantId())
                    .eq("pool_id", pool.getId())
                    .eq("instance_status", DataInstanceStatus.FAILED.getValue())
                    .eq("deleted", Boolean.FALSE));
            pool.setTotalCount(totalCount == null ? 0L : totalCount.longValue());
            pool.setReadyCount(readyCount == null ? 0L : readyCount.longValue());
            pool.setFailedCount(failedCount == null ? 0L : failedCount.longValue());
            pool.setPoolStatus(resolvePoolStatus(pool));
            pool.setUpdateTime(LocalDateTime.now());
            teachingDataPoolMapper.updateById(pool);
        }
    }

    /**
     * 根据数据池统计推导池状态。
     * <p>
     * 只要存在已校验通过的实例，池就可以对外提供领取；全部实例失败时标记失败；其他中间态保持准备中，
     * 便于后续补偿或异步校验继续推进。
     *
     * @param pool 数据池。
     * @return 数据池状态编码。
     */
    private String resolvePoolStatus(TeachingDataPool pool) {
        if (pool.getReadyCount() != null && pool.getReadyCount() > 0) {
            return DataPoolStatus.READY.getValue();
        }
        if (pool.getTotalCount() != null && pool.getTotalCount() > 0
                && pool.getFailedCount() != null && pool.getFailedCount().equals(pool.getTotalCount())) {
            return DataPoolStatus.FAILED.getValue();
        }
        return DataPoolStatus.PREPARING.getValue();
    }

    /**
     * 定位需求明细对应的数据池。
     * <p>
     * 明细与实例一一对应，但池是按题目聚合的，因此创建实例时需要重新通过相同分组规则找到归属池。
     *
     * @param poolMap 数据池映射。
     * @param item 需求明细。
     * @return 需求明细所属数据池。
     */
    private TeachingDataPool resolvePool(Map<String, TeachingDataPool> poolMap, DataRequirementItem item) {
        return poolMap.get(poolKey(item));
    }

    /**
     * 构造数据池分组键。
     * <p>
     * 有题目时按题目隔离；没有题目时退化为批次级公共池，保证早期模板或通用数据准备场景仍能工作。
     *
     * @param item 需求明细。
     * @return 数据池分组键。
     */
    private String poolKey(DataRequirementItem item) {
        return StringUtils.hasText(item.getQuestionId()) ? item.getQuestionId() : "_all";
    }

    /**
     * 取第一个有内容的文本。
     *
     * @param first 首选文本。
     * @param second 兜底文本。
     * @return 第一个非空文本。
     */
    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    /**
     * 构造数据池幂等键。
     * <p>
     * 幂等范围选择 requirement + requestBatch + question，是因为同一需求可能多次创建批次，而同一批次同一题目
     * 在业务上只能对应一个准备池。
     *
     * @param item 需求明细。
     * @param key 数据池分组键。
     * @return 数据池幂等键。
     */
    private String buildPoolIdempotencyKey(DataRequirementItem item, String key) {
        return "pool:" + item.getRequirementId() + ":" + item.getRequestBatchId() + ":" + key;
    }

    /**
     * 查询需求绑定的数据策略。
     * <p>
     * 数据池需要沉淀策略编号，后续审计“为什么这批数据按这种规则准备”时，可以从池追溯到需求和策略快照。
     *
     * @param requirementId 数据需求编号。
     * @return 策略编号；需求不存在时返回 null。
     */
    private String getRequirementStrategyId(String requirementId) {
        if (!StringUtils.hasText(requirementId)) {
            return null;
        }
        DataRequirement requirement = dataRequirementMapper.selectById(requirementId);
        return requirement == null ? null : requirement.getStrategyId();
    }

    /**
     * 构造原平台业务数据校验请求。
     *
     * @param instance 教学数据实例。
     * @return 原平台校验请求。
     */
    private OriginDataPrepareAdapter.ValidationRequest buildValidationRequest(TeachingDataInstance instance) {
        DataRequirementItem item = getExistingRequirementItem(instance);
        OriginDataPrepareAdapter.ValidationRequest request = new OriginDataPrepareAdapter.ValidationRequest();
        request.setTenantId(instance.getTenantId());
        request.setConnectorSystemId(instance.getConnectorSystemId());
        request.setModuleCode(item.getModuleCode());
        request.setExternalBusinessId(instance.getExternalBusinessId());
        request.setRequiredExternalOrgId(instance.getRequiredExternalOrgId());
        request.setRequiredExternalRoleId(instance.getRequiredExternalRoleId());
        request.setRequiredActionsJson(item.getRequiredActionsJson());
        return request;
    }

    /**
     * 根据实例绑定读取需求明细，校验时需要从明细取得业务模块和动作约束。
     *
     * @param instance 教学数据实例。
     * @return 数据需求明细。
     */
    private DataRequirementItem getExistingRequirementItem(TeachingDataInstance instance) {
        requireText(instance.getRequirementItemId());
        DataRequirementItem item = dataRequirementItemMapper.selectById(instance.getRequirementItemId());
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return item;
    }

    /**
     * 应用原平台校验结果并推进实例状态。
     *
     * @param instance 教学数据实例。
     * @param response 原平台校验响应。
     */
    private void applyValidationResponse(TeachingDataInstance instance,
                                         OriginDataPrepareAdapter.ValidationResponse response) {
        LocalDateTime now = LocalDateTime.now();
        if (response != null && response.isPassed()) {
            instance.setValidationStatus(ValidationStatus.PASSED.getValue());
            instance.setValidationResultJson(response.getValidationResultJson());
            instance.setFailureReason(null);
            instance.setInstanceStatus(DataInstanceStatus.READY.getValue());
        } else {
            instance.setValidationStatus(ValidationStatus.FAILED.getValue());
            instance.setValidationResultJson(response == null ? null : response.getValidationResultJson());
            instance.setFailureReason(response == null ? "原平台未返回校验结果" : response.getErrorMessage());
            instance.setInstanceStatus(DataInstanceStatus.FAILED.getValue());
        }
        instance.setValidationTime(now);
        instance.setUpdateTime(now);
        teachingDataInstanceMapper.updateById(instance);
    }

    /**
     * 同步更新需求明细校验结果，保证批次视角和实例视角一致。
     *
     * @param instance 教学数据实例。
     * @param response 原平台校验响应。
     */
    private void updateRequirementItemValidation(TeachingDataInstance instance,
                                                 OriginDataPrepareAdapter.ValidationResponse response) {
        if (!StringUtils.hasText(instance.getRequirementItemId())) {
            return;
        }
        DataRequirementItem item = dataRequirementItemMapper.selectById(instance.getRequirementItemId());
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (response != null && response.isPassed()) {
            item.setValidationStatus(ValidationStatus.PASSED.getValue());
            item.setValidationResultJson(response.getValidationResultJson());
            item.setItemStatus(RequirementItemStatus.READY.getValue());
            item.setFailureReason(null);
        } else {
            item.setValidationStatus(ValidationStatus.FAILED.getValue());
            item.setValidationResultJson(response == null ? null : response.getValidationResultJson());
            item.setItemStatus(RequirementItemStatus.VALIDATION_FAILED.getValue());
            item.setFailureReason(response == null ? "原平台未返回校验结果" : response.getErrorMessage());
        }
        item.setValidationTime(now);
        item.setUpdateTime(now);
        dataRequirementItemMapper.updateById(item);
    }

    /**
     * 标记原平台未返回结果时的任务失败。
     *
     * @param job 数据准备任务。
     * @param errorMessage 错误消息。
     */
    private void markJobFailed(DataPrepareJob job, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        job.setJobStatus(PrepareJobStatus.FAILED.getValue());
        job.setFailedCount(job.getExpectedCount());
        job.setSuccessCount(0L);
        job.setErrorMessage(errorMessage);
        job.setEndTime(now);
        job.setUpdateTime(now);
        dataPrepareJobMapper.updateById(job);
        updateRequirementFinished(job, 0L, job.getExpectedCount());
    }

    /**
     * 任务开始时推进批次状态，保证批次视角能反映正在执行的数据准备过程。
     *
     * @param job 数据准备任务。
     */
    private void updateRequirementPreparing(DataPrepareJob job) {
        DataRequirement requirement = getRequirementByJob(job);
        if (requirement == null) {
            return;
        }
        requirement.setRequirementStatus(RequirementStatus.PREPARING.getValue());
        requirement.setExpectedCount(job.getExpectedCount());
        requirement.setUpdateTime(LocalDateTime.now());
        dataRequirementMapper.updateById(requirement);
    }

    /**
     * 任务结束时回写批次汇总，避免出现任务成功但批次仍为 CREATED 的状态分裂。
     *
     * @param job 数据准备任务。
     * @param successCount 成功数量。
     * @param failedCount 失败数量。
     */
    private void updateRequirementFinished(DataPrepareJob job, long successCount, long failedCount) {
        DataRequirement requirement = getRequirementByJob(job);
        if (requirement == null) {
            return;
        }
        requirement.setExpectedCount(job.getExpectedCount());
        requirement.setSuccessCount(successCount);
        requirement.setFailedCount(failedCount);
        requirement.setRequirementStatus(resolveRequirementStatus(successCount, failedCount));
        requirement.setUpdateTime(LocalDateTime.now());
        dataRequirementMapper.updateById(requirement);
    }

    /**
     * 通过同一批次的需求项定位需求批次，避免在任务表未扩展 requirementId 前丢失汇总回写目标。
     *
     * @param job 数据准备任务。
     * @return 数据需求批次；找不到时返回 null。
     */
    private DataRequirement getRequirementByJob(DataPrepareJob job) {
        DataRequirementItem item = dataRequirementItemMapper.selectOne(new QueryWrapper<DataRequirementItem>()
                .eq("tenant_id", job.getTenantId())
                .eq("request_batch_id", job.getRequestBatchId())
                .eq("connector_system_id", job.getConnectorSystemId())
                .eq("module_code", job.getModuleCode())
                .eq("scene_type", job.getSceneType())
                .eq("deleted", Boolean.FALSE)
                .last("limit 1"));
        if (item == null || !StringUtils.hasText(item.getRequirementId())) {
            return null;
        }
        DataRequirement requirement = dataRequirementMapper.selectById(item.getRequirementId());
        if (requirement == null || Boolean.TRUE.equals(requirement.getDeleted())) {
            return null;
        }
        return requirement;
    }

    /**
     * 按成功和失败数量解析批次终态，使批次状态与任务状态保持同一口径。
     *
     * @param successCount 成功数量。
     * @param failedCount 失败数量。
     * @return 数据需求批次状态。
     */
    private String resolveRequirementStatus(long successCount, long failedCount) {
        if (successCount > 0 && failedCount == 0) {
            return RequirementStatus.READY.getValue();
        }
        if (successCount > 0) {
            return RequirementStatus.PARTIAL_FAILED.getValue();
        }
        return RequirementStatus.FAILED.getValue();
    }

    /**
     * 根据逐条处理结果更新任务终态。
     *
     * @param job 数据准备任务。
     * @param response 原平台批量响应。
     * @param successCount 成功数量。
     * @param failedCount 失败数量。
     */
    private void markJobFinished(DataPrepareJob job,
                                 OriginDataPrepareAdapter.BatchCreateResponse response,
                                 long successCount,
                                 long failedCount) {
        LocalDateTime now = LocalDateTime.now();
        job.setExternalRequestId(response.getExternalRequestId());
        job.setResultJson(response.getResultJson());
        job.setSuccessCount(successCount);
        job.setFailedCount(failedCount);
        job.setJobStatus(resolveJobStatus(successCount, failedCount));
        job.setEndTime(now);
        job.setUpdateTime(now);
        dataPrepareJobMapper.updateById(job);
        updateRequirementFinished(job, successCount, failedCount);
    }

    /**
     * 根据成功和失败数量计算任务终态。
     *
     * @param successCount 成功数量。
     * @param failedCount 失败数量。
     * @return 数据准备任务状态。
     */
    private String resolveJobStatus(long successCount, long failedCount) {
        if (successCount > 0 && failedCount == 0) {
            return PrepareJobStatus.SUCCESS.getValue();
        }
        if (successCount > 0) {
            return PrepareJobStatus.PARTIAL_FAILED.getValue();
        }
        return PrepareJobStatus.FAILED.getValue();
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
}
