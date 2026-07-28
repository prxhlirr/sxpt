package com.sxpt.module.teachingdata;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import com.sxpt.module.teachingdata.entity.DataRequirement;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.PrepareJobStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.TriggerType;
import com.sxpt.module.teachingdata.service.DataPrepareFacadeService;
import com.sxpt.module.teachingdata.service.DataPrepareJobService;
import com.sxpt.module.teachingdata.service.DataPrepareOrchestrationService;
import com.sxpt.module.teachingdata.service.DataRequirementGenerationService;
import com.sxpt.module.teachingdata.service.impl.DataPrepareFacadeServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据准备门面服务测试。
 *
 * 业务功能：
 * 1. 验证门面服务能按统一入口串联需求明细生成、准备任务创建和编排执行。
 * 2. 验证触发方式校验和幂等键生成规则，避免后续练习、考试入口重复拼接流程。
 *
 * 关键流程：
 * 1. 使用 Mockito 固定三个下游服务的返回。
 * 2. 捕获创建的数据准备任务，检查批次号、数量、任务类型和幂等键。
 */
class DataPrepareFacadeServiceImplTests {

    private final DataRequirementGenerationService requirementGenerationService =
            mock(DataRequirementGenerationService.class);

    private final DataPrepareJobService dataPrepareJobService = mock(DataPrepareJobService.class);

    private final DataPrepareOrchestrationService orchestrationService =
            mock(DataPrepareOrchestrationService.class);

    private final DataPrepareFacadeService service = new DataPrepareFacadeServiceImpl(
            requirementGenerationService, dataPrepareJobService, orchestrationService);

    /**
     * 验证门面服务会先生成明细，再创建任务，最后执行编排任务。
     */
    @Test
    void prepareAndExecuteShouldGenerateItemsCreateJobAndExecute() {
        DataPrepareFacadeService.PrepareAndExecuteRequest request = buildRequest();
        DataRequirement requirement = buildRequirement();
        DataRequirementItem firstItem = new DataRequirementItem();
        DataRequirementItem secondItem = new DataRequirementItem();
        when(requirementGenerationService.generateRequirementItems(request.getGenerateRequest()))
                .thenReturn(new DataRequirementGenerationService.GenerateResult(
                        requirement, Arrays.asList(firstItem, secondItem)));
        DataPrepareJob savedJob = new DataPrepareJob();
        savedJob.setId("job_001");
        when(dataPrepareJobService.getByIdempotencyKey(
                "tenant_001", "prepare:requirement_001:batch_001:ON_DEMAND")).thenReturn(null);
        when(dataPrepareJobService.createDataPrepareJob(any(DataPrepareJob.class)))
                .thenReturn(savedJob);
        DataPrepareJob executedJob = new DataPrepareJob();
        executedJob.setId("job_001");
        executedJob.setSuccessCount(2L);
        when(orchestrationService.executeCreateJob("job_001")).thenReturn(executedJob);

        DataPrepareJob result = service.prepareAndExecute(request);

        assertSame(executedJob, result);
        ArgumentCaptor<DataPrepareJob> jobCaptor = ArgumentCaptor.forClass(DataPrepareJob.class);
        verify(dataPrepareJobService).createDataPrepareJob(jobCaptor.capture());
        DataPrepareJob createdJob = jobCaptor.getValue();
        assertEquals("tenant_001", createdJob.getTenantId());
        assertEquals("connector_001", createdJob.getConnectorSystemId());
        assertEquals("task_001", createdJob.getTaskId());
        assertEquals("record_apply", createdJob.getModuleCode());
        assertEquals("PRACTICE", createdJob.getSceneType());
        assertEquals("CREATE", createdJob.getJobType());
        assertEquals("batch_001", createdJob.getRequestBatchId());
        assertEquals(2L, createdJob.getExpectedCount());
        assertEquals("prepare:requirement_001:batch_001:ON_DEMAND", createdJob.getIdempotencyKey());
        assertEquals(TriggerType.ON_DEMAND.getValue(), createdJob.getTriggerType());
        verify(orchestrationService).executeCreateJob("job_001");
    }

    /**
     * 验证显式传入任务 ID、任务类型和幂等键时不会被默认规则覆盖。
     */
    @Test
    void prepareAndExecuteShouldKeepExplicitJobFields() {
        DataPrepareFacadeService.PrepareAndExecuteRequest request = buildRequest();
        request.setJobId("job_custom");
        request.setJobType("RETRY_CREATE");
        request.setIdempotencyKey("custom:key");
        when(requirementGenerationService.generateRequirementItems(request.getGenerateRequest()))
                .thenReturn(new DataRequirementGenerationService.GenerateResult(
                        buildRequirement(), Arrays.asList(new DataRequirementItem())));
        DataPrepareJob savedJob = new DataPrepareJob();
        savedJob.setId("job_custom");
        when(dataPrepareJobService.getByIdempotencyKey("tenant_001", "custom:key")).thenReturn(null);
        when(dataPrepareJobService.createDataPrepareJob(any(DataPrepareJob.class)))
                .thenReturn(savedJob);
        when(orchestrationService.executeCreateJob("job_custom")).thenReturn(savedJob);

        service.prepareAndExecute(request);

        ArgumentCaptor<DataPrepareJob> jobCaptor = ArgumentCaptor.forClass(DataPrepareJob.class);
        verify(dataPrepareJobService).createDataPrepareJob(jobCaptor.capture());
        assertEquals("job_custom", jobCaptor.getValue().getId());
        assertEquals("RETRY_CREATE", jobCaptor.getValue().getJobType());
        assertEquals("custom:key", jobCaptor.getValue().getIdempotencyKey());
    }

