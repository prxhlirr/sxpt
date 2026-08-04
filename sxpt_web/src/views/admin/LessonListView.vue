<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
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
const activeActionsLessonId = ref('');
const actionsMenu = ref<HTMLElement | null>(null);
const actionsMenuTriggers = new Map<string, HTMLElement>();
const actionsMenuStyle = reactive({
  top: '0px',
  left: '0px',
  visibility: 'hidden' as 'hidden' | 'visible'
});
const createForm = reactive({
  code: '',
  title: '',
  businessPlatformId: '',
  businessPlatformModuleId: '',
  description: ''
});

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

onMounted(() => {
  document.addEventListener('pointerdown', handleOutsideActionsMenu);
  document.addEventListener('keydown', handleActionsMenuKeydown);
  window.addEventListener('resize', positionActionsMenu);
  window.addEventListener('scroll', positionActionsMenu, true);
});

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleOutsideActionsMenu);
  document.removeEventListener('keydown', handleActionsMenuKeydown);
  window.removeEventListener('resize', positionActionsMenu);
  window.removeEventListener('scroll', positionActionsMenu, true);
});

watch(filteredLessons, (lessons) => {
  if (
    activeActionsLessonId.value &&
    !lessons.some((lesson) => lesson.id === activeActionsLessonId.value)
  ) {
    closeActionsMenu();
  }
});

function setActionsMenuTrigger(lessonId: string, element: unknown) {
  if (element instanceof HTMLElement) {
    actionsMenuTriggers.set(lessonId, element);
  } else {
    actionsMenuTriggers.delete(lessonId);
  }
}

function toggleActionsMenu(lessonId: string) {
  if (activeActionsLessonId.value === lessonId) {
    closeActionsMenu();
    return;
  }
  activeActionsLessonId.value = lessonId;
  actionsMenuStyle.visibility = 'hidden';
  void nextTick(positionActionsMenu);
}

function positionActionsMenu() {
  if (!activeActionsLessonId.value) return;
  const trigger = actionsMenuTriggers.get(activeActionsLessonId.value);
  const menu = actionsMenu.value;
  if (!trigger || !menu) return;

  const viewportPadding = 12;
  const gap = 7;
  const triggerRect = trigger.getBoundingClientRect();
  const menuWidth = Math.min(280, window.innerWidth - viewportPadding * 2);
  const menuHeight = menu.offsetHeight;
  const left = Math.min(
    window.innerWidth - menuWidth - viewportPadding,
    Math.max(viewportPadding, triggerRect.right - menuWidth)
  );
  const spaceBelow = window.innerHeight - triggerRect.bottom - viewportPadding;
  const openAbove =
    spaceBelow < menuHeight + gap &&
    triggerRect.top - viewportPadding >= menuHeight + gap;
  const top = openAbove
    ? triggerRect.top - menuHeight - gap
    : Math.min(
        triggerRect.bottom + gap,
        window.innerHeight - menuHeight - viewportPadding
      );

  actionsMenuStyle.left = `${Math.round(left)}px`;
  actionsMenuStyle.top = `${Math.max(viewportPadding, Math.round(top))}px`;
  actionsMenuStyle.visibility = 'visible';
}

function closeActionsMenu() {
  activeActionsLessonId.value = '';
  actionsMenuStyle.visibility = 'hidden';
}

function handleOutsideActionsMenu(event: PointerEvent) {
  const target = event.target;
  if (!(target instanceof Node)) return;
  if (actionsMenu.value?.contains(target)) return;
  if (
    activeActionsLessonId.value &&
    actionsMenuTriggers.get(activeActionsLessonId.value)?.contains(target)
  ) {
    return;
  }
  closeActionsMenu();
}

function handleActionsMenuKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') closeActionsMenu();
}

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
  createForm.description = '';
  createDialog.value?.showModal();
}

function closeCreateDialog() {
  createDialog.value?.close();
}

async function createLesson() {
  if (
    !createForm.code.trim() ||
    !createForm.title.trim() ||
    !createForm.businessPlatformId ||
    !createForm.businessPlatformModuleId
  ) {
    feedbackTone.value = 'danger';
    feedback.value = '请填写教案编号和名称，并选择业务平台及平台模块。';
    return;
  }
  try {
    const lesson = store.createLesson({
      code: createForm.code.trim(),
      title: createForm.title.trim(),
      moduleName: selectedCreateModule.value?.name,
      businessPlatformId: createForm.businessPlatformId,
      businessPlatformModuleId: createForm.businessPlatformModuleId,
      description: createForm.description.trim()
    });
    closeCreateDialog();
    await router.push({ name: 'lesson-editor', params: { lessonId: lesson.id } });
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '教案创建失败';
  }
}

function duplicateLesson(lesson: LessonPlan) {
  try {
    const copied = store.duplicateLesson(lesson.id);
    feedbackTone.value = 'success';
    feedback.value = `已复制“${lesson.title}”，新教案为“${copied.title}”。`;
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '复制失败';
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
      : `“${lesson.title}”已发布，可继续配置考试与分组。`;
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '发布校验未通过';
  } finally {
    publishingLessonId.value = '';
  }
}
</script>

