<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import StatusPill from '../../components/ui/StatusPill.vue';
import { useTrainingStore } from '../../stores/trainingStore';

const store = useTrainingStore();
store.refreshPublishedTaskStatuses();
const route = useRoute();
const message = ref('');
const errorMessage = ref('');
const attemptMessage = ref('');
const showHelp = ref(true);
const checks = reactive({
  entered: false,
  located: false,
  reviewed: false
});
const submissionValues = reactive<Record<string, string>>({});

const task = computed(() =>
  store.state.studentTasks.find(
    (item) => item.id === String(route.params.taskId)
  )
);
const lesson = computed(() =>
  store.state.lessons.find((item) => item.id === task.value?.lessonId)
);
const groupPlan = computed(() =>
  task.value ? store.state.groupPlans[task.value.lessonId] : undefined
);
const examSettings = computed(() =>
  task.value ? store.state.examSettings[task.value.lessonId] : undefined
);
const dataItem = computed(() =>
  task.value
    ? (store.state.dataItems[task.value.lessonId] ?? []).find(
        (item) => item.id === task.value?.dataItemId
      )
    : undefined
);
const visibleStages = computed(
  () =>
    lesson.value?.stages.filter(
      (stage) => task.value && stage.visibility[task.value.mode]
    ) ?? []
);
const assignedStageIds = computed(() => {
  if (!task.value) return new Set<string>();
  const visibleIds = new Set(visibleStages.value.map((stage) => stage.id));
  const ids = groupPlan.value?.roles
    .filter((role) => task.value?.groupKeys.includes(role.key))
    .flatMap((role) => role.stageIds)
    .filter((id) => visibleIds.has(id));
  if (ids?.length) return new Set(ids);
  return new Set(
    visibleStages.value
      .filter((stage) => task.value?.groupKeys.includes(stage.groupKey))
      .map((stage) => stage.id)
  );
});
const nextStage = computed(() =>
  lesson.value?.stages.find(
    (stage) =>
      assignedStageIds.value.has(stage.id) &&
      !task.value?.completedStageIds.includes(stage.id)
  )
);
const nextStageIndex = computed(() =>
  lesson.value?.stages.findIndex((stage) => stage.id === nextStage.value?.id)
);
const allAssignedStagesComplete = computed(
  () =>
    [...assignedStageIds.value].length > 0 &&
    [...assignedStageIds.value].every((id) =>
      task.value?.completedStageIds.includes(id)
    )
);
const canRecordCompletion = computed(
  () =>
    task.value?.status === 'DOING' &&
    checks.entered &&
    checks.located &&
    checks.reviewed &&
    Boolean(nextStage.value)
);
const roleNames = computed(() =>
  task.value?.groupKeys.map(
    (key) =>
      groupPlan.value?.roles.find((role) => role.key === key)?.name ?? key
  )
);
const collaborationTasks = computed(() =>
  task.value
    ? store.state.studentTasks.filter(
        (item) =>
          item.publishedTaskId === task.value?.publishedTaskId &&
          item.dataItemId === task.value?.dataItemId
      )
    : []
);
const collaborationSubmitted = computed(
  () =>
    collaborationTasks.value.length > 0 &&
    collaborationTasks.value.every(
      (item) => item.status === 'SUBMITTED' || item.status === 'GRADED'
    )
);
const canRestartAttempt = computed(
  () =>
    Boolean(examSettings.value?.allowRetry) &&
    !collaborationSubmitted.value &&
    (task.value?.attemptNumber ?? 1) <
      (examSettings.value?.maxAttempts ?? 1)
);
const requiredSubmissionComplete = computed(() =>
  (examSettings.value?.submissionFields ?? []).every(
    (field) => !field.required || submissionValues[field.key]?.trim()
  )
);

function syncSubmissionValues() {
  Object.keys(submissionValues).forEach((key) => {
    delete submissionValues[key];
  });
  Object.assign(submissionValues, task.value?.submissionValues ?? {});
}

watch(
  () => nextStage.value?.id,
  () => {
    checks.entered = false;
    checks.located = false;
    checks.reviewed = false;
    message.value = '';
    errorMessage.value = '';
  }
);

