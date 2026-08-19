<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import TimedToast from '../../components/ui/TimedToast.vue';
import {
  authApi,
  dataPrepareApi,
  type BusinessModule,
  type ClassicCaseAsset,
  type ClassicCaseLaunchResult,
  type ClassicCaseVersion,
  type ConnectorSystem
} from '../../services/trainingApi';

type NoticeTone = 'success' | 'error' | 'info';

const session = authApi.getSession();
const loading = ref(false);
const refreshing = ref(false);
const systems = ref<ConnectorSystem[]>([]);
const modules = ref<BusinessModule[]>([]);
const cases = ref<ClassicCaseAsset[]>([]);
const versions = ref<ClassicCaseVersion[]>([]);
const selectedCase = ref<ClassicCaseAsset | null>(null);
const selectedVersion = ref<ClassicCaseVersion | null>(null);
const launchResult = ref<ClassicCaseLaunchResult | null>(null);
const notice = reactive({
  show: false,
  type: 'info' as NoticeTone,
  message: ''
});
const text = {
  dataPrepare: '数据准备',
  pageTitle: '经典案例',
  pageIntro: '查看原平台推送的经典案例，并在教学前生成学习环境启动上下文。',
  backToBatchPrepare: '返回批次准备',
  learningEnvironment: '学习环境',
  allEnvironments: '全部环境',
  businessModule: '业务模块',
  allModules: '全部模块',
  teachingPoint: '教学点',
  optional: '可选',
  refreshing: '刷新中',
  search: '查询',
  totalCases: '案例总数',
  activeCases: '启用案例',
  environment: '当前环境',
  module: '当前模块',
  caseList: '案例列表',
  items: '条',
  case: '案例',
  status: '状态',
  updated: '更新时间',
  action: '操作',
  view: '查看',
  noClassicCases: '暂无经典案例',
  noClassicCasesHint: '请确认原平台正式环境是否已推送脱敏后的经典案例内容。',
  caseDetail: '案例详情',
  generateLaunch: '生成并进入',
  title: '标题',
  noSummary: '未填写摘要',
  caseCode: '案例编码',
  learningEnv: '学习环境',
  currentVersion: '当前版本',
  versions: '版本记录',
  versionDetail: '版本内容（已脱敏）',
  identityBinding: '身份绑定',
  replayPayload: '案例数据',
  formatPayload: '格式数据',
  noVersionRecords: '暂无版本记录',
  launchContext: '启动上下文',
  businessNo: '业务编号',
  instanceId: '实例标识',
  token: '启动令牌',
  targetUrl: '目标地址',
  selectCase: '请选择案例',
  selectCaseHint: '选择后可查看版本，并生成教学副本。',
  casesLoaded: '经典案例列表已加载',
  selectCaseFirst: '请先选择经典案例',
  launchGenerated: '学习环境数据与启动上下文已生成',
  operationFailed: '操作失败'
};
const statusTextMap: Record<string, string> = {
  ACTIVE: '启用',
  AVAILABLE: '可用',
  SUCCESS: '成功',
  FAILED: '失败',
  DISABLED: '停用',
  PENDING: '待处理',
  DRAFT: '草稿'
};

const filters = reactive({
  tenantId: session?.user.tenantId || 'demo-tenant',
  learningConnectorSystemId: '',
  moduleCode: '',
  teachingPointId: ''
});

const selectedSystem = computed(() =>
  systems.value.find((item) => item.id === filters.learningConnectorSystemId)
);
const selectedModule = computed(() =>
  modules.value.find((item) => item.moduleCode === filters.moduleCode)
);
const enabledCaseCount = computed(() =>
  cases.value.filter((item) => item.status === 'ACTIVE' || item.status === 'AVAILABLE').length
);
const latestVersion = computed(() => versions.value[0]);

onMounted(initialize);

watch(
  () => filters.learningConnectorSystemId,
  async () => {
    await loadModules();
    await loadCases();
  }
);

watch(
  () => filters.moduleCode,
  async () => {
    await loadCases();
  }
);

async function initialize() {
  await run(async () => {
    systems.value = (await dataPrepareApi.listConnectorSystems(filters.tenantId))
      .filter((item) => !item.environmentType || item.environmentType === 'LEARNING');
    // 默认查看全部学习环境，避免历史环境配置排在首位时把 OA 已推送案例过滤掉。
    filters.learningConnectorSystemId = '';
    await loadModules();
    await loadCases();
  }, text.casesLoaded);
}

