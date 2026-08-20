<script setup lang="ts">
import {
  computed,
  onMounted,
  reactive,
  ref,
  watch
} from 'vue';
import { useRouter } from 'vue-router';
import MetricCard from '../../components/ui/MetricCard.vue';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusPill from '../../components/ui/StatusPill.vue';
import type { LessonPlan, LessonStatus } from '../../domain/models';
import {
  dataPrepareApi,
  type LessonPlanClassicCaseOption
} from '../../services/trainingApi';
import { useTrainingStore } from '../../stores/trainingStore';

const store = useTrainingStore();
const router = useRouter();
const createDialog = ref<HTMLDialogElement | null>(null);
const keyword = ref('');
const statusFilter = ref<'ALL' | LessonStatus>('ALL');
const moduleFilter = ref('ALL');
const feedback = ref('');
const feedbackTone = ref<'success' | 'danger'>('success');
const publishingLessonId = ref('');
const withdrawingLessonId = ref('');
const creatingLesson = ref(false);
const duplicatingLessonId = ref('');
const deletingLessonId = ref('');
const createForm = reactive({
  code: '',
  title: '',
  businessPlatformId: '',
  businessPlatformModuleId: '',
  generationSource: 'NORMAL' as 'NORMAL' | 'CLASSIC_CASE',
  classicCaseId: '',
  caseVersionId: '',
  generationMode: 'REPLAY_CASE' as 'REPLAY_CASE' | 'FORMAT_DEMO',
  description: ''
});
const classicCaseOptions = ref<LessonPlanClassicCaseOption[]>([]);
const classicCaseLoading = ref(false);
const classicCaseError = ref('');

const statusLabels: Record<LessonStatus, string> = {
  DRAFT: '草稿',
  RECORDED: '已录制',
  PUBLISHED: '已发布',
  ARCHIVED: '已归档'
};

const moduleOptions = computed(() =>
  [...new Set(store.state.lessons.map((lesson) => lesson.moduleName))].sort()
);
const enabledBusinessPlatforms = computed(() =>
  store.state.businessPlatforms.filter(
    (platform) =>
      platform.status === 'ENABLED' &&
      platform.modules.some(
        (businessModule) => businessModule.status === 'ENABLED'
      )
  )
);
const selectedCreatePlatform = computed(() =>
  store.getBusinessPlatform(createForm.businessPlatformId)
);
const enabledBusinessPlatformModules = computed(() =>
  (selectedCreatePlatform.value?.modules ?? []).filter(
    (businessModule) => businessModule.status === 'ENABLED'
  )
);
const selectedCreateModule = computed(() =>
  store.getBusinessPlatformModule(
    createForm.businessPlatformId,
    createForm.businessPlatformModuleId
  )
);
const classicCaseChoices = computed(() => {
  const result = new Map<string, LessonPlanClassicCaseOption>();
  classicCaseOptions.value.forEach((option) => {
    if (!result.has(option.classicCaseId)) result.set(option.classicCaseId, option);
  });
  return [...result.values()];
});
const selectedClassicCaseVersions = computed(() =>
  classicCaseOptions.value.filter(
    (option) => option.classicCaseId === createForm.classicCaseId
  )
);
const selectedClassicCaseOption = computed(() =>
  selectedClassicCaseVersions.value.find(
    (option) => option.caseVersionId === createForm.caseVersionId
  )
);

watch(
  () => createForm.businessPlatformId,
  () => {
    if (
      !enabledBusinessPlatformModules.value.some(
        (businessModule) =>
          businessModule.id === createForm.businessPlatformModuleId
      )
    ) {
      createForm.businessPlatformModuleId =
        enabledBusinessPlatformModules.value[0]?.id ?? '';
    }
  }
);

watch(
  () => [
    createForm.generationSource,
    createForm.businessPlatformId,
    createForm.businessPlatformModuleId
  ],
  () => {
    createForm.classicCaseId = '';
    createForm.caseVersionId = '';
    classicCaseOptions.value = [];
    classicCaseError.value = '';
    if (createForm.generationSource === 'CLASSIC_CASE') {
      void loadClassicCaseOptions();
    }
  }
);

