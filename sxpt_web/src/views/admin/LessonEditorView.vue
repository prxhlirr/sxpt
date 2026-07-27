<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusPill from '../../components/ui/StatusPill.vue';
import type {
  CompletionMethod,
  LessonStage,
  RecordedStep,
  RunMode
} from '../../domain/models';
import { useTrainingStore } from '../../stores/trainingStore';

const route = useRoute();
const store = useTrainingStore();
const selectedStageId = ref('');
const feedback = ref('');
const feedbackTone = ref<'success' | 'danger'>('success');
const lessonId = computed(() => String(route.params.lessonId ?? ''));
const lesson = computed(() => store.getLesson(lessonId.value));
const configurationLocked = computed(() =>
  store.state.publishedTasks.some(
    (task) => task.lessonId === lessonId.value
  )
);

const basicForm = reactive({
  code: '',
  title: '',
  moduleName: '',
  description: '',
  objectiveMaxScore: 0,
  subjectiveMaxScore: 0,
  tags: ''
});

type StageDraft = Omit<LessonStage, 'recordedSteps'>;
const stageForm = ref<StageDraft | null>(null);
const selectedStage = computed(() =>
  lesson.value?.stages.find((stage) => stage.id === selectedStageId.value)
);

const objectiveStageScore = computed(
  () => lesson.value?.stages.reduce((total, stage) => total + Number(stage.score), 0) ?? 0
);
const totalScore = computed(
  () =>
    Number(lesson.value?.objectiveMaxScore ?? 0) +
    Number(lesson.value?.subjectiveMaxScore ?? 0)
);
const recordedStepCount = computed(
  () =>
    lesson.value?.stages.reduce((total, stage) => total + stage.recordedSteps.length, 0) ??
    0
);
const validationMessages = computed<string[]>(() => {
  if (!lesson.value) return ['教案不存在'];
  try {
    return store.validateLesson(lesson.value.id);
  } catch (error) {
    return [error instanceof Error ? error.message : '教案校验失败'];
  }
});

const completionMethods: Array<{ value: CompletionMethod; label: string }> = [
  { value: 'click', label: '完成关键点击' },
  { value: 'business_check', label: '业务状态校验' },
  { value: 'submission', label: '完成提交' },
  { value: 'manual', label: '教师人工确认' },
  { value: 'mixed', label: '流程操作综合判断' }
];

const modes: Array<{ key: RunMode; label: string; hint: string }> = [
  { key: 'LEARNING', label: '演示学习', hint: '只讲解，不办理真实业务' },
  { key: 'PRACTICE', label: '练习', hint: '使用练习数据完成流程' },
  { key: 'EXAM', label: '考试', hint: '计分并记录操作轨迹' }
];

watch(
  lesson,
  (current) => {
    if (!current) return;
    basicForm.code = current.code;
    basicForm.title = current.title;
    basicForm.moduleName = current.moduleName;
    basicForm.description = current.description;
    basicForm.objectiveMaxScore = current.objectiveMaxScore;
    basicForm.subjectiveMaxScore = current.subjectiveMaxScore;
    basicForm.tags = current.tags.join('，');
    if (!current.stages.some((stage) => stage.id === selectedStageId.value)) {
      selectedStageId.value = current.stages[0]?.id ?? '';
    } else {
      loadStageDraft();
    }
  },
  { immediate: true }
);

watch(selectedStageId, () => loadStageDraft());

function loadStageDraft() {
  const stage = selectedStage.value;
  stageForm.value = stage
    ? {
        id: stage.id,
        stageKey: stage.stageKey,
        name: stage.name,
        groupKey: stage.groupKey,
        description: stage.description,
        required: stage.required,
        score: stage.score,
        completionMethod: stage.completionMethod,
        visibility: { ...stage.visibility }
      }
    : null;
}

function showFeedback(message: string, tone: 'success' | 'danger' = 'success') {
  feedback.value = message;
  feedbackTone.value = tone;
}