async function loadModules() {
  if (!filters.learningConnectorSystemId) {
    modules.value = [];
    filters.moduleCode = '';
    return;
  }
  modules.value = await dataPrepareApi.listBusinessModules({
    tenantId: filters.tenantId,
    connectorSystemId: filters.learningConnectorSystemId
  });
  if (!modules.value.some((item) => item.moduleCode === filters.moduleCode)) {
    filters.moduleCode = '';
  }
}

async function loadCases() {
  refreshing.value = true;
  try {
    cases.value = await dataPrepareApi.listClassicCases({
      tenantId: filters.tenantId,
      learningConnectorSystemId: filters.learningConnectorSystemId || undefined,
      moduleCode: filters.moduleCode || undefined,
      teachingPointId: filters.teachingPointId.trim() || undefined
    });
    if (!cases.value.some((item) => item.id === selectedCase.value?.id)) {
      selectedCase.value = cases.value[0] || null;
      await loadVersions(selectedCase.value);
    }
  } finally {
    refreshing.value = false;
  }
}

async function loadVersions(caseAsset: ClassicCaseAsset | null) {
  selectedCase.value = caseAsset;
  selectedVersion.value = null;
  launchResult.value = null;
  if (!caseAsset) {
    versions.value = [];
    return;
  }
  versions.value = await dataPrepareApi.listClassicCaseVersions(caseAsset.id, filters.tenantId);
  if (versions.value[0]) {
    await loadVersionDetail(versions.value[0]);
  }
}

async function loadVersionDetail(version: ClassicCaseVersion) {
  if (!selectedCase.value) return;
  selectedVersion.value = await dataPrepareApi.getClassicCaseVersion(
    selectedCase.value.id,
    version.caseVersionId || version.id
  );
}

async function generateTeachingReplica() {
  if (!selectedCase.value) {
    notify('error', text.selectCaseFirst);
    return;
  }
  await run(async () => {
    const requestId = `classic-case-${Date.now()}`;
    launchResult.value = await dataPrepareApi.generateClassicCaseLaunch({
      tenantId: filters.tenantId,
      caseAssetId: selectedCase.value!.id,
      caseVersionId: latestVersion.value?.id,
      usageScene: 'TEACHING_REPLICA',
      sceneType: 'TEACHING',
      taskId: `classic-case-${selectedCase.value!.caseCode}`,
      ownerUserId: session?.user.userId || '',
      requestBatchId: requestId,
      requestItemId: selectedCase.value!.id,
      traceId: requestId,
      sdkMode: 'RECORD_SDK'
    });
  }, text.launchGenerated);
}

async function run(action: () => Promise<void>, successMessage: string) {
  loading.value = true;
  closeNotice();
  try {
    await action();
    notify('success', successMessage);
  } catch (err) {
    notify('error', friendlyErrorMessage(err));
  } finally {
    loading.value = false;
  }
}

function notify(type: NoticeTone, message: string) {
  notice.type = type;
  notice.message = message;
  notice.show = true;
}

function closeNotice() {
  notice.show = false;
  notice.message = '';
}

function friendlyErrorMessage(err: unknown) {
  return err instanceof Error ? err.message : text.operationFailed;
}

function statusClass(status?: string) {
  if (status === 'ACTIVE' || status === 'AVAILABLE' || status === 'SUCCESS') return 'success';
  if (status === 'FAILED') return 'danger';
  return 'info';
}

function formatStatus(status?: string) {
  return status ? statusTextMap[status] ?? status : '-';
}

function formatTime(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}
</script>