<template>
  <div class="page lesson-list-page">
    <PageHeader
      eyebrow="LESSON AUTHORING"
      title="教案管理"
      description="集中管理业务录制成果，从教案编排一路进入考试、分组、数据与发布。"
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
      <MetricCard label="已发布" :value="metrics.published" hint="可进入考试设置" tone="green">
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
                    :to="{ name: 'lesson-recording', params: { lessonId: lesson.id } }"
                  >
                    查看录制
                  </RouterLink>
                  <button
                    :ref="(element) => setActionsMenuTrigger(lesson.id, element)"
                    class="lesson-actions-trigger"
                    type="button"
                    aria-haspopup="menu"
                    :aria-expanded="activeActionsLessonId === lesson.id"
                    @click.stop="toggleActionsMenu(lesson.id)"
                  >
                    业务配置
                    <span aria-hidden="true">⌄</span>
                  </button>
                  <button class="compact" type="button" @click="duplicateLesson(lesson)">复制</button>
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

    <Teleport to="body">
      <div
        v-if="activeActionsLessonId"
        ref="actionsMenu"
        class="lesson-actions-popover"
        :style="actionsMenuStyle"
        role="menu"
        aria-label="教案业务配置"
      >
        <RouterLink
          :to="{
            name: 'lesson-editor',
            params: { lessonId: activeActionsLessonId }
          }"
          role="menuitem"
          @click="closeActionsMenu"
        >
          <span>⌘</span>
              <span><strong>录制与编排教案</strong><small>业务界面录制、教学点与节点</small></span>
        </RouterLink>
        <RouterLink
          :to="{ name: 'exam-setup', params: { lessonId: activeActionsLessonId } }"
          role="menuitem"
          @click="closeActionsMenu"
        >
          <span>◫</span>
          <span><strong>考试设置</strong><small>时间、计分与提交规则</small></span>
        </RouterLink>
        <RouterLink
          :to="{ name: 'group-setup', params: { lessonId: activeActionsLessonId } }"
          role="menuitem"
          @click="closeActionsMenu"
        >
          <span>♟</span>
          <span><strong>分组设置</strong><small>角色分组与成员安排</small></span>
        </RouterLink>
        <RouterLink
          :to="{ name: 'exam-data', params: { lessonId: activeActionsLessonId } }"
          role="menuitem"
          @click="closeActionsMenu"
        >
          <span>◈</span>
          <span><strong>考试数据</strong><small>生成、检查与替换数据</small></span>
        </RouterLink>
        <RouterLink
          :to="{ name: 'publish-center', params: { lessonId: activeActionsLessonId } }"
          role="menuitem"
          @click="closeActionsMenu"
        >
          <span>↗</span>
          <span><strong>发布中心</strong><small>就绪检查与任务发布</small></span>
        </RouterLink>
      </div>
    </Teleport>

    <dialog ref="createDialog" class="native-dialog" @cancel="closeCreateDialog">
      <form method="dialog" @submit.prevent="createLesson">
        <div class="dialog-header">
          <div>
            <span class="eyebrow">NEW LESSON</span>
            <h2>新建教案</h2>
          </div>
          <button class="icon-button" type="button" aria-label="关闭" @click="closeCreateDialog">×</button>
        </div>
        <div class="dialog-body">
          <div v-if="feedback && feedbackTone === 'danger'" class="notice danger">{{ feedback }}</div>
          <div class="form-grid">
            <label>
              <span>教案编号 *</span>
              <input v-model="createForm.code" required />
            </label>
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
            <label class="wide">
              <span>教案名称 *</span>
              <input v-model="createForm.title" required placeholder="例如：采购申请与审批全流程" />
            </label>
            <label class="wide">
              <span>教学简介</span>
              <textarea v-model="createForm.description" rows="3" />
            </label>
          </div>
        </div>
        <div class="dialog-footer">
          <button type="button" @click="closeCreateDialog">取消</button>
          <button class="primary" type="submit">创建并开始编排</button>
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
  min-width: 330px;
  align-items: center;
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

.lesson-actions-trigger[aria-expanded="true"] {
  border-color: #8679ed;
  background: #eeeaff;
}

.lesson-actions-popover {
  position: fixed;
  z-index: 3000;
  display: grid;
  width: min(280px, calc(100vw - 24px));
  max-height: calc(100vh - 24px);
  overflow: auto;
  overscroll-behavior: contain;
  border: 1px solid #e0e3eb;
  border-radius: 11px;
  padding: 6px;
  background: #fff;
  box-shadow: 0 18px 45px rgb(27 32 65 / 18%);
}

.lesson-actions-popover a {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  border-radius: 8px;
  padding: 8px;
}

.lesson-actions-popover a:hover,
.lesson-actions-popover a:focus-visible {
  outline: 0;
  background: #f5f3ff;
}

.lesson-actions-popover a > span:first-child {
  display: grid;
  width: 29px;
  height: 29px;
  place-items: center;
  border-radius: 8px;
  color: #6555d8;
  background: #eeebff;
  font-size: 12px;
}

.lesson-actions-popover a > span:last-child {
  display: grid;
  gap: 2px;
}

.lesson-actions-popover strong {
  color: #3f4a5d;
  font-size: 10px;
}

.lesson-actions-popover small {
  color: #8b95a5;
  font-size: 8px;
}

.native-dialog {
  width: min(680px, calc(100vw - 28px));
  max-height: 92vh;
  overflow: auto;
  border: 0;
  border-radius: 16px;
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
}
</style>
