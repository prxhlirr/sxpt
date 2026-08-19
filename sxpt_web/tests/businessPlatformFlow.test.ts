import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import { createTrainingStore } from '../src/stores/trainingStore';
import { createMockTrainingState } from '../src/data/mockSeed';
import {
  createMemoryStorage,
  TRAINING_STORAGE_KEY
} from '../src/services/trainingApi';

const source = (path: string) =>
  readFileSync(resolve(process.cwd(), path), 'utf8');

describe('业务平台维护、教案绑定与录制加载', () => {
  it('自动迁移升级前没有业务平台字段的浏览器数据', () => {
    const legacy = createMockTrainingState() as unknown as Record<string, unknown>;
    delete legacy.businessPlatforms;
    for (const lesson of legacy.lessons as Array<Record<string, unknown>>) {
      delete lesson.businessPlatformId;
      delete lesson.businessPlatformModuleId;
    }
    const storage = createMemoryStorage({
      [TRAINING_STORAGE_KEY]: JSON.stringify(legacy)
    });

    const store = createTrainingStore({ storage });

    expect(store.state.businessPlatforms.length).toBeGreaterThan(0);
    expect(
      store.state.lessons.every((lesson) => Boolean(lesson.businessPlatformId))
    ).toBe(true);
    expect(
      store.state.businessPlatforms.every((platform) =>
        Array.isArray(platform.modules)
      )
    ).toBe(true);
    expect(
      store.state.lessons.every((lesson) =>
        Boolean(lesson.businessPlatformModuleId)
      )
    ).toBe(true);
  });

  it('自动迁移已有平台但尚无下属模块的浏览器数据', () => {
    const legacy = createMockTrainingState() as unknown as Record<string, unknown>;
    for (const platform of legacy.businessPlatforms as Array<
      Record<string, unknown>
    >) {
      delete platform.modules;
    }
    for (const lesson of legacy.lessons as Array<Record<string, unknown>>) {
      delete lesson.businessPlatformModuleId;
    }
    const storage = createMemoryStorage({
      [TRAINING_STORAGE_KEY]: JSON.stringify(legacy)
    });

    const store = createTrainingStore({ storage });

    expect(
      store.state.businessPlatforms.every(
        (platform) => platform.modules.length > 0
      )
    ).toBe(true);
    expect(
      store.state.lessons.every((lesson) =>
        Boolean(
          store.getBusinessPlatformModule(
            lesson.businessPlatformId,
            lesson.businessPlatformModuleId
          )
        )
      )
    ).toBe(true);
  });

  it('管理员可以维护业务平台，已绑定的平台不能直接删除', () => {
    let sequence = 0;
    const store = createTrainingStore({
      storage: createMemoryStorage(),
      now: () => '2026-07-27T10:00:00.000Z',
      idFactory: (prefix) => `${prefix}-test-${++sequence}`
    });

    const platform = store.createBusinessPlatform({
      code: 'CRM',
      name: '客户关系业务平台',
      baseUrl: 'https://crm.example.test',
      description: '客户业务实训'
    });
    const businessModule = store.createBusinessPlatformModule(platform.id, {
      code: 'CUSTOMER_APPROVAL',
      name: '客户审批',
      path: '/customer-approval',
      description: '客户业务审批'
    });
    const lesson = store.createLesson({
      code: 'CRM-LESSON',
      title: '客户业务教案',
      moduleName: '客户管理',
      businessPlatformId: platform.id,
      businessPlatformModuleId: businessModule.id
    });

    expect(lesson.businessPlatformId).toBe(platform.id);
    expect(lesson.businessPlatformModuleId).toBe(businessModule.id);
    expect(store.getBusinessPlatform(platform.id)?.baseUrl).toBe(
      'https://crm.example.test'
    );
    expect(() => store.removeBusinessPlatform(platform.id)).toThrow(
      '已被教案使用'
    );
    expect(() =>
      store.removeBusinessPlatformModule(platform.id, businessModule.id)
    ).toThrow('已被教案使用');
  });

  it('业务平台停用后不允许新教案继续绑定', () => {
    const store = createTrainingStore({ storage: createMemoryStorage() });
    const platform = store.state.businessPlatforms[0];

    store.updateBusinessPlatform(platform.id, { status: 'DISABLED' });

    expect(() =>
      store.createLesson({
        title: '不可绑定平台的教案',
        businessPlatformId: platform.id
      })
    ).toThrow('请选择已启用的业务平台');
  });

  it('后台提供业务平台维护页和管理员专属路由', () => {
    const router = source('src/router.ts');
    const view = source('src/views/admin/BusinessPlatformManagementView.vue');

    expect(router).toContain("name: 'business-platforms'");
    expect(router).toContain("meta: { title: '业务平台管理', role: 'admin' }");
    expect(view).toContain('store.createBusinessPlatform');
    expect(view).toContain('store.updateBusinessPlatform');
    expect(view).toContain('store.removeBusinessPlatform');
    expect(view).toContain('store.createBusinessPlatformModule');
    expect(view).toContain('store.updateBusinessPlatformModule');
    expect(view).toContain('store.removeBusinessPlatformModule');
    expect(view).toContain('platform.modules');
    expect(view).toContain('新增模块');
  });

  it('新建教案必须选择已启用业务平台', () => {
    const list = source('src/views/admin/LessonListView.vue');

    expect(list).toContain('enabledBusinessPlatforms');
    expect(list).toContain('createForm.businessPlatformId');
    expect(list).toContain('createForm.businessPlatformModuleId');
    expect(list).toContain('enabledBusinessPlatformModules');
    expect(list).toContain('请选择录制时打开的业务平台');
    expect(list).toContain('请选择录制时进入的平台模块');
    expect(list).not.toContain('createForm.objectiveMaxScore');
    expect(list).not.toContain('createForm.subjectiveMaxScore');
  });

  it('录制工作区按教案绑定地址加载外部业务平台', () => {
    const editor = source('src/views/admin/LessonEditorView.vue');

    expect(editor).toContain('effectiveBusinessPlatformUrl');
    expect(editor).toContain('businessPlatformModule');
    expect(editor).toContain('configured-business-frame');
    expect(editor).toContain('useEmbeddedBusinessSimulation');
    expect(editor).toContain(':src="businessFrameUrl"');
    expect(editor).toContain(
      "['BUSINESS_ACTION', 'SXPT_BUSINESS_ACTION'].includes(messageType)"
    );
    expect(editor).toContain('normalizeBusinessPageSnapshot(payload.pageSnapshot');
    expect(editor).toContain("window.addEventListener('message'");
  });

  it('讲解学习高亮录制操作位置，并允许隐藏上层菜单', () => {
    const snapshot = source('src/utils/businessSnapshot.ts');
    const snapshotFrame = source(
      'src/components/lesson/BusinessSnapshotFrame.vue'
    );
    const captureFrame = source(
      'src/components/lesson/BusinessCaptureFrame.vue'
    );
    const viewportScaling = source('src/utils/viewportScaling.ts');
    const playback = source(
      'src/components/lesson/LessonPlaybackPlayer.vue'
    );
    const recording = source('src/views/admin/RecordingPreviewView.vue');
    const runner = source('src/views/student/StudentTaskRunnerView.vue');

    expect(snapshot).toContain('sxpt-recorded-operation-target');
    expect(snapshot).toContain("target.classList.add('sxpt-recorded-operation-target')");
    expect(snapshot).toContain('rebaseSnapshotMarkupResources');
    expect(snapshot).toContain("'srcset'");
    expect(snapshot).toContain('resourceBaseUrl?: string');
    expect(snapshotFrame).toContain('recorded-rect-highlight');
    expect(snapshotFrame).toContain('calculateContainedViewport');
    expect(snapshotFrame).toContain('mapRectToContainedViewport');
    expect(snapshotFrame).toContain('mapRectToFilledViewport');
    expect(snapshotFrame).toContain('has-recorded-viewport');
    expect(snapshotFrame).toContain("fitMode: 'fill'");
    expect(snapshotFrame).toContain('ResizeObserver');
    expect(captureFrame).toContain('DEFAULT_RECORDING_VIEWPORT');
    expect(captureFrame).toContain("fitMode: 'fill'");
    expect(captureFrame).toContain('自适应全屏');
    expect(playback).toContain('fit-mode="fill"');
    expect(playback).toContain('businessResourceBaseUrl');
    expect(viewportScaling).toContain('Math.min(');
    expect(viewportScaling).toContain('containerWidth / safeViewport.width');
    expect(viewportScaling).toContain('containerHeight / safeViewport.height');
    expect(viewportScaling).toContain('containerHeight / safeViewport.height');
    expect(playback).toContain('currentStep.selectorCandidates');
    expect(playback).toContain(
      ':selector="showStageIntroduction ? undefined : currentStep.selector"'
    );
    expect(playback).toContain('本节点说明');
    expect(playback).toContain('教学点说明');
    expect(playback).toContain('currentStep.teachingText ||');
    expect(playback).toContain('showStageIntroduction');
    expect(playback).toContain('进入本教学点');
    expect(playback).toContain('class="playback-menu-shell"');
    expect(playback).toContain('class="playback-menu-rail"');
    expect(playback).toContain('class="playback-directory-panel"');
    expect(playback).toContain('class="playback-floating-launcher"');
    expect(playback).toContain('startLauncherDrag');
    expect(playback).toContain('sxpt:lesson-playback-launcher');
    expect(playback).toContain("'stage-prompt': showStageIntroduction");
    expect(playback).toContain("'node-prompt': !showStageIntroduction");
    expect(playback).toContain('下一步 →');
    expect(playback).toContain('cyclePromptPosition');
    expect(playback).toContain('AttachmentPanel');
    expect(playback).toContain('教学说明');
    expect(playback).toContain('currentStep.actionLabel');
    expect(playback).toContain('参考时长');
    expect(playback).not.toContain('<dt>pageTitle</dt>');
    expect(playback).not.toContain('<dt>actionLabel</dt>');
    expect(playback).not.toContain('<dt>selector</dt>');
    expect(playback).not.toContain('<dt>durationSeconds</dt>');
    expect(recording).toContain("import LessonPlaybackPlayer");
    expect(recording).toContain('<LessonPlaybackPlayer');
    expect(runner).toContain("import LessonPlaybackPlayer");
    expect(runner).toContain('<LessonPlaybackPlayer');
    expect(runner).toContain('v-if="isLearning"');
    expect(runner).toContain('legacyLearningPlaybackEnabled = false');
    expect(runner).toContain('task.value?.attemptNumber] as const');
    expect(runner).toContain('正在初始化任务');
    expect(runner).toContain("if (task.value.mode === 'LEARNING')");
    expect(runner).toContain('return lesson.value.stages');
    expect(runner).toContain("task.value?.mode === 'LEARNING'");
    expect(runner).toContain("task.value?.mode === 'PRACTICE'");
    expect(runner).toContain('playbackStages.value.map((stage) => stage.id)');
    expect(runner).toContain('currentLearningInstruction');
    expect(runner).toContain('学习内容与教师讲解一致');
    expect(runner).toContain('selectLearningTeachingPoint');
    expect(runner).toContain('selectLearningStep');
    expect(runner).toContain('moveLearningPlayback');
  });

  it('录制支持类似开发者工具的任意元素拾取并绑定说明节点', () => {
    const editor = source('src/views/admin/LessonEditorView.vue');
    const captureFrame = source(
      'src/components/lesson/BusinessCaptureFrame.vue'
    );
    const selector = source('src/utils/elementSelector.ts');
    const businessPage = source('public/lesson-business-capture.html');

    expect(editor).toContain('⌖ 元素选择模式');
    expect(editor).toContain('＋ 添加节点');
    expect(editor).not.toContain('authoring-rail-action capture');
    expect(editor).toContain('startElementPick();');
    expect(editor).toContain('startAddingStep');
    expect(editor).toContain('pendingNewStepStageId');
    expect(editor).toContain('选取完成后将创建节点并打开右侧属性配置');
    expect(editor).toContain('authoring-qq-shell');
    expect(editor).toContain('authoring-floating-launcher');
    expect(editor).toContain('handleAuthoringLauncherPointerMove');
    expect(editor).toContain('persistAuthoringLauncherPosition');
    expect(editor).toContain('authoring-right-drawer');
    expect(editor).toContain('closeAuthoringDrawers');
    expect(editor).toContain('cyclePickerToolbarPosition');
    expect(editor).toContain('换个角落');
    expect(editor).toContain('@pointermove.capture="handleInternalPickMove"');
    expect(editor).toContain('@element-picked="handleElementPicked"');
    expect(editor).toContain("kind: 'guide'");
    expect(editor).toContain('selectorCandidates: payload.selectorCandidates');
    expect(editor).toContain('⌖ 重新选择元素');
    expect(editor).toContain('authoringLaunchContextKey');
    expect(editor).toMatch(/watch\(\s*authoringLaunchContextKey/);
    expect(editor).toContain('@frame-load="handleBusinessFrameLoad"');
    expect(editor).toContain('业务页面已恢复，请继续选择要绑定的目标元素');
    expect(captureFrame).toContain("post({ type: 'SXPT_START_ELEMENT_PICK' })");
    expect(captureFrame).toContain("post({ type: 'START_ELEMENT_PICK' })");
    expect(captureFrame).toContain("message.type === 'ELEMENT_PICKED'");
    expect(selector).toContain('buildStableElementSelector');
    expect(selector).toContain(':nth-of-type(');
    expect(businessPage).toContain('body.picking *');
    expect(businessPage).toContain('"pointermove"');
    expect(businessPage).toContain('getPickedElement(event.target)');
    expect(businessPage).toContain('pageSnapshot: createPageSnapshot()');
  });

  it('练习全屏打开原业务系统，自由操作并在后台静默记录评分点', () => {
    const runner = source('src/views/student/StudentTaskRunnerView.vue');
    const captureFrame = source(
      'src/components/lesson/BusinessCaptureFrame.vue'
    );
    const backend = source('src/services/backendTrainingApi.ts');
    const store = source('src/stores/trainingStore.ts');

    expect(runner).toContain('<BusinessCaptureFrame');
    expect(runner).toContain('class="practice-live-business-view"');
    expect(runner).toContain(':src="practiceBusinessUrl"');
    expect(runner).toContain('monitor-actions');
    expect(runner).toContain(':show-resolution="false"');
    expect(runner).toContain('v-else-if="!isPractice && displayedLearningStep"');
    expect(runner).toContain('pendingPracticeStepKeys');
    expect(runner).toContain('recordStudentPracticeStepRemote');
    expect(runner).toContain('finishPracticeWhenEvidenceComplete');
    expect(runner).toContain('requiredPracticeSteps');
    expect(runner).toContain('const matched = requiredPracticeSteps.value');
    expect(runner).toContain('hasPracticeStepEvidence(stage.id, step.id)');
    expect(runner).toContain(
      '(step) => !isPractice.value || isRequiredPracticeStep(step)'
    );
    expect(store).toContain('if (!step || !isRequiredPracticeStep(step))');
    expect(store).toContain('`${result.stageId}\\u0000${result.stepId}`');
    expect(runner).toContain('await store.flushAuthenticatedWorkspace()');
    expect(runner).toContain('正在上传练习证据');
    expect(runner).toContain('操作记录和截屏证据已全部上传');
    expect(runner).toContain('evidenceScreenshot: payload.evidenceScreenshot');
    expect(runner).not.toContain('practice-trace-feedback');
    expect(runner).not.toContain('已捕获原平台操作，但未匹配到教案中的操作点');
    expect(runner).toContain('重新加载业务平台');
    expect(runner).toContain("task.status === 'DOING'");
    expect(runner).toContain('!practiceBusinessUrl');
    expect(runner).toContain('practiceActionMatchPriority');
    expect(runner).toContain('const started = await startTask()');
    expect(runner).toContain(
      'questionId: safeCode(task.value.dataItemId || task.value.id, 64)'
    );
    expect(runner).toContain('class="practice-edge-toolbar"');
    expect(runner).toContain('to="/student/tasks"');
    expect(runner).toContain("{{ showPracticeGuide ? '收起提示' : '教案提示' }}");
    expect(runner).toContain('class="practice-hint-drawer"');
    expect(runner).toContain('<PlaybackNavigationTree');
    expect(runner).toContain('@select-step="selectPracticeHintStep"');
    expect(runner).toContain('practiceFrameRef.value?.previewStep(');
    expect(runner).toContain('practiceFrameRef.value?.clearStepPreview()');
    expect(captureFrame).toContain('clearStepPreview');
    expect(captureFrame).toContain('createBusinessStepPreviewClearMessages');
    expect(runner).toContain("task.value?.mode === 'PRACTICE'");
    expect(runner).toContain(
      'return new Set(playbackStages.value.map((stage) => stage.id))'
    );
    expect(captureFrame).toContain("studentMode?: 'LEARNING' | 'PRACTICE'");
    expect(captureFrame).toContain('monitorActions?: boolean');
    expect(captureFrame).toContain("type: 'SET_RECORDING_STATE', enabled: true, monitorOnly: true");
    expect(backend).toContain('reportStudentPracticeStep(');
    expect(backend).toContain(
      'teachingPointId: publishedTask.remoteTeachingPointId'
    );
    expect(backend).toContain("traceType: 'STEP_COMPLETED'");
    expect(backend).toContain('observedTraceType: mapPracticeStepActionType(step)');
    expect(store).toContain('completedPracticeStepIds');
    expect(store).toContain('syncPracticeStagesFromEvidence');
    expect(store).toContain("task.mode === 'PRACTICE'");
    expect(store).toContain('stage.visibility.PRACTICE &&');
  });

  it('教师端可查看学生练习操作点并把主观评分同步到后端', () => {
    const review = source('src/views/teacher/TeacherReviewView.vue');
    const store = source('src/stores/trainingStore.ts');
    const backend = source('src/services/backendTrainingApi.ts');
    const workspace = source('src/services/trainingApi.ts');
    const evaluation = source('src/api/evaluation.ts');

    expect(review).toContain('学生练习操作记录');
    expect(review).toContain('selectedPracticeSteps');
    expect(review).toContain('学生操作截屏');
    expect(review).toContain('step.evidenceScreenshot?.dataUrl');
    expect(review).toContain('openEvidencePreview');
    expect(review).toContain('学生操作证据 · 连续预览');
    expect(review).toContain('moveEvidencePreview(-1)');
    expect(review).toContain('moveEvidencePreview(1)');
    expect(review).toContain("event.key === 'ArrowLeft'");
    expect(review).toContain('grid-template-rows: auto minmax(0, 1fr)');
    expect(review).toContain('overflow-y: auto');
    expect(review).toContain('class="practice-evidence-placeholder');
    expect(review).toContain('class="evidence-index"');
    expect(review).toContain('minmax(72px, auto)');
    expect(review).not.toContain('实际操作 {{ step.observedActionType }}');
    expect(review).not.toContain('页面 {{ step.observedUrl');
    expect(review).not.toContain('recordedActionType?.toUpperCase()');
    expect(review).toContain('gradeStudentTaskRemote');
    expect(workspace).toContain('practiceStepResults: Array.isArray');
    expect(store).toContain('reviewStudentTask(');
    expect(backend).toContain('evaluationApi.review(');
    expect(evaluation).toContain("url: '/evaluation/results/review'");
  });

  it('考试界面仅保留可隐藏的任务说明浮栏', () => {
    const runner = source('src/views/student/StudentTaskRunnerView.vue');

    expect(runner).toContain('!isExam && !isPractice && showRunnerMenu');
    expect(runner).toContain("task.mode === 'PRACTICE'");
    expect(runner).toContain('v-if="isExam && showHelp" class="exam-task-panel"');
    expect(runner).toContain('显示考试说明');
    expect(runner).toContain("task.value?.mode === 'EXAM'");
  });
});