<template>
  <section class="config-page classic-case-page">
    <TimedToast
      :show="notice.show"
      :message="notice.message"
      :type="notice.type"
      @close="closeNotice"
    />

    <header class="page-heading">
      <div>
        <p>{{ text.dataPrepare }}</p>
        <h1>{{ text.pageTitle }}</h1>
        <p>{{ text.pageIntro }}</p>
      </div>
      <RouterLink class="secondary page-link" to="/admin/data-prepare">{{ text.backToBatchPrepare }}</RouterLink>
    </header>

    <section class="toolbar classic-toolbar">
      <label>
        <span>{{ text.learningEnvironment }}</span>
        <select v-model="filters.learningConnectorSystemId">
          <option value="">{{ text.allEnvironments }}</option>
          <option v-for="system in systems" :key="system.id" :value="system.id">
            {{ system.systemName }} ({{ system.environmentGroupCode || system.systemCode }})
          </option>
        </select>
      </label>
      <label>
        <span>{{ text.businessModule }}</span>
        <select v-model="filters.moduleCode">
          <option value="">{{ text.allModules }}</option>
          <option v-for="module in modules" :key="module.id" :value="module.moduleCode">
            {{ module.moduleName }} ({{ module.moduleCode }})
          </option>
        </select>
      </label>
      <label>
        <span>{{ text.teachingPoint }}</span>
        <input
          v-model.trim="filters.teachingPointId"
          type="text"
          :placeholder="text.optional"
          @keyup.enter="loadCases"
        />
      </label>
      <button type="button" :disabled="loading || refreshing" @click="loadCases">
        {{ refreshing ? text.refreshing : text.search }}
      </button>
    </section>

    <section class="metrics">
      <article>
        <span>{{ text.totalCases }}</span>
        <strong>{{ cases.length }}</strong>
      </article>
      <article>
        <span>{{ text.activeCases }}</span>
        <strong>{{ enabledCaseCount }}</strong>
      </article>
      <article>
        <span>{{ text.environment }}</span>
        <strong>{{ selectedSystem?.environmentGroupCode || '-' }}</strong>
      </article>
      <article>
        <span>{{ text.module }}</span>
        <strong>{{ selectedModule?.moduleName || '-' }}</strong>
      </article>
    </section>

    <main class="classic-layout">
      <section class="panel">
        <header class="panel-header">
          <h2>{{ text.caseList }}</h2>
          <span>{{ cases.length }} {{ text.items }}</span>
        </header>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ text.case }}</th>
                <th>{{ text.module }}</th>
                <th>{{ text.teachingPoint }}</th>
                <th>{{ text.status }}</th>
                <th>{{ text.updated }}</th>
                <th>{{ text.action }}</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in cases"
                :key="item.id"
                :class="{ selected: item.id === selectedCase?.id }"
              >
                <td>
                  <strong>{{ item.caseTitle }}</strong>
                  <span>{{ item.caseCode }}</span>
                </td>
                <td>{{ item.moduleCode }}</td>
                <td>{{ item.teachingPointId || '-' }}</td>
                <td>
                  <span class="status-badge" :class="statusClass(item.status)">
                    {{ formatStatus(item.status) }}
                  </span>
                </td>
                <td>{{ formatTime(item.updateTime || item.createTime) }}</td>
                <td>
                  <button type="button" class="secondary row-button" @click="loadVersions(item)">
                    {{ text.view }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="cases.length === 0" class="empty-state">
            <strong>{{ text.noClassicCases }}</strong>
            <p>{{ text.noClassicCasesHint }}</p>
          </div>
        </div>
      </section>

      <aside class="panel detail-panel">
        <header class="panel-header">
          <h2>{{ text.caseDetail }}</h2>
          <button
            type="button"
            :disabled="loading || !selectedCase"
            @click="generateTeachingReplica"
          >
            {{ text.generateLaunch }}
          </button>
        </header>

        <div v-if="selectedCase" class="detail-body">
          <section class="summary-card">
            <span>{{ text.title }}</span>
            <strong>{{ selectedCase.caseTitle }}</strong>
            <p>{{ selectedCase.caseSummary || text.noSummary }}</p>
          </section>

          <dl class="detail-grid">
            <dt>{{ text.caseCode }}</dt><dd>{{ selectedCase.caseCode }}</dd>
            <dt>{{ text.learningEnv }}</dt><dd>{{ selectedCase.learningConnectorSystemId }}</dd>
            <dt>{{ text.module }}</dt><dd>{{ selectedCase.moduleCode }}</dd>
            <dt>{{ text.teachingPoint }}</dt><dd>{{ selectedCase.teachingPointId || '-' }}</dd>
            <dt>{{ text.currentVersion }}</dt><dd>{{ selectedCase.currentVersionId || '-' }}</dd>
          </dl>

          <section class="version-section">
            <h3>{{ text.versions }}</h3>
            <ul v-if="versions.length" class="version-list">
              <li
                v-for="version in versions"
                :key="version.id"
                :class="{ selected: version.id === selectedVersion?.id }"
                @click="loadVersionDetail(version)"
              >
                <div>
                  <strong>v{{ version.versionNo }}</strong>
                  <span>{{ version.caseVersionId || version.id }}</span>
                  <span>{{ version.payloadSchemaVersion || '-' }}</span>
                </div>
                <span class="status-badge" :class="statusClass(version.status)">
                  {{ formatStatus(version.status) }}
                </span>
              </li>
            </ul>
            <p v-else class="helper-text">{{ text.noVersionRecords }}</p>
          </section>

          <section v-if="selectedVersion" class="version-section version-detail">
            <h3>{{ text.versionDetail }}</h3>
            <label v-if="selectedVersion.identityBindingJson">
              <span>{{ text.identityBinding }}</span>
              <pre>{{ selectedVersion.identityBindingJson }}</pre>
            </label>
            <label v-if="selectedVersion.desensitizedCasePayloadJson">
              <span>{{ text.replayPayload }}</span>
              <pre>{{ selectedVersion.desensitizedCasePayloadJson }}</pre>
            </label>
            <label v-if="selectedVersion.caseDataFormatJson">
              <span>{{ text.formatPayload }}</span>
              <pre>{{ selectedVersion.caseDataFormatJson }}</pre>
            </label>
          </section>

          <section v-if="launchResult" class="launch-box">
            <h3>{{ text.launchContext }}</h3>
            <dl class="detail-grid">
              <dt>{{ text.businessNo }}</dt><dd>{{ launchResult.usage.externalBusinessNo || '-' }}</dd>
              <dt>{{ text.instanceId }}</dt><dd>{{ launchResult.usage.teachingDataInstanceId || '-' }}</dd>
              <dt>{{ text.token }}</dt><dd>{{ launchResult.launchContext.launchToken }}</dd>
              <dt>{{ text.targetUrl }}</dt><dd>{{ launchResult.launchContext.targetUrl || launchResult.usage.targetUrl || '-' }}</dd>
            </dl>
          </section>
        </div>

        <div v-else class="empty-state">
          <strong>{{ text.selectCase }}</strong>
          <p>{{ text.selectCaseHint }}</p>
        </div>
      </aside>
    </main>
  </section>
</template>

<style scoped>
@import './dataPrepareConfig.css';

.classic-case-page {
  padding: 24px;
  background: var(--dp-bg);
  min-height: 100%;
}

.page-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  border: 1px solid var(--dp-line-strong);
  border-radius: 6px;
  background: #fff;
  color: #334155;
  font-size: 13px;
  font-weight: 800;
  padding: 0 14px;
  text-decoration: none;
}

