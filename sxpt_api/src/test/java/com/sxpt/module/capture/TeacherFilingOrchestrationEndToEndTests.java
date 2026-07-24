package com.sxpt.module.capture;

import com.sxpt.module.capture.entity.CaptureActionDraft;
import com.sxpt.module.capture.entity.CaptureEvent;
import com.sxpt.module.capture.mapper.CaptureActionDraftMapper;
import com.sxpt.module.capture.mapper.CaptureEventMapper;
import com.sxpt.module.capture.service.CaptureActionDraftGenerateService;
import com.sxpt.module.capture.service.CaptureActionDraftService;
import com.sxpt.module.capture.service.CaptureDraftPublishService;
import com.sxpt.module.capture.service.TeacherFilingOrchestrationService;
import com.sxpt.module.capture.service.impl.CaptureActionDraftGenerateServiceImpl;
import com.sxpt.module.capture.service.impl.CaptureActionDraftServiceImpl;
import com.sxpt.module.capture.service.impl.CaptureDraftPublishServiceImpl;
import com.sxpt.module.capture.service.impl.TeacherFilingOrchestrationServiceImpl;
import com.sxpt.module.capture.vo.TeacherFilingOrchestrationResultVO;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.entity.EvaluationRule;
import com.sxpt.module.evaluation.service.EvaluationConfigService;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.service.TaskStepService;
import com.sxpt.module.teaching.service.TeachingAssetPublishCheckService;
import com.sxpt.module.teaching.service.impl.TeachingAssetPublishCheckServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 老师备案服务级端到端测试。
 *
 * 业务功能：
 * 1. 验证采集事件可以经过草稿生成、老师确认、编排发布、评分项生成和发布前校验形成最小备案闭环。
 * 2. 使用真实 Service 实现串联业务流程，避免只用单点测试证明局部能力。
 *
 * 关键流程：
 * 1. 用内存列表模拟 Mapper 持久化，避免端到端测试依赖真实数据库。
 * 2. 先生成 PENDING 草稿，再调用确认服务模拟老师确认，最后由编排服务完成发布和校验。
 */
class TeacherFilingOrchestrationEndToEndTests {

    private final List<CaptureActionDraft> draftStore = new ArrayList<>();

    private final List<TaskStep> taskStepStore = new ArrayList<>();

    private final List<EvaluationItem> evaluationItemStore = new ArrayList<>();

    /**
     * 校验一次采集事件可以形成可发布的老师备案最小闭环。
     */
    @Test
    void teacherFilingShouldRunFromCapturedEventToReadyAsset() {
        CaptureEventMapper eventMapper = mock(CaptureEventMapper.class);
        CaptureActionDraftMapper draftMapper = buildDraftMapper();
        TaskStepService taskStepService = buildTaskStepService();
        EvaluationConfigService evaluationConfigService = buildEvaluationConfigService();
        CaptureActionDraftService actionDraftService = new CaptureActionDraftServiceImpl(draftMapper);
        CaptureActionDraftGenerateService generateService = new CaptureActionDraftGenerateServiceImpl(
                eventMapper, draftMapper, actionDraftService);
        CaptureDraftPublishService publishService = new CaptureDraftPublishServiceImpl(
                draftMapper, buildTaskStepMapper(), taskStepService, evaluationConfigService);
        TeachingAssetPublishCheckService publishCheckService = new TeachingAssetPublishCheckServiceImpl(
                taskStepService, evaluationConfigService);
        TeacherFilingOrchestrationService orchestrationService = new TeacherFilingOrchestrationServiceImpl(
                generateService, actionDraftService, publishService, publishCheckService);
        when(eventMapper.selectList(any())).thenReturn(Collections.singletonList(buildClickEvent()));

        List<CaptureActionDraft> generatedDrafts = generateService.generateDraftsFromSession(
                "tenant_001", "session_001", "teacher_001");
        CaptureActionDraft generatedDraft = generatedDrafts.get(0);
        actionDraftService.confirmActionDraft(generatedDraft.getId(), "保存备案申请", "点击保存按钮",
                "点击保存按钮提交备案申请。", "提交前检查必填项。", "res_001", "teacher_001");

        TeacherFilingOrchestrationResultVO result = orchestrationService.prepareTeachingAssets(
                "tenant_001", "session_001", "task_001", "tp_001", "rule_001", "teacher_001");

        assertEquals(Integer.valueOf(0), result.getGeneratedDraftCount());
        assertEquals(Integer.valueOf(1), result.getPublishedStepCount());
        assertEquals(Boolean.TRUE, result.getReadyToPublish());
        assertFalse(taskStepStore.isEmpty());
        assertFalse(evaluationItemStore.isEmpty());
        assertEquals("draft_001", taskStepStore.get(0).getSourceActionDraftId());
        assertEquals(taskStepStore.get(0).getId(), evaluationItemStore.get(0).getRelatedTaskStepId());
        assertEquals("TRACE_EXISTS", evaluationItemStore.get(0).getAssertionType());
    }

