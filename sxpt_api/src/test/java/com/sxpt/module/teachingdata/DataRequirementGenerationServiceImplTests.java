package com.sxpt.module.teachingdata;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.ModuleDataStrategy;
import com.sxpt.module.connector.service.ModuleDataStrategyService;
import com.sxpt.module.teachingdata.entity.DataRequirement;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RequirementItemStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.ValidationStatus;
import com.sxpt.module.teachingdata.mapper.DataRequirementItemMapper;
import com.sxpt.module.teachingdata.mapper.DataRequirementMapper;
import com.sxpt.module.teachingdata.service.DataRequirementGenerationService;
import com.sxpt.module.teachingdata.service.impl.DataRequirementGenerationServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据需求明细生成服务测试。
 *
 * 业务功能：
 * 1. 验证生成服务能把批次展开为逐条数据需求明细。
 * 2. 验证学生、单位、角色和评分点快照会被固化到明细中。
 *
 * 关键流程：
 * 1. 使用 Mockito 隔离 Mapper。
 * 2. 覆盖成功生成、缺少参与者和缺少角色三个关键边界。
 */
class DataRequirementGenerationServiceImplTests {

    private final DataRequirementMapper dataRequirementMapper = mock(DataRequirementMapper.class);

    private final DataRequirementItemMapper dataRequirementItemMapper = mock(DataRequirementItemMapper.class);

    private final ModuleDataStrategyService moduleDataStrategyService = mock(ModuleDataStrategyService.class);

    private final DataRequirementGenerationService service = new DataRequirementGenerationServiceImpl(
            dataRequirementMapper, dataRequirementItemMapper, moduleDataStrategyService);