function saveBasicInformation() {
  if (!lesson.value) return;
  try {
    store.updateLesson(lesson.value.id, {
      code: basicForm.code.trim(),
      title: basicForm.title.trim(),
      moduleName: basicForm.moduleName.trim(),
      description: basicForm.description.trim(),
      objectiveMaxScore: Number(basicForm.objectiveMaxScore),
      subjectiveMaxScore: Number(basicForm.subjectiveMaxScore),
      tags: basicForm.tags
        .split(/[，,]/)
        .map((tag) => tag.trim())
        .filter(Boolean)
    });
    showFeedback('基础信息已保存。');
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '保存失败', 'danger');
  }
}

function saveStage() {
  if (!lesson.value || !stageForm.value) return;
  try {
    store.updateStage(lesson.value.id, stageForm.value.id, {
      stageKey: stageForm.value.stageKey.trim(),
      name: stageForm.value.name.trim(),
      groupKey: stageForm.value.groupKey.trim(),
      description: stageForm.value.description.trim(),
      required: stageForm.value.required,
      score: Number(stageForm.value.score),
      completionMethod: stageForm.value.completionMethod,
      visibility: { ...stageForm.value.visibility }
    });
    showFeedback(`“${stageForm.value.name}”已保存。`);
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '阶段保存失败', 'danger');
  }
}

function addStage() {
  if (!lesson.value) return;
  const index = lesson.value.stages.length + 1;
  const allocated = objectiveStageScore.value;
  const remaining = Math.max(0, lesson.value.objectiveMaxScore - allocated);
  try {
    const stage = store.addStage(lesson.value.id, {
      stageKey: `stage_${index}`,
      name: `业务阶段 ${index}`,
      groupKey: `role_${index}`,
      description: '说明本阶段的业务目标、移交条件和操作注意事项。',
      required: true,
      score: remaining,
      completionMethod: 'mixed',
      visibility: { LEARNING: true, PRACTICE: true, EXAM: true },
      recordedSteps: []
    });
    selectedStageId.value = stage.id;
    showFeedback(`已添加“${stage.name}”。`);
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '阶段添加失败', 'danger');
  }
}

function removeSelectedStage() {
  if (!lesson.value || !selectedStage.value) return;
  if (!window.confirm(`确认删除“${selectedStage.value.name}”及其录制步骤吗？`)) return;
  const currentIndex = lesson.value.stages.findIndex(
    (stage) => stage.id === selectedStageId.value
  );
  try {
    store.removeStage(lesson.value.id, selectedStage.value.id);
    selectedStageId.value =
      lesson.value.stages[Math.max(0, currentIndex - 1)]?.id ??
      lesson.value.stages[0]?.id ??
      '';
    showFeedback('阶段已删除。');
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '阶段删除失败', 'danger');
  }
}

function moveSelectedStage(direction: 'up' | 'down') {
  if (!lesson.value || !selectedStage.value) return;
  try {
    store.moveStage(lesson.value.id, selectedStage.value.id, direction);
    showFeedback(direction === 'up' ? '阶段已上移。' : '阶段已下移。');
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '阶段排序失败', 'danger');
  }
}

function addRecordedStep() {
  if (!lesson.value || !selectedStage.value) return;
  const order = selectedStage.value.recordedSteps.length + 1;
  const step: RecordedStep = {
    id: `record-${selectedStage.value.id}-${Date.now()}`,
    title: `录制操作 ${order}`,
    pageTitle: '业务系统页面',
    actionLabel: '点击业务操作',
    selector: `[data-training-step="${order}"]`,
    durationSeconds: 8,
    note: '说明本步应观察的页面反馈和业务结果。'
  };
  store.updateStage(lesson.value.id, selectedStage.value.id, {
    recordedSteps: [...selectedStage.value.recordedSteps, step]
  });
  showFeedback('已添加模拟录制步骤，可继续完善步骤信息。');
}