watch(
  () => task.value?.id,
  syncSubmissionValues,
  { immediate: true }
);

function startTask() {
  if (!task.value) return;
  try {
    store.startStudentTask(task.value.id);
    message.value = '任务已开始。请按右侧提示完成本阶段的流程性操作。';
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '任务启动失败。';
  }
}

function completeStage() {
  if (!task.value || !nextStage.value) return;
  message.value = '';
  errorMessage.value = '';
  try {
    store.completeStudentStage(task.value.id, nextStage.value.id);
    message.value =
      '平台已记录本阶段的页面访问与流程操作。若有下一阶段，请继续办理。';
  } catch (error) {
    errorMessage.value =
      error instanceof Error
        ? error.message
        : '本阶段暂不能完成，请确认前序角色是否已提交。';
  }
}

function submitTask() {
  if (!task.value) return;
  message.value = '';
  errorMessage.value = '';
  try {
    store.submitStudentTask(task.value.id, { ...submissionValues });
    message.value =
      '答卷已提交，系统客观分已经生成；教师完成主观评分后会推送完整结果。';
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '提交失败，请检查阶段完成情况。';
  }
}

function resetLocalAttempt() {
  checks.entered = false;
  checks.located = false;
  checks.reviewed = false;
  message.value = '已重新开始当前未提交阶段，平台不会回退已经完成的业务数据。';
  errorMessage.value = '';
}

function restartAttempt() {
  if (!task.value) return;
  const participantCount = collaborationTasks.value.length;
  const confirmed = window.confirm(
    `重新作答将调用 Mock 业务接口生成一条同单位的新业务数据，并重置共享该数据的 ${participantCount} 个学员任务；旧业务数据不会回滚。确认继续吗？`
  );
  if (!confirmed) return;
  message.value = '';
  errorMessage.value = '';
  attemptMessage.value = '';
  try {
    store.restartStudentAttempt(task.value.id);
    checks.entered = false;
    checks.located = false;
    checks.reviewed = false;
    syncSubmissionValues();
    attemptMessage.value =
      '已生成新的同条件业务数据，协作单元已整体进入下一次作答；旧数据仅停用，不执行回滚。';
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '重新作答失败，请稍后重试。';
  }
}
</script>

