<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import TimedToast from '../../components/ui/TimedToast.vue';
import {
  authApi,
  dataPrepareApi,
  type ConnectorSystem,
  type CreateConnectorSystemRequest,
  type DataPrepareMetadata,
  type ExternalCredential,
  type PlatformCapability,
  type PlatformCapabilityRequest,
  type UpdateConnectorSystemRequest
} from '../../services/trainingApi';

type ConnectorSystemForm = Omit<CreateConnectorSystemRequest, 'tenantId' | 'configJson'> & {
  environmentType: string;
  environmentGroupCode: string;
  configJson: string;
};

type PlatformDialogMode = 'none' | 'create' | 'detail' | 'edit';
type CapabilityDialogMode = 'none' | 'list' | 'create' | 'edit';

const PAGE_SIZE = 10;

const session = authApi.getSession();
const tenantId = ref(session?.user.tenantId || 'demo-tenant');
const metadata = ref<DataPrepareMetadata | null>(null);
const systems = ref<ConnectorSystem[]>([]);
const loading = ref(false);
const systemKeyword = ref('');
const currentPage = ref(1);
const dialogMode = ref<PlatformDialogMode>('none');
const selectedSystem = ref<ConnectorSystem | null>(null);
const selectedExternalCredential = ref<ExternalCredential | null>(null);
const generatedExternalApiKey = ref('');
const capabilityDialogMode = ref<CapabilityDialogMode>('none');
const capabilitySystem = ref<ConnectorSystem | null>(null);
const selectedCapability = ref<PlatformCapability | null>(null);
const capabilities = ref<PlatformCapability[]>([]);
const notice = reactive({
  show: false,
  type: 'info' as 'success' | 'error' | 'info',
  message: ''
});

const form = reactive<ConnectorSystemForm>({
  systemName: '',
  systemCode: '',
  systemType: 'LOCAL_DEV',
  environmentType: '',
  environmentGroupCode: '',
  baseUrl: '',
  authType: 'API_KEY',
  configJson: '{\n  "apiKey": "",\n  "headerName": "X-API-Key"\n}'
});

const capabilityForm = reactive<PlatformCapabilityRequest>({
  tenantId: tenantId.value,
  connectorSystemId: '',
  capabilityCode: 'DATA_CREATE',
  capabilityName: '批量创建教学初始数据',
  capabilityType: 'DATA_CREATE',
  supportFlag: true,
  endpointUrl: '/openapi/teaching-data/batch-create',
  method: 'POST',
  requestSchemaJson: '{\n  "required": ["requestBatchId", "items"]\n}',
  responseSchemaJson: '{\n  "required": ["requestBatchId", "items"]\n}',
  timeoutMs: 10000,
  retryPolicyJson: '{\n  "maxAttempts": 3,\n  "backoffMs": 1000\n}',
  createBy: session?.user.userId || 'admin',
  updateBy: session?.user.userId || 'admin'
});

const activeCount = computed(
  () => systems.value.filter((item) => item.status === 'ACTIVE').length
);

const filteredSystems = computed(() => {
  const keyword = systemKeyword.value.trim().toLowerCase();
  if (!keyword) return systems.value;
  return systems.value.filter((item) =>
    (item.systemName || '').toLowerCase().includes(keyword)
  );
});

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredSystems.value.length / PAGE_SIZE))
);

const pagedSystems = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE;
  return filteredSystems.value.slice(start, start + PAGE_SIZE);
});

const isDialogOpen = computed(() => dialogMode.value !== 'none');
const isCapabilityDialogOpen = computed(() => capabilityDialogMode.value !== 'none');
const selectedSystemCanGenerateExternalKey = computed(
  () => selectedSystem.value?.environmentType === 'PROD'
);
const configJsonPlaceholder = computed(() =>
  dialogMode.value === 'edit'
    ? '留空表示不修改原认证配置'
    : '例如：{"apiKey":"your-api-key","headerName":"X-API-Key"}'
);
const visibleCapabilities = computed(() =>
  (metadata.value?.capabilities ?? [{ code: 'DATA_CREATE', label: '数据创建', visible: true }])
    .filter((item) => item.visible)
);
const visibleAuthTypes = computed(() =>
  (metadata.value?.authTypes ?? [{ code: 'API_KEY', label: 'API Key', visible: true }])
    .filter((item) => item.visible)
);

onMounted(initialize);

async function initialize() {
  await loadMetadata();
  await loadSystems();
}

async function loadMetadata() {
  try {
    metadata.value = await dataPrepareApi.getDataPrepareMetadata();
    resetForm();
    applyCapabilityPreset();
  } catch {
    metadata.value = null;
  }
}

/**
 * 业务功能：读取当前租户已接入的原平台系统，给管理员提供平台维护的事实列表。
 * 关键流程：只查询平台边界信息；模块、模板和策略由后续菜单按平台继续维护。
 */
async function loadSystems() {
  await run(async () => {
    systems.value = await dataPrepareApi.listConnectorSystems(tenantId.value);
    normalizePage();
  }, '平台列表已刷新');
}

/**
 * 业务功能：打开新增平台弹窗，不直接写入数据库。
 * 关键流程：清理旧表单后进入创建模式，保存按钮才真正调用创建接口。
 */
function openCreateForm() {
  resetForm();
  selectedSystem.value = null;
  dialogMode.value = 'create';
  closeNotice();
}

