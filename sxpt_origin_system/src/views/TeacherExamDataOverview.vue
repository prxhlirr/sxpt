<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { createApiLogStore } from '../api/apiLogStore';
import { createAuthenticatedApiClient } from '../api/v1/authenticatedApiClient';
import { createRunBatchApiFromConfig } from '../api/v1/runBatchApiFactory';
import RunDataItemDrawer from '../components/run-batch/RunDataItemDrawer.vue';
import RunDataTable from '../components/run-batch/RunDataTable.vue';
import { trainingConfig } from '../config/trainingConfig';
import { createRunDataPool } from '../composables/useRunDataPool';
import type {
  RunDataFilter,
  RunDataItem,
  RunDataItemStatus,
  WorkflowRunBatch
} from '../types/runBatch';

type ItemAction = 'disable' | 'promote' | 'replace' | 'retry';

const route = useRoute();
const router = useRouter();
const apiLogStore = createApiLogStore();
const authenticatedClient = createAuthenticatedApiClient(
  trainingConfig,
  apiLogStore
);
const runBatchApi = createRunBatchApiFromConfig(
  trainingConfig,
  authenticatedClient
);
const pool = createRunDataPool(runBatchApi);

const batch = ref<WorkflowRunBatch | null>(null);
const notice = ref('');
const pageLoading = ref(false);
const unitId = ref('');
const status = ref<RunDataItemStatus | ''>('');
const kind = ref<'FORMAL' | 'SPARE' | ''>('');
const pendingAction = ref<{ action: ItemAction; item: RunDataItem } | null>(null);
const reason = ref('');
const replacementJson = ref('{}');
const confirmationError = ref('');

const batchId = computed(() => String(route.params.batchId ?? '').trim());
const pageResult = computed(() => pool.pageResult.value);
const totalPages = computed(() => Math.max(
  1,
  Math.ceil((pageResult.value?.total ?? 0) / (pageResult.value?.pageSize ?? 20))
));
const title = computed(() =>
  batch.value?.runType === 'PRACTICE' ? '练习数据总览' : '考试数据总览'
);
const routeRunType = computed(() =>
  route.path.includes('/practices/') ? 'PRACTICE' : 'EXAM'
);
const loadedRunTypeMismatch = computed(() =>
  Boolean(batch.value && batch.value.runType !== routeRunType.value)
);
const readablePoolError = computed(() => readableError(pool.error.value));
const statusOptions: RunDataItemStatus[] = [
  'GENERATING',
  'READY',
  'IN_USE',
  'RETIRED',
  'GENERATION_FAILED',
  'QUARANTINED',
  'DISABLED'
];
let loadVersion = 0;

watch(batchId, (nextBatchId) => {
  void loadOverview(nextBatchId);
}, { immediate: true });

async function loadOverview(nextBatchId: string) {
  const version = ++loadVersion;
  batch.value = null;
  if (!nextBatchId) {
    pageLoading.value = false;
    notice.value = '路由中缺少 batchId，无法读取数据池。';
    return;
  }
  pageLoading.value = true;
  try {
    const [loaded] = await Promise.all([
      runBatchApi.getBatch(nextBatchId),
      pool.load(nextBatchId, buildFilter(1))
    ]);
    if (version !== loadVersion) return;
    batch.value = loaded;
    notice.value = '已从 API 加载批次与数据池最新状态。';
  } catch (error) {
    if (version === loadVersion) notice.value = readableError(error);
  } finally {
    if (version === loadVersion) pageLoading.value = false;
  }
}

async function applyFilter(page = 1) {
  try {
    await pool.setFilter(buildFilter(page));
  } catch (error) {
    notice.value = readableError(error);
  }
}

function buildFilter(page: number): RunDataFilter {
  return {
    page,
    pageSize: pool.filter.value.pageSize ?? 20,
    ...(unitId.value ? { unitId: unitId.value } : {}),
    ...(status.value ? { status: status.value } : {}),
    ...(kind.value ? { kind: kind.value } : {})
  };
}

async function selectItem(itemId: string) {
  try {
    await pool.select(itemId);
  } catch (error) {
    notice.value = readableError(error);
  }
}