<template>
  <section v-if="task && lesson" class="runner-page">
    <header class="runner-header">
      <div>
        <RouterLink to="/student/tasks">← 返回任务中心</RouterLink>
        <span class="runner-divider"></span>
        <strong>{{ task.title }}</strong>
        <StatusPill
          :status="task.status"
          :label="
            task.status === 'TODO'
              ? '待开始'
              : task.status === 'DOING'
                ? '办理中'
                : task.status === 'SUBMITTED'
                  ? '已提交'
                  : '已评分'
          "
        />
      </div>
      <div>
        <span>学员 {{ task.studentName }}</span>
        <span>角色 {{ roleNames?.join(' / ') }}</span>
        <span>
          第 {{ task.attemptNumber }} /
          {{ examSettings?.maxAttempts ?? 1 }} 次作答
        </span>
        <button
          v-if="canRestartAttempt"
          type="button"
          class="secondary restart-attempt"
          @click="restartAttempt"
        >
          重新作答并换新数据
        </button>
        <button type="button" class="secondary" @click="showHelp = !showHelp">
          {{ showHelp ? '隐藏操作引导' : '显示操作引导' }}
        </button>
      </div>
    </header>

    <div class="runner-layout" :class="{ 'help-hidden': !showHelp }">
      <aside class="stage-sidebar">
        <span class="runner-kicker">BUSINESS WORKFLOW</span>
        <h1>业务阶段</h1>
        <p>
          各角色按顺序完成同一批业务流程；你仅需办理分配给自己的阶段。
        </p>
        <div v-if="examSettings?.showProgress !== false" class="stage-list">
          <div
            v-for="(stage, index) in visibleStages"
            :key="stage.id"
            :class="{
              assigned: assignedStageIds.has(stage.id),
              complete: task.completedStageIds.includes(stage.id),
              current: stage.id === nextStage?.id
            }"
          >
            <span>
              {{ task.completedStageIds.includes(stage.id) ? '✓' : index + 1 }}
            </span>
            <section>
              <strong>{{ stage.name }}</strong>
              <small>
                {{
                  assignedStageIds.has(stage.id)
                    ? '由我办理'
                    : '等待其他角色办理'
                }}
              </small>
            </section>
          </div>
        </div>
        <div v-else class="hidden-progress">
          <strong>考试进度已隐藏</strong>
          <p>按考试设置，仅显示当前操作引导，不展示完整业务阶段。</p>
        </div>
        <div class="evidence-boundary">
          <strong>当前判定口径</strong>
          <p>
            访问目标页面 + 定位业务待办 + 执行流程按钮，即判定本阶段完成。
          </p>
        </div>
      </aside>

      <main class="business-canvas">
        <div class="mock-browser">
          <div class="browser-bar">
            <div><i></i><i></i><i></i></div>
            <span>业务系统演示窗口 · 非真实业务提交</span>
            <b>安全代理会话</b>
          </div>
          <div class="business-app">
            <nav>
              <strong>协同业务平台</strong>
              <a class="active">我的待办</a>
              <a>业务申请</a>
              <a>审批中心</a>
              <a>归档查询</a>
              <small>当前单位：第一事业部</small>
            </nav>
            <section class="business-content">
              <header>
                <div>
                  <span>业务办理 / 我的待办</span>
                  <h2>{{ nextStage?.name ?? '本角色阶段已完成' }}</h2>
                </div>
                <em>演示模式 · 操作不会写入真实业务系统</em>
              </header>

              <div v-if="task.status === 'TODO'" class="business-start">
                <span>01</span>
                <h3>准备进入业务办理</h3>
                <p>
                  点击开始后，平台将创建本次学员会话轨迹并展示录制好的操作指引。
                </p>
                <button class="primary" type="button" @click="startTask">
                  开始本次任务
                </button>
              </div>

              <div
                v-else-if="task.status === 'SUBMITTED' || task.status === 'GRADED'"
                class="business-start success"
              >
                <span>✓</span>
                <h3>本次答卷已经提交</h3>
                <p>
                  客观分 {{ task.objectiveScore ?? 0 }} 分。教师评分后，可在成绩反馈中查看主观分和批语。
                </p>
                <RouterLink class="button primary" to="/student/results">
                  查看成绩反馈
                </RouterLink>
                <button
                  v-if="canRestartAttempt"
                  class="secondary"
                  type="button"
                  @click="restartAttempt"
                >
                  协作单元尚未全部提交，重新作答
                </button>
              </div>

              <div
                v-else-if="allAssignedStagesComplete"
                class="business-start success"
              >
                <span>✓</span>
                <h3>你负责的业务阶段已全部完成</h3>
                <p>
                  提交答卷后系统将固化客观操作成绩；提交前仍可检查本次流程记录。
                </p>
                <div
                  v-if="examSettings?.submissionFields.length"
                  class="submission-fields"
                >
                  <label
                    v-for="field in examSettings.submissionFields"
                    :key="field.key"
                  >
                    <span>
                      {{ field.label }}
                      <b v-if="field.required">*</b>
                    </span>
                    <input
                      v-model="submissionValues[field.key]"
                      :placeholder="`请输入${field.label}`"
                    />
                  </label>
                </div>
                <button
                  class="primary"
                  type="button"
                  :disabled="!requiredSubmissionComplete"
                  @click="submitTask"
                >
                  提交本次答卷
                </button>
              </div>

              <template v-else>
                <div class="business-search">
                  <label>
                    待办标题
                    <input
                      value="采购业务办理"
                      readonly
                      @focus="checks.entered = true"
                    />
                  </label>
                  <label>
                    业务状态
                    <select @change="checks.entered = true">
                      <option>待办理</option>
                      <option>全部</option>
                    </select>
                  </label>
                  <button
                    class="business-search-button"
                    type="button"
                    @click="
                      checks.entered = true;
                      checks.located = true;
                    "
                  >
                    查询待办
                  </button>
                </div>

                <div class="business-table">
                  <div class="business-table__head">
                    <span>业务编号</span>
                    <span>业务类型</span>
                    <span>发起单位</span>
                    <span>当前环节</span>
                    <span>操作</span>
                  </div>
                  <div
                    class="business-table__row"
                    :class="{ highlighted: checks.located }"
                  >
                    <strong>
                      {{
                        checks.located
                          ? dataItem?.maskedReference ?? 'BUS-****-2801'
                          : '请先查询'
                      }}
                    </strong>
                    <span>采购申请</span>
                    <span>第一事业部</span>
                    <span>{{ nextStage?.name }}</span>
                    <button
                      type="button"
                      :disabled="!checks.located"
                      @click="checks.reviewed = true"
                    >
                      进入办理
                    </button>
                  </div>
                </div>

                <div v-if="checks.reviewed" class="business-form">
                  <div>
                    <span>业务摘要</span>
                    <strong>办公设备集中采购申请</strong>
                    <small>
                      仅展示平台演示字段，不呈现真实业务数据内容。
                    </small>
                  </div>
                  <label>
                    办理意见
                    <textarea
                      rows="3"
                      :placeholder="
                        nextStage?.completionMethod === 'submission'
                          ? '请填写提交说明'
                          : '按录制教案完成流程操作'
                      "
                    ></textarea>
                  </label>
                  <div class="business-form__actions">
                    <button
                      type="button"
                      class="secondary"
                      @click="resetLocalAttempt"
                    >
                      重新进入本阶段
                    </button>
                    <button
                      type="button"
                      class="primary"
                      :disabled="!canRecordCompletion"
                      @click="completeStage"
                    >
                      {{ nextStage?.recordedSteps.at(-1)?.actionLabel || '提交办理' }}
                    </button>
                  </div>
                </div>
              </template>
            </section>
          </div>
        </div>
      </main>

      <aside v-if="showHelp" class="guide-panel">
        <span class="runner-kicker">RECORDED GUIDE</span>
        <h2>{{ nextStage?.name ?? '阶段完成' }}</h2>
        <p>{{ nextStage?.description || '按照录制路径完成当前业务阶段。' }}</p>

        <div v-if="nextStage" class="guide-checks">
          <div :class="{ done: checks.entered }">
            <span>{{ checks.entered ? '✓' : '1' }}</span>
            <p>
              <strong>访问目标页面</strong>
              <small>{{ nextStage.recordedSteps[0]?.pageTitle || '我的待办' }}</small>
            </p>
          </div>
          <div :class="{ done: checks.located }">
            <span>{{ checks.located ? '✓' : '2' }}</span>
            <p>
              <strong>定位业务待办</strong>
              <small>点击“查询待办”出现演示数据</small>
            </p>
          </div>
          <div :class="{ done: checks.reviewed }">
            <span>{{ checks.reviewed ? '✓' : '3' }}</span>
            <p>
              <strong>进入并执行流程</strong>
              <small>
                点击“进入办理”，再执行录制步骤中的提交动作
              </small>
            </p>
          </div>
        </div>

        <div v-if="nextStage?.recordedSteps.length" class="recorded-steps">
          <strong>录制步骤</strong>
          <div
            v-for="(step, index) in nextStage.recordedSteps"
            :key="step.id"
          >
            <span>{{ index + 1 }}</span>
            <p>
              <b>{{ step.title }}</b>
              <small>{{ step.note }}</small>
            </p>
            <em>{{ step.durationSeconds }}s</em>
          </div>
        </div>

        <p v-if="message" class="notice success">{{ message }}</p>
        <p v-if="attemptMessage" class="notice success">
          {{ attemptMessage }}
        </p>
        <p v-if="errorMessage" class="notice danger">{{ errorMessage }}</p>

        <div class="session-info">
          <span>会话证据</span>
          <strong>页面访问</strong>
          <strong>流程按钮</strong>
          <strong>阶段时序</strong>
          <strong>第 {{ task.attemptNumber }} 次作答</strong>
          <small>不包含业务系统具体数据关联证明</small>
        </div>
      </aside>
    </div>
  </section>

  <section v-else class="standalone-state">
    <span>404</span>
    <h1>未找到该学员任务</h1>
    <p>任务可能尚未发布，或演示数据已被重置。</p>
    <RouterLink class="button primary" to="/student/tasks">
      返回任务中心
    </RouterLink>
  </section>