    /**
     * 验证参与者约束会被展开为数据需求明细，并回写批次预期数量。
     */
    @Test
    void generateRequirementItemsShouldCreateItemsAndUpdateExpectedCount() {
        DataRequirement requirement = buildRequirement();
        when(dataRequirementMapper.selectById("requirement_001")).thenReturn(requirement);
        when(dataRequirementItemMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(moduleDataStrategyService.getModuleDataStrategyById("strategy_001")).thenReturn(buildStrategy());
        DataRequirementGenerationService.GenerateRequest request = buildGenerateRequest(
                buildParticipant("student_001", "question_001"),
                buildParticipant("student_002", "question_002"));

        DataRequirementGenerationService.GenerateResult result = service.generateRequirementItems(request);

        assertEquals(requirement, result.getRequirement());
        assertEquals(2, result.getItems().size());
        assertEquals(2L, requirement.getExpectedCount());
        verify(dataRequirementItemMapper, times(2)).insert(any(DataRequirementItem.class));
        verify(dataRequirementMapper).updateById(requirement);

        ArgumentCaptor<DataRequirementItem> captor = ArgumentCaptor.forClass(DataRequirementItem.class);
        verify(dataRequirementItemMapper, times(2)).insert(captor.capture());
        DataRequirementItem first = captor.getAllValues().get(0);
        assertEquals("tenant_001", first.getTenantId());
        assertEquals("requirement_001", first.getRequirementId());
        assertTrue(first.getRequestItemId().startsWith("batch_001_"));
        assertEquals("student_001", first.getStudentId());
        assertEquals("question_001", first.getQuestionId());
        assertEquals("org_owner", first.getOwnerExternalOrgId());
        assertEquals("org_required", first.getRequiredExternalOrgId());
        assertEquals("role_required", first.getRequiredExternalRoleId());
        assertEquals("{\"score\":10}", first.getScorePointSnapshotJson());
        assertEquals(RequirementItemStatus.CREATED.getValue(), first.getItemStatus());
        assertEquals(ValidationStatus.NOT_CHECKED.getValue(), first.getValidationStatus());
        assertNotNull(first.getCreateTime());
    }

    /**
     * 发布任务使用较长批次号时，请求明细编码仍须满足数据库 varchar(64) 约束。
     */
    @Test
    void generateRequirementItemsShouldLimitRequestItemIdToDatabaseLength() {
        DataRequirement requirement = buildRequirement();
        when(dataRequirementMapper.selectById("requirement_001")).thenReturn(requirement);
        when(dataRequirementItemMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(moduleDataStrategyService.getModuleDataStrategyById("strategy_001")).thenReturn(buildStrategy());
        DataRequirementGenerationService.GenerateRequest request = buildGenerateRequest(
                buildParticipant("student_001", "question_001"));
        request.setRequestBatchId("publish-bc373c54f439447bb67094bf1c346965-1786070000000");

        service.generateRequirementItems(request);

        ArgumentCaptor<DataRequirementItem> captor = ArgumentCaptor.forClass(DataRequirementItem.class);
        verify(dataRequirementItemMapper).insert(captor.capture());
        assertTrue(captor.getValue().getRequestItemId().length() <= 64);
    }

    /**
     * 验证同一 requestBatchId 已经生成过明细时直接返回既有明细，避免同一次 attempt 重试重复造数。
     */
    @Test
    void generateRequirementItemsShouldReturnExistingItemsWhenBatchAlreadyGenerated() {
        DataRequirement requirement = buildRequirement();
        DataRequirementItem existingItem = new DataRequirementItem();
        existingItem.setId("item_existing");
        when(dataRequirementMapper.selectById("requirement_001")).thenReturn(requirement);
        when(dataRequirementItemMapper.selectList(any())).thenReturn(Collections.singletonList(existingItem));
        DataRequirementGenerationService.GenerateRequest request = buildGenerateRequest(
                buildParticipant("student_001", "question_001"));

        DataRequirementGenerationService.GenerateResult result = service.generateRequirementItems(request);

        assertEquals(1, result.getItems().size());
        assertSame(existingItem, result.getItems().get(0));
        verify(dataRequirementItemMapper, times(0)).insert(any(DataRequirementItem.class));
        verify(dataRequirementMapper, times(0)).updateById(any(DataRequirement.class));
    }

    /**
     * 验证空参与者列表会被拒绝，避免生成无意义批次。
     */
    @Test
    void generateRequirementItemsShouldRejectEmptyParticipants() {
        DataRequirementGenerationService.GenerateRequest request = buildGenerateRequest();
        request.setParticipants(Collections.emptyList());

        assertThrows(BusinessException.class, () -> service.generateRequirementItems(request));
        verify(dataRequirementItemMapper, times(0)).insert(any(DataRequirementItem.class));
    }

    /**
     * 验证缺少原平台角色会被拒绝，避免生成无法操作的数据。
     */
    @Test
    void generateRequirementItemsShouldRejectMissingRole() {
        when(dataRequirementMapper.selectById("requirement_001")).thenReturn(buildRequirement());
        when(dataRequirementItemMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(moduleDataStrategyService.getModuleDataStrategyById("strategy_001")).thenReturn(buildStrategy());
        DataRequirementGenerationService.ParticipantRequirement participant = buildParticipant("student_001", "question_001");
        participant.setRequiredExternalRoleId(" ");
        DataRequirementGenerationService.GenerateRequest request = buildGenerateRequest(participant);

        assertThrows(BusinessException.class, () -> service.generateRequirementItems(request));
        verify(dataRequirementItemMapper, times(0)).insert(any(DataRequirementItem.class));
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
        requirement.setBusinessModuleId("module_001");
        requirement.setModuleCode("record_apply");
        requirement.setStrategyId("strategy_001");
        requirement.setTemplateId("tpl_001");
        requirement.setTaskId("task_001");
        requirement.setSceneType("PRACTICE");
        requirement.setDeleted(Boolean.FALSE);
        return requirement;
    }

    /**
     * 构造与需求批次一致的启用策略，保证生成明细时使用固定的策略快照。
     *
     * @return 数据准备策略。
     */
    private ModuleDataStrategy buildStrategy() {
        ModuleDataStrategy strategy = new ModuleDataStrategy();
        strategy.setId("strategy_001");
        strategy.setTenantId("tenant_001");
        strategy.setConnectorSystemId("connector_001");
        strategy.setBusinessModuleId("module_001");
        strategy.setModuleCode("record_apply");
        strategy.setSceneType("PRACTICE");
        strategy.setInitExternalStatus("DRAFT");
        strategy.setTargetExternalStatus("READY");
        strategy.setValidationPolicyJson("{\"required\":true}");
        return strategy;
    }

    /**
     * 构造生成请求。
     *
     * @param participants 参与者约束。
     * @return 生成请求。
     */
    private DataRequirementGenerationService.GenerateRequest buildGenerateRequest(
            DataRequirementGenerationService.ParticipantRequirement... participants) {
        DataRequirementGenerationService.GenerateRequest request =
                new DataRequirementGenerationService.GenerateRequest();
        request.setRequirementId("requirement_001");
        request.setRequestBatchId("batch_001");
        request.setCreateBy("teacher_001");
        request.setUpdateBy("teacher_001");
        request.setParticipants(Arrays.asList(participants));
        return request;
    }

    /**
     * 构造参与者约束。
     *
     * @param studentId 学生 ID。
     * @param questionId 题目 ID。
     * @return 参与者约束。
     */
    private DataRequirementGenerationService.ParticipantRequirement buildParticipant(String studentId, String questionId) {
        DataRequirementGenerationService.ParticipantRequirement participant =
                new DataRequirementGenerationService.ParticipantRequirement();
        participant.setStudentId(studentId);
        participant.setQuestionId(questionId);
        participant.setActorType("student");
        participant.setOwnerExternalOrgId("org_owner");
        participant.setRequiredExternalOrgId("org_required");
        participant.setRequiredExternalRoleId("role_required");
        participant.setDataScopeJson("{\"scope\":\"demo\"}");
        participant.setRequiredActionsJson("[\"submit\"]");
        participant.setScorePointSnapshotJson("{\"score\":10}");
        return participant;
    }
}
