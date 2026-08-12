package com.sxpt.module.teachingdata.service.impl;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import com.sxpt.module.teachingdata.entity.DataRequirement;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.PrepareJobStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.TriggerType;
import com.sxpt.module.teachingdata.service.DataPrepareFacadeService;
import com.sxpt.module.teachingdata.service.DataPrepareJobService;
import com.sxpt.module.teachingdata.service.DataPrepareOrchestrationService;
import com.sxpt.module.teachingdata.service.DataRequirementGenerationService;
import com.sxpt.module.teachingdata.service.DataRequirementService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 数据准备门面服务实现。
 *
 * 业务功能：
 * 1. 串联数据准备模块的主链路，形成“需求明细 -> 准备任务 -> 原平台创建 -> 实例落库”的最小闭环。
 * 2. 保证同一入口统一生成任务字段、批次号、幂等键和审计信息，降低后续入口重复实现风险。
 *
 * 关键流程：
 * 1. 校验触发请求、生成请求、任务类型和触发方式。
 * 2. 调用 DataRequirementGenerationService 生成需求明细并取得批次信息。
 * 3. 创建 DataPrepareJob 后立即调用编排服务执行创建任务。
 */
@Service
@Profile("!test")
public class DataPrepareFacadeServiceImpl implements DataPrepareFacadeService {

    private static final String DEFAULT_JOB_TYPE = "CREATE";

    private final DataRequirementGenerationService requirementGenerationService;

    private final DataPrepareJobService dataPrepareJobService;

    private final DataPrepareOrchestrationService dataPrepareOrchestrationService;

    private final DataRequirementService dataRequirementService;

    public DataPrepareFacadeServiceImpl(DataRequirementGenerationService requirementGenerationService,
                                        DataPrepareJobService dataPrepareJobService,
                                        DataPrepareOrchestrationService dataPrepareOrchestrationService,
                                        DataRequirementService dataRequirementService) {
        this.requirementGenerationService = requirementGenerationService;
        this.dataPrepareJobService = dataPrepareJobService;
        this.dataPrepareOrchestrationService = dataPrepareOrchestrationService;
        this.dataRequirementService = dataRequirementService;
    }

    /**
     * 生成需求明细并立即执行原平台数据创建任务。
     *
     * @param request 数据准备触发请求。
     * @return 执行后的数据准备任务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataPrepareJob prepareAndExecute(PrepareAndExecuteRequest request) {
        validateRequest(request);
        DataRequirementGenerationService.GenerateResult generateResult =
                requirementGenerationService.generateRequirementItems(request.getGenerateRequest());
        freezeRequirementPolicySnapshot(generateResult, request);
        DataPrepareJob job = buildJob(request, generateResult);
        DataPrepareJob existingJob = dataPrepareJobService.getByIdempotencyKey(
                job.getTenantId(), job.getIdempotencyKey());
        if (existingJob != null) {
            return handleExistingJob(existingJob);
        }
        DataPrepareJob savedJob = dataPrepareJobService.createDataPrepareJob(job);
        return dataPrepareOrchestrationService.executeCreateJob(savedJob.getId());
    }

    /**
     * 人工重试失败的数据准备任务。
     *
     * @param request 失败任务重试请求。
     * @return 重试执行后的数据准备任务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataPrepareJob retryFailedJob(RetryFailedJobRequest request) {
        validateRetryRequest(request);
        DataPrepareJob job = dataPrepareJobService.getByTenantAndId(request.getTenantId(), request.getJobId());
        if (job == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        PrepareJobStatus status = PrepareJobStatus.fromValue(job.getJobStatus());
        if (status == null || !status.isRetryable()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        DataPrepareJob retryingJob = dataPrepareJobService.markRetrying(
                request.getTenantId(), request.getJobId(), request.getUpdateBy());
        if (PrepareJobStatus.PARTIAL_FAILED.equals(status) && defaultLong(job.getSuccessCount()) > 0L) {
            return dataPrepareOrchestrationService.retryFailedItemsJob(retryingJob.getId());
        }
        return dataPrepareOrchestrationService.executeCreateJob(retryingJob.getId());
    }

    /**
     * 处理幂等命中的既有任务。
     *
     * @param existingJob 既有数据准备任务。
     * @return 既有任务或继续执行后的任务。
     */
    private DataPrepareJob handleExistingJob(DataPrepareJob existingJob) {
        PrepareJobStatus status = PrepareJobStatus.fromValue(existingJob.getJobStatus());
        if (PrepareJobStatus.CREATED.equals(status)) {
            return dataPrepareOrchestrationService.executeCreateJob(existingJob.getId());
        }
        return existingJob;
    }