watch(
  () => createForm.classicCaseId,
  () => {
    const option = selectedClassicCaseVersions.value[0];
    createForm.caseVersionId = option?.caseVersionId ?? '';
    createForm.generationMode = option?.defaultGenerationMode ?? 'REPLAY_CASE';
  }
);

watch(
  () => createForm.caseVersionId,
  () => {
    const option = selectedClassicCaseOption.value;
    if (option && !option.supportedGenerationModes.includes(createForm.generationMode)) {
      createForm.generationMode = option.defaultGenerationMode;
    }
  }
);

async function loadClassicCaseOptions() {
  if (
    !store.remote.enabled ||
    !selectedCreateModule.value?.code ||
    !createForm.businessPlatformId
  ) return;
  classicCaseLoading.value = true;
  classicCaseError.value = '';
  try {
    classicCaseOptions.value = await dataPrepareApi.listLessonPlanClassicCaseOptions({
      businessModuleCode: selectedCreateModule.value.code,
      connectorSystemId: createForm.businessPlatformId
    });
  } catch (error) {
    classicCaseError.value = error instanceof Error ? error.message : '经典案例加载失败';
  } finally {
    classicCaseLoading.value = false;
  }
}

const filteredLessons = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return store.state.lessons.filter((lesson) => {
    const matchesKeyword =
      !query ||
      [lesson.title, lesson.code, lesson.moduleName, lesson.teacherName, ...lesson.tags]
        .join(' ')
        .toLowerCase()
        .includes(query);
    const matchesStatus =
      statusFilter.value === 'ALL' || lesson.status === statusFilter.value;
    const matchesModule =
      moduleFilter.value === 'ALL' || lesson.moduleName === moduleFilter.value;
    return matchesKeyword && matchesStatus && matchesModule;
  });
});

const metrics = computed(() => ({
  total: store.state.lessons.length,
  editing: store.state.lessons.filter((lesson) =>
    ['DRAFT', 'RECORDED'].includes(lesson.status)
  ).length,
  published: store.state.lessons.filter((lesson) => lesson.status === 'PUBLISHED').length,
  stages: store.state.lessons.reduce((sum, lesson) => sum + lesson.stages.length, 0)
}));

onMounted(async () => {
  if (!store.remote.enabled) return;
  try {
    await store.syncBusinessPlatforms();
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value =
      error instanceof Error ? error.message : '业务平台同步失败';
  }
});

