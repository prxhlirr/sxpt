<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusPill from '../../components/ui/StatusPill.vue';
import WorkflowStepper from '../../components/exam/WorkflowStepper.vue';
import {
  getDataActions,
  summarizeExamData,
  type DataAction
} from '../../components/exam/workflow';
import type {
  ExamDataItem,
  RoleGroup,
  UnitDataPlan
} from '../../domain/models';
import { useTrainingStore } from '../../stores/trainingStore';

const route = useRoute();
const router = useRouter();
const store = useTrainingStore();
const lessonId = String(route.params.lessonId);
const lesson = computed(() => store.getLesson(lessonId));
const groupPlan = computed(() => store.state.groupPlans[lessonId]);
const dataItems = computed(() => store.state.dataItems[lessonId] ?? []);
const configurationLocked = computed(() =>
  store.state.publishedTasks.some((task) => task.lessonId === lessonId)
);

function suggestedFormalCount(unitId: string) {
  const plan = groupPlan.value;
  if (!plan) return 1;
  return Math.max(
    1,
    ...plan.roles.map(
      (role) =>
        plan.members.filter(
          (member) => member.unitId === unitId && member.groupKey === role.key
        ).length
    )
  );
}

function defaultPlans(): UnitDataPlan[] {
  const members = groupPlan.value?.members ?? [];
  const units = new Map(
    members.map((member) => [
      member.unitId,
      { unitId: member.unitId, unitName: member.unitName }
    ])
  );
  if (!units.size) {
    units.set('unit-east', { unitId: 'unit-east', unitName: '东区运营中心' });
    units.set('unit-west', { unitId: 'unit-west', unitName: '西区业务中心' });
  }
  return [...units.values()].map((unit) => {
    const formalCount = suggestedFormalCount(unit.unitId);
    return {
      ...unit,
      formalCount,
      spareCount: Math.max(1, Math.ceil(formalCount * 0.25)),
      scenario: '标准业务办理场景',
      simulateFailure: false
    };
  });
}

const unitPlans = ref<UnitDataPlan[]>(
  (store.state.unitDataPlans[lessonId] ?? defaultPlans()).map((plan) => ({
    ...plan
  }))
);
const filters = reactive({
  keyword: '',
  unitId: 'ALL',
  status: 'ALL',
  groupKey: 'ALL'
});
const feedback = ref('');
const generating = ref(false);
const auditItem = ref<ExamDataItem>();
const pendingAction = ref<{ action: DataAction; item: ExamDataItem }>();
const actionReason = ref('');

const summary = computed(() => summarizeExamData(dataItems.value));
const requestedTotal = computed(() =>
  unitPlans.value.reduce(
    (total, plan) => total + Number(plan.formalCount || 0) + Number(plan.spareCount || 0),
    0
  )
);
const failureUnits = computed(
  () => unitPlans.value.filter((plan) => plan.simulateFailure).length
);
const roleGroups = computed<RoleGroup[]>(() => groupPlan.value?.roles ?? []);
const unitOptions = computed(() => {
  const units = new Map<string, string>();
  unitPlans.value.forEach((plan) => units.set(plan.unitId, plan.unitName));
  dataItems.value.forEach((item) => units.set(item.unitId, item.unitName));
  return [...units].map(([id, name]) => ({ id, name }));
});
const filteredItems = computed(() => {
  const selectedRoleUnits =
    filters.groupKey === 'ALL'
      ? undefined
      : new Set(
          (groupPlan.value?.members ?? [])
            .filter((member) => member.groupKey === filters.groupKey)
            .map((member) => member.unitId)
        );
  const keyword = filters.keyword.trim().toLowerCase();
  return dataItems.value.filter((item) => {
    if (filters.unitId !== 'ALL' && item.unitId !== filters.unitId) return false;
    if (filters.status !== 'ALL' && item.status !== filters.status) return false;
    if (selectedRoleUnits && !selectedRoleUnits.has(item.unitId)) return false;
    if (
      keyword &&
      !`${item.maskedReference} ${item.id} ${item.unitName}`
        .toLowerCase()
        .includes(keyword)
    ) {
      return false;
    }
    return true;
  });
});
const coverageRows = computed(() =>
  unitPlans.value.map((plan) => {
    const demand = suggestedFormalCount(plan.unitId);
    const ready = dataItems.value.filter(
      (item) =>
        item.unitId === plan.unitId &&
        item.kind === 'FORMAL' &&
        item.status === 'READY'
    ).length;
    return {
      unitId: plan.unitId,
      unitName: plan.unitName,
      demand,
      ready,
      passed: ready >= demand
    };
  })
);
const dataReady = computed(
  () =>
    coverageRows.value.length > 0 &&
    coverageRows.value.every((row) => row.passed)
);