/**
 * 业务功能：为本地联调场景填充默认值，但不触发保存。
 * 关键流程：默认编码带时间戳降低重复概率，管理员仍可在保存前调整。
 */
function fillLocalDevDefaults() {
  const stamp = Date.now();
  form.systemName = '本地联调原平台';
  form.systemCode = `LOCAL_ORIGIN_${stamp}`;
  form.systemType = metadata.value?.platformDefaults.systemType || 'LOCAL_DEV';
  form.environmentType = 'LEARNING';
  form.environmentGroupCode = 'LOCAL_DEV';
  form.baseUrl = 'http://127.0.0.1:8080/local-origin';
  form.authType = metadata.value?.platformDefaults.authType || 'API_KEY';
  form.configJson = metadata.value?.platformDefaults.authConfigJson || '{\n  "apiKey": "local-dev-api-key",\n  "headerName": "X-API-Key"\n}';
  notify('info', '已填充本地联调默认值，确认无误后再保存');
}

/**
 * 业务功能：打开平台详情弹窗。
 * 关键流程：从后端拉取最新详情，避免列表缓存导致展示信息过期。
 */
async function showSystemDetail(system: ConnectorSystem) {
  await run(async () => {
    selectedSystem.value = await dataPrepareApi.getConnectorSystem(system.id);
    generatedExternalApiKey.value = '';
    selectedExternalCredential.value = selectedSystemCanGenerateExternalKey.value
      ? await dataPrepareApi.getExternalApiKey(system.id)
      : null;
    dialogMode.value = 'detail';
  }, '平台详情已加载');
}

/**
 * 业务功能：打开平台编辑弹窗。
 * 关键流程：先取最新详情再回填编辑表单，系统编码只展示不允许修改。
 */
async function editSystem(system: ConnectorSystem) {
  await run(async () => {
    const detail = await dataPrepareApi.getConnectorSystem(system.id);
    selectedSystem.value = detail;
    selectedExternalCredential.value = null;
    generatedExternalApiKey.value = '';
    form.systemName = detail.systemName || '';
    form.systemCode = detail.systemCode || '';
    form.systemType = detail.systemType || metadata.value?.platformDefaults.systemType || 'LOCAL_DEV';
    form.environmentType = detail.environmentType || '';
    form.environmentGroupCode = detail.environmentGroupCode || '';
    form.baseUrl = detail.baseUrl || '';
    form.authType = detail.authType || metadata.value?.platformDefaults.authType || 'API_KEY';
    form.configJson = detail.configJson || '';
    dialogMode.value = 'edit';
  }, '平台编辑信息已加载');
}

/**
 * 业务功能：关闭当前弹窗并清理编辑状态。
 * 关键流程：只影响前端状态，不调用后端，避免取消动作改变数据库。
 */
function closeDialog() {
  resetForm();
  selectedSystem.value = null;
  selectedExternalCredential.value = null;
  generatedExternalApiKey.value = '';
  dialogMode.value = 'none';
  closeNotice();
}

/**
 * 业务功能：为原平台正式环境生成第三方对接 Key。
 * 关键流程：只允许未生成过的正式环境调用；生成后立即展示一次明文，并保存凭证摘要用于后续只读展示。
 */
async function generateExternalApiKey() {
  if (!selectedSystem.value) return;
  if (!selectedSystemCanGenerateExternalKey.value) {
    notify('error', '只有正式环境平台允许生成第三方对接 Key');
    return;
  }
  if (selectedExternalCredential.value) {
    notify('error', '该系统已存在第三方对接 Key，不允许重复生成');
    return;
  }
  await run(async () => {
    const credential = await dataPrepareApi.generateExternalApiKey(
      selectedSystem.value!.id,
      session?.user.userId || 'admin'
    );
    selectedExternalCredential.value = credential;
    generatedExternalApiKey.value = credential.apiKey || '';
  }, '第三方对接 Key 已生成，请立即保存明文');
}

/**
 * 业务功能：创建平台。
 * 关键流程：先做必填校验，再调用创建接口；成功后插入列表首位并回到第一页。
 */
async function saveConnectorSystem() {
  const validationMessage = validateForm();
  if (validationMessage) {
    notify('error', validationMessage);
    return;
  }

  await run(async () => {
    const created = await dataPrepareApi.createConnectorSystem({
      tenantId: tenantId.value,
      systemName: form.systemName.trim(),
      systemCode: form.systemCode.trim(),
      systemType: form.systemType,
      environmentType: form.environmentType || undefined,
      environmentGroupCode: form.environmentGroupCode.trim() || undefined,
      baseUrl: form.baseUrl.trim(),
      authType: form.authType,
      configJson: form.configJson.trim() || undefined
    });
    systems.value = [created, ...systems.value];
    currentPage.value = 1;
    closeDialog();
  }, '平台已创建');
}

/**
 * 业务功能：保存平台修改。
 * 关键流程：调用后端更新接口并局部替换当前行，保持分页和筛选上下文不变。
 */
async function updateConnectorSystem() {
  if (!selectedSystem.value) return;
  const validationMessage = validateEditForm();
  if (validationMessage) {
    notify('error', validationMessage);
    return;
  }

  const request: UpdateConnectorSystemRequest = {
    id: selectedSystem.value.id,
    systemName: form.systemName.trim(),
    systemType: form.systemType,
    environmentType: form.environmentType || '',
    environmentGroupCode: form.environmentGroupCode.trim(),
    baseUrl: form.baseUrl.trim(),
    authType: form.authType,
    configJson: form.configJson.trim() || undefined
  };

  await run(async () => {
    const updated = await dataPrepareApi.updateConnectorSystem(request);
    systems.value = systems.value.map((item) => (item.id === updated.id ? updated : item));
    closeDialog();
  }, '平台已更新');
}