function updateRecordedStep(stepId: string, patch: Partial<RecordedStep>) {
  if (!lesson.value || !selectedStage.value) return;
  store.updateStage(lesson.value.id, selectedStage.value.id, {
    recordedSteps: selectedStage.value.recordedSteps.map((step) =>
      step.id === stepId ? { ...step, ...patch } : step
    )
  });
}

function removeRecordedStep(stepId: string) {
  if (!lesson.value || !selectedStage.value) return;
  store.updateStage(lesson.value.id, selectedStage.value.id, {
    recordedSteps: selectedStage.value.recordedSteps.filter((step) => step.id !== stepId)
  });
  showFeedback('录制步骤已移除。');
}

function inputValue(event: Event) {
  return (event.target as HTMLInputElement).value;
}

function numberValue(event: Event) {
  return Number((event.target as HTMLInputElement).value);
}

function publishLesson() {
  if (!lesson.value) return;
  try {
    store.publishLesson(lesson.value.id);
    showFeedback('教案发布成功，现在可以进入考试设置并配置人员分组。');
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '发布校验未通过', 'danger');
  }
}
</script>

<template>
  <div v-if="lesson" class="page editor-page">
    <PageHeader
      eyebrow="LESSON WORKBENCH"
      :title="lesson.title"
      description="把录制步骤编排成任意数量的串行业务阶段，并定义每个角色的完成标准。"
    >
      <RouterLink
        class="button secondary"
        :to="{ name: 'lesson-recording', params: { lessonId: lesson.id } }"
      >
        ▶ 录制回看
      </RouterLink>
      <button
        class="primary"
        type="button"
        :disabled="configurationLocked"
        @click="publishLesson"
      >
        {{ configurationLocked ? '考试已发布 · 配置冻结' : '发布教案' }}
      </button>
    </PageHeader>

    <nav class="flow-nav" aria-label="业务配置流程">
      <span class="active"><i>1</i> 教案编排</span>
      <RouterLink :to="{ name: 'exam-setup', params: { lessonId: lesson.id } }">
        <i>2</i> 考试设置
      </RouterLink>
      <RouterLink :to="{ name: 'group-setup', params: { lessonId: lesson.id } }">
        <i>3</i> 分组设置
      </RouterLink>
      <RouterLink :to="{ name: 'exam-data', params: { lessonId: lesson.id } }">
        <i>4</i> 考试数据
      </RouterLink>
      <RouterLink :to="{ name: 'publish-center', params: { lessonId: lesson.id } }">
        <i>5</i> 发布任务
      </RouterLink>
    </nav>

    <div v-if="feedback" class="notice" :class="feedbackTone">{{ feedback }}</div>
    <div v-if="configurationLocked" class="notice">
      此教案已有考试任务，当前版本已冻结以保护在考和历史成绩。需要调整时，请在教案列表复制为新版本后再编排。
    </div>

    <section
      class="card basic-card"
      :class="{ 'configuration-locked': configurationLocked }"
    >
      <div class="card-header">
        <div>
          <h2>基础信息与总分</h2>
          <p>基础信息、标签和评分上限将贯穿考试发布与成绩展示。</p>
        </div>
        <div class="header-badges">
          <StatusPill :status="lesson.status" />
          <span>V{{ lesson.version }}</span>
        </div>
      </div>
      <div class="card-body">
        <div class="form-grid three">
          <label>
            <span>教案编号</span>
            <input v-model="basicForm.code" />
          </label>
          <label>
            <span>教案名称</span>
            <input v-model="basicForm.title" />
          </label>
          <label>
            <span>业务模块</span>
            <input v-model="basicForm.moduleName" />
          </label>
          <label class="wide">
            <span>教学简介</span>
            <textarea v-model="basicForm.description" rows="3" />
          </label>
          <label>
            <span>客观分上限</span>
            <input v-model.number="basicForm.objectiveMaxScore" type="number" min="0" />
          </label>
          <label>
            <span>主观分上限</span>
            <input v-model.number="basicForm.subjectiveMaxScore" type="number" min="0" />
          </label>
          <label>
            <span>标签（逗号分隔）</span>
            <input v-model="basicForm.tags" placeholder="采购，审批，多角色" />
          </label>
        </div>
        <div class="score-summary">
          <span><small>教案总分</small><strong>{{ totalScore }}</strong></span>
          <span><small>阶段客观分合计</small><strong>{{ objectiveStageScore }}</strong></span>
          <span><small>动态阶段</small><strong>{{ lesson.stages.length }}</strong></span>
          <span><small>录制步骤</small><strong>{{ recordedStepCount }}</strong></span>
          <button class="secondary" type="button" @click="saveBasicInformation">保存基础信息</button>
        </div>
      </div>
    </section>

    <section
      class="editor-grid"
      :class="{ 'configuration-locked': configurationLocked }"
    >
      <aside class="card stage-rail">
        <div class="card-header">
          <div>
            <h2>阶段时间线</h2>
            <p>不限角色数量，按实际业务顺序串联。</p>
          </div>
          <button class="primary compact" type="button" @click="addStage">＋ 阶段</button>
        </div>
        <div class="timeline">
          <button
            v-for="(stage, index) in lesson.stages"
            :key="stage.id"
            class="timeline-item"
            :class="{ active: stage.id === selectedStageId }"
            type="button"
            @click="selectedStageId = stage.id"
          >
            <span class="stage-index">{{ index + 1 }}</span>
            <span>
              <strong>{{ stage.name }}</strong>
              <small>{{ stage.groupKey || '未指定角色' }} · {{ stage.score }} 分</small>
            </span>
            <em>{{ stage.recordedSteps.length }}</em>
          </button>
          <div v-if="!lesson.stages.length" class="empty-stage">
            暂无阶段。添加第一个业务阶段开始编排。
          </div>
        </div>
      </aside>

      <main v-if="stageForm && selectedStage" class="stage-workspace">
        <section class="card">
          <div class="card-header">
            <div>
              <h2>阶段规则</h2>
              <p>角色、计分与完成方式共同决定学员在该阶段的任务。</p>
            </div>
            <div class="inline-actions">
              <button
                type="button"
                :disabled="lesson.stages[0]?.id === selectedStage.id"
                @click="moveSelectedStage('up')"
              >
                ↑ 上移
              </button>
              <button
                type="button"
                :disabled="lesson.stages.at(-1)?.id === selectedStage.id"
                @click="moveSelectedStage('down')"
              >
                ↓ 下移
              </button>
              <button class="danger" type="button" @click="removeSelectedStage">删除阶段</button>
            </div>
          </div>
          <div class="card-body">
            <div class="form-grid three">
              <label>
                <span>阶段名称</span>
                <input v-model="stageForm.name" />
              </label>
              <label>
                <span>阶段标识 stageKey</span>
                <input v-model="stageForm.stageKey" />
              </label>
              <label>
                <span>负责角色 groupKey</span>
                <input v-model="stageForm.groupKey" placeholder="例如 maker / reviewer" />
              </label>
              <label>
                <span>阶段分值</span>
                <input v-model.number="stageForm.score" type="number" min="0" />
              </label>
              <label>
                <span>完成方式 completionMethod</span>
                <select v-model="stageForm.completionMethod">
                  <option
                    v-for="method in completionMethods"
                    :key="method.value"
                    :value="method.value"
                  >
                    {{ method.label }}
                  </option>
                </select>
              </label>
              <label class="required-toggle">
                <span>任务要求</span>
                <span>
                  <input v-model="stageForm.required" type="checkbox" />
                  {{ stageForm.required ? '必做阶段' : '选做阶段' }}
                </span>
              </label>
              <label class="wide">
                <span>教学说明</span>
                <textarea
                  v-model="stageForm.description"
                  rows="4"
                  placeholder="说明业务目标、前置条件、移交规则和易错点"
                />
              </label>
            </div>

            <div class="visibility-panel">
              <div>
                <strong>三种模式可见性</strong>
                <small>同一教案可针对演示、练习和考试显示不同阶段。</small>
              </div>
              <label v-for="mode in modes" :key="mode.key">
                <input v-model="stageForm.visibility[mode.key]" type="checkbox" />
                <span><strong>{{ mode.label }}</strong><small>{{ mode.hint }}</small></span>
              </label>
            </div>
            <div class="save-row">
              <button class="primary" type="button" @click="saveStage">保存阶段规则</button>
            </div>
          </div>
        </section>

        <section class="card recording-card">
          <div class="card-header">
            <div>
              <h2>录制步骤</h2>
              <p>保留页面、动作、元素选择器、时长与逐步讲解，演示时不执行业务。</p>
            </div>
            <button class="secondary" type="button" @click="addRecordedStep">
              ＋ 添加模拟录制步骤
            </button>
          </div>
          <div class="recorded-list">
            <article
              v-for="(step, stepIndex) in selectedStage.recordedSteps"
              :key="step.id"
              class="recorded-step"
            >
              <span class="recorded-index">{{ stepIndex + 1 }}</span>
              <div class="recorded-fields">
                <label>
                  <span>步骤标题</span>
                  <input
                    :value="step.title"
                    @change="updateRecordedStep(step.id, { title: inputValue($event) })"
                  />
                </label>
                <label>
                  <span>页面 pageTitle</span>
                  <input
                    :value="step.pageTitle"
                    @change="updateRecordedStep(step.id, { pageTitle: inputValue($event) })"
                  />
                </label>
                <label>
                  <span>动作 actionLabel</span>
                  <input
                    :value="step.actionLabel"
                    @change="updateRecordedStep(step.id, { actionLabel: inputValue($event) })"
                  />
                </label>
                <label>
                  <span>选择器 selector</span>
                  <input
                    :value="step.selector"
                    class="mono"
                    @change="updateRecordedStep(step.id, { selector: inputValue($event) })"
                  />
                </label>
                <label>
                  <span>时长 durationSeconds（秒）</span>
                  <input
                    :value="step.durationSeconds"
                    type="number"
                    min="1"
                    @change="
                      updateRecordedStep(step.id, { durationSeconds: numberValue($event) })
                    "
                  />
                </label>
                <label class="step-note">
                  <span>逐步讲解</span>
                  <input
                    :value="step.note"
                    @change="updateRecordedStep(step.id, { note: inputValue($event) })"
                  />
                </label>
              </div>
              <button
                class="danger compact"
                type="button"
                title="删除录制步骤"
                @click="removeRecordedStep(step.id)"
              >
                删除
              </button>
            </article>
            <div v-if="!selectedStage.recordedSteps.length" class="empty-state">
              <div>
                <strong>该阶段还没有录制步骤</strong><br />
                可添加 Mock 步骤完善演示，也可等待后续接入真实录制数据。
              </div>
            </div>
          </div>
        </section>
      </main>

      <section v-else class="card no-stage">
        <div class="empty-state">
          <div><strong>请选择或添加一个阶段</strong><br />阶段可按实际业务需要动态增加。</div>
        </div>
      </section>
    </section>

    <section class="card publish-check">
      <div>
        <span class="eyebrow">PUBLISH CHECK</span>
        <h2>发布校验</h2>
        <p>发布前检查基础信息、阶段分值、角色与录制步骤；通过后再进入考试设置。</p>
      </div>
      <div class="check-result">
        <span v-if="!validationMessages.length" class="check-ok">✓ 校验通过，可以发布</span>
        <ul v-else>
          <li v-for="message in validationMessages" :key="message">{{ message }}</li>
        </ul>
      </div>
      <div class="publish-actions">
        <button class="secondary" type="button" @click="publishLesson">执行发布校验</button>
        <RouterLink
          class="button primary next-step"
          :to="{ name: 'exam-setup', params: { lessonId: lesson.id } }"
        >
          下一步：考试设置 →
        </RouterLink>
      </div>
    </section>
  </div>

  <section v-else class="standalone-state">
    <span>404</span>
    <h1>教案不存在</h1>
    <p>该教案可能已被删除或链接已失效。</p>
    <RouterLink class="button primary" :to="{ name: 'lesson-list' }">返回教案列表</RouterLink>
  </section>