function dateLabel(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

function openCreateDialog() {
  feedback.value = '';
  createForm.code = `LESSON-${String(store.state.lessons.length + 1).padStart(3, '0')}`;
  createForm.title = '';
  createForm.businessPlatformId =
    enabledBusinessPlatforms.value[0]?.id ?? '';
  createForm.businessPlatformModuleId =
    enabledBusinessPlatformModules.value[0]?.id ?? '';
  createForm.generationSource = 'NORMAL';
  createForm.classicCaseId = '';
  createForm.caseVersionId = '';
  createForm.generationMode = 'REPLAY_CASE';
  classicCaseOptions.value = [];
  classicCaseError.value = '';
  createForm.description = '';
  createDialog.value?.showModal();
}

function closeCreateDialog() {
  createDialog.value?.close();
}

async function createLesson() {
  if (creatingLesson.value) return;
  if (
    !createForm.code.trim() ||
    !createForm.title.trim() ||
    !createForm.businessPlatformId ||
    !createForm.businessPlatformModuleId ||
    (createForm.generationSource === 'CLASSIC_CASE' &&
      (!selectedClassicCaseOption.value || !createForm.generationMode))
  ) {
    feedbackTone.value = 'danger';
    feedback.value = createForm.generationSource === 'CLASSIC_CASE'
      ? '请填写教案信息，并选择可用的经典案例、版本和生成模式。'
      : '请填写教案编号和名称，并选择业务平台及平台模块。';
    return;
  }
  creatingLesson.value = true;
  try {
    const lesson = await store.createLessonRemote({
      code: createForm.code.trim(),
      title: createForm.title.trim(),
      moduleName: selectedCreateModule.value?.name,
      businessPlatformId: createForm.businessPlatformId,
      businessPlatformModuleId: createForm.businessPlatformModuleId,
      generationSource: createForm.generationSource,
      classicCaseConfig:
        createForm.generationSource === 'CLASSIC_CASE' && selectedClassicCaseOption.value
          ? {
              connectorSystemId: selectedClassicCaseOption.value.connectorSystemId,
              classicCaseId: selectedClassicCaseOption.value.classicCaseId,
              caseCode: selectedClassicCaseOption.value.caseCode,
              caseName: selectedClassicCaseOption.value.caseName,
              caseVersionId: selectedClassicCaseOption.value.caseVersionId,
              generationMode: createForm.generationMode
            }
          : undefined,
      description: createForm.description.trim()
    });
    closeCreateDialog();
    await router.push({ name: 'lesson-editor', params: { lessonId: lesson.id } });
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '教案创建失败';
  } finally {
    creatingLesson.value = false;
  }
}

async function duplicateLesson(lesson: LessonPlan) {
  if (duplicatingLessonId.value) return;
  duplicatingLessonId.value = lesson.id;
  try {
    const copied = await store.duplicateLessonRemote(lesson.id);
    feedbackTone.value = 'success';
    feedback.value = `已复制“${lesson.title}”，新教案为“${copied.title}”。`;
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '复制失败';
  } finally {
    duplicatingLessonId.value = '';
  }
}

async function deleteLesson(lesson: LessonPlan) {
  if (deletingLessonId.value) return;
  const confirmed = window.confirm(
    `确认删除教案“${lesson.title}”吗？该教案关联的学习、练习、考试任务及本地配置也会一并移除。`
  );
  if (!confirmed) return;
  deletingLessonId.value = lesson.id;
  try {
    await store.deleteLessonRemote(lesson.id);
    feedbackTone.value = 'success';
    feedback.value = `已删除教案“${lesson.title}”。`;
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '教案删除失败';
  } finally {
    deletingLessonId.value = '';
  }
}

async function publishLesson(lesson: LessonPlan) {
  if (publishingLessonId.value) return;
  publishingLessonId.value = lesson.id;
  try {
    await store.publishLessonRemote(lesson.id);
    feedbackTone.value = 'success';
    feedback.value = store.remote.enabled
      ? `“${lesson.title}”已完成备案，教学点已发布。`
      : `“${lesson.title}”已发布，可进入发布中心。`;
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '发布校验未通过';
  } finally {
    publishingLessonId.value = '';
  }
}

async function withdrawLesson(lesson: LessonPlan) {
  if (withdrawingLessonId.value) return;
  const confirmed = window.confirm(
    `确认撤回教案“${lesson.title}”的发布吗？撤回后可以继续编辑，再次发布后才能下发新任务。`
  );
  if (!confirmed) return;
  withdrawingLessonId.value = lesson.id;
  try {
    await store.withdrawLessonRemote(lesson.id);
    feedbackTone.value = 'success';
    feedback.value = `“${lesson.title}”已撤回发布，可继续编辑。`;
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '撤回发布失败';
  } finally {
    withdrawingLessonId.value = '';
  }
}
</script>

<template>
  <div class="page lesson-list-page">
    <PageHeader
      eyebrow="LESSON AUTHORING"
      title="教案管理"
      description="集中管理业务录制成果，从教案编排直接进入教师讲解与学习、练习发布。"
    >
      <button class="primary" type="button" @click="openCreateDialog">＋ 新建教案</button>
    </PageHeader>

    <section class="metric-grid">
      <MetricCard label="教案总数" :value="metrics.total" hint="覆盖全部业务模块">
        <template #icon>▤</template>
      </MetricCard>
      <MetricCard label="待完善" :value="metrics.editing" hint="草稿与已录制教案" tone="amber">
        <template #icon>✎</template>
      </MetricCard>
      <MetricCard label="已发布" :value="metrics.published" hint="可进入发布中心" tone="green">
        <template #icon>✓</template>
      </MetricCard>
      <MetricCard label="教学点" :value="metrics.stages" hint="动态串联办理角色" tone="blue">
        <template #icon>⌘</template>
      </MetricCard>
    </section>

    <div v-if="feedback" class="notice" :class="feedbackTone">{{ feedback }}</div>

    <section class="card lesson-catalog">
      <div class="catalog-toolbar">
        <label class="search-field">
          <span>搜索教案</span>
          <input v-model="keyword" type="search" placeholder="名称、编号、教师或标签" />
        </label>
        <label>
          <span>状态筛选</span>
          <select v-model="statusFilter">
            <option value="ALL">全部状态</option>
            <option value="DRAFT">草稿</option>
            <option value="RECORDED">已录制</option>
            <option value="PUBLISHED">已发布</option>
            <option value="ARCHIVED">已归档</option>
          </select>
        </label>
        <label>
          <span>业务模块</span>
          <select v-model="moduleFilter">
            <option value="ALL">全部模块</option>
            <option v-for="moduleName in moduleOptions" :key="moduleName" :value="moduleName">
              {{ moduleName }}
            </option>
          </select>
        </label>
        <span class="result-count">共 {{ filteredLessons.length }} 个教案</span>
      </div>

      <div v-if="filteredLessons.length" class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>教案</th>
              <th>业务模块</th>
              <th>业务平台</th>
                  <th>录制与教学点</th>
              <th>版本 / 状态</th>
              <th>最近更新</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="lesson in filteredLessons" :key="lesson.id">
              <td>
                <div class="lesson-name">
                  <strong>{{ lesson.title }}</strong>
                  <span>{{ lesson.code }} · {{ lesson.teacherName }}</span>
                  <div class="tag-line">
                    <small v-for="tag in lesson.tags.slice(0, 3)" :key="tag">{{ tag }}</small>
                  </div>
                </div>
              </td>
              <td>{{ lesson.moduleName }}</td>
              <td class="business-platform-cell">
                <strong>
                  {{ store.getBusinessPlatform(lesson.businessPlatformId)?.name ?? '平台已删除' }}
                </strong>
                <small>
                  {{
                    store.getBusinessPlatformModule(
                      lesson.businessPlatformId,
                      lesson.businessPlatformModuleId
                    )?.name ?? '模块未配置'
                  }}
                  ·
                  {{
                    store.getBusinessPlatformModule(
                      lesson.businessPlatformId,
                      lesson.businessPlatformModuleId
                    )?.path ?? '未配置路径'
                  }}
                </small>
              </td>
              <td>
                    <strong>{{ lesson.stages.length }} 个教学点</strong>
                <span class="subtle">
                  {{ lesson.stages.reduce((sum, stage) => sum + stage.recordedSteps.length, 0) }}
                  个录制步骤
                </span>
              </td>
              <td>
                <div class="version-status">
                  <span>V{{ lesson.version }}</span>
                  <StatusPill :status="lesson.status" :label="statusLabels[lesson.status]" />
                </div>
              </td>
              <td>{{ dateLabel(lesson.updatedAt) }}</td>
              <td>
                <div class="row-actions">
                  <RouterLink
                    class="button secondary compact"
                    :to="{ name: 'lesson-editor', params: { lessonId: lesson.id } }"
                  >
                    编辑
                  </RouterLink>
                  <RouterLink
                    class="button secondary compact"
                    :to="{ name: 'lesson-recording', params: { lessonId: lesson.id } }"
                  >
                    查看录制
                  </RouterLink>
                  <RouterLink
                    class="button secondary compact lesson-actions-trigger"
                    :to="{ name: 'publish-center', params: { lessonId: lesson.id } }"
                  >
                    发布中心
                  </RouterLink>
                  <button
                    class="compact"
                    type="button"
                    :disabled="Boolean(duplicatingLessonId)"
                    @click="duplicateLesson(lesson)"
                  >
                    {{ duplicatingLessonId === lesson.id ? '保存中...' : '复制' }}
                  </button>
                  <button
                    v-if="lesson.status === 'PUBLISHED'"
                    class="compact withdraw-action"
                    type="button"
                    :disabled="Boolean(withdrawingLessonId)"
                    @click="withdrawLesson(lesson)"
                  >
                    {{ withdrawingLessonId === lesson.id ? '撤回中…' : '撤回发布' }}
                  </button>
                  <button
                    class="primary compact"
                    type="button"
                    :disabled="
                      Boolean(publishingLessonId) ||
                      lesson.status === 'ARCHIVED' ||
                      (lesson.status === 'PUBLISHED' &&
                        (!store.remote.enabled || Boolean(lesson.teachingPointId)))
                    "
                    @click="publishLesson(lesson)"
                  >
                    {{
                      publishingLessonId === lesson.id
                        ? '同步中…'
                        : lesson.status === 'PUBLISHED' &&
                            (!store.remote.enabled || lesson.teachingPointId)
                          ? '已备案'
                          : lesson.status === 'PUBLISHED'
                            ? '同步备案'
                            : '备案并发布'
                    }}
                  </button>
                  <button
                    class="compact danger-action"
                    type="button"
                    :disabled="Boolean(deletingLessonId)"
                    @click="deleteLesson(lesson)"
                  >
                    {{ deletingLessonId === lesson.id ? '删除中…' : '删除' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="empty-state">
        <div><strong>没有匹配的教案</strong><br />调整搜索词或筛选条件后再试。</div>
      </div>
    </section>

    <dialog
      ref="createDialog"
      class="native-dialog"
      aria-labelledby="create-lesson-title"
      @cancel="closeCreateDialog"
    >
      <form class="create-lesson-form" method="dialog" @submit.prevent="createLesson">
        <div class="dialog-header create-lesson-header">
          <div>
            <span class="eyebrow">NEW LESSON</span>
            <h2 id="create-lesson-title">新建教案</h2>
            <p>快速完成教案基础配置，创建后即可进入编排</p>
          </div>
          <button class="icon-button" type="button" aria-label="关闭" @click="closeCreateDialog">×</button>
        </div>
        <div class="dialog-body create-lesson-body">
          <div v-if="feedback && feedbackTone === 'danger'" class="notice danger">{{ feedback }}</div>
          <section class="create-lesson-section" aria-labelledby="create-basic-info">
            <div class="create-section-heading">
              <span id="create-basic-info" class="create-section-kicker">01 基本信息</span>
              <p>填写便于检索与识别的教案资料</p>
            </div>
            <div class="form-grid">
              <label>
                <span>教案编号 *</span>
                <input v-model="createForm.code" required />
              </label>
              <label>
                <span>教案名称 *</span>
                <input v-model="createForm.title" required placeholder="例如：采购申请与审批全流程" />
              </label>
              <label class="wide">
                <span>教学简介</span>
                <textarea v-model="createForm.description" rows="3" placeholder="简要说明教学目标与适用场景" />
              </label>
            </div>
          </section>

          <section class="create-lesson-section" aria-labelledby="create-business-context">
            <div class="create-section-heading">
              <span id="create-business-context" class="create-section-kicker">02 业务场景</span>
              <p>选择录制和案例数据所归属的平台模块</p>
            </div>
            <div class="form-grid">
              <label class="wide">
                <span>业务平台 *</span>
                <select v-model="createForm.businessPlatformId" required>
                  <option value="" disabled>请选择录制时打开的业务平台</option>
                  <option
                    v-for="platform in enabledBusinessPlatforms"
                    :key="platform.id"
                    :value="platform.id"
                  >
                    {{ platform.name }} · {{ platform.baseUrl }}
                  </option>
                </select>
                <small v-if="!enabledBusinessPlatforms.length" class="field-warning">
                  暂无已启用业务平台，请管理员先到“业务平台管理”中维护。
                </small>
              </label>
              <label class="wide">
                <span>平台模块 *</span>
                <select
                  v-model="createForm.businessPlatformModuleId"
                  :disabled="!createForm.businessPlatformId"
                  required
                >
                  <option value="" disabled>
                    {{
                      createForm.businessPlatformId
                        ? '请选择录制时进入的平台模块'
                        : '请先选择业务平台'
                    }}
                  </option>
                  <option
                    v-for="businessModule in enabledBusinessPlatformModules"
                    :key="businessModule.id"
                    :value="businessModule.id"
                  >
                    {{ businessModule.name }} · {{ businessModule.path }}
                  </option>
                </select>
                <small
                  v-if="
                    createForm.businessPlatformId &&
                    !enabledBusinessPlatformModules.length
                  "
                  class="field-warning"
                >
                  当前平台暂无已启用模块，请管理员先为该平台新增模块。
                </small>
              </label>
            </div>
          </section>

          <section
            :class="[
              'create-lesson-section generation-section',
              { 'has-classic-case': createForm.generationSource === 'CLASSIC_CASE' }
            ]"
            aria-labelledby="create-generation-source"
          >
            <div class="create-section-heading">
              <span id="create-generation-source" class="create-section-kicker">03 数据生成方式</span>
              <p>使用常规造数，或锁定 OA 推送的经典案例版本</p>
            </div>
            <div class="form-grid">
              <fieldset class="wide generation-source-fieldset">
                <legend class="sr-only">数据生成方式 *</legend>
                <div class="generation-source-grid">
                  <label :class="['generation-source-card', { selected: createForm.generationSource === 'NORMAL' }]">
                    <input v-model="createForm.generationSource" type="radio" value="NORMAL"  style="width: auto;"/>
                    <span><strong>普通数据</strong><small>按模块造数模板生成练习数据</small></span>
                  </label>
                  <label :class="['generation-source-card', { selected: createForm.generationSource === 'CLASSIC_CASE' }]">
                    <input v-model="createForm.generationSource" type="radio" value="CLASSIC_CASE" style="width: auto;" />
                    <span><strong>经典案例</strong><small>锁定 OA 已推送的脱敏案例版本</small></span>
                  </label>
                </div>
              </fieldset>
              <template v-if="createForm.generationSource === 'CLASSIC_CASE'">
                <label class="wide classic-case-field">
                  <span>经典案例 *</span>
                  <select v-model="createForm.classicCaseId" :disabled="classicCaseLoading" required>
                    <option value="" disabled>{{ classicCaseLoading ? '正在加载案例' : '请选择可用经典案例' }}</option>
                    <option v-for="option in classicCaseChoices" :key="option.classicCaseId" :value="option.classicCaseId">
                      {{ option.caseName }} · {{ option.caseCode }}
                    </option>
                  </select>
                  <small v-if="classicCaseError" class="field-warning">{{ classicCaseError }}</small>
                  <small v-else-if="!classicCaseLoading && !classicCaseChoices.length" class="field-warning">
                    当前平台模块暂无可用经典案例，请先由 OA 推送并审核案例。
                  </small>
                </label>
                <label>
                  <span>案例版本 *</span>
                  <select v-model="createForm.caseVersionId" :disabled="!createForm.classicCaseId" required>
                    <option value="" disabled>请选择锁定版本</option>
                    <option v-for="option in selectedClassicCaseVersions" :key="option.caseVersionId" :value="option.caseVersionId">
                      V{{ option.versionNo }} · {{ option.caseVersionId }}
                    </option>
                  </select>
                </label>
                <label>
                  <span>生成模式 *</span>
                  <select v-model="createForm.generationMode" :disabled="!selectedClassicCaseOption" required>
                    <option
                      v-for="mode in selectedClassicCaseOption?.supportedGenerationModes ?? []"
                      :key="mode"
                      :value="mode"
                    >
                      {{ mode === 'REPLAY_CASE' ? '复刻脱敏案例' : '按格式生成 Demo' }}
                    </option>
                  </select>
                </label>
                <div v-if="selectedClassicCaseOption" class="wide classic-case-summary">
                  <strong>{{ selectedClassicCaseOption.caseName }}</strong>
                  <span>{{ selectedClassicCaseOption.summary || '暂无案例摘要' }}</span>
                  <small>保存后锁定版本 {{ selectedClassicCaseOption.caseVersionId }}；浏览器不会提交完整案例内容。</small>
                </div>
              </template>
            </div>
          </section>
        </div>
        <div class="dialog-footer create-lesson-footer">
          <button type="button" :disabled="creatingLesson" @click="closeCreateDialog">取消</button>
          <button class="primary" type="submit" :disabled="creatingLesson">
            {{ creatingLesson ? '正在保存...' : '创建并开始编排' }}
          </button>
        </div>
      </form>
    </dialog>
  </div>
</template>

<style scoped>
.lesson-list-page {
  display: grid;
  gap: 18px;
}

.lesson-list-page :deep(.page-header) {
  margin-bottom: 0;
}

.lesson-catalog {
  overflow: hidden;
}

.catalog-toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 1.4fr) minmax(150px, 0.5fr) minmax(170px, 0.6fr) auto;
  align-items: end;
  gap: 12px;
  border-bottom: 1px solid #eceff5;
  padding: 16px 18px;
}

