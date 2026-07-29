package com.sxpt.module.teachingdata;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.mapper.TeachingDataInstanceMapper;
import com.sxpt.module.connector.service.OriginDataPrepareAdapter;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataInstanceStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.PrepareJobStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RequirementItemStatus;
import com.sxpt.module.teachingdata.mapper.DataPrepareJobMapper;
import com.sxpt.module.teachingdata.mapper.DataRequirementItemMapper;
import com.sxpt.module.teachingdata.mapper.DataRequirementMapper;
import com.sxpt.module.teachingdata.mapper.TeachingDataPoolMapper;
import com.sxpt.module.teachingdata.service.DataPrepareOrchestrationService;
import com.sxpt.module.teachingdata.service.impl.DataPrepareOrchestrationServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据准备编排服务测试。
 *
 * 业务功能：
 * 1. 验证创建型数据准备任务能调用原平台 Adapter，并按 requestItemId 回填结果。
 * 2. 验证成功结果会生成教学平台侧的数据实例引用，失败结果不会落实例。
 *
 * 关键流程：
 * 1. 使用 Mockito 隔离数据库和原平台调用。
 * 2. 覆盖全成功、部分失败和空明细三个关键边界。
 */
class DataPrepareOrchestrationServiceImplTests {

    private final DataPrepareJobMapper dataPrepareJobMapper = mock(DataPrepareJobMapper.class);

    private final DataRequirementItemMapper dataRequirementItemMapper = mock(DataRequirementItemMapper.class);

    private final DataRequirementMapper dataRequirementMapper = mock(DataRequirementMapper.class);

    private final TeachingDataPoolMapper teachingDataPoolMapper = mock(TeachingDataPoolMapper.class);

    private final TeachingDataInstanceMapper teachingDataInstanceMapper = mock(TeachingDataInstanceMapper.class);

    private final OriginDataPrepareAdapter originDataPrepareAdapter = mock(OriginDataPrepareAdapter.class);

    private final DataPrepareOrchestrationService service = new DataPrepareOrchestrationServiceImpl(
            dataPrepareJobMapper,
            dataRequirementItemMapper,
            dataRequirementMapper,
            teachingDataPoolMapper,
            teachingDataInstanceMapper,
            originDataPrepareAdapter);

    /**
     * 验证所有需求项成功时，任务进入 SUCCESS，明细 READY，并创建实例引用。
     */
    @Test
    void executeCreateJobShouldCreateInstancesWhenAllItemsSuccess() {
        DataPrepareJob job = buildJob();
        DataRequirementItem first = buildItem("item_001", "student_001");
        DataRequirementItem second = buildItem("item_002", "student_002");
        when(dataPrepareJobMapper.selectById("job_001")).thenReturn(job);
        when(dataRequirementItemMapper.selectList(any())).thenReturn(Arrays.asList(first, second));
        mockInsertedInstanceValidation(first, true, null);
        when(originDataPrepareAdapter.createTeachingData(any())).thenReturn(buildResponse(
                buildSuccessResponseItem("item_001", "biz_001"),
                buildSuccessResponseItem("item_002", "biz_002")));

        DataPrepareJob result = service.executeCreateJob("job_001");

        assertEquals(PrepareJobStatus.SUCCESS.getValue(), result.getJobStatus());
        assertEquals(2L, result.getSuccessCount());
        assertEquals(0L, result.getFailedCount());
        assertEquals(RequirementItemStatus.READY.getValue(), first.getItemStatus());
        assertEquals("biz_001", first.getExternalBusinessId());
        verify(teachingDataInstanceMapper, times(2)).insert(any(TeachingDataInstance.class));
        verify(dataPrepareJobMapper, times(2)).updateById(job);
    }

    /**
     * 验证部分需求项失败时，任务进入 PARTIAL_FAILED，且只为成功项创建实例。
     */
    @Test
    void executeCreateJobShouldMarkPartialFailedWhenSomeItemsFailed() {
        DataPrepareJob job = buildJob();
        DataRequirementItem first = buildItem("item_001", "student_001");
        DataRequirementItem second = buildItem("item_002", "student_002");
        when(dataPrepareJobMapper.selectById("job_001")).thenReturn(job);
        when(dataRequirementItemMapper.selectList(any())).thenReturn(Arrays.asList(first, second));
        mockInsertedInstanceValidation(first, true, null);
        when(originDataPrepareAdapter.createTeachingData(any())).thenReturn(buildResponse(
                buildSuccessResponseItem("item_001", "biz_001"),
                buildFailedResponseItem("item_002", "单位角色不满足")));

        DataPrepareJob result = service.executeCreateJob("job_001");

        assertEquals(PrepareJobStatus.PARTIAL_FAILED.getValue(), result.getJobStatus());
        assertEquals(1L, result.getSuccessCount());
        assertEquals(1L, result.getFailedCount());
        assertEquals(RequirementItemStatus.FAILED.getValue(), second.getItemStatus());
        assertEquals("单位角色不满足", second.getFailureReason());
        verify(teachingDataInstanceMapper, times(1)).insert(any(TeachingDataInstance.class));
    }