</template>

<style scoped>
.editor-page {
  display: grid;
  gap: 16px;
}

.editor-page :deep(.page-header) {
  margin-bottom: 0;
}

.flow-nav {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  overflow: hidden;
  border: 1px solid #e5e8ef;
  border-radius: 12px;
  background: #fff;
}

.flow-nav a,
.flow-nav span {
  display: flex;
  min-height: 52px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-right: 1px solid #eceef3;
  color: #707c8f;
  font-size: 12px;
  font-weight: 800;
}

.flow-nav > :last-child {
  border-right: 0;
}

.flow-nav i {
  display: grid;
  width: 23px;
  height: 23px;
  place-items: center;
  border-radius: 50%;
  background: #eef1f6;
  font-style: normal;
  font-size: 10px;
}

.flow-nav .active {
  color: #5b4bd8;
  background: #f4f1ff;
}

.flow-nav .active i {
  color: #fff;
  background: var(--purple);
}

.basic-card {
  overflow: hidden;
}

.header-badges {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #788396;
  font-size: 11px;
}

.score-summary {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-top: 17px;
  border-top: 1px dashed #e0e4ec;
  padding-top: 16px;
}

.score-summary > span {
  display: grid;
  gap: 3px;
}

.score-summary small {
  color: #8490a2;
  font-size: 10px;
}