async function refreshPage() {
  try {
    await pool.refreshPage();
    notice.value = '已刷新数据池。';
  } catch (error) {
    notice.value = readableError(error);
  }
}

function openAction(payload: { action: ItemAction; item: RunDataItem }) {
  if (!isActionAllowed(payload.action, payload.item)) {
    notice.value = '当前条目状态不允许执行该操作，请刷新后重试。';
    return;
  }
  pendingAction.value = payload;
  reason.value = '';
  replacementJson.value = JSON.stringify(
    payload.item.generationParameters,
    null,
    2
  );
  confirmationError.value = '';
}

function closeAction() {
  if (pool.mutating.value) return;
  pendingAction.value = null;
  reason.value = '';
  confirmationError.value = '';
}

async function confirmAction() {
  const pending = pendingAction.value;
  if (!pending) return;
  if (!reason.value.trim()) {
    confirmationError.value = '请输入本次操作原因。';
    return;
  }
  confirmationError.value = '';
  try {
    let result: RunDataItem;
    if (pending.action === 'disable') {
      result = await pool.disable(pending.item.id, reason.value);
    } else if (pending.action === 'promote') {
      result = await pool.promote(pending.item.id, reason.value);
    } else if (pending.action === 'replace') {
      result = await pool.replace(
        pending.item.id,
        reason.value,
        parseReplacementParameters()
      );
    } else {
      result = await pool.retry(pending.item.id, reason.value);
    }
    pendingAction.value = null;
    notice.value = `操作成功，最新条目为 ${result.id}。`;
  } catch (error) {
    confirmationError.value = readableError(error);
  }
}

function parseReplacementParameters(): Record<string, unknown> {
  const parsed = JSON.parse(replacementJson.value) as unknown;
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('替换参数必须是 JSON 对象。');
  }
  return parsed as Record<string, unknown>;
}

function isActionAllowed(action: ItemAction, item: RunDataItem) {
  if (action === 'disable' || action === 'replace') {
    return item.status === 'READY';
  }
  if (action === 'promote') {
    return item.kind === 'SPARE' && item.status === 'READY';
  }
  return item.status === 'GENERATION_FAILED';
}

function actionTitle(action: ItemAction) {
  return {
    disable: '停用未使用数据',
    promote: '备用数据转正式',
    replace: '替换未使用数据',
    retry: '重试失败造数'
  }[action];
}

async function goToCanonicalOverview() {
  const current = batch.value;
  if (!current) return;
  await router.replace(
    current.runType === 'EXAM'
      ? `/teacher/exams/${encodeURIComponent(current.id)}/data`
      : `/teacher/practices/${encodeURIComponent(current.id)}/data`
  );
}

async function returnToBatch() {
  const current = batch.value;
  if (!current) return;
  await router.push({
    path: current.runType === 'EXAM'
      ? '/teacher/exams/new'
      : '/teacher/practices/new',
    query: { batchId: current.id }
  });
}

function readableError(error: unknown) {
  if (!error) return '';
  if (error instanceof SyntaxError) return '替换参数不是有效 JSON。';
  if (error instanceof Error) return error.message;
  return typeof error === 'string' ? error : '操作失败，请稍后重试。';
}
</script>