    /**
     * 验证空批次不调用原平台，避免无意义的外部请求。
     */
    @Test
    void executeCreateJobShouldRejectEmptyRequirementItems() {
        DataPrepareJob job = buildJob();
        when(dataPrepareJobMapper.selectById("job_001")).thenReturn(job);
        when(dataRequirementItemMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> service.executeCreateJob("job_001"));
        verify(originDataPrepareAdapter, times(0)).createTeachingData(any());
    }

    /**
     * 验证原平台校验通过时，实例和需求明细都记录 PASSED。
     */
    @Test
    void validatePreparedInstanceShouldMarkPassedWhenOriginValidationPassed() {
        TeachingDataInstance instance = buildInstance();
        DataRequirementItem item = buildItem("item_001", "student_001");
        when(teachingDataInstanceMapper.selectById("instance_001")).thenReturn(instance);
        when(dataRequirementItemMapper.selectById("req_item_001")).thenReturn(item);
        when(originDataPrepareAdapter.validateTeachingData(any())).thenReturn(buildValidationResponse(true, null));

        TeachingDataInstance result = service.validatePreparedInstance("instance_001");

        assertEquals("PASSED", result.getValidationStatus());
        assertEquals(DataInstanceStatus.READY.getValue(), result.getInstanceStatus());
        assertEquals("PASSED", item.getValidationStatus());
        assertEquals(RequirementItemStatus.READY.getValue(), item.getItemStatus());
        verify(teachingDataInstanceMapper).updateById(instance);
        verify(dataRequirementItemMapper).updateById(item);
    }

    /**
     * 验证原平台校验失败时，实例不可用于后续 launchToken。
     */
    @Test
    void validatePreparedInstanceShouldMarkFailedWhenOriginValidationFailed() {
        TeachingDataInstance instance = buildInstance();
        DataRequirementItem item = buildItem("item_001", "student_001");
        when(teachingDataInstanceMapper.selectById("instance_001")).thenReturn(instance);
        when(dataRequirementItemMapper.selectById("req_item_001")).thenReturn(item);
        when(originDataPrepareAdapter.validateTeachingData(any())).thenReturn(
                buildValidationResponse(false, "角色无权限"));

        TeachingDataInstance result = service.validatePreparedInstance("instance_001");

        assertEquals("FAILED", result.getValidationStatus());
        assertEquals(DataInstanceStatus.FAILED.getValue(), result.getInstanceStatus());
        assertEquals("角色无权限", result.getFailureReason());
        assertEquals("FAILED", item.getValidationStatus());
        assertEquals(RequirementItemStatus.VALIDATION_FAILED.getValue(), item.getItemStatus());
        verify(teachingDataInstanceMapper).updateById(instance);
        verify(dataRequirementItemMapper).updateById(item);
    }