.score-summary strong {
  font-size: 20px;
}

.score-summary button {
  margin-left: auto;
}

.editor-grid {
  display: grid;
  grid-template-columns: 285px minmax(0, 1fr);
  align-items: start;
  gap: 16px;
}

.stage-rail {
  position: sticky;
  top: 88px;
  overflow: hidden;
}

.compact {
  min-height: 31px;
  padding: 0 10px;
  font-size: 11px;
}

.timeline {
  display: grid;
  padding: 10px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  min-height: 64px;
  align-items: center;
  gap: 9px;
  border: 1px solid transparent;
  padding: 8px;
  text-align: left;
}

.timeline-item:hover,
.timeline-item.active {
  border-color: #ded9ff;
  background: #f7f5ff;
  box-shadow: none;
  transform: none;
}

.stage-index,
.recorded-index {
  display: grid;
  width: 27px;
  height: 27px;
  place-items: center;
  border-radius: 9px;
  color: #6757da;
  background: #efedff;
  font-size: 11px;
  font-weight: 900;
}

.timeline-item > span:nth-child(2) {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.timeline-item strong {
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.timeline-item small {
  color: #8b95a7;
  font-size: 9px;
}

.timeline-item em {
  display: grid;
  min-width: 21px;
  height: 21px;
  place-items: center;
  border-radius: 999px;
  color: #718096;
  background: #eef1f5;
  font-style: normal;
  font-size: 9px;
}

.empty-stage {
  padding: 25px 12px;
  color: #8b95a7;
  font-size: 11px;
  line-height: 1.7;
  text-align: center;
}

.stage-workspace {
  display: grid;
  min-width: 0;
  gap: 16px;
}

.required-toggle > span:last-child {
  display: flex;
  min-height: 41px;
  align-items: center;
  gap: 8px;
  border: 1px solid #dce1eb;
  border-radius: 9px;
  padding: 0 11px;
  color: #4b586c;
  background: #fff;
}

.required-toggle input,
.visibility-panel input {
  width: auto;
}

.visibility-panel {
  display: grid;
  grid-template-columns: minmax(170px, 1fr) repeat(3, minmax(150px, 0.8fr));
  align-items: stretch;
  gap: 8px;
  margin-top: 16px;
  border-radius: 11px;
  padding: 12px;
  background: #f7f8fb;
}

.visibility-panel > div,
.visibility-panel label {
  display: flex;
  align-items: center;
}

.visibility-panel > div {
  display: grid;
  gap: 4px;
}

.visibility-panel small {
  color: #8993a4;
  font-size: 9px;
  line-height: 1.45;
}

.visibility-panel label {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px;
  border: 1px solid #e4e7ee;
  border-radius: 9px;
  padding: 9px;
  background: #fff;
}

.visibility-panel label span {
  display: grid;
  gap: 3px;
}

.visibility-panel label strong {
  font-size: 11px;
}

.save-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.recording-card {
  overflow: hidden;
}

.recorded-list {
  display: grid;
}

.recorded-step {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) auto;
  align-items: start;
  gap: 11px;
  border-bottom: 1px solid #edf0f5;
  padding: 15px 18px;
}

.recorded-step:last-child {
  border-bottom: 0;
}

.recorded-fields {
  display: grid;
  grid-template-columns: repeat(5, minmax(110px, 1fr));
  gap: 9px;
}

.recorded-fields label {
  min-width: 0;
}

.recorded-fields input {
  padding: 8px;
  font-size: 11px;
}

.step-note {
  grid-column: 1 / -1;
}

.mono {
  font-family: "Cascadia Code", Consolas, monospace;
}

.no-stage {
  min-height: 340px;
}

.publish-check {
  display: grid;
  grid-template-columns: minmax(230px, 0.9fr) minmax(270px, 1.3fr) auto;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background:
    radial-gradient(circle at 90% 0, rgb(109 93 252 / 10%), transparent 35%),
    #fff;
}

.publish-check h2,
.publish-check p {
  margin: 0;
}

.publish-check p {
  margin-top: 6px;
  color: #798497;
  font-size: 11px;
  line-height: 1.65;
}

.check-result {
  color: #bd4852;
  font-size: 11px;
}

.check-result ul {
  margin: 0;
  padding-left: 18px;
}

.check-result li + li {
  margin-top: 5px;
}

.check-ok {
  color: #07835e;
  font-weight: 800;
}

.publish-actions {
  display: flex;
  gap: 8px;
}

.next-step {
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
}

.configuration-locked input,
.configuration-locked select,
.configuration-locked textarea,
.configuration-locked button {
  pointer-events: none;
  opacity: 0.62;
}

.editor-grid.configuration-locked .timeline-item {
  pointer-events: auto;
  opacity: 1;
}

@media (max-width: 1180px) {
  .editor-grid {
    grid-template-columns: 240px minmax(0, 1fr);
  }

  .recorded-fields,
  .visibility-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .visibility-panel > div {
    grid-column: 1 / -1;
  }

  .publish-check {
    grid-template-columns: 1fr 1fr;
  }

  .publish-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}

@media (max-width: 820px) {
  .flow-nav {
    grid-template-columns: 1fr;
  }

  .flow-nav a,
  .flow-nav span {
    min-height: 42px;
    border-right: 0;
    border-bottom: 1px solid #eceef3;
  }

  .editor-grid {
    grid-template-columns: 1fr;
  }

  .stage-rail {
    position: static;
  }
}

@media (max-width: 620px) {
  .score-summary {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .score-summary button {
    grid-column: 1 / -1;
    margin-left: 0;
  }

  .recorded-fields,
  .visibility-panel,
  .publish-check {
    grid-template-columns: 1fr;
  }

  .recorded-step {
    grid-template-columns: 26px minmax(0, 1fr);
  }

  .recorded-step > button {
    grid-column: 2;
    justify-self: start;
  }

  .visibility-panel > div,
  .publish-actions {
    grid-column: auto;
  }

  .publish-actions {
    flex-direction: column;
  }
}
</style>