/**
 * 业务功能：启用或停用平台。
 * 关键流程：调用后端真实启停用接口并局部替换当前行。
 */
async function toggleSystemStatus(system: ConnectorSystem) {
  await run(async () => {
    const updated =
      system.status === 'ACTIVE'
        ? await dataPrepareApi.disableConnectorSystem(system.id)
        : await dataPrepareApi.enableConnectorSystem(system.id);
    systems.value = systems.value.map((item) => (item.id === updated.id ? updated : item));
  }, system.status === 'ACTIVE' ? '平台已停用' : '平台已启用');
}

/**
 * 业务功能：打开某个原平台的能力维护弹窗。
 * 关键流程：先绑定当前系统，再读取该系统下全部能力声明，保证后续新增和编辑都落在同一平台边界内。
 */
async function openCapabilityDialog(system: ConnectorSystem) {
  capabilitySystem.value = system;
  capabilityDialogMode.value = 'list';
  await loadCapabilities(system);
}

/**
 * 业务功能：读取当前原平台下的能力声明列表。
 * 关键流程：按租户和 connectorSystemId 查询，避免跨平台展示或误启停能力。
 */
async function loadCapabilities(system = capabilitySystem.value) {
  if (!system) return;
  await run(async () => {
    const capabilityList = await dataPrepareApi.listPlatformCapabilities({
      tenantId: tenantId.value,
      connectorSystemId: system.id
    });
    const allowedCodes = visibleCapabilities.value.map((item) => item.code);
    capabilities.value = capabilityList.filter((item) => allowedCodes.includes(item.capabilityCode));
  }, '能力列表已刷新');
}

/**
 * 业务功能：打开能力新增表单。
 * 关键流程：以当前原平台为边界初始化默认 DATA_CREATE 能力，管理员可在保存前调整接口地址和 Schema。
 */
function openCreateCapabilityForm() {
  if (!capabilitySystem.value) return;
  resetCapabilityForm(capabilitySystem.value);
  selectedCapability.value = null;
  capabilityDialogMode.value = 'create';
}

/**
 * 业务功能：打开能力编辑表单。
 * 关键流程：先把列表行完整回填到表单，保留 Schema、超时和重试策略等运行参数。
 */
function editCapability(capability: PlatformCapability) {
  selectedCapability.value = capability;
  capabilityForm.tenantId = capability.tenantId;
  capabilityForm.connectorSystemId = capability.connectorSystemId;
  capabilityForm.capabilityCode = capability.capabilityCode;
  capabilityForm.capabilityName = capability.capabilityName;
  capabilityForm.capabilityType = capability.capabilityType;
  capabilityForm.supportFlag = capability.supportFlag;
  capabilityForm.endpointUrl = capability.endpointUrl;
  capabilityForm.method = capability.method;
  capabilityForm.requestSchemaJson = capability.requestSchemaJson || '';
  capabilityForm.responseSchemaJson = capability.responseSchemaJson || '';
  capabilityForm.timeoutMs = capability.timeoutMs || 10000;
  capabilityForm.retryPolicyJson = capability.retryPolicyJson || '';
  capabilityForm.updateBy = session?.user.userId || 'admin';
  capabilityDialogMode.value = 'edit';
}

/**
 * 业务功能：创建原平台能力声明。
 * 关键流程：先校验必填和 JSON，再调用后端创建接口；成功后回到能力列表并刷新事实状态。
 */
async function saveCapability() {
  const validationMessage = validateCapabilityForm();
  if (validationMessage) {
    notify('error', validationMessage);
    return;
  }
  await run(async () => {
    await dataPrepareApi.createPlatformCapability({ ...capabilityForm });
    capabilityDialogMode.value = 'list';
    await loadCapabilities();
  }, '能力已创建');
}

/**
 * 业务功能：更新原平台能力声明。
 * 关键流程：不允许在编辑中改变能力归属边界，只提交当前能力的可维护运行参数。
 */
async function updateCapability() {
  if (!selectedCapability.value) return;
  const validationMessage = validateCapabilityForm();
  if (validationMessage) {
    notify('error', validationMessage);
    return;
  }
  await run(async () => {
    await dataPrepareApi.updatePlatformCapability(selectedCapability.value!.id, { ...capabilityForm });
    capabilityDialogMode.value = 'list';
    await loadCapabilities();
  }, '能力已更新');
}

/**
 * 业务功能：启用或停用原平台能力声明。
 * 关键流程：策略启用只认 ACTIVE 且 supportFlag=true 的能力，状态切换后必须刷新列表。
 */
async function toggleCapabilityStatus(capability: PlatformCapability) {
  await run(async () => {
    if (capability.status === 'ACTIVE') {
      await dataPrepareApi.disablePlatformCapability(capability.id);
    } else {
      await dataPrepareApi.enablePlatformCapability(capability.id);
    }
    await loadCapabilities();
  }, capability.status === 'ACTIVE' ? '能力已停用' : '能力已启用');
}

/**
 * 业务功能：关闭能力维护弹窗。
 * 关键流程：只清理前端能力编辑态，不影响原平台系统弹窗和已保存配置。
 */