const statusLabels: Record<ExamDataItem['status'], string> = {
  GENERATING: '生成中',
  READY: '可用',
  GENERATION_FAILED: '生成失败',
  DISABLED: '已禁用',
  IN_USE: '使用中'
};
const kindLabels: Record<ExamDataItem['kind'], string> = {
  FORMAL: '正式',
  SPARE: '备用'
};
const actionLabels: Record<DataAction, string> = {
  retry: '失败重试',
  disable: '禁用数据',
  promote: '备用转正',
  replace: '替换数据'
};

function persistPlans() {
  store.saveUnitDataPlans(
    lessonId,
    unitPlans.value.map((plan) => ({
      ...plan,
      formalCount: Number(plan.formalCount),
      spareCount: Number(plan.spareCount)
    }))
  );
}

function savePlans() {
  feedback.value = '';
  try {
    persistPlans();
    feedback.value = '单位数据生成计划已保存。';
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '数据计划保存失败';
  }
}

function generateData() {
  feedback.value = '';
  generating.value = true;
  try {
    persistPlans();
    const result = store.generateData(lessonId);
    feedback.value = `业务接口 Mock 已完成：计划 ${result.requestedCount} 条，成功 ${result.readyCount} 条，失败 ${result.failedCount} 条。`;
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '考试数据生成失败';
  } finally {
    generating.value = false;
  }
}

function openAction(action: DataAction, item: ExamDataItem) {
  pendingAction.value = { action, item };
  actionReason.value = '';
}

function closeAction() {
  pendingAction.value = undefined;
  actionReason.value = '';
}

function executeAction() {
  if (!pendingAction.value || !actionReason.value.trim()) return;
  const { action, item } = pendingAction.value;
  try {
    if (action === 'retry') {
      store.retryData(lessonId, item.id, actionReason.value.trim());
    } else if (action === 'disable') {
      store.disableData(lessonId, item.id, actionReason.value.trim());
    } else if (action === 'promote') {
      store.promoteData(lessonId, item.id, actionReason.value.trim());
    } else {
      store.replaceData(lessonId, item.id, actionReason.value.trim());
    }
    feedback.value = `已执行「${actionLabels[action]}」，操作记录已写入审计日志。`;
    closeAction();
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '数据操作失败';
  }
}

function goPublish() {
  feedback.value = '';
  try {
    persistPlans();
    void router.push({ name: 'publish-center', params: { lessonId } });
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '数据计划保存失败';
  }
}
</script>