    /**
     * 验证实例不存在时不调用原平台，避免产生无法对账的校验请求。
     */
    @Test
    void validatePreparedInstanceShouldRejectMissingInstance() {
        when(teachingDataInstanceMapper.selectById("instance_001")).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.validatePreparedInstance("instance_001"));
        verify(originDataPrepareAdapter, times(0)).validateTeachingData(any());
    }

    /**
     * 构造最小有效数据准备任务。
     *
     * @return 数据准备任务。
     */
    private DataPrepareJob buildJob() {
        DataPrepareJob job = new DataPrepareJob();
        job.setId("job_001");
        job.setTenantId("tenant_001");
        job.setConnectorSystemId("connector_001");
        job.setModuleCode("record_apply");
        job.setSceneType("PRACTICE");
        job.setRequestBatchId("batch_001");
        job.setIdempotencyKey("idem_001");
        job.setCreateBy("teacher_001");
        job.setUpdateBy("teacher_001");
        job.setDeleted(Boolean.FALSE);
        return job;
    }

    /**
     * 构造最小有效数据需求明细。
     *
     * @param requestItemId 逐条请求 ID。
     * @param studentId 学生 ID。
     * @return 数据需求明细。
     */
    private DataRequirementItem buildItem(String requestItemId, String studentId) {
        DataRequirementItem item = new DataRequirementItem();
        item.setId("req_" + requestItemId);
        item.setTenantId("tenant_001");
        item.setRequirementId("requirement_001");
        item.setRequestBatchId("batch_001");
        item.setRequestItemId(requestItemId);
        item.setConnectorSystemId("connector_001");
        item.setModuleCode("record_apply");
        item.setTemplateId("tpl_001");
        item.setSceneType("PRACTICE");
        item.setTaskId("task_001");
        item.setStudentId(studentId);
        item.setActorType("student");
        item.setOwnerExternalOrgId("org_owner");
        item.setRequiredExternalOrgId("org_required");
        item.setRequiredExternalRoleId("role_required");
        item.setDataScopeJson("{\"scope\":\"demo\"}");
        item.setRequiredActionsJson("[\"submit\"]");
        return item;
    }

    /**
     * 构造最小有效教学数据实例。
     *
     * @return 教学数据实例。
     */
    private TeachingDataInstance buildInstance() {
        TeachingDataInstance instance = new TeachingDataInstance();
        instance.setId("instance_001");
        instance.setTenantId("tenant_001");
        instance.setConnectorSystemId("connector_001");
        instance.setRequirementItemId("req_item_001");
        instance.setExternalBusinessId("biz_001");
        instance.setRequiredExternalOrgId("org_required");
        instance.setRequiredExternalRoleId("role_required");
        instance.setInstanceStatus(DataInstanceStatus.READY.getValue());
        instance.setDeleted(Boolean.FALSE);
        return instance;
    }

    /**
     * 构造原平台批量创建响应。
     *
     * @param items 逐条响应。
     * @return 批量响应。
     */
    private OriginDataPrepareAdapter.BatchCreateResponse buildResponse(
            OriginDataPrepareAdapter.ResponseItem... items) {
        OriginDataPrepareAdapter.BatchCreateResponse response = new OriginDataPrepareAdapter.BatchCreateResponse();
        response.setExternalRequestId("origin_req_001");
        response.setRequestBatchId("batch_001");
        response.setAdapterStatus("SUCCESS");
        response.setResultJson("{\"ok\":true}");
        response.setItems(Arrays.asList(items));
        return response;
    }

    /**
     * 构造成功的原平台逐条响应。
     *
     * @param requestItemId 逐条请求 ID。
     * @param externalBusinessId 原平台业务数据 ID。
     * @return 逐条响应。
     */
    private OriginDataPrepareAdapter.ResponseItem buildSuccessResponseItem(String requestItemId,
                                                                           String externalBusinessId) {
        OriginDataPrepareAdapter.ResponseItem item = new OriginDataPrepareAdapter.ResponseItem();
        item.setRequestItemId(requestItemId);
        item.setExternalBusinessId(externalBusinessId);
        item.setExternalBusinessNo("NO-" + externalBusinessId);
        item.setExternalStatus("DRAFT");
        item.setTargetUrl("/origin/" + externalBusinessId);
        item.setItemStatus("SUCCESS");
        return item;
    }

    /**
     * 构造失败的原平台逐条响应。
     *
     * @param requestItemId 逐条请求 ID。
     * @param errorMessage 错误消息。
     * @return 逐条响应。
     */
    /**
     * 补齐创建成功后的自动校验链路。
     * <p>
     * 编排服务在插入实例后会按实例 ID 查回数据并调用原平台校验，因此测试需要保存刚插入的实例，
     * 让后续 selectById 能返回同一条对象，才能验证创建、校验、入池是一条连续链路。
     *
     * @param validationItem 校验时回写的需求明细。
     * @param passed 原平台校验是否通过。
     * @param errorMessage 原平台校验错误消息。
     */
    private void mockInsertedInstanceValidation(DataRequirementItem validationItem,
                                                boolean passed,
                                                String errorMessage) {
        List<TeachingDataInstance> insertedInstances = new ArrayList<>();
        when(teachingDataInstanceMapper.insert(any(TeachingDataInstance.class))).thenAnswer(invocation -> {
            insertedInstances.add(invocation.getArgument(0));
            return 1;
        });
        when(teachingDataInstanceMapper.selectById(any())).thenAnswer(invocation ->
                insertedInstances.isEmpty() ? null : insertedInstances.get(insertedInstances.size() - 1));
        when(dataRequirementItemMapper.selectById(any())).thenReturn(validationItem);
        when(originDataPrepareAdapter.validateTeachingData(any()))
                .thenReturn(buildValidationResponse(passed, errorMessage));
    }

    private OriginDataPrepareAdapter.ResponseItem buildFailedResponseItem(String requestItemId, String errorMessage) {
        OriginDataPrepareAdapter.ResponseItem item = new OriginDataPrepareAdapter.ResponseItem();
        item.setRequestItemId(requestItemId);
        item.setItemStatus("FAILED");
        item.setErrorMessage(errorMessage);
        return item;
    }

    /**
     * 构造原平台校验响应。
     *
     * @param passed 是否通过。
     * @param errorMessage 错误消息。
     * @return 校验响应。
     */
    private OriginDataPrepareAdapter.ValidationResponse buildValidationResponse(boolean passed, String errorMessage) {
        OriginDataPrepareAdapter.ValidationResponse response = new OriginDataPrepareAdapter.ValidationResponse();
        response.setPassed(passed);
        response.setValidationStatus(passed ? "PASSED" : "FAILED");
        response.setValidationResultJson("{\"passed\":" + passed + "}");
        response.setErrorMessage(errorMessage);
        return response;
    }
}