function closeCapabilityDialog() {
  capabilityDialogMode.value = 'none';
  capabilitySystem.value = null;
  selectedCapability.value = null;
  capabilities.value = [];
}

/**
 * 业务功能：回到能力列表。
 * 关键流程：保留当前平台上下文，取消新增或编辑表单。
 */
function backToCapabilityList() {
  selectedCapability.value = null;
  capabilityDialogMode.value = 'list';
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

function validateForm() {
  if (!form.systemName.trim()) return '请填写平台名称';
  if (!form.systemCode.trim()) return '请填写平台编码';
  if (!form.baseUrl.trim()) return '请填写平台地址';
  if (form.environmentType && !['PROD', 'LEARNING'].includes(form.environmentType)) return '环境类型仅支持 PROD 或 LEARNING';
  if (form.authType !== defaultAuthType()) return `首期仅支持 ${defaultAuthType()} 认证`;
  const authMessage = validateApiKeyConfig();
  if (authMessage) return authMessage;
  return '';
}

function validateEditForm() {
  if (!form.systemName.trim()) return '请填写平台名称';
  if (!form.baseUrl.trim()) return '请填写平台地址';
  if (form.environmentType && !['PROD', 'LEARNING'].includes(form.environmentType)) return '环境类型仅支持 PROD 或 LEARNING';
  if (form.authType !== defaultAuthType()) return `首期仅支持 ${defaultAuthType()} 认证`;
  if (!form.configJson.trim()) return '';
  const authMessage = validateApiKeyConfig();
  if (authMessage) return authMessage;
  return '';
}

function resetForm() {
  form.systemName = '';
  form.systemCode = '';
  form.systemType = metadata.value?.platformDefaults.systemType || 'LOCAL_DEV';
  form.environmentType = '';
  form.environmentGroupCode = '';
  form.baseUrl = '';
  form.authType = defaultAuthType();
  form.configJson = metadata.value?.platformDefaults.authConfigJson || '{\n  "apiKey": "",\n  "headerName": "X-API-Key"\n}';
}

function validateApiKeyConfig() {
  try {
    const config = JSON.parse(form.configJson || '{}') as { apiKey?: unknown };
    if (!config || typeof config !== 'object' || Array.isArray(config)) return '认证配置必须是 JSON 对象';
    if (typeof config.apiKey !== 'string' || !config.apiKey.trim()) return '认证配置必须填写 apiKey';
    return '';
  } catch {
    return '认证配置必须是合法 JSON';
  }
}

function normalizePage() {
  if (currentPage.value > totalPages.value) {
    currentPage.value = totalPages.value;
  }
}

function previousPage() {
  currentPage.value = Math.max(1, currentPage.value - 1);
}

function nextPage() {
  currentPage.value = Math.min(totalPages.value, currentPage.value + 1);
}

function handleKeywordChange() {
  currentPage.value = 1;
  normalizePage();
}

function notify(type: 'success' | 'error' | 'info', text: string) {
  notice.type = type;
  notice.message = text;
  notice.show = true;
}

function closeNotice() {
  notice.show = false;
  notice.message = '';
}

function friendlyErrorMessage(err: unknown) {
  const rawMessage = err instanceof Error ? err.message : '操作失败';
  if (rawMessage.includes('数据保存失败') || rawMessage.includes('数据约束')) {
    return '平台保存失败，请检查平台编码是否重复，或确认必填字段是否完整。';
  }
  return rawMessage;
}

function statusClass(status?: string) {
  return status === 'ACTIVE' ? 'success' : 'info';
}

function resetCapabilityForm(system: ConnectorSystem) {
  capabilityForm.tenantId = tenantId.value;
  capabilityForm.connectorSystemId = system.id;
  capabilityForm.capabilityCode = defaultCapabilityCode();
  applyCapabilityPreset(defaultCapabilityCode());
  capabilityForm.createBy = session?.user.userId || 'admin';
  capabilityForm.updateBy = session?.user.userId || 'admin';
}

function applyCapabilityPreset(code = capabilityForm.capabilityCode) {
  if (!visibleCapabilities.value.some((item) => item.code === code)) {
    code = defaultCapabilityCode();
  }
  const defaults = metadata.value?.platformDefaults;
  const option = visibleCapabilities.value.find((item) => item.code === code);
  capabilityForm.capabilityCode = code;
  capabilityForm.capabilityType = defaults?.capabilityType || code;
  capabilityForm.supportFlag = true;
  capabilityForm.method = defaults?.capabilityMethod || 'POST';
  capabilityForm.timeoutMs = defaults?.capabilityTimeoutMs || 10000;
  capabilityForm.requestSchemaJson = '{\n  "required": ["requestBatchId", "items"]\n}';
  capabilityForm.responseSchemaJson = '{\n  "required": ["requestBatchId", "items"]\n}';
  capabilityForm.retryPolicyJson = '{\n  "maxAttempts": 3,\n  "backoffMs": 1000\n}';
  const endpoints: Record<string, string> = {
    DATA_CREATE: '/openapi/teaching-data/batch-create'
  };
  capabilityForm.capabilityName = defaults?.capabilityName || option?.label || code;
  capabilityForm.endpointUrl = endpoints[code] || '/openapi/teaching-data';
}

function defaultAuthType() {
  return metadata.value?.platformDefaults.authType || visibleAuthTypes.value[0]?.code || 'API_KEY';
}

function defaultCapabilityCode() {
  return metadata.value?.platformDefaults.capabilityCode || visibleCapabilities.value[0]?.code || 'DATA_CREATE';
}

function validateCapabilityForm() {
  if (!capabilityForm.capabilityCode.trim()) return '请填写能力编码';
  if (!capabilityForm.capabilityName.trim()) return '请填写能力名称';
  if (!capabilityForm.endpointUrl.trim()) return '请填写能力接口地址';
  if (!capabilityForm.method.trim()) return '请填写 HTTP 方法';
  if (!isJsonObjectText(capabilityForm.requestSchemaJson)) return '请求 Schema 必须是 JSON 对象';
  if (!isJsonObjectText(capabilityForm.responseSchemaJson)) return '响应 Schema 必须是 JSON 对象';
  if (!isJsonObjectText(capabilityForm.retryPolicyJson)) return '重试策略必须是 JSON 对象';
  return '';
}

function isJsonObjectText(value?: string) {
  if (!value?.trim()) return true;
  try {
    const parsed = JSON.parse(value);
    return Boolean(parsed && typeof parsed === 'object' && !Array.isArray(parsed));
  } catch {
    return false;
  }
}
</script>

<template>
  <section class="config-page platform-page">
    <TimedToast
      :show="notice.show"
      :type="notice.type"
      :message="notice.message"
      @close="closeNotice"
    />

    <header class="page-heading platform-heading">
      <div>
        <p>数据准备 / 平台接入</p>
        <h1>原平台系统维护</h1>
        <span>先维护原平台接入边界，再在平台下配置业务模块、数据模板和准备策略。</span>
      </div>
      <button
        type="button"
        class="page-action"
        :disabled="loading"
        @click="openCreateForm"
      >
        创建本地联调平台
      </button>
    </header>

    <section class="query-panel">
      <label>
        <span>系统名称</span>
        <input
          v-model="systemKeyword"
          type="search"
          placeholder="输入系统名称模糊查询"
          @input="handleKeywordChange"
        />
      </label>
    </section>

    <section class="metrics">
      <article>
        <span>接入平台</span>
        <strong>{{ systems.length }}</strong>
      </article>
      <article>
        <span>启用中</span>
        <strong>{{ activeCount }}</strong>
      </article>
      <article>
        <span>待补模块</span>
        <strong>{{ systems.length - activeCount }}</strong>
      </article>
    </section>

    <section class="panel">
      <header class="panel-header">
        <h2>平台列表</h2>
        <span class="helper-text">共 {{ filteredSystems.length }} 条，每页 {{ PAGE_SIZE }} 条</span>
      </header>
      <div class="panel-body table-wrap">
        <table>
          <thead>
            <tr>
              <th>环境</th>
              <th>环境组</th>
              <th>系统名称</th>
              <th>系统编码</th>
              <th>类型</th>
              <th>认证</th>
              <th>地址</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="system in pagedSystems" :key="system.id">
              <td>{{ system.environmentType || '-' }}</td>
              <td>{{ system.environmentGroupCode || '-' }}</td>
              <td>{{ system.systemName }}</td>
              <td>{{ system.systemCode }}</td>
              <td>{{ system.systemType }}</td>
              <td>{{ system.authType }}</td>
              <td>{{ system.baseUrl }}</td>
              <td>
                <span class="status-badge" :class="statusClass(system.status)">
                  {{ system.status || '未设置' }}
                </span>
              </td>
              <td>
                <div class="row-actions">
                  <button type="button" @click="showSystemDetail(system)">详情</button>
                  <button type="button" @click="editSystem(system)">编辑</button>
                  <button type="button" :disabled="loading" @click="openCapabilityDialog(system)">能力</button>
                  <button type="button" :disabled="loading" @click="toggleSystemStatus(system)">
                    {{ system.status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="filteredSystems.length === 0" class="empty-state">
          <strong>暂无原平台</strong>
          <p>未找到匹配的原平台。</p>
        </div>
      </div>
      <footer v-if="filteredSystems.length > PAGE_SIZE" class="pagination-bar">
        <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
        <div>
          <button type="button" :disabled="currentPage === 1" @click="previousPage">上一页</button>
          <button type="button" :disabled="currentPage === totalPages" @click="nextPage">下一页</button>
        </div>
      </footer>
    </section>

    <div
      v-if="isDialogOpen"
      class="modal-overlay"
      role="presentation"
      @click.self="closeDialog"
    >
      <section
        class="create-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="platform-dialog-title"
      >
        <header class="dialog-header">
          <div>
            <p>平台接入</p>
            <h2 id="platform-dialog-title">
              {{ dialogMode === 'create' ? '创建本地联调平台' : dialogMode === 'edit' ? '编辑平台' : '平台详情' }}
            </h2>
          </div>
          <button
            type="button"
            class="icon-close"
            aria-label="关闭平台弹窗"
            :disabled="loading"
            @click="closeDialog"
          >
            ×
          </button>
        </header>

        <p class="dialog-helper">
          平台只维护接入边界；业务模块、模板和策略在后续菜单中维护。
        </p>

        <div v-if="dialogMode === 'detail' && selectedSystem" class="detail-section">
          <div class="detail-grid">
            <span>系统名称</span><strong>{{ selectedSystem.systemName }}</strong>
            <span>系统编码</span><strong>{{ selectedSystem.systemCode }}</strong>
            <span>类型</span><strong>{{ selectedSystem.systemType }}</strong>
            <span>环境类型</span><strong>{{ selectedSystem.environmentType || '-' }}</strong>
            <span>环境组编码</span><strong>{{ selectedSystem.environmentGroupCode || '-' }}</strong>
            <span>认证</span><strong>{{ selectedSystem.authType }}</strong>
            <span>地址</span><strong>{{ selectedSystem.baseUrl }}</strong>
            <span>状态</span><strong>{{ selectedSystem.status || '未设置' }}</strong>
            <span>创建时间</span><strong>{{ selectedSystem.createTime || '-' }}</strong>
            <span>更新时间</span><strong>{{ selectedSystem.updateTime || '-' }}</strong>
          </div>

          <section class="external-key-panel">
            <header>
              <div>
                <h3>第三方对接 Key</h3>
                <p>用于原平台正式环境调用教学平台经典案例接收接口。</p>
              </div>
              <button
                v-if="selectedSystemCanGenerateExternalKey && !selectedExternalCredential"
                type="button"
                class="primary-action"
                :disabled="loading"
                @click="generateExternalApiKey"
              >
                生成第三方对接 Key
              </button>
            </header>

            <div v-if="!selectedSystemCanGenerateExternalKey" class="external-key-empty">
              学习环境不需要生成第三方对接 Key。
            </div>

            <div v-else-if="selectedExternalCredential" class="external-key-detail">
              <span>Key 前缀</span><strong>{{ selectedExternalCredential.apiKeyPrefix }}</strong>
              <span>状态</span><strong>{{ selectedExternalCredential.status || '-' }}</strong>
              <span>生成时间</span><strong>{{ selectedExternalCredential.createTime || '-' }}</strong>
              <span>最近使用</span><strong>{{ selectedExternalCredential.lastUsedTime || '-' }}</strong>
            </div>

            <div v-else class="external-key-empty">
              当前正式环境尚未生成第三方对接 Key。
            </div>

            <label v-if="generatedExternalApiKey" class="external-key-secret">
              <span>本次生成的完整 Key</span>
              <textarea :value="generatedExternalApiKey" readonly rows="3"></textarea>
              <small>完整 Key 只展示本次，请立即交付给第三方正式系统并妥善保存。</small>
            </label>
          </section>
        </div>

        <div v-else class="create-form">
          <label>
            <span>平台名称 <em>*</em></span>
            <input v-model="form.systemName" type="text" placeholder="例如：采购审批原平台" />
          </label>
          <label>
            <span>平台编码 <em v-if="dialogMode === 'create'">*</em></span>
            <input
              v-model="form.systemCode"
              type="text"
              :readonly="dialogMode === 'edit'"
              placeholder="例如：PURCHASE_APPROVAL"
            />
          </label>
          <label>
            <span>平台类型</span>
            <select v-model="form.systemType">
              <option value="LOCAL_DEV">本地联调</option>
              <option value="CUSTOM">自定义系统</option>
              <option value="OA">OA 系统</option>
              <option value="ERP">ERP 系统</option>
            </select>
          </label>
          <label>
            <span>认证方式</span>
            <select v-model="form.authType">
              <option
                v-for="authType in visibleAuthTypes"
                :key="authType.code"
                :value="authType.code"
              >
                {{ authType.label }}
              </option>
            </select>
          </label>
          <label>
            <span>环境类型</span>
            <select v-model="form.environmentType">
              <option value="">未设置</option>
              <option value="PROD">正式环境</option>
              <option value="LEARNING">学习环境</option>
            </select>
          </label>
          <label>
            <span>环境组编码</span>
            <input v-model="form.environmentGroupCode" type="text" placeholder="例如：OA_PURCHASE" />
          </label>
          <label class="field-wide">
            <span>平台地址 <em>*</em></span>
            <input v-model="form.baseUrl" type="text" placeholder="例如：http://127.0.0.1:8080/local-origin" />
          </label>
          <label class="field-wide">
            <span>扩展配置</span>
            <textarea
              v-model="form.configJson"
              rows="4"
              :placeholder="configJsonPlaceholder"
            ></textarea>
          </label>
        </div>

        <footer class="dialog-actions">
          <button
            v-if="dialogMode === 'create'"
            type="button"
            class="ghost-action"
            :disabled="loading"
            @click="fillLocalDevDefaults"
          >
            填充默认值
          </button>
          <span v-else></span>
          <div>
            <button type="button" class="secondary-action" :disabled="loading" @click="closeDialog">
              关闭
            </button>
            <button
              v-if="dialogMode === 'create'"
              type="button"
              class="primary-action"
              :disabled="loading"
              @click="saveConnectorSystem"
            >
              保存平台
            </button>
            <button
              v-if="dialogMode === 'edit'"
              type="button"
              class="primary-action"
              :disabled="loading"
              @click="updateConnectorSystem"
            >
              保存修改
            </button>
          </div>
        </footer>
      </section>
    </div>

    <div
      v-if="isCapabilityDialogOpen"
      class="modal-overlay"
      role="presentation"
      @click.self="closeCapabilityDialog"
    >
      <section
        class="create-dialog capability-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="capability-dialog-title"
      >
        <header class="dialog-header">
          <div>
            <p>平台能力</p>
            <h2 id="capability-dialog-title">
              {{ capabilitySystem?.systemName || '原平台' }}
            </h2>
          </div>
          <button
            type="button"
            class="icon-close"
            aria-label="关闭能力维护弹窗"
            :disabled="loading"
            @click="closeCapabilityDialog"
          >
            ×
          </button>
        </header>

        <p class="dialog-helper">
          首期仅维护 DATA_CREATE，用于确认原平台支持创建教学业务数据。
        </p>

        <section v-if="capabilityDialogMode === 'list'" class="capability-section">
          <div class="capability-toolbar">
            <div>
              <strong>{{ capabilities.length }} 项能力</strong>
              <span>{{ capabilitySystem?.systemCode || '-' }}</span>
            </div>
            <button type="button" class="primary-action" :disabled="loading" @click="openCreateCapabilityForm">
              新增能力
            </button>
          </div>

          <div class="table-wrap compact-table">
            <table>
              <thead>
                <tr>
                  <th>能力编码</th>
                  <th>名称</th>
                  <th>接口</th>
                  <th>支持</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="capability in capabilities" :key="capability.id">
                  <td>{{ capability.capabilityCode }}</td>
                  <td>{{ capability.capabilityName }}</td>
                  <td>{{ capability.method }} {{ capability.endpointUrl }}</td>
                  <td>{{ capability.supportFlag ? '是' : '否' }}</td>
                  <td>
                    <span class="status-badge" :class="statusClass(capability.status)">
                      {{ capability.status || '未设置' }}
                    </span>
                  </td>
                  <td>
                    <div class="row-actions">
                      <button type="button" @click="editCapability(capability)">编辑</button>
                      <button type="button" :disabled="loading" @click="toggleCapabilityStatus(capability)">
                        {{ capability.status === 'ACTIVE' ? '停用' : '启用' }}
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="capabilities.length === 0" class="empty-state">
              <strong>暂无能力声明</strong>
              <p>策略启用前至少需要 DATA_CREATE 能力。</p>
            </div>
          </div>
        </section>

        <section v-else class="create-form capability-form">
          <label>
            <span>能力编码 <em>*</em></span>
            <select
              v-model="capabilityForm.capabilityCode"
              :disabled="capabilityDialogMode === 'edit'"
              @change="applyCapabilityPreset()"
            >
              <option
                v-for="capability in visibleCapabilities"
                :key="capability.code"
                :value="capability.code"
              >
                {{ capability.code }}
              </option>
            </select>
          </label>
          <label>
            <span>能力名称 <em>*</em></span>
            <input v-model="capabilityForm.capabilityName" type="text" />
          </label>
          <label>
            <span>能力类型</span>
            <input v-model="capabilityForm.capabilityType" type="text" />
          </label>
          <label class="check-line">
            <input v-model="capabilityForm.supportFlag" type="checkbox" />
            <span>原平台支持该能力</span>
          </label>
          <label>
            <span>HTTP 方法 <em>*</em></span>
            <select v-model="capabilityForm.method">
              <option value="POST">POST</option>
              <option value="GET">GET</option>
              <option value="PUT">PUT</option>
              <option value="PATCH">PATCH</option>
              <option value="DELETE">DELETE</option>
            </select>
          </label>
          <label>
            <span>超时毫秒</span>
            <input v-model.number="capabilityForm.timeoutMs" type="number" min="0" />
          </label>
          <label class="field-wide">
            <span>接口地址 <em>*</em></span>
            <input v-model="capabilityForm.endpointUrl" type="text" />
          </label>
          <label class="field-wide">
            <span>请求 Schema JSON</span>
            <textarea v-model="capabilityForm.requestSchemaJson" rows="4" />
          </label>
          <label class="field-wide">
            <span>响应 Schema JSON</span>
            <textarea v-model="capabilityForm.responseSchemaJson" rows="4" />
          </label>
          <label class="field-wide">
            <span>重试策略 JSON</span>
            <textarea v-model="capabilityForm.retryPolicyJson" rows="3" />
          </label>
        </section>

        <footer class="dialog-actions">
          <button
            type="button"
            class="secondary-action"
            :disabled="loading"
            @click="capabilityDialogMode === 'list' ? closeCapabilityDialog() : backToCapabilityList()"
          >
            {{ capabilityDialogMode === 'list' ? '关闭' : '返回列表' }}
          </button>
          <div>
            <button
              v-if="capabilityDialogMode === 'create'"
              type="button"
              class="primary-action"
              :disabled="loading"
              @click="saveCapability"
            >
              保存能力
            </button>
            <button
              v-if="capabilityDialogMode === 'edit'"
              type="button"
              class="primary-action"
              :disabled="loading"
              @click="updateCapability"
            >
              保存修改
            </button>
          </div>
        </footer>
      </section>
    </div>
  </section>
</template>

<style scoped>
@import './dataPrepareConfig.css';

.platform-page {
  position: relative;
}

.platform-heading {
  align-items: center;
  padding: 18px 20px;
}

.platform-heading h1 {
  font-size: 22px;
}

.platform-heading span {
  display: block;
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.page-action {
  width: auto;
  min-width: 144px;
  min-height: 40px;
  border-radius: 6px;
  padding: 0 14px;
  font-size: 14px;
  white-space: nowrap;
  box-shadow: none;
}

.query-panel {
  display: grid;
  grid-template-columns: minmax(260px, 420px);
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  padding: 14px 16px;
}

.query-panel label {
  display: grid;
  gap: 6px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.query-panel input {
  min-height: 40px;
  border: 1px solid #d7e0ec;
  border-radius: 6px;
  background: #fff;
  color: #172033;
  padding: 0 11px;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.row-actions button,
.pagination-bar button {
  min-height: 32px;
  border: 1px solid #d7e0ec;
  border-radius: 6px;
  background: #fff;
  color: #2563eb;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
  padding: 0 10px;
}

.row-actions button:disabled,
.pagination-bar button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid #edf2f7;
  padding: 12px 16px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.pagination-bar div {
  display: flex;
  gap: 8px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.46);
}

.create-dialog {
  width: min(760px, 100%);
  max-height: min(86vh, 720px);
  overflow: auto;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.16);
}

.dialog-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px 12px;
  border-bottom: 1px solid #edf2f7;
}

.dialog-header p {
  margin: 0 0 4px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.dialog-header h2 {
  margin: 0;
  color: #172033;
  font-size: 20px;
  line-height: 1.35;
}

.icon-close {
  display: inline-grid;
  width: 36px;
  height: 36px;
  min-width: 36px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  place-items: center;
  background: #f8fafc;
  color: #64748b;
  font-size: 18px;
  font-weight: 800;
  line-height: 1;
  padding: 0;
  cursor: pointer;
  transition: border-color 160ms ease, background-color 160ms ease, color 160ms ease, box-shadow 160ms ease;
}

.icon-close:hover {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #2563eb;
}

.icon-close:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.14);
}

.icon-close:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.dialog-helper {
  margin: 0;
  padding: 12px 22px 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.create-form,
.detail-section,
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
  padding: 18px 22px 20px;
}

.create-form label {
  display: grid;
  gap: 7px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.create-form em {
  color: #dc2626;
  font-style: normal;
}

.create-form input,
.create-form select,
.create-form textarea {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  border: 1px solid #d7e0ec;
  border-radius: 6px;
  background: #ffffff;
  color: #172033;
  font: inherit;
  font-weight: 600;
  outline: none;
}

.create-form input,
.create-form select {
  min-height: 44px;
  padding: 0 11px;
}

.create-form select {
  max-width: 360px;
}

.create-form .field-wide select {
  max-width: 480px;
}

.create-form input[readonly] {
  background: #f8fafc;
  color: #64748b;
}

.create-form textarea {
  min-height: 96px;
  padding: 10px 11px;
  resize: vertical;
  line-height: 1.5;
}

.create-form input:focus,
.create-form select:focus,
.create-form textarea:focus {
  border-color: #2f6df6;
  box-shadow: 0 0 0 3px rgba(47, 109, 246, 0.12);
}

.field-wide {
  grid-column: 1 / -1;
}

.detail-grid {
  grid-column: 1 / -1;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 10px 14px;
  padding: 0;
}

.detail-grid span {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.detail-grid strong {
  color: #172033;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.external-key-panel {
  grid-column: 1 / -1;
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid #d7e0ec;
  border-radius: 8px;
  background: #f8fafc;
}

.external-key-panel header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.external-key-panel h3 {
  margin: 0;
  color: #172033;
  font-size: 15px;
}

.external-key-panel p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.external-key-detail {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 8px 12px;
  color: #172033;
  font-size: 13px;
}

.external-key-detail span,
.external-key-empty,
.external-key-secret small {
  color: #64748b;
}

.external-key-detail strong {
  overflow-wrap: anywhere;
}

.external-key-empty {
  font-size: 13px;
}

.external-key-secret {
  display: grid;
  gap: 7px;
}

.external-key-secret span {
  color: #172033;
  font-size: 13px;
  font-weight: 700;
}

.external-key-secret textarea {
  width: 100%;
  min-height: 72px;
  resize: vertical;
  box-sizing: border-box;
  border: 1px solid #d7e0ec;
  border-radius: 6px;
  padding: 10px 11px;
  background: #ffffff;
  color: #172033;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
}

.dialog-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 22px 20px;
  border-top: 1px solid #edf2f7;
  background: #fbfdff;
}

.dialog-actions > div {
  display: flex;
  gap: 10px;
}

.primary-action,
.secondary-action,
.ghost-action {
  min-height: 40px;
  border-radius: 6px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.primary-action {
  border: 1px solid #2f6df6;
  background: #2f6df6;
  color: #ffffff;
}

.secondary-action,
.ghost-action {
  border: 1px solid #d7e0ec;
  background: #ffffff;
  color: #334155;
}

.ghost-action {
  color: #2563eb;
}

.capability-dialog {
  width: min(980px, 100%);
}

.capability-section {
  padding: 16px 22px 20px;
}

.capability-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.capability-toolbar div {
  display: grid;
  gap: 4px;
}

.capability-toolbar strong {
  color: #172033;
  font-size: 15px;
}

.capability-toolbar span {
  color: #64748b;
  font-size: 12px;
}

.compact-table table {
  min-width: 780px;
}

.compact-table td {
  overflow-wrap: anywhere;
}

.capability-form {
  padding-top: 18px;
}

.check-line {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  margin-top: 20px;
}

.check-line input {
  width: 16px;
  min-width: 16px;
  height: 16px;
}

@media (max-width: 720px) {
  .page-action {
    width: 100%;
  }

  .query-panel,
  .create-form,
  .detail-section,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .external-key-panel header {
    align-items: stretch;
    flex-direction: column;
  }

  .modal-overlay {
    align-items: end;
    padding: 12px;
  }

  .create-dialog {
    max-height: 92vh;
  }

  .dialog-actions,
  .dialog-actions > div,
  .pagination-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
