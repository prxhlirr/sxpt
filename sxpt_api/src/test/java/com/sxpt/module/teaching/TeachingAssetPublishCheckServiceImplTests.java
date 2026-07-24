package com.sxpt.module.teaching;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.service.EvaluationConfigService;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.service.TaskStepService;
import com.sxpt.module.teaching.service.TeachingAssetPublishCheckService;
import com.sxpt.module.teaching.service.impl.TeachingAssetPublishCheckServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 教学资产发布前完整性校验服务测试。
 *
 * 业务功能：
 * 1. 验证教学资产发布前必须具备教学步骤和关联评分项。
 * 2. 验证不完整备案资产会在发布前被拦截，避免学生进入无法执行或无法评分的任务。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代教学步骤和评分配置服务。
 * 2. 直接调用发布校验服务，聚焦发布闸门规则本身。
 */
class TeachingAssetPublishCheckServiceImplTests {

    private final TaskStepService taskStepService = mock(TaskStepService.class);

    private final EvaluationConfigService evaluationConfigService = mock(EvaluationConfigService.class);

    private final TeachingAssetPublishCheckService checkService =
            new TeachingAssetPublishCheckServiceImpl(taskStepService, evaluationConfigService);

    /**
     * 校验教学步骤和关联评分项都存在时允许发布。
     */
    @Test
    void validateReadyToPublishShouldPassWhenStepAndEvaluationItemExist() {
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildTaskStep("step_001")));
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildEvaluationItem("tp_001", "step_001")));

        assertDoesNotThrow(() -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001"));
    }

    /**
     * 校验缺少教学步骤时拒绝发布，避免学生 runtime 无步骤可执行。
     */
    @Test
    void validateReadyToPublishShouldRejectMissingTaskStep() {
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001"));
    }

    /**
     * 校验必做步骤缺少顺序号时拒绝发布，避免学生 runtime 无法稳定排序。
     */
    @Test
    void validateReadyToPublishShouldRejectRequiredTaskStepWithoutSequenceNo() {
        TaskStep taskStep = buildTaskStep("step_001");
        taskStep.setSequenceNo(null);
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(taskStep));

        assertThrows(BusinessException.class, () -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001"));
    }

    /**
     * 校验必做步骤缺少名称时拒绝发布，避免学生端展示不可理解的步骤。
     */
    @Test
    void validateReadyToPublishShouldRejectRequiredTaskStepWithoutStepName() {
        TaskStep taskStep = buildTaskStep("step_001");
        taskStep.setStepName(" ");
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(taskStep));

        assertThrows(BusinessException.class, () -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001"));
    }

    /**
     * 校验可选步骤允许暂缺执行字段，避免非强制扩展步骤阻塞 MVP 发布。
     */
    @Test
    void validateReadyToPublishShouldAllowOptionalTaskStepWithoutSequenceNo() {
        TaskStep taskStep = buildTaskStep("step_001");
        taskStep.setRequired(Boolean.FALSE);
        taskStep.setSequenceNo(null);
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(taskStep));
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildEvaluationItem("tp_001", "step_001")));

        assertDoesNotThrow(() -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001"));
    }

    /**
     * 校验身份切换字段完整时允许发布，保证学生端可以生成原平台启动上下文。
     */
    @Test
    void validateReadyToPublishShouldAllowCompleteIdentitySwitchStep() {
        TaskStep taskStep = buildTaskStep("step_001");
        taskStep.setActorType("AUDITOR");
        taskStep.setRequiredExternalOrgId("org_ext_001");
        taskStep.setRequiredExternalRoleId("role_ext_001");
        taskStep.setSwitchStrategy("LAUNCH_TOKEN");
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(taskStep));
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildEvaluationItem("tp_001", "step_001")));

        assertDoesNotThrow(() -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001"));
    }

    /**
     * 校验声明身份切换但缺少外部角色时拒绝发布，避免运行态无法生成 launchToken。
     */
    @Test
    void validateReadyToPublishShouldRejectIncompleteIdentitySwitchStep() {
        TaskStep taskStep = buildTaskStep("step_001");
        taskStep.setSwitchStrategy("LAUNCH_TOKEN");
        taskStep.setActorType("AUDITOR");
        taskStep.setRequiredExternalOrgId("org_ext_001");
        taskStep.setRequiredExternalRoleId(" ");
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(taskStep));

        assertThrows(BusinessException.class, () -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001"));
    }

    /**
     * 校验空资源数组被拒绝发布，避免步骤表达资源引用但 SDK 侧拿不到有效资源。
     */
    @Test
    void validateReadyToPublishShouldRejectEmptyResourceReference() {
        TaskStep taskStep = buildTaskStep("step_001");
        taskStep.setRelatedResourceIds("[]");
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(taskStep));

        assertThrows(BusinessException.class, () -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001"));
    }

    /**
     * 校验缺少评分项时拒绝发布，避免学生提交后无法自动评分。
     */
    @Test
    void validateReadyToPublishShouldRejectMissingEvaluationItem() {
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildTaskStep("step_001")));
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001"));
    }

    /**
     * 校验评分项缺少分值时拒绝发布，避免自动评分命中后仍只能得到 0 分。
     */
    @Test
    void validateReadyToPublishShouldRejectEvaluationItemWithoutScore() {
        EvaluationItem item = buildEvaluationItem("tp_001", "step_001");
        item.setScore(null);
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildTaskStep("step_001")));
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(item));

        assertThrows(BusinessException.class, () -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001"));
    }

    /**
     * 校验评分项缺少断言配置时拒绝发布，避免评分器无法判断轨迹类型。
     */
    @Test
    void validateReadyToPublishShouldRejectEvaluationItemWithoutAssertionConfig() {
        EvaluationItem item = buildEvaluationItem("tp_001", "step_001");
        item.setAssertionConfigJson("{}");
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildTaskStep("step_001")));
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(item));

        assertThrows(BusinessException.class, () -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001"));
    }

    /**
     * 校验非 MVP 支持断言类型被拒绝发布，避免发布后自动评分只能记录未支持证据。
     */
    @Test
    void validateReadyToPublishShouldRejectUnsupportedAssertionType() {
        EvaluationItem item = buildEvaluationItem("tp_001", "step_001");
        item.setAssertionType("RESULT_EXISTS");
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildTaskStep("step_001")));
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(item));

        assertThrows(BusinessException.class, () -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001"));
    }

    /**
     * 校验评分项没有关联当前教学点步骤时拒绝发布，避免错误规则被误认为完整资产。
     */
    @Test
    void validateReadyToPublishShouldRejectEvaluationItemForOtherTaskStep() {
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildTaskStep("step_001")));
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildEvaluationItem("tp_001", "step_999")));

        assertThrows(BusinessException.class, () -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001"));
    }

    /**
     * 校验缺少归属参数时拒绝发布校验，避免无范围查询造成误判。
     */
    @Test
    void validateReadyToPublishShouldRejectMissingScope() {
        assertThrows(BusinessException.class, () -> checkService.validateReadyToPublish(
                "tenant_001", " ", "tp_001", "rule_001"));
        assertThrows(BusinessException.class, () -> checkService.validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", " "));
    }

    /**
     * 构造教学步骤。
     *
     * @param id 教学步骤 ID。
     * @return 教学步骤实体。
     */
    private TaskStep buildTaskStep(String id) {
        TaskStep taskStep = new TaskStep();
        taskStep.setId(id);
        taskStep.setTenantId("tenant_001");
        taskStep.setTaskId("task_001");
        taskStep.setTeachingPointId("tp_001");
        taskStep.setStepName("填写申请信息");
        taskStep.setSequenceNo(1L);
        taskStep.setRequired(Boolean.TRUE);
        return taskStep;
    }

    /**
     * 构造评分项。
     *
     * @param teachingPointId 教学点 ID。
     * @param relatedTaskStepId 关联教学步骤 ID。
     * @return 评分项实体。
     */
    private EvaluationItem buildEvaluationItem(String teachingPointId, String relatedTaskStepId) {
        EvaluationItem item = new EvaluationItem();
        item.setId("item_001");
        item.setTenantId("tenant_001");
        item.setEvaluationRuleId("rule_001");
        item.setTeachingPointId(teachingPointId);
        item.setItemCode("ITEM_STEP_001");
        item.setRelatedTaskStepId(relatedTaskStepId);
        item.setScore(new BigDecimal("10.00"));
        item.setAssertionType("TRACE_EXISTS");
        item.setAssertionConfigJson("{\"traceType\":\"CLICK\"}");
        return item;
    }
}