.result-count {
  padding: 0 0 11px;
  color: #8993a4;
  font-size: 12px;
  white-space: nowrap;
}

.create-lesson-form {
  display: grid;
  max-height: min(90vh, 860px);
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.create-lesson-header {
  align-items: flex-start;
  padding: 22px 26px 18px;
  background: linear-gradient(135deg, #ffffff 0%, #f6f7ff 100%);
}

.create-lesson-header > div {
  display: grid;
  gap: 3px;
}

.create-lesson-header h2 {
  font-size: 22px;
}

.create-lesson-header p,
.create-section-heading p {
  margin: 0;
  color: #7d8798;
}

.create-lesson-header p {
  font-size: 12px;
}

.create-lesson-body {
  display: grid;
  overflow-y: auto;
  gap: 16px;
  padding: 20px 24px 24px;
  background: #f5f7fb;
  scrollbar-gutter: stable;
}

.create-lesson-section {
  display: grid;
  gap: 16px;
  border: 1px solid #e4e8f0;
  border-radius: 14px;
  padding: 18px;
  background: #fff;
  box-shadow: 0 6px 18px rgb(31 42 68 / 5%);
}

.create-lesson-section.generation-section {
  transition: border-color 160ms ease, box-shadow 160ms ease;
}

.create-lesson-section.has-classic-case {
  border-color: #cfc9ff;
  box-shadow: 0 8px 24px rgb(92 75 196 / 10%);
}

.create-section-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #eef1f6;
  padding-bottom: 11px;
}

.create-section-kicker {
  color: #2f3950;
  font-size: 14px;
  font-weight: 900;
  letter-spacing: 0.01em;
}

.create-section-heading p {
  font-size: 11px;
  text-align: right;
}

.create-lesson-section .form-grid {
  gap: 14px;
}

.create-lesson-section .wide {
  grid-column: 1 / -1;
}

.create-lesson-footer {
  padding: 16px 24px;
  background: #fff;
  box-shadow: 0 -8px 20px rgb(31 42 68 / 4%);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  margin: -1px;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
}

.generation-source-fieldset {
  margin: 0;
  border: 0;
  padding: 0;
}

.generation-source-fieldset legend {
  margin-bottom: 8px;
  color: #4a5568;
  font-size: 13px;
}

.generation-source-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.generation-source-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px solid #dfe4ec;
  border-radius: 12px;
  padding: 14px;
  background: #fff;
  cursor: pointer;
  transition: border-color 160ms ease, box-shadow 160ms ease, transform 160ms ease;
}

