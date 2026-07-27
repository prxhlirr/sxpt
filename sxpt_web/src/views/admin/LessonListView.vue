<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
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
const createForm = reactive({
  code: '',
  title: '',
  moduleName: '',
  description: '',
  objectiveMaxScore: 80,
  subjectiveMaxScore: 20
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
  createForm.moduleName = moduleOptions.value[0] ?? '综合业务';
  createForm.description = '';
  createForm.objectiveMaxScore = 80;
  createForm.subjectiveMaxScore = 20;
  createDialog.value?.showModal();
}

function closeCreateDialog() {
  createDialog.value?.close();
}

async function createLesson() {
  if (!createForm.code.trim() || !createForm.title.trim() || !createForm.moduleName.trim()) {
    feedbackTone.value = 'danger';
    feedback.value = '请填写教案编号、名称和业务模块。';
    return;
  }
  try {
    const lesson = store.createLesson({
      code: createForm.code.trim(),
      title: createForm.title.trim(),
      moduleName: createForm.moduleName.trim(),
      description: createForm.description.trim(),
      objectiveMaxScore: Number(createForm.objectiveMaxScore),
      subjectiveMaxScore: Number(createForm.subjectiveMaxScore)
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

function publishLesson(lesson: LessonPlan) {
  try {
    store.publishLesson(lesson.id);
    feedbackTone.value = 'success';
    feedback.value = `“${lesson.title}”已发布，可继续配置考试与分组。`;
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '发布校验未通过';
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
      <MetricCard label="业务阶段" :value="metrics.stages" hint="动态串联办理角色" tone="blue">
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
              <th>录制与阶段</th>
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
              <td>
                <strong>{{ lesson.stages.length }} 个阶段</strong>
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
                  <RouterLink
                    class="button secondary compact"
                    :to="{ name: 'lesson-editor', params: { lessonId: lesson.id } }"
                  >
                    教案编排
                  </RouterLink>
                  <button class="compact" type="button" @click="duplicateLesson(lesson)">复制</button>
                  <button
                    class="primary compact"
                    type="button"
                    :disabled="lesson.status === 'PUBLISHED' || lesson.status === 'ARCHIVED'"
                    @click="publishLesson(lesson)"
                  >
                    {{ lesson.status === 'PUBLISHED' ? '已发布' : '发布' }}
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
            <label>
              <span>业务模块 *</span>
              <input v-model="createForm.moduleName" required />
            </label>
            <label class="wide">
              <span>教案名称 *</span>
              <input v-model="createForm.title" required placeholder="例如：采购申请与审批全流程" />
            </label>
            <label class="wide">
              <span>教学简介</span>
              <textarea v-model="createForm.description" rows="3" />
            </label>
            <label>
              <span>客观分</span>
              <input v-model.number="createForm.objectiveMaxScore" type="number" min="0" max="100" />
            </label>
            <label>
              <span>主观分</span>
              <input v-model.number="createForm.subjectiveMaxScore" type="number" min="0" max="100" />
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
  min-width: 310px;
  gap: 6px;
}

.compact {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  padding: 0 10px;
  font-size: 11px;
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