</template>

<style scoped>
.runner-page {
  width: 100%;
  min-height: calc(100vh - 128px);
}

.runner-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
  border: 1px solid #e4e7ee;
  border-radius: 13px;
  background: #fff;
  padding: 11px 14px;
}

.runner-header > div {
  display: flex;
  align-items: center;
  gap: 9px;
}

.runner-header a,
.runner-header span {
  color: #7f899a;
  font-size: 10px;
}

.runner-header strong {
  color: #344056;
  font-size: 12px;
}

.runner-divider {
  width: 1px;
  height: 18px;
  background: #e1e4ea;
}

.runner-header button {
  min-height: 31px;
  font-size: 9px;
}

.runner-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 275px;
  min-height: calc(100vh - 198px);
  overflow: hidden;
  border: 1px solid #e1e5ec;
  border-radius: 15px;
  background: #fff;
  box-shadow: 0 12px 30px rgb(30 36 79 / 5%);
}

.runner-layout.help-hidden {
  grid-template-columns: 220px minmax(0, 1fr);
}

.stage-sidebar,
.guide-panel {
  padding: 19px;
}

.stage-sidebar {
  border-right: 1px solid #e8ebf1;
  background: #fafbfc;
}

.runner-kicker {
  color: var(--purple);
  font-size: 8px;
  font-weight: 900;
  letter-spacing: 0.15em;
}