    /**
     * 将发布时策略快照沉淀到批次层，保证后续任务重试或证据查看不依赖单个 job。
     *
     * @param generateResult 需求明细生成结果。
     * @param request 数据准备触发请求。
     */
    private void freezeRequirementPolicySnapshot(DataRequirementGenerationService.GenerateResult generateResult,
                                                 PrepareAndExecuteRequest request) {
        if (generateResult == null || generateResult.getRequirement() == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        DataRequirement requirement = generateResult.getRequirement();
        System.out.println(request.getRequestJson());
        dataRequirementService.freezeRequirementPolicySnapshot(
                requirement.getTenantId(), requirement.getId(), request.getRequestJson());
    }

    /**
     * 构造数据准备任务，确保任务和需求批次使用同一个 requestBatchId。
     *
     * @param request 数据准备触发请求。
     * @param generateResult 需求明细生成结果。
     * @return 数据准备任务。
     */
    private DataPrepareJob buildJob(PrepareAndExecuteRequest request,
                                    DataRequirementGenerationService.GenerateResult generateResult) {
        DataRequirement requirement = generateResult.getRequirement();
        DataPrepareJob job = new DataPrepareJob();
        job.setId(resolveJobId(request));
        job.setTenantId(requirement.getTenantId());
        job.setConnectorSystemId(requirement.getConnectorSystemId());
        job.setTaskId(requirement.getTaskId());
        job.setModuleCode(requirement.getModuleCode());
        job.setSceneType(requirement.getSceneType());
        job.setJobType(resolveJobType(request));
        job.setRequestBatchId(request.getGenerateRequest().getRequestBatchId());
        job.setExpectedCount((long) generateResult.getItems().size());
        job.setIdempotencyKey(resolveIdempotencyKey(request));
        job.setTriggerType(request.getTriggerType());
        job.setTraceId(request.getTraceId());
        job.setRequestJson(request.getRequestJson());
        job.setCreateBy(request.getGenerateRequest().getCreateBy());
        job.setUpdateBy(request.getGenerateRequest().getUpdateBy());
        return job;
    }

    /**
     * 校验触发请求，避免缺少批次、参与者或非法触发方式时创建半成品任务。
     *
     * @param request 数据准备触发请求。
     */
    private void validateRequest(PrepareAndExecuteRequest request) {
        if (request == null || request.getGenerateRequest() == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getTriggerType());
        if (TriggerType.fromValue(request.getTriggerType()) == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验失败重试请求的最小字段。
     *
     * @param request 失败任务重试请求。
     */
    private void validateRetryRequest(RetryFailedJobRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getTenantId());
        requireText(request.getJobId());
    }

    /**
     * 解析任务 ID，调用方未指定时使用应用层 UUID。
     *
     * @param request 数据准备触发请求。
     * @return 任务 ID。
     */
    private String resolveJobId(PrepareAndExecuteRequest request) {
        if (StringUtils.hasText(request.getJobId())) {
            return request.getJobId();
        }
        return generateId();
    }

    /**
     * 解析任务类型，默认只承担创建原平台数据职责。
     *
     * @param request 数据准备触发请求。
     * @return 任务类型。
     */
    private String resolveJobType(PrepareAndExecuteRequest request) {
        if (StringUtils.hasText(request.getJobType())) {
            return request.getJobType();
        }
        return DEFAULT_JOB_TYPE;
    }

    /**
     * 解析幂等键，默认用需求批次、请求批次和触发方式组成稳定业务键。
     *
     * 这里不使用“学生 + 模块 + 题目”作为幂等键，因为重复练习本身应该生成新原平台数据。
     * 调用方必须让 requestBatchId 绑定一次具体 attempt 或 resetNo，才能区分“新练习”和“同一次练习重试”。
     *
     * @param request 数据准备触发请求。
     * @return 幂等键。
     */
    private String resolveIdempotencyKey(PrepareAndExecuteRequest request) {
        if (StringUtils.hasText(request.getIdempotencyKey())) {
            return request.getIdempotencyKey();
        }
        DataRequirementGenerationService.GenerateRequest generateRequest = request.getGenerateRequest();
        return "prepare:" + generateRequest.getRequirementId()
                + ":" + generateRequest.getRequestBatchId()
                + ":" + request.getTriggerType();
    }

    /**
     * 生成应用层主键。
     *
     * @return 无横线 UUID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
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
     * 将空计数字段按 0 处理，避免历史任务判断部分失败时出现空指针。
     *
     * @param value 原始计数。
     * @return 非空计数。
     */
    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