<template>
  <main class="overview-page">
    <header class="page-hero">
      <div>
        <p class="hero-kicker">RUN DATA POOL</p>
        <h1>{{ title }}</h1>
        <p>查看正式与备用数据状态、脱敏业务引用、生成修订及审计历史，并按状态门禁执行数据调整。</p>
      </div>
      <div class="hero-meta">
        <span>{{ batch?.runType ?? 'LOADING' }}</span>
        <strong>{{ batch?.status ?? '—' }}</strong>
        <small>{{ batch?.id ?? batchId }}</small>
      </div>
    </header>

    <p v-if="notice" class="notice">{{ notice }}</p>
    <p v-if="readablePoolError && readablePoolError !== notice" class="error-banner">{{ readablePoolError }}</p>
    <div v-if="loadedRunTypeMismatch" class="route-warning">
      <span>当前 URL 类型与批次真实类型不一致；页面已按 batch.runType 展示。</span>
      <button type="button" @click="goToCanonicalOverview">转到正确地址</button>
    </div>

    <section class="overview-card">
      <div class="toolbar">
        <button type="button" class="back-button" :disabled="!batch || pool.mutating.value" @click="returnToBatch">返回批次配置</button>
        <label>单位
          <select v-model="unitId" :disabled="pool.loading.value || pool.mutating.value">
            <option value="">全部单位</option>
            <option v-for="plan in batch?.unitPlans ?? []" :key="plan.unitId" :value="plan.unitId">{{ plan.unitId }}</option>
          </select>
        </label>
        <label>数据状态
          <select v-model="status" :disabled="pool.loading.value || pool.mutating.value">
            <option value="">全部状态</option>
            <option v-for="option in statusOptions" :key="option" :value="option">{{ option }}</option>
          </select>
        </label>
        <label>正式/备用
          <select v-model="kind" :disabled="pool.loading.value || pool.mutating.value">
            <option value="">全部类型</option>
            <option value="FORMAL">正式</option>
            <option value="SPARE">备用</option>
          </select>
        </label>
        <button type="button" :disabled="pool.loading.value || pool.mutating.value" @click="applyFilter(1)">应用筛选</button>
        <button type="button" class="secondary" :disabled="pool.loading.value || pool.mutating.value" @click="refreshPage">刷新</button>
      </div>

      <div class="summary-strip">
        <div><span>批次</span><strong>{{ batch?.batchName ?? '—' }}</strong></div>
        <div><span>数据总数</span><strong>{{ pageResult?.total ?? 0 }}</strong></div>
        <div><span>当前页</span><strong>{{ pageResult?.page ?? 1 }} / {{ totalPages }}</strong></div>
        <div><span>业务关联判断</span><strong>平台暂不可可靠判定</strong></div>
      </div>

      <RunDataTable
        :items="pageLoading ? [] : (pageResult?.items ?? [])"
        :loading="pageLoading || pool.loading.value"
        :mutating="pool.mutating.value"
        @select="selectItem"
        @action="openAction"
      />

      <div class="pagination">
        <button type="button" class="secondary" :disabled="pool.loading.value || (pageResult?.page ?? 1) <= 1" @click="applyFilter((pageResult?.page ?? 1) - 1)">上一页</button>
        <span>第 {{ pageResult?.page ?? 1 }} 页，共 {{ totalPages }} 页</span>
        <button type="button" class="secondary" :disabled="pool.loading.value || (pageResult?.page ?? 1) >= totalPages" @click="applyFilter((pageResult?.page ?? 1) + 1)">下一页</button>
      </div>
    </section>

    <RunDataItemDrawer
      :item="pool.selectedItem.value"
      :loading="pool.loading.value && Boolean(pool.selectedItem.value)"
      @close="pool.select(null)"
    />

    <div v-if="pendingAction" class="confirm-backdrop" @click.self="closeAction">
      <section class="confirm-card" role="dialog" aria-modal="true">
        <header><div><span>CONFIRM ACTION</span><h2>{{ actionTitle(pendingAction.action) }}</h2></div><button type="button" class="secondary" :disabled="pool.mutating.value" @click="closeAction">关闭</button></header>
        <p>目标条目：<code>{{ pendingAction.item.id }}</code> · {{ pendingAction.item.status }} · {{ pendingAction.item.kind }}</p>
        <label>操作原因（必填）
          <textarea v-model="reason" rows="3" :disabled="pool.mutating.value" placeholder="说明调整原因，内容将进入审计历史" />
        </label>
        <label v-if="pendingAction.action === 'replace'">新生成参数（JSON 对象）
          <textarea v-model="replacementJson" rows="9" :disabled="pool.mutating.value" />
        </label>
        <p v-if="confirmationError" class="confirm-error">{{ confirmationError }}</p>
        <footer><button type="button" class="secondary" :disabled="pool.mutating.value" @click="closeAction">取消</button><button type="button" :disabled="pool.mutating.value || !reason.trim()" @click="confirmAction">{{ pool.mutating.value ? '提交中…' : '确认执行' }}</button></footer>
      </section>
    </div>
  </main>
</template>