.stage-sidebar h1,
.guide-panel h2 {
  margin: 7px 0 5px;
  color: #2f3b50;
  font-size: 17px;
}

.stage-sidebar > p,
.guide-panel > p {
  margin: 0;
  color: #8a94a5;
  font-size: 9px;
  line-height: 1.65;
}

.stage-list {
  display: grid;
  margin-top: 18px;
}

.stage-list > div {
  position: relative;
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 9px;
  min-height: 63px;
  opacity: 0.5;
}

.stage-list > div::before {
  position: absolute;
  top: 30px;
  bottom: 0;
  left: 13px;
  width: 1px;
  background: #dce0e8;
  content: "";
}

.stage-list > div:last-child::before {
  display: none;
}

.stage-list > div.assigned,
.stage-list > div.complete {
  opacity: 1;
}

.stage-list > div > span {
  position: relative;
  z-index: 1;
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border: 1px solid #d9dee7;
  border-radius: 9px;
  color: #8490a2;
  background: #fff;
  font-size: 9px;
  font-weight: 900;
}

.stage-list > div.current > span {
  border-color: #7161e8;
  color: #fff;
  background: #7161e8;
  box-shadow: 0 0 0 4px rgb(113 97 232 / 10%);
}

.stage-list > div.complete > span {
  border-color: #bee6d7;
  color: #087c59;
  background: #e9f8f2;
}

.stage-list section {
  display: grid;
  align-content: start;
  gap: 4px;
  padding-top: 3px;
}

.stage-list strong {
  font-size: 10px;
}

.stage-list small {
  color: #8993a4;
  font-size: 8px;
}

.evidence-boundary {
  margin-top: 20px;
  border: 1px solid #e0dcff;
  border-radius: 10px;
  padding: 10px;
  background: #f5f3ff;
}

.hidden-progress {
  margin-top: 18px;
  border: 1px dashed #d8dce5;
  border-radius: 10px;
  padding: 13px;
  background: #fff;
}

.hidden-progress strong {
  color: #566176;
  font-size: 10px;
}

.hidden-progress p {
  margin: 5px 0 0;
  color: #8a94a5;
  font-size: 8px;
  line-height: 1.55;
}

.evidence-boundary strong {
  color: #5747c3;
  font-size: 9px;
}

.evidence-boundary p {
  margin: 4px 0 0;
  color: #776da0;
  font-size: 8px;
  line-height: 1.55;
}

.business-canvas {
  min-width: 0;
  padding: 18px;
  background:
    radial-gradient(circle at 50% 20%, rgb(109 93 252 / 7%), transparent 25%),
    #f1f3f7;
}