    /**
     * 构造内存化动作草稿 Mapper。
     *
     * @return 动作草稿 Mapper。
     */
    private CaptureActionDraftMapper buildDraftMapper() {
        CaptureActionDraftMapper draftMapper = mock(CaptureActionDraftMapper.class);
        when(draftMapper.insert(any(CaptureActionDraft.class))).thenAnswer(invocation -> {
            CaptureActionDraft draft = invocation.getArgument(0);
            draft.setId("draft_001");
            draftStore.add(draft);
            return 1;
        });
        when(draftMapper.selectOne(any())).thenAnswer(invocation -> draftStore.isEmpty() ? null : draftStore.get(0));
        when(draftMapper.selectById(any())).thenAnswer(invocation -> draftStore.isEmpty() ? null : draftStore.get(0));
        when(draftMapper.updateById(any(CaptureActionDraft.class))).thenAnswer(invocation -> 1);
        when(draftMapper.selectList(any())).thenAnswer(invocation -> new ArrayList<>(draftStore));
        return draftMapper;
    }

    /**
     * 构造内存化任务步骤 Mapper。
     *
     * @return 任务步骤 Mapper。
     */
    private com.sxpt.module.teaching.mapper.TaskStepMapper buildTaskStepMapper() {
        com.sxpt.module.teaching.mapper.TaskStepMapper taskStepMapper =
                mock(com.sxpt.module.teaching.mapper.TaskStepMapper.class);
        when(taskStepMapper.selectOne(any())).thenAnswer(invocation ->
                taskStepStore.isEmpty() ? null : taskStepStore.get(0));
        return taskStepMapper;
    }

    /**
     * 构造内存化任务步骤服务。
     *
     * @return 任务步骤服务。
     */
    private TaskStepService buildTaskStepService() {
        TaskStepService taskStepService = mock(TaskStepService.class);
        when(taskStepService.createTaskStep(any(TaskStep.class))).thenAnswer(invocation -> {
            TaskStep taskStep = invocation.getArgument(0);
            taskStepStore.add(taskStep);
            return taskStep;
        });
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenAnswer(invocation -> new ArrayList<>(taskStepStore));
        return taskStepService;
    }

    /**
     * 构造内存化评分配置服务。
     *
     * @return 评分配置服务。
     */
    private EvaluationConfigService buildEvaluationConfigService() {
        return new EvaluationConfigService() {
            @Override
            public EvaluationRule createEvaluationRule(EvaluationRule evaluationRule) {
                return evaluationRule;
            }

            @Override
            public List<EvaluationRule> listRules(String tenantId, String taskId, String teachingPointId) {
                return Collections.emptyList();
            }

            @Override
            public EvaluationItem createEvaluationItem(EvaluationItem evaluationItem) {
                evaluationItemStore.add(evaluationItem);
                return evaluationItem;
            }

            @Override
            public List<EvaluationItem> listItemsByRule(String tenantId, String evaluationRuleId) {
                return new ArrayList<>(evaluationItemStore);
            }
        };
    }

    /**
     * 构造点击采集事件。
     *
     * @return 采集事件。
     */
    private CaptureEvent buildClickEvent() {
        CaptureEvent event = new CaptureEvent();
        event.setId("event_001");
        event.setTenantId("tenant_001");
        event.setCaptureSessionId("session_001");
        event.setEventType("CLICK");
        event.setTargetText("保存");
        event.setPageUrl("/filing/create");
        event.setSequenceNo(1L);
        return event;
    }
}