.generation-source-card.selected {
  border-color: #315efb;
  background: #f4f7ff;
  box-shadow: 0 0 0 3px rgb(49 94 251 / 9%);
  transform: translateY(-1px);
}

.generation-source-card span,
.classic-case-summary {
  display: grid;
  gap: 4px;
}

.generation-source-card small,
.classic-case-summary span,
.classic-case-summary small {
  color: #7d8798;
  font-size: 12px;
}

.classic-case-summary {
  border: 1px solid #d9d4ff;
  border-radius: 12px;
  background: linear-gradient(135deg, #f8f7ff 0%, #f4f7ff 100%);
  padding: 14px;
}

.lesson-name {
  display: grid;
  min-width: 190px;
  gap: 4px;
}

.lesson-name > span,
.subtle {
  display: block;
  color: #8a94a6;
  font-size: 11px;
}

.business-platform-cell {
  max-width: 190px;
}

.business-platform-cell strong,
.business-platform-cell small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.business-platform-cell strong {
  font-size: 11px;
}

.business-platform-cell small {
  max-width: 180px;
  margin-top: 4px;
  color: #8b95a6;
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 8px;
}

.field-warning {
  color: #c27a22;
  font-size: 10px;
  font-weight: 600;
}

.tag-line {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.tag-line small {
  border-radius: 999px;
  padding: 2px 6px;
  color: #6556d7;
  background: #f1efff;
}

.version-status {
  display: flex;
  align-items: center;
  gap: 7px;
}

.version-status > span {
  color: #737e90;
  font-size: 11px;
  font-weight: 800;
}

.row-actions {
  display: flex;
  min-width: 480px;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.compact {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  padding: 0 10px;
  font-size: 11px;
}

.lesson-actions-trigger {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  gap: 6px;
  border: 1px solid #d9d4ff;
  border-radius: 8px;
  padding: 0 10px;
  color: #5d4ed1;
  background: #f7f5ff;
  font-size: 11px;
  font-weight: 800;
  cursor: pointer;
}

.danger-action {
  border-color: #f1c5c9;
  color: #b42332;
  background: #fff7f8;
}

.danger-action:hover:not(:disabled) {
  border-color: #df8d96;
  background: #fff0f2;
}

.withdraw-action {
  border-color: #ecd2a1;
  color: #9a6516;
  background: #fffaf0;
}

.withdraw-action:hover:not(:disabled) {
  border-color: #d9ae61;
  background: #fff4dd;
}

.native-dialog {
  width: min(820px, calc(100vw - 28px));
  max-height: 90vh;
  overflow: hidden;
  border: 0;
  border-radius: 18px;
  padding: 0;
  color: var(--ink);
  box-shadow: 0 30px 80px rgb(15 19 40 / 28%);
}

.native-dialog::backdrop {
  background: rgb(16 20 38 / 52%);
  backdrop-filter: blur(5px);
}

.native-dialog h2 {
  margin: 0;
}

@media (max-width: 960px) {
  .catalog-toolbar {
    grid-template-columns: 1fr 1fr;
  }

  .search-field {
    grid-column: 1 / -1;
  }
}

@media (max-width: 620px) {
  .catalog-toolbar {
    grid-template-columns: 1fr;
  }

  .search-field {
    grid-column: auto;
  }

  .native-dialog {
    width: calc(100vw - 16px);
    max-height: calc(100dvh - 16px);
    border-radius: 14px;
  }

  .create-lesson-form {
    max-height: calc(100dvh - 16px);
  }

  .create-lesson-header,
  .create-lesson-body,
  .create-lesson-footer {
    padding-right: 16px;
    padding-left: 16px;
  }

  .create-lesson-section {
    padding: 15px;
  }

  .create-section-heading {
    display: grid;
    gap: 3px;
  }

  .create-section-heading p {
    text-align: left;
  }

  .generation-source-grid {
    grid-template-columns: 1fr;
  }
}
</style>