.mock-browser {
  height: 100%;
  min-height: 600px;
  overflow: hidden;
  border: 1px solid #d8dde6;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 15px 35px rgb(32 39 74 / 10%);
}

.browser-bar {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 37px;
  border-bottom: 1px solid #dde1e8;
  background: #f3f4f7;
  padding: 0 12px;
}

.browser-bar > div {
  display: flex;
  gap: 5px;
}

.browser-bar i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #cbd0d9;
}

.browser-bar span {
  overflow: hidden;
  border: 1px solid #e0e3e9;
  border-radius: 6px;
  padding: 5px 8px;
  color: #8992a1;
  background: #fff;
  font-size: 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.browser-bar b {
  color: #19815f;
  font-size: 8px;
}

.business-app {
  display: grid;
  grid-template-columns: 125px minmax(0, 1fr);
  min-height: 563px;
}

.business-app > nav {
  display: grid;
  grid-auto-rows: 36px;
  align-content: start;
  border-right: 1px solid #e7e9ee;
  background: #222d42;
  padding: 16px 9px;
}

.business-app nav strong {
  margin-bottom: 15px;
  color: #fff;
  font-size: 11px;
}

.business-app nav a {
  display: flex;
  align-items: center;
  border-radius: 6px;
  padding: 0 8px;
  color: #aeb7c7;
  font-size: 9px;
}

.business-app nav a.active {
  color: #fff;
  background: #5364c9;
}

.business-app nav small {
  margin-top: 20px;
  color: #798499;
  font-size: 7px;
  line-height: 1.5;
}

.business-content {
  min-width: 0;
  padding: 17px;
}

.business-content > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid #eceef2;
  padding-bottom: 12px;
}

.business-content header span,
.business-content header em {
  color: #9099a8;
  font-size: 8px;
  font-style: normal;
}

.business-content header h2 {
  margin: 5px 0 0;
  font-size: 15px;
}

.business-content header em {
  border-radius: 999px;
  padding: 5px 8px;
  color: #896d32;
  background: #fff4de;
}

.business-start {
  display: grid;
  min-height: 420px;
  place-items: center;
  align-content: center;
  gap: 10px;
  text-align: center;
}

.business-start > span {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border-radius: 17px;
  color: #5e4bd4;
  background: #eeebff;
  font-size: 17px;
  font-weight: 900;
}

.business-start.success > span {
  color: #08805c;
  background: #e8f8f2;
}

.business-start h3,
.business-start p {
  margin: 0;
}

.business-start h3 {
  color: #344056;
  font-size: 14px;
}

.business-start p {
  max-width: 390px;
  color: #8993a4;
  font-size: 9px;
  line-height: 1.6;
}

.submission-fields {
  display: grid;
  width: min(440px, 100%);
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
  margin: 6px 0;
  text-align: left;
}

.submission-fields label {
  font-size: 9px;
}

.submission-fields label span {
  color: #657186;
}

.submission-fields label b {
  color: #d64c59;
}

.submission-fields input {
  padding: 8px 9px;
  font-size: 9px;
}

.business-search {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(130px, 0.7fr) auto;
  align-items: end;
  gap: 9px;
  margin-top: 14px;
  border: 1px solid #e7e9ee;
  border-radius: 8px;
  padding: 11px;
  background: #fafbfc;
}

.business-search label,
.business-form label {
  font-size: 8px;
}

.business-search input,
.business-search select,
.business-form textarea {
  border-radius: 5px;
  padding: 7px 8px;
  font-size: 9px;
}

.business-search-button {
  min-height: 31px;
  border-color: #6373ce;
  border-radius: 5px;
  color: #fff;
  background: #6373ce;
  font-size: 8px;
}