.classic-toolbar {
  grid-template-columns: minmax(220px, 1fr) minmax(220px, 1fr) minmax(180px, 0.8fr) auto;
}

.classic-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(360px, 0.75fr);
  gap: 18px;
}

td strong,
td span {
  display: block;
}

td span {
  margin-top: 4px;
  color: var(--dp-muted);
  font-size: 12px;
}

tr.selected {
  background: #f8fbff;
}

.row-button {
  min-height: 32px;
  padding: 0 10px;
}

.detail-panel {
  align-self: start;
}

.detail-body {
  display: grid;
  gap: 16px;
  padding: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 10px 12px;
  margin: 0;
}

.detail-grid dt {
  color: var(--dp-muted);
  font-size: 13px;
  font-weight: 800;
}

.detail-grid dd {
  margin: 0;
  color: var(--dp-text);
  font-size: 13px;
  overflow-wrap: anywhere;
}

.version-section,
.launch-box {
  display: grid;
  gap: 10px;
  border-top: 1px solid var(--dp-line);
  padding-top: 14px;
}

.version-section h3,
.launch-box h3 {
  margin: 0;
  font-size: 15px;
}

.version-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.version-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid var(--dp-line);
  border-radius: 6px;
  padding: 10px;
  cursor: pointer;
}

.version-list li.selected {
  border-color: #2563eb;
  background: #eff6ff;
}

.version-list div {
  display: grid;
  gap: 4px;
}

.version-list strong {
  font-size: 13px;
}

.version-list span {
  color: var(--dp-muted);
  font-size: 12px;
}

.version-detail label {
  display: grid;
  gap: 6px;
}

.version-detail label > span {
  color: var(--dp-muted);
  font-size: 12px;
  font-weight: 800;
}

.version-detail pre {
  max-height: 220px;
  margin: 0;
  overflow: auto;
  border: 1px solid var(--dp-line);
  border-radius: 6px;
  background: #f8fafc;
  padding: 10px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 12px;
}

.launch-box {
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  padding: 14px;
}

.status-badge.danger {
  background: var(--dp-danger-soft);
  color: var(--dp-danger);
}

@media (max-width: 1100px) {
  .classic-layout,
  .classic-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