<template>
  <main class="page exam-page">
    <PageHeader
      eyebrow="LESSON BUSINESS CHAIN · 04"
      title="考试数据生成"
      description="按学员单位配置正式与备用数据，通过业务方接口 Mock 生成，并对异常数据执行可审计的调整。"
    >
      <button class="secondary" type="button" @click="router.push({ name: 'group-setup', params: { lessonId } })">
        上一步
      </button>
      <button
        class="primary"
        type="button"
        :disabled="generating || configurationLocked"
        @click="generateData"
      >
        {{ generating ? '正在生成…' : '调用接口生成数据' }}
      </button>
    </PageHeader>

    <WorkflowStepper
      :lesson-id="lessonId"
      current="data"
      aria-label="教案编排、考试设置、分组设置、数据生成、发布任务"
    />
    <div v-if="configurationLocked" class="notice">
      此教案已有考试任务，生成计划已冻结；仍可查看审计记录并处理尚未使用的数据。
    </div>

    <section class="metric-strip four">
      <article><span>计划生成</span><strong>{{ requestedTotal }}</strong><small>{{ unitPlans.length }} 个单位</small></article>
      <article><span>当前可用</span><strong>{{ summary.ready }}</strong><small>正式 {{ summary.readyFormal }} / 备用 {{ summary.readySpare }}</small></article>
      <article :class="{ warning: summary.failed > 0 }"><span>生成异常</span><strong>{{ summary.failed }}</strong><small>{{ failureUnits }} 个单位开启失败模拟</small></article>
      <article :class="{ warning: !dataReady }"><span>可用率</span><strong>{{ summary.availability }}%</strong><small>{{ dataReady ? '正式数据覆盖通过' : '正式数据尚未覆盖' }}</small></article>
    </section>

    <article class="card data-plan-card">
      <div class="card-header">
        <div>
          <h2>单位数据生成计划</h2>
          <p>正式数量至少覆盖该单位人数最多的角色组；备用数据用于重答与异常替换。</p>
        </div>
        <span class="environment-badge"><i /> 业务接口 Mock</span>
      </div>
      <div class="table-wrap plan-table">
        <table>
          <thead>
            <tr>
              <th>学员单位</th>
              <th>需求下限</th>
              <th>正式数量</th>
              <th>备用数量</th>
              <th>业务场景</th>
              <th>失败模拟</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="plan in unitPlans" :key="plan.unitId">
              <td><strong>{{ plan.unitName }}</strong><small>{{ plan.unitId }}</small></td>
              <td><span class="demand-badge">≥ {{ suggestedFormalCount(plan.unitId) }} 条</span></td>
              <td><input v-model.number="plan.formalCount" class="number-input" type="number" min="0" /></td>
              <td><input v-model.number="plan.spareCount" class="number-input" type="number" min="0" /></td>
              <td><input v-model.trim="plan.scenario" /></td>
              <td>
                <label class="switch-label">
                  <input v-model="plan.simulateFailure" type="checkbox" />
                  <span>{{ plan.simulateFailure ? '开启' : '关闭' }}</span>
                </label>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <footer class="card-footer">
        <span>开启 simulateFailure 后，该单位将产生一条失败数据，用于演示重试处理。</span>
        <div class="inline-actions">
          <button
            class="secondary"
            type="button"
            :disabled="configurationLocked"
            @click="savePlans"
          >
            保存计划
          </button>
          <button
            class="primary"
            type="button"
            :disabled="generating || configurationLocked"
            @click="generateData"
          >
            生成 / 重新生成
          </button>
        </div>
      </footer>
    </article>

    <div v-if="feedback" class="notice">{{ feedback }}</div>

    <article class="card data-overview-card">
      <div class="card-header">
        <div>
          <h2>考试数据总览</h2>
          <p>数据不展示真实敏感内容，只保留脱敏引用、状态、版本和操作审计。</p>
        </div>
        <span class="status-pill" :class="dataReady ? 'success' : 'warning'">
          {{ dataReady ? '数据就绪' : '等待补齐' }}
        </span>
      </div>
      <div class="filter-bar">
        <label>
          搜索
          <input v-model.trim="filters.keyword" placeholder="数据编号 / 单位" />
        </label>
        <label>
          单位
          <select v-model="filters.unitId">
            <option value="ALL">全部单位</option>
            <option v-for="unit in unitOptions" :key="unit.id" :value="unit.id">{{ unit.name }}</option>
          </select>
        </label>
        <label>
          状态
          <select v-model="filters.status">
            <option value="ALL">全部状态</option>
            <option value="READY">可用</option>
            <option value="GENERATION_FAILED">生成失败</option>
            <option value="DISABLED">已禁用</option>
            <option value="IN_USE">使用中</option>
          </select>
        </label>
        <label>
          适用角色组
          <select v-model="filters.groupKey">
            <option value="ALL">全部角色</option>
            <option v-for="role in roleGroups" :key="role.key" :value="role.key">{{ role.name }}</option>
          </select>
        </label>
      </div>
      <div v-if="filteredItems.length" class="table-wrap data-table">
        <table>
          <thead>
            <tr>
              <th>脱敏数据引用</th>
              <th>单位</th>
              <th>类型</th>
              <th>状态</th>
              <th>版本</th>
              <th>适用范围</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredItems" :key="item.id">
              <td><strong>{{ item.maskedReference }}</strong><small>{{ item.id }}</small></td>
              <td>{{ item.unitName }}</td>
              <td><span class="kind-pill" :class="item.kind.toLowerCase()">{{ kindLabels[item.kind] }}</span></td>
              <td>
                <StatusPill :status="item.status" :label="statusLabels[item.status]" />
                <small v-if="item.failureReason" class="failure-text">{{ item.failureReason }}</small>
              </td>
              <td>R{{ item.revision }}</td>
              <td>{{ roleGroups.map((role) => role.name).join('、') || '本单位全部角色' }}</td>
              <td>
                <div class="inline-actions compact">
                  <button class="link-button" type="button" @click="auditItem = item">审计详情</button>
                  <button
                    v-for="action in getDataActions(item)"
                    :key="action"
                    class="link-button"
                    :class="{ danger: action === 'disable' }"
                    type="button"
                    @click="openAction(action, item)"
                  >
                    {{ actionLabels[action] }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="empty-state">
        <div>
          <strong>{{ dataItems.length ? '未找到匹配数据' : '尚未生成考试数据' }}</strong>
          <p>{{ dataItems.length ? '请调整筛选条件。' : '保存单位计划后调用业务接口 Mock 生成。' }}</p>
        </div>
      </div>
    </article>

    <article class="card coverage-card">
      <div class="card-header">
        <div><h2>正式数据覆盖检查</h2><p>按单位比较可用正式数据与人数最多角色组的需求量。</p></div>
      </div>
      <div class="coverage-list">
        <div v-for="row in coverageRows" :key="row.unitId" :class="{ passed: row.passed }">
          <span class="coverage-icon">{{ row.passed ? '✓' : '!' }}</span>
          <span><strong>{{ row.unitName }}</strong><small>可用 {{ row.ready }} 条 / 至少 {{ row.demand }} 条</small></span>
          <b>{{ row.passed ? '已覆盖' : `缺少 ${Math.max(0, row.demand - row.ready)} 条` }}</b>
        </div>
      </div>
    </article>

    <div class="bottom-actions">
      <button class="secondary" type="button" @click="router.push({ name: 'group-setup', params: { lessonId } })">返回分组</button>
      <button class="primary" type="button" :disabled="!dataReady" @click="goPublish">下一步：发布任务 →</button>
    </div>

    <div v-if="pendingAction" class="dialog-backdrop" @click.self="closeAction">
      <section class="dialog action-dialog">
        <header class="dialog-header">
          <div>
            <h2>{{ actionLabels[pendingAction.action] }}</h2>
            <small>{{ pendingAction.item.maskedReference }} · R{{ pendingAction.item.revision }}</small>
          </div>
          <button class="icon-action" type="button" @click="closeAction">×</button>
        </header>
        <div class="dialog-body">
          <div class="notice">
            此操作不会直接修改业务库，由实训平台调用业务方接口完成，并记录操作人、时间和原因。
          </div>
          <label class="field">
            操作原因
            <textarea v-model.trim="actionReason" rows="4" placeholder="请填写本次调整的业务原因，不能为空" />
          </label>
        </div>
        <footer class="dialog-footer">
          <button class="secondary" type="button" @click="closeAction">取消</button>
          <button class="primary" type="button" :disabled="!actionReason.trim()" @click="executeAction">确认执行</button>
        </footer>
      </section>
    </div>

    <div v-if="auditItem" class="drawer-backdrop" @click.self="auditItem = undefined">
      <aside class="audit-drawer">
        <header>
          <div><span class="eyebrow">DATA AUDIT</span><h2>审计详情</h2><p>{{ auditItem.maskedReference }}</p></div>
          <button class="icon-action" type="button" @click="auditItem = undefined">×</button>
        </header>
        <section class="audit-meta">
          <div><span>数据状态</span><StatusPill :status="auditItem.status" :label="statusLabels[auditItem.status]" /></div>
          <div><span>所属单位</span><strong>{{ auditItem.unitName }}</strong></div>
          <div><span>当前版本</span><strong>R{{ auditItem.revision }}</strong></div>
          <div><span>数据类型</span><strong>{{ kindLabels[auditItem.kind] }}</strong></div>
        </section>
        <section class="audit-timeline">
          <article v-for="event in auditItem.audit" :key="event.id">
            <i />
            <span><strong>{{ event.type }}</strong><small>{{ event.at }}</small><p>{{ event.reason || '系统自动记录' }}</p></span>
          </article>
          <div v-if="!auditItem.audit.length" class="inline-empty">暂无审计事件。</div>
        </section>
      </aside>
    </div>
  </main>
</template>