.business-table {
  margin-top: 12px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.business-table__head,
.business-table__row {
  display: grid;
  grid-template-columns: 1.2fr 0.9fr 0.9fr 0.9fr 60px;
  align-items: center;
  gap: 8px;
  min-height: 39px;
  padding: 0 10px;
  font-size: 8px;
}

.business-table__head {
  color: #7f8998;
  background: #f4f5f8;
  font-weight: 800;
}

.business-table__row {
  color: #697587;
}

.business-table__row.highlighted {
  background: #fcfbff;
}

.business-table__row strong {
  color: #4f5ac0;
}

.business-table__row button {
  min-height: 25px;
  border: 0;
  padding: 0 7px;
  color: #5261bd;
  background: #edf0ff;
  font-size: 8px;
}

.business-form {
  display: grid;
  gap: 12px;
  margin-top: 12px;
  border: 1px solid #dedafc;
  border-radius: 8px;
  padding: 13px;
  background: #fbfaff;
}

.business-form > div:first-child {
  display: grid;
  gap: 4px;
}

.business-form span,
.business-form small {
  color: #8d86a8;
  font-size: 8px;
}

.business-form strong {
  color: #3f4960;
  font-size: 10px;
}

.business-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.business-form__actions button {
  min-height: 30px;
  font-size: 8px;
}

.guide-panel {
  border-left: 1px solid #e8ebf1;
  background: #fff;
}

.guide-checks {
  display: grid;
  margin-top: 17px;
}

.guide-checks > div {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 9px;
  min-height: 66px;
  opacity: 0.52;
}

.guide-checks > div.done {
  opacity: 1;
}

.guide-checks > div > span {
  display: grid;
  width: 27px;
  height: 27px;
  place-items: center;
  border-radius: 8px;
  color: #7566dc;
  background: #efedff;
  font-size: 9px;
  font-weight: 900;
}

.guide-checks > div.done > span {
  color: #087b59;
  background: #e8f8f2;
}

.guide-checks p,
.recorded-steps p {
  display: grid;
  gap: 3px;
  margin: 0;
}

.guide-checks strong,
.recorded-steps strong,
.recorded-steps b {
  color: #3b475c;
  font-size: 9px;
}

.guide-checks small,
.recorded-steps small {
  color: #8b95a6;
  font-size: 8px;
  line-height: 1.45;
}

.recorded-steps {
  display: grid;
  gap: 8px;
  margin-top: 5px;
  border-top: 1px solid #eceef3;
  padding-top: 15px;
}

.recorded-steps > div {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) auto;
  gap: 7px;
  border-radius: 8px;
  padding: 8px;
  background: #f8f9fb;
}

.recorded-steps > div > span {
  display: grid;
  width: 18px;
  height: 18px;
  place-items: center;
  border-radius: 5px;
  color: #5e4bd0;
  background: #ece9ff;
  font-size: 7px;
}

.recorded-steps em {
  color: #9aa3b1;
  font-size: 7px;
  font-style: normal;
}

.guide-panel .notice {
  font-size: 8px;
  line-height: 1.5;
}

.session-info {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 14px;
  border-top: 1px solid #eceef3;
  padding-top: 14px;
}

.session-info span,
.session-info small {
  width: 100%;
  color: #8a94a5;
  font-size: 8px;
}

.session-info strong {
  border: 1px solid #dcd8ff;
  border-radius: 5px;
  padding: 4px 6px;
  color: #5d4dca;
  background: #f6f4ff;
  font-size: 7px;
}

@media (max-width: 1250px) {
  .runner-layout,
  .runner-layout.help-hidden {
    grid-template-columns: 190px minmax(0, 1fr);
  }

  .guide-panel {
    grid-column: 1 / -1;
    border-top: 1px solid #e8ebf1;
    border-left: 0;
  }

  .guide-checks {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 800px) {
  .runner-header,
  .runner-header > div {
    align-items: flex-start;
    flex-direction: column;
  }

  .runner-layout,
  .runner-layout.help-hidden {
    grid-template-columns: 1fr;
  }

  .stage-sidebar {
    border-right: 0;
    border-bottom: 1px solid #e8ebf1;
  }

  .business-search,
  .guide-checks,
  .submission-fields {
    grid-template-columns: 1fr;
  }

  .browser-bar {
    grid-template-columns: 55px minmax(0, 1fr);
  }

  .browser-bar b {
    display: none;
  }

  .business-app {
    grid-template-columns: 1fr;
  }

  .business-app > nav {
    display: none;
  }
}
</style>