    /**
     * 验证同一次 attempt 批次幂等命中成功任务时，不会重复创建任务或调用原平台。
     */
    @Test
    void prepareAndExecuteShouldReturnExistingTerminalJobWhenIdempotencyHit() {
        DataPrepareFacadeService.PrepareAndExecuteRequest request = buildRequest();
        DataPrepareJob existingJob = new DataPrepareJob();
        existingJob.setId("job_existing");
        existingJob.setJobStatus(PrepareJobStatus.SUCCESS.getValue());
        when(requirementGenerationService.generateRequirementItems(request.getGenerateRequest()))
                .thenReturn(new DataRequirementGenerationService.GenerateResult(
                        buildRequirement(), Arrays.asList(new DataRequirementItem())));
        when(dataPrepareJobService.getByIdempotencyKey(
                "tenant_001", "prepare:requirement_001:batch_001:ON_DEMAND")).thenReturn(existingJob);

        DataPrepareJob result = service.prepareAndExecute(request);

        assertSame(existingJob, result);
        verify(dataPrepareJobService, times(0))
                .createDataPrepareJob(any(DataPrepareJob.class));
        verify(orchestrationService, times(0)).executeCreateJob(anyString());
    }

    /**
     * 验证同一次 attempt 批次幂等命中已创建未执行任务时，复用原任务继续执行。
     */
    @Test
    void prepareAndExecuteShouldExecuteExistingCreatedJobWhenIdempotencyHit() {
        DataPrepareFacadeService.PrepareAndExecuteRequest request = buildRequest();
        DataPrepareJob existingJob = new DataPrepareJob();
        existingJob.setId("job_existing");
        existingJob.setJobStatus(PrepareJobStatus.CREATED.getValue());
        DataPrepareJob executedJob = new DataPrepareJob();
        executedJob.setId("job_existing");
        executedJob.setJobStatus(PrepareJobStatus.SUCCESS.getValue());
        when(requirementGenerationService.generateRequirementItems(request.getGenerateRequest()))
                .thenReturn(new DataRequirementGenerationService.GenerateResult(
                        buildRequirement(), Arrays.asList(new DataRequirementItem())));
        when(dataPrepareJobService.getByIdempotencyKey(
                "tenant_001", "prepare:requirement_001:batch_001:ON_DEMAND")).thenReturn(existingJob);
        when(orchestrationService.executeCreateJob("job_existing")).thenReturn(executedJob);

        DataPrepareJob result = service.prepareAndExecute(request);

        assertSame(executedJob, result);
        verify(dataPrepareJobService, times(0))
                .createDataPrepareJob(any(DataPrepareJob.class));
        verify(orchestrationService).executeCreateJob("job_existing");
    }

    /**
     * 验证非法触发方式会被拒绝，避免写入不可识别的任务来源。
     */
    @Test
    void prepareAndExecuteShouldRejectInvalidTriggerType() {
        DataPrepareFacadeService.PrepareAndExecuteRequest request = buildRequest();
        request.setTriggerType("BAD_TRIGGER");

        assertThrows(BusinessException.class, () -> service.prepareAndExecute(request));
    }

    /**
     * 构造数据准备触发请求。
     *
     * @return 数据准备触发请求。
     */
    private DataPrepareFacadeService.PrepareAndExecuteRequest buildRequest() {
        DataRequirementGenerationService.GenerateRequest generateRequest =
                new DataRequirementGenerationService.GenerateRequest();
        generateRequest.setRequirementId("requirement_001");
        generateRequest.setRequestBatchId("batch_001");
        generateRequest.setCreateBy("teacher_001");
        generateRequest.setUpdateBy("teacher_001");
        DataPrepareFacadeService.PrepareAndExecuteRequest request =
                new DataPrepareFacadeService.PrepareAndExecuteRequest();
        request.setTriggerType(TriggerType.ON_DEMAND.getValue());
        request.setGenerateRequest(generateRequest);
        return request;
    }

    /**
     * 构造数据需求批次。
     *
     * @return 数据需求批次。
     */
    private DataRequirement buildRequirement() {
        DataRequirement requirement = new DataRequirement();
        requirement.setId("requirement_001");
        requirement.setTenantId("tenant_001");
        requirement.setConnectorSystemId("connector_001");
        requirement.setTaskId("task_001");
        requirement.setModuleCode("record_apply");
        requirement.setSceneType("PRACTICE");
        return requirement;
    }
}