<style scoped>
.overview-page { min-height: 100vh; padding: 30px clamp(14px, 4vw, 56px) 70px; color: #0f172a; background: radial-gradient(circle at top right, #dbeafe 0, transparent 34%), #f8fafc; }
.page-hero { max-width: 1600px; margin: 0 auto 18px; display: flex; justify-content: space-between; gap: 28px; padding: 30px; border-radius: 24px; color: #fff; background: linear-gradient(120deg, #172554, #1e3a8a 52%, #6d28d9); box-shadow: 0 24px 55px rgb(30 58 138 / 20%); }
.hero-kicker { margin: 0; color: #bfdbfe; font-size: 12px; font-weight: 850; letter-spacing: .16em; }
h1 { margin: 8px 0; font-size: clamp(28px, 4vw, 44px); }
.page-hero p:last-child { max-width: 800px; margin-bottom: 0; color: #dbeafe; line-height: 1.7; }
.hero-meta { min-width: 220px; align-self: center; display: grid; gap: 8px; padding: 18px; border: 1px solid rgb(255 255 255 / 20%); border-radius: 18px; background: rgb(255 255 255 / 10%); }
.hero-meta strong { color: #ddd6fe; font-size: 19px; }
.overview-card, .notice, .error-banner, .route-warning { max-width: 1600px; margin-left: auto; margin-right: auto; }
.notice, .error-banner, .route-warning { padding: 11px 14px; border-radius: 12px; }
.notice { color: #1e40af; border: 1px solid #bfdbfe; background: #eff6ff; }
.error-banner { color: #b91c1c; border: 1px solid #fecaca; background: #fef2f2; }
.route-warning { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: #92400e; border: 1px solid #fde68a; background: #fffbeb; }
.overview-card { overflow: hidden; border: 1px solid #e2e8f0; border-radius: 18px; background: #fff; box-shadow: 0 12px 30px rgb(15 23 42 / 6%); }
.toolbar { display: grid; grid-template-columns: auto repeat(3, minmax(160px, 1fr)) auto auto; gap: 10px; align-items: end; padding: 16px; border-bottom: 1px solid #e2e8f0; }
label { display: grid; gap: 5px; color: #475569; font-size: 12px; font-weight: 750; }
.secondary { border-color: #cbd5e1; color: #475569; background: #fff; }
.back-button { border-color: #c4b5fd; color: #5b21b6; background: #f5f3ff; }
.summary-strip { display: grid; grid-template-columns: 2fr .7fr .7fr 1.3fr; gap: 10px; padding: 14px 16px; background: #f8fafc; }
.summary-strip div { display: grid; gap: 4px; min-width: 0; }
.summary-strip span { color: #64748b; font-size: 11px; }
.summary-strip strong { overflow-wrap: anywhere; font-size: 14px; }
.pagination { display: flex; align-items: center; justify-content: center; gap: 16px; padding: 16px; border-top: 1px solid #e2e8f0; color: #64748b; font-size: 12px; }
.confirm-backdrop { position: fixed; z-index: 40; inset: 0; display: grid; place-items: center; padding: 16px; background: rgb(15 23 42 / 55%); }
.confirm-card { width: min(620px, 96vw); max-height: 92vh; overflow-y: auto; padding: 22px; border-radius: 18px; background: #fff; box-shadow: 0 30px 80px rgb(15 23 42 / 30%); }
.confirm-card header, .confirm-card footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.confirm-card header span { color: #7c3aed; font-size: 11px; font-weight: 850; letter-spacing: .14em; }
.confirm-card h2 { margin: 4px 0; font-size: 22px; }
.confirm-card > p { color: #64748b; }
.confirm-card label { margin-top: 14px; }
.confirm-card footer { justify-content: flex-end; margin-top: 18px; }
.confirm-error { padding: 10px; border-radius: 8px; color: #b91c1c !important; background: #fef2f2; }
@media (max-width: 900px) { .page-hero { align-items: stretch; flex-direction: column; } .hero-meta { min-width: 0; } .toolbar { grid-template-columns: 1fr 1fr; } .summary-strip { grid-template-columns: 1fr 1fr; } }
@media (max-width: 560px) { .toolbar, .summary-strip { grid-template-columns: 1fr; } .route-warning { align-items: stretch; flex-direction: column; } }
</style>
