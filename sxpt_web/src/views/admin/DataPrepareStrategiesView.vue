<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import TimedToast from '../../components/ui/TimedToast.vue';
import {
  authApi,
  dataPrepareApi,
  isDataPrepareConfigIncompleteError,
  type BusinessModule,
  type ConnectorSystem,
  type DataPrepareMetadata,
  type ModuleDataStrategy,
  type ModuleDataStrategyRequest,
  type TeachingDataTemplate
} from '../../services/trainingApi';

const PAGE_SIZE = 10;

const session = authApi.getSession();
const tenantId = ref(session?.user.tenantId || 'demo-tenant');
const operatorId = session?.user.userId || 'admin';
const metadata = ref<DataPrepareMetadata | null>(null);
const connectorSystemId = ref('');
const systemKeyword = ref('');
const businessModuleId = ref('');
const templateId = ref('');
const sceneType = ref('PRACTICE');
const systems = ref<ConnectorSystem[]>([]);
const modules = ref<BusinessModule[]>([]);
const templates = ref<TeachingDataTemplate[]>([]);
const strategies = ref<ModuleDataStrategy[]>([]);
const loading = ref(false);
const currentPage = ref(1);
const dialogMode = ref<'none' | 'detail' | 'create' | 'edit'>('none');
const selectedStrategy = ref<ModuleDataStrategy | null>(null);
const notice = ref({
  show: false,
  type: 'info' as 'success' | 'error' | 'info',
  message: ''
});

const strategyForm = reactive<ModuleDataStrategyRequest>({
  strategyCode: '',
  moduleName: '',
  templateId: '',
  needPreData: true,
  dataSourceStrategy: 'MOCK_GENERATE',
  initExternalStatus: 'DRAFT',
  targetExternalStatus: 'SUBMITTED',
  defaultOrgRolePolicyJson: '{"org":"required","role":"required"}',
  sharePolicy: 'ATTEMPT_EXCLUSIVE',
  regeneratePolicy: 'ON_ATTEMPT',
  lockPolicy: 'NONE',
  prepareTiming: 'ON_DEMAND',
  poolSizePolicyJson: '',
  validationPolicyJson: '{"requiredStatus":"DRAFT"}',
  expirePolicyJson: '',
  resultCheckPolicyJson: '',
  archivePolicyJson: '',
  updateBy: operatorId
});

const strategyJsonBuilder = reactive({
  requiredOrg: 'required',
  requiredRole: 'required',
  minReadyCount: 1,
  maxReadyCount: 50,
  requiredStatus: 'DRAFT',
  expireHours: 24,
  archiveMode: 'MANUAL'
});

const sceneLabelFallback: Record<string, string> = {
  RECORD: '\u5907\u6848',
  LEARN: '\u5b66\u4e60',
  PRACTICE: '\u7ec3\u4e60',
  EXAM: '\u8003\u8bd5'
};

const dataSourceStrategyLabels: Record<string, string> = {
  MOCK_GENERATE: '\u7cfb\u7edf\u751f\u6210',
  THIRD_PARTY_CREATE: '\u539f\u5e73\u53f0\u751f\u6210',
  TEMPLATE_CREATE: '\u6309\u6a21\u677f\u751f\u6210',
  CLASSIC_CASE_CREATE: '\u6309\u7ecf\u5178\u6848\u4f8b\u751f\u6210'
};

const sharePolicyLabels: Record<string, string> = {
  ATTEMPT_EXCLUSIVE: '\u6bcf\u6b21\u4f5c\u4e1a\u72ec\u5360',
  STUDENT_EXCLUSIVE: '\u5b66\u751f\u72ec\u5360',
  SHARED_READONLY: '\u5171\u4eab\u53ea\u8bfb'
};

const regeneratePolicyLabels: Record<string, string> = {
  ON_ATTEMPT: '\u6bcf\u6b21\u8fdb\u5165\u91cd\u65b0\u751f\u6210',
  ON_FAILURE: '\u5931\u8d25\u540e\u91cd\u65b0\u751f\u6210',
  NEVER: '\u4e0d\u81ea\u52a8\u91cd\u5efa'
};

const lockPolicyLabels: Record<string, string> = {
  NONE: '\u4e0d\u9501\u5b9a',
  LOCK_ON_ASSIGN: '\u5206\u914d\u540e\u9501\u5b9a',
  LOCK_ON_START: '\u5f00\u59cb\u540e\u9501\u5b9a'
};

const prepareTimingLabels: Record<string, string> = {
  ON_PUBLISH: '\u53d1\u5e03\u65f6\u51c6\u5907',
  BEFORE_START: '\u5f00\u59cb\u524d\u51c6\u5907',
  ON_DEMAND: '\u6309\u9700\u51c6\u5907'
};

const requirementLabels: Record<string, string> = {
  required: '\u5fc5\u586b',
  optional: '\u53ef\u9009',
  none: '\u4e0d\u6821\u9a8c'
};

const archiveModeLabels: Record<string, string> = {
  MANUAL: '\u624b\u52a8\u5f52\u6863',
  AFTER_EXPIRE: '\u8fc7\u671f\u540e\u5f52\u6863',
  AFTER_TASK_END: '\u4efb\u52a1\u7ed3\u675f\u540e\u5f52\u6863'
};

function getDefaultStrategyForm(): ModuleDataStrategyRequest {
  return {
    strategyCode: `LOCAL_STRATEGY_${Date.now()}`,
    moduleName: selectedModule.value?.moduleName || '',
    templateId: templateId.value,
    needPreData: true,
    dataSourceStrategy: metadata.value?.strategyDefaults.dataSourceStrategy || 'MOCK_GENERATE',
    initExternalStatus: metadata.value?.strategyDefaults.initExternalStatus || 'DRAFT',
    targetExternalStatus: metadata.value?.strategyDefaults.targetExternalStatus || 'SUBMITTED',
    defaultOrgRolePolicyJson: '{"org":"required","role":"required"}',
    sharePolicy: metadata.value?.strategyDefaults.sharePolicy || 'ATTEMPT_EXCLUSIVE',
    regeneratePolicy: metadata.value?.strategyDefaults.regeneratePolicy || 'ON_ATTEMPT',
    lockPolicy: metadata.value?.strategyDefaults.lockPolicy || 'NONE',
    prepareTiming: metadata.value?.strategyDefaults.prepareTiming || 'ON_DEMAND',
    poolSizePolicyJson: '',
    validationPolicyJson: metadata.value?.strategyDefaults.validationPolicyJson || '{"requiredStatus":"DRAFT"}',
    expirePolicyJson: '',
    resultCheckPolicyJson: '',
    archivePolicyJson: '',
    createBy: operatorId,
    updateBy: operatorId
  };
}

const selectedModule = computed(() =>
  modules.value.find((item) => item.id === businessModuleId.value)
);

const selectedTemplate = computed(() =>
  templates.value.find((item) => item.id === templateId.value)
);

const filteredSystems = computed(() => {
  const keyword = systemKeyword.value.trim().toLowerCase();
  if (!keyword) return systems.value;
  return systems.value.filter((item) =>
    (item.systemName || '').toLowerCase().includes(keyword)
  );
});

const totalPages = computed(() => Math.max(1, Math.ceil(strategies.value.length / PAGE_SIZE)));

const pagedStrategies = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE;
  return strategies.value.slice(start, start + PAGE_SIZE);
});
const visibleSceneTypes = computed(() =>
  (metadata.value?.sceneTypes ?? [
    { code: 'RECORD', label: '备案', visible: true },
    { code: 'LEARN', label: '学习', visible: true },
    { code: 'PRACTICE', label: '练习', visible: true },
    { code: 'EXAM', label: '考试', visible: true }
  ]).filter((item) => item.visible)
);
const visibleDataSourceStrategies = computed(() =>
  (metadata.value?.dataSourceStrategies ?? [{ code: 'MOCK_GENERATE', label: '系统生成', visible: true }])
    .filter((item) => item.visible)
);
const visiblePrepareTimings = computed(() =>
  (metadata.value?.prepareTimings ?? [{ code: 'ON_DEMAND', label: '按需准备', visible: true }])
    .filter((item) => item.visible)
);
const visibleSharePolicies = computed(() =>
  (metadata.value?.sharePolicies ?? [{ code: 'ATTEMPT_EXCLUSIVE', label: '作业独占', visible: true }])
    .filter((item) => item.visible)
);
const visibleRegeneratePolicies = computed(() =>
  (metadata.value?.regeneratePolicies ?? [{ code: 'ON_ATTEMPT', label: '每次进入重新生成', visible: true }])
    .filter((item) => item.visible)
);
const visibleLockPolicies = computed(() =>
  (metadata.value?.lockPolicies ?? [{ code: 'NONE', label: '不锁定', visible: true }])
    .filter((item) => item.visible)
);
const strategyEnableChecklist = computed(() => {
  const orgRole = parseJsonObject(strategyForm.defaultOrgRolePolicyJson);
  const validation = parseJsonObject(strategyForm.validationPolicyJson);
  const poolSize = parseJsonObject(strategyForm.poolSizePolicyJson);
  const minReadyCount = numberValue(poolSize.minReadyCount, Number.NaN);
  const maxReadyCount = numberValue(poolSize.maxReadyCount, Number.NaN);
  const hasPoolBounds = Number.isFinite(minReadyCount) && Number.isFinite(maxReadyCount);
  return [
    {
      label: '已绑定当前场景下的启用模板',
      done: Boolean(strategyForm.templateId && templates.value.some((template) => template.id === strategyForm.templateId))
    },
    {
      label: '已配置单位和角色要求',
      done: hasText(orgRole.org) && hasText(orgRole.role)
    },
    {
      label: '已配置初始状态校验',
      done: hasText(validation.requiredStatus)
    },
    {
      label: '数据池容量有效且最小值不大于最大值',
      done: hasPoolBounds && minReadyCount >= 0 && maxReadyCount >= 0 && minReadyCount <= maxReadyCount
    }
  ];
});

onMounted(initialize);

watch(connectorSystemId, async () => {
  businessModuleId.value = '';
  templateId.value = '';
  currentPage.value = 1;
  await loadModules();
  await loadTemplates();
  await loadStrategies();
  if (dialogMode.value === 'create') {
    strategyForm.moduleName = selectedModule.value?.moduleName || '';
    strategyForm.templateId = templateId.value;
  }
});

watch([businessModuleId, sceneType], async () => {
  templateId.value = '';
  currentPage.value = 1;
  await loadTemplates();
  await loadStrategies();
  if (dialogMode.value === 'create') {
    strategyForm.moduleName = selectedModule.value?.moduleName || '';
    strategyForm.templateId = templateId.value;
  }
});

watch(strategyJsonBuilder, () => syncStrategyJsonFieldsFromBuilder(), { deep: true });

/**
 * 业务功能：初始化策略管理页面上下文，确保策略始终挂在真实平台、模块和模板之下。
 * 关键流程：按平台、模块、场景逐层加载，避免编辑策略时引用已经失效的业务边界。
 */
async function initialize() {
  await run(async () => {
    await loadMetadata();
    systems.value = await dataPrepareApi.listConnectorSystems(tenantId.value);
    connectorSystemId.value = systems.value[0]?.id || '';
    await loadModules();
    await loadTemplates();
    await loadStrategies();
  }, '\u7b56\u7565\u5217\u8868\u5df2\u52a0\u8f7d');
}

async function loadMetadata() {
  try {
    metadata.value = await dataPrepareApi.getDataPrepareMetadata();
    sceneType.value = metadata.value.strategyDefaults.sceneType || sceneType.value;
    const validation = parseJsonObject(metadata.value.strategyDefaults.validationPolicyJson);
    strategyJsonBuilder.requiredStatus =
      String(validation.requiredStatus || strategyJsonBuilder.requiredStatus);
    fillStrategyForm(getDefaultStrategyForm());
  } catch {
    metadata.value = null;
  }
}

/**
 * 业务功能：读取当前平台下的业务模块，作为策略归属的第一层约束。
 * 关键流程：平台切换后清空旧模块和旧模板，防止策略编辑错绑旧上下文。
 */
async function loadModules() {
  modules.value = [];
  if (!connectorSystemId.value) return;
  modules.value = await dataPrepareApi.listAllBusinessModules({
    tenantId: tenantId.value,
    connectorSystemId: connectorSystemId.value
  });
  businessModuleId.value = modules.value[0]?.id || '';
}

/**
 * 业务功能：读取当前模块和场景下可绑定的初始数据模板。
 * 关键流程：策略启用依赖模板，因此没有模板时只允许查看和编辑已有策略。
 */
async function loadTemplates() {
  templates.value = [];
  if (!connectorSystemId.value || !selectedModule.value) return;
  templates.value = await dataPrepareApi.listActiveTemplatesByModuleScene({
    tenantId: tenantId.value,
    connectorSystemId: connectorSystemId.value,
    moduleCode: selectedModule.value.moduleCode,
    sceneType: sceneType.value
  });
  templateId.value = templates.value[0]?.id || '';
}

/**
 * 业务功能：读取当前模块下的策略列表，供管理员核对可发布的准备规则。
 * 关键流程：管理页展示草稿和启用策略，避免新建未启用策略刷新后从列表消失。
 */
async function loadStrategies() {
  strategies.value = [];
  if (!connectorSystemId.value || !businessModuleId.value) return;
  strategies.value = await dataPrepareApi.listModuleDataStrategies({
    tenantId: tenantId.value,
    connectorSystemId: connectorSystemId.value,
    businessModuleId: businessModuleId.value
  });
  normalizePage();
}

/**
 * 业务功能：创建初始数据策略草稿，管理员确认配置完整后再手动启用。
 * 关键流程：创建前固定当前模块和模板快照，避免异步选择变化造成错配。
 */
function openCreateStrategyDialog() {
  fillStrategyForm(getDefaultStrategyForm());
  syncStrategyJsonFieldsFromBuilder();
  dialogMode.value = 'create';
}

async function createLocalStrategy() {
  const module = selectedModule.value;
  syncStrategyJsonFieldsFromBuilder();
  if (!connectorSystemId.value || !module || !strategyForm.templateId) {
    notify('error', '\u8bf7\u5148\u9009\u62e9\u539f\u5e73\u53f0\u3001\u4e1a\u52a1\u6a21\u5757\u548c\u6570\u636e\u6a21\u677f');
    return;
  }
  await run(async () => {
    const created = await dataPrepareApi.createModuleDataStrategy({
      tenantId: tenantId.value,
      connectorSystemId: connectorSystemId.value,
      businessModuleId: module.id,
      moduleCode: module.moduleCode,
      moduleName: strategyForm.moduleName?.trim() || module.moduleName,
      sceneType: sceneType.value,
      ...buildStrategyUpdateRequest(),
      templateId: strategyForm.templateId,
      createBy: operatorId,
      updateBy: operatorId
    });
    strategies.value = [created, ...strategies.value];
    currentPage.value = 1;
    closeDialog();
  }, '\u5df2\u521b\u5efa\u521d\u59cb\u6570\u636e\u7b56\u7565\uff0c\u8bf7\u786e\u8ba4\u914d\u7f6e\u5b8c\u6574\u540e\u624b\u52a8\u542f\u7528');
}

/**
 * 业务功能：打开策略详情弹窗，展示后端真实详情而不是列表缓存。
 * 关键流程：按 ID 重新查询详情，确保版本、模板和策略参数与数据库一致。
 */
async function showStrategyDetail(strategy: ModuleDataStrategy) {
  await run(async () => {
    selectedStrategy.value = await dataPrepareApi.getModuleDataStrategy(strategy.id);
    dialogMode.value = 'detail';
  }, '\u7b56\u7565\u8be6\u60c5\u5df2\u52a0\u8f7d');
}

/**
 * 业务功能：打开策略编辑弹窗，维护影响实例分配的核心规则。
 * 关键流程：先读取详情再回填表单，避免列表简略字段覆盖完整策略配置。
 */
async function editStrategy(strategy: ModuleDataStrategy) {
  await run(async () => {
    const detail = await dataPrepareApi.getModuleDataStrategy(strategy.id);
    selectedStrategy.value = detail;
    fillStrategyForm(detail);
    dialogMode.value = 'edit';
  }, '\u7b56\u7565\u7f16\u8f91\u4fe1\u606f\u5df2\u52a0\u8f7d');
}

/**
 * 业务功能：提交策略编辑，更新初始数据生成、分配、重建、锁定、模板和组织角色策略。
 * 关键流程：只提交后端允许更新的字段，并在本地替换对应列表行。
 */
async function updateStrategy() {
  if (!selectedStrategy.value) return;
  syncStrategyJsonFieldsFromBuilder();
  if (!strategyForm.moduleName?.trim() || !strategyForm.templateId?.trim()) {
    notify('error', '\u8bf7\u586b\u5199\u6a21\u5757\u540d\u79f0\u5e76\u9009\u62e9\u6a21\u677f');
    return;
  }
  await run(async () => {
    const updated = await dataPrepareApi.updateModuleDataStrategy(
      selectedStrategy.value!.id,
      buildStrategyUpdateRequest()
    );
    strategies.value = strategies.value.map((item) => (item.id === updated.id ? updated : item));
    selectedStrategy.value = updated;
    closeDialog();
  }, '\u7b56\u7565\u5df2\u66f4\u65b0');
}

/**
 * 业务功能：启用或停用模块数据策略。
 * 关键流程：启用由后端校验模板、模块和平台能力，前端只同步当前行状态。
 */
async function toggleStrategyStatus(strategy: ModuleDataStrategy) {
  if (strategy.status !== 'ACTIVE') {
    loading.value = true;
    closeNotice();
    try {
      const updated = await dataPrepareApi.enableModuleDataStrategy(strategy.id);
      strategies.value = strategies.value.map((item) => (item.id === updated.id ? updated : item));
      notify('success', '\u7b56\u7565\u5df2\u542f\u7528');
    } catch (err) {
      notify('error', strategyEnableErrorMessage(err));
      await openStrategyEditorAfterEnableFailure(strategy);
    } finally {
      loading.value = false;
    }
    return;
  }
  await run(async () => {
    const updated = await dataPrepareApi.disableModuleDataStrategy(strategy.id);
    strategies.value = strategies.value.map((item) => (item.id === updated.id ? updated : item));
  }, '\u7b56\u7565\u5df2\u505c\u7528');
}

/**
 * 业务功能：策略启用失败后打开编辑弹窗，方便管理员立即补齐数据分配规则。
 * 关键流程：不复用通用 run，避免清空刚刚展示的启用失败提示。
 */
async function openStrategyEditorAfterEnableFailure(strategy: ModuleDataStrategy) {
  try {
    const detail = await dataPrepareApi.getModuleDataStrategy(strategy.id);
    selectedStrategy.value = detail;
    fillStrategyForm(detail);
    dialogMode.value = 'edit';
  } catch {
    // 保留启用失败提示，详情加载失败不覆盖原始原因。
  }
}

/**
 * 业务功能：把后端策略启用参数错误翻译成维护人员可执行的配置要求。
 * 关键流程：策略启用依赖模板、准备时机、单位角色策略、状态校验和平台能力。
 */
function strategyEnableErrorMessage(err: unknown) {
  const rawMessage = err instanceof Error ? err.message : '\u64cd\u4f5c\u5931\u8d25';
  const normalized = rawMessage.trim();
  const guide = '\u4e0b\u4e00\u6b65\uff1a\u5982\u7f3a\u6a21\u677f\u6216\u7b56\u7565\u914d\u7f6e\uff0c\u8bf7\u5728\u5f53\u524d\u7f16\u8f91\u5f39\u7a97\u8865\u9f50\uff1b\u5982\u7f3a\u5e73\u53f0\u80fd\u529b\uff0c\u8bf7\u5230\u201c\u539f\u5e73\u53f0\u7cfb\u7edf\u7ef4\u62a4 -> \u80fd\u529b\u201d\u8865\u9f50\u80fd\u529b\u58f0\u660e\u3002';
  if (isDataPrepareConfigIncompleteError(err)) {
    return `策略启用失败：${normalized}。${guide}`;
  }
  return `${normalized}。${guide}`;
}

/**
 * 业务功能：将策略详情转换为表单字段。
 * 关键流程：保留后端已有参数配置，避免编辑非参数字段时丢失组织角色、校验和归档策略。
 */
function fillStrategyForm(strategy: ModuleDataStrategy | ModuleDataStrategyRequest) {
  strategyForm.strategyCode = strategy.strategyCode || '';
  strategyForm.moduleName = strategy.moduleName || '';
  strategyForm.templateId = strategy.templateId || '';
  strategyForm.needPreData = strategy.needPreData ?? true;
  strategyForm.dataSourceStrategy = strategy.dataSourceStrategy || metadata.value?.strategyDefaults.dataSourceStrategy || 'MOCK_GENERATE';
  strategyForm.initExternalStatus = strategy.initExternalStatus || metadata.value?.strategyDefaults.initExternalStatus || '';
  strategyForm.targetExternalStatus = strategy.targetExternalStatus || metadata.value?.strategyDefaults.targetExternalStatus || '';
  strategyForm.defaultOrgRolePolicyJson = strategy.defaultOrgRolePolicyJson || '';
  strategyForm.sharePolicy = strategy.sharePolicy || metadata.value?.strategyDefaults.sharePolicy || 'ATTEMPT_EXCLUSIVE';
  strategyForm.regeneratePolicy = strategy.regeneratePolicy || metadata.value?.strategyDefaults.regeneratePolicy || 'ON_ATTEMPT';
  strategyForm.lockPolicy = strategy.lockPolicy || metadata.value?.strategyDefaults.lockPolicy || 'NONE';
  strategyForm.prepareTiming = strategy.prepareTiming || metadata.value?.strategyDefaults.prepareTiming || 'ON_DEMAND';
  strategyForm.poolSizePolicyJson = strategy.poolSizePolicyJson || '';
  strategyForm.validationPolicyJson = strategy.validationPolicyJson || '';
  strategyForm.expirePolicyJson = strategy.expirePolicyJson || '';
  strategyForm.resultCheckPolicyJson = strategy.resultCheckPolicyJson || '';
  strategyForm.archivePolicyJson = strategy.archivePolicyJson || '';
  strategyForm.updateBy = operatorId;
  syncStrategyJsonBuilderFromForm();
}

/**
 * 业务功能：用结构化表单生成策略参数，避免管理员直接维护 JSON。
 * 关键流程：把身份、池容量、校验、过期和归档策略转换为后端兼容字段。
 */
function syncStrategyJsonFieldsFromBuilder() {
  strategyForm.defaultOrgRolePolicyJson = stringifyJson({
    org: strategyJsonBuilder.requiredOrg,
    role: strategyJsonBuilder.requiredRole
  });
  strategyForm.poolSizePolicyJson = stringifyJson({
    minReadyCount: Number(strategyJsonBuilder.minReadyCount) || 1,
    maxReadyCount: Number(strategyJsonBuilder.maxReadyCount) || 50
  });
  strategyForm.validationPolicyJson = stringifyJson({
    requiredStatus: strategyJsonBuilder.requiredStatus.trim() || metadata.value?.strategyDefaults.initExternalStatus || 'DRAFT'
  });
  strategyForm.expirePolicyJson = stringifyJson({
    expireHours: Number(strategyJsonBuilder.expireHours) || 24
  });
  strategyForm.archivePolicyJson = stringifyJson({
    mode: strategyJsonBuilder.archiveMode
  });
}

function applyStrategyJsonBuilder() {
  syncStrategyJsonFieldsFromBuilder();
  notify('success', '\u5df2\u6839\u636e\u8868\u5355\u751f\u6210\u7b56\u7565\u53c2\u6570');
}

/**
 * 业务功能：编辑已有策略时反向读取参数字段，尽量回填到结构化表单。
 * 关键流程：能识别的字段回填，不能识别的自定义配置仍保留在原始参数里。
 */
function syncStrategyJsonBuilderFromForm() {
  const orgRole = parseJsonObject(strategyForm.defaultOrgRolePolicyJson);
  const poolSize = parseJsonObject(strategyForm.poolSizePolicyJson);
  const validation = parseJsonObject(strategyForm.validationPolicyJson);
  const expire = parseJsonObject(strategyForm.expirePolicyJson);
  const archive = parseJsonObject(strategyForm.archivePolicyJson);
  strategyJsonBuilder.requiredOrg = stringValue(orgRole.org, strategyJsonBuilder.requiredOrg);
  strategyJsonBuilder.requiredRole = stringValue(orgRole.role, strategyJsonBuilder.requiredRole);
  strategyJsonBuilder.requiredStatus = stringValue(validation.requiredStatus, strategyJsonBuilder.requiredStatus);
  strategyJsonBuilder.archiveMode = stringValue(archive.mode, strategyJsonBuilder.archiveMode);
  strategyJsonBuilder.minReadyCount = numberValue(poolSize.minReadyCount, strategyJsonBuilder.minReadyCount);
  strategyJsonBuilder.maxReadyCount = numberValue(poolSize.maxReadyCount, strategyJsonBuilder.maxReadyCount);
  strategyJsonBuilder.expireHours = numberValue(expire.expireHours, strategyJsonBuilder.expireHours);
}

/**
 * 业务功能：构造策略更新请求。
 * 关键流程：后端更新接口按 null 覆盖字段，因此前端显式携带全部可编辑字段。
 */
function buildStrategyUpdateRequest(): ModuleDataStrategyRequest {
  return {
    strategyCode: strategyForm.strategyCode?.trim(),
    moduleName: strategyForm.moduleName?.trim(),
    templateId: strategyForm.templateId,
    needPreData: strategyForm.needPreData,
    dataSourceStrategy: strategyForm.dataSourceStrategy,
    initExternalStatus: strategyForm.initExternalStatus?.trim(),
    targetExternalStatus: strategyForm.targetExternalStatus?.trim(),
    defaultOrgRolePolicyJson: strategyForm.defaultOrgRolePolicyJson?.trim(),
    sharePolicy: strategyForm.sharePolicy,
    regeneratePolicy: strategyForm.regeneratePolicy,
    lockPolicy: strategyForm.lockPolicy,
    prepareTiming: strategyForm.prepareTiming,
    poolSizePolicyJson: strategyForm.poolSizePolicyJson?.trim(),
    validationPolicyJson: strategyForm.validationPolicyJson?.trim(),
    expirePolicyJson: strategyForm.expirePolicyJson?.trim(),
    resultCheckPolicyJson: strategyForm.resultCheckPolicyJson?.trim(),
    archivePolicyJson: strategyForm.archivePolicyJson?.trim(),
    updateBy: operatorId
  };
}

async function run(action: () => Promise<void>, successMessage: string) {
  loading.value = true;
  closeNotice();
  try {
    await action();
    notify('success', successMessage);
  } catch (err) {
    notify('error', err instanceof Error ? err.message : '\u64cd\u4f5c\u5931\u8d25');
  } finally {
    loading.value = false;
  }
}

function handleSystemKeywordChange() {
  if (
    connectorSystemId.value &&
    !filteredSystems.value.some((item) => item.id === connectorSystemId.value)
  ) {
    connectorSystemId.value = filteredSystems.value[0]?.id || '';
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

function closeDialog() {
  dialogMode.value = 'none';
  selectedStrategy.value = null;
}

function notify(type: 'success' | 'error' | 'info', text: string) {
  notice.value = { show: true, type, message: text };
}

function closeNotice() {
  notice.value = { ...notice.value, show: false, message: '' };
}

function stringifyJson(value: Record<string, unknown>) {
  return JSON.stringify(value, null, 2);
}

function parseJsonObject(value?: string) {
  if (!value) return {} as Record<string, unknown>;
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? (parsed as Record<string, unknown>)
      : {};
  } catch {
    return {};
  }
}

function stringValue(value: unknown, fallback: string) {
  return typeof value === 'string' && value.trim() ? value : fallback;
}

function hasText(value: unknown) {
  return typeof value === 'string' && value.trim().length > 0;
}

function numberValue(value: unknown, fallback: number) {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}

function optionText(value: string | undefined, labels: Record<string, string>) {
  if (!value) return '-';
  return labels[value] || value;
}

function jsonText(value: unknown, labels?: Record<string, string>) {
  if (typeof value !== 'string' || !value.trim()) return '-';
  return labels?.[value] || value;
}

/**
 * 业务功能：把策略 JSON 存储字段翻译为运维可读摘要。
 * 关键流程：只解析已知字段并保留原始值兜底，避免老数据或自定义策略在详情页丢失语义。
 */
function buildStrategySummary(strategy: ModuleDataStrategy | ModuleDataStrategyRequest) {
  const orgRole = parseJsonObject(strategy.defaultOrgRolePolicyJson);
  const poolSize = parseJsonObject(strategy.poolSizePolicyJson);
  const validation = parseJsonObject(strategy.validationPolicyJson);
  const expire = parseJsonObject(strategy.expirePolicyJson);
  const archive = parseJsonObject(strategy.archivePolicyJson);
  return [
    { label: '单位要求', value: jsonText(orgRole.org, requirementLabels) },
    { label: '角色要求', value: jsonText(orgRole.role, requirementLabels) },
    { label: '最小可用数', value: String(numberValue(poolSize.minReadyCount, 0)) },
    { label: '最大可用数', value: String(numberValue(poolSize.maxReadyCount, 0)) },
    { label: '初始状态校验', value: jsonText(validation.requiredStatus) },
    { label: '过期时间', value: `${numberValue(expire.expireHours, 0)} 小时` },
    { label: '归档方式', value: jsonText(archive.mode, archiveModeLabels) }
  ];
}

function policyText(value: string | undefined, labels: Record<string, string>) {
  return optionText(value, labels);
}

function metadataOptionText(
  value: string | undefined,
  options: Array<{ code: string; label: string }>,
  labels: Record<string, string>
) {
  if (!value) return '-';
  return options.find((item) => item.code === value)?.label || optionText(value, labels);
}

function sceneText(value: string) {
  return visibleSceneTypes.value.find((item) => item.code === value)?.label || sceneLabelFallback[value] || value;
}

function statusClass(status?: string) {
  return status === 'ACTIVE' ? 'success' : 'info';
}
</script>

<template>
  <section class="config-page strategy-page">
    <TimedToast
      :show="notice.show"
      :type="notice.type"
      :message="notice.message"
      @close="closeNotice"
    />

    <header class="page-heading">
      <div>
        <p>数据准备 / 初始数据策略</p>
        <h1>模块初始数据准备策略</h1>
        <p>定义练习、学习和考试进入原平台学习环境前的数据生成、分配、重建和锁定规则。</p>
      </div>
      <button type="button" :disabled="loading || systems.length === 0" @click="openCreateStrategyDialog">
        创建初始数据策略
      </button>
    </header>

    <section class="context-panel">
      <label>
        <span>系统名称</span>
        <input
          v-model="systemKeyword"
          type="search"
          placeholder="输入系统名称模糊查询"
          @input="handleSystemKeywordChange"
        />
      </label>
      <label>
        <span>原平台</span>
        <select v-model="connectorSystemId">
          <option value="">请选择原平台</option>
          <option v-for="system in filteredSystems" :key="system.id" :value="system.id">
            {{ system.systemName }}
          </option>
        </select>
      </label>
      <label>
        <span>业务模块</span>
        <select v-model="businessModuleId">
          <option value="">请选择业务模块</option>
          <option v-for="module in modules" :key="module.id" :value="module.id">
            {{ module.moduleName }} / {{ module.moduleCode }}
          </option>
        </select>
      </label>
      <label>
        <span>教学场景</span>
        <select v-model="sceneType">
          <option v-for="scene in visibleSceneTypes" :key="scene.code" :value="scene.code">
            {{ scene.label }}
          </option>
        </select>
      </label>
      <label>
        <span>模板</span>
        <select v-model="templateId">
          <option value="">请选择模板</option>
          <option v-for="template in templates" :key="template.id" :value="template.id">
            {{ template.templateName }}
          </option>
        </select>
      </label>
    </section>

    <section class="metrics">
      <article>
        <span>全部策略</span>
        <strong>{{ strategies.length }}</strong>
      </article>
      <article>
        <span>可选模板</span>
        <strong>{{ templates.length }}</strong>
      </article>
      <article>
        <span>当前场景</span>
        <strong>{{ sceneText(sceneType) }}</strong>
      </article>
    </section>

    <section class="panel">
      <header class="panel-header">
        <h2>策略列表</h2>
        <span class="helper-text">共 {{ strategies.length }} 条，每页 {{ PAGE_SIZE }} 条</span>
      </header>
      <div class="panel-body table-wrap">
        <table>
          <thead>
            <tr>
              <th>策略编码</th>
              <th>模块</th>
              <th>场景</th>
              <th>模板</th>
              <th>来源</th>
              <th>分配</th>
              <th>重建</th>
              <th>锁定</th>
              <th>时机</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="strategy in pagedStrategies" :key="strategy.id">
              <td>{{ strategy.strategyCode || strategy.id }}</td>
              <td>{{ strategy.moduleName }} / {{ strategy.moduleCode }}</td>
              <td>{{ sceneText(strategy.sceneType) }}</td>
              <td>{{ strategy.templateId }}</td>
              <td>{{ metadataOptionText(strategy.dataSourceStrategy, visibleDataSourceStrategies, dataSourceStrategyLabels) }}</td>
              <td>{{ metadataOptionText(strategy.sharePolicy, visibleSharePolicies, sharePolicyLabels) }}</td>
              <td>{{ metadataOptionText(strategy.regeneratePolicy, visibleRegeneratePolicies, regeneratePolicyLabels) }}</td>
              <td>{{ metadataOptionText(strategy.lockPolicy, visibleLockPolicies, lockPolicyLabels) }}</td>
              <td>{{ metadataOptionText(strategy.prepareTiming, visiblePrepareTimings, prepareTimingLabels) }}</td>
              <td>
                <span class="status-badge" :class="statusClass(strategy.status)">
                  {{ strategy.status === 'ACTIVE' ? '已启用' : '未启用' }}
                </span>
              </td>
              <td>
                <div class="row-actions">
                  <button type="button" @click="showStrategyDetail(strategy)">详情</button>
                  <button type="button" @click="editStrategy(strategy)">编辑</button>
                  <button type="button" :disabled="loading" @click="toggleStrategyStatus(strategy)">
                    {{ strategy.status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="strategies.length === 0" class="empty-state">
          <strong>暂无策略</strong>
          <p>当前模块和场景还没有初始数据准备策略，创建并启用后才能进入发布和造数链路。</p>
        </div>
      </div>
      <footer v-if="strategies.length > PAGE_SIZE" class="pagination-bar">
        <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
        <div>
          <button type="button" :disabled="currentPage === 1" @click="previousPage">上一页</button>
          <button type="button" :disabled="currentPage === totalPages" @click="nextPage">下一页</button>
        </div>
      </footer>
    </section>

    <div v-if="dialogMode !== 'none'" class="dialog-backdrop" @click.self="closeDialog">
      <section class="dialog-panel">
        <header class="dialog-header">
          <div>
            <p>初始数据策略 {{ dialogMode === 'detail' ? '详情' : dialogMode === 'create' ? '创建' : '编辑' }}</p>
            <h2>{{ dialogMode === 'create' ? '创建初始数据策略' : selectedStrategy?.strategyCode || selectedStrategy?.id }}</h2>
          </div>
          <button type="button" class="icon-button" @click="closeDialog">×</button>
        </header>

        <div v-if="dialogMode === 'detail' && selectedStrategy" class="detail-content">
          <div class="detail-grid">
            <span>模块</span><strong>{{ selectedStrategy.moduleName }}</strong>
            <span>模块编码</span><strong>{{ selectedStrategy.moduleCode }}</strong>
            <span>场景</span><strong>{{ sceneText(selectedStrategy.sceneType) }}</strong>
            <span>模板 ID</span><strong>{{ selectedStrategy.templateId || '-' }}</strong>
            <span>数据来源</span><strong>{{ metadataOptionText(selectedStrategy.dataSourceStrategy, visibleDataSourceStrategies, dataSourceStrategyLabels) }}</strong>
            <span>分配策略</span><strong>{{ metadataOptionText(selectedStrategy.sharePolicy, visibleSharePolicies, sharePolicyLabels) }}</strong>
            <span>重建策略</span><strong>{{ metadataOptionText(selectedStrategy.regeneratePolicy, visibleRegeneratePolicies, regeneratePolicyLabels) }}</strong>
            <span>锁定策略</span><strong>{{ metadataOptionText(selectedStrategy.lockPolicy, visibleLockPolicies, lockPolicyLabels) }}</strong>
            <span>准备时机</span><strong>{{ metadataOptionText(selectedStrategy.prepareTiming, visiblePrepareTimings, prepareTimingLabels) }}</strong>
            <span>进入前造数</span><strong>{{ selectedStrategy.needPreData ? '需要' : '不需要' }}</strong>
            <span>策略版本</span><strong>{{ selectedStrategy.strategyVersion || 0 }}</strong>
            <span>状态</span><strong>{{ selectedStrategy.status === 'ACTIVE' ? '已启用' : '未启用' }}</strong>
          </div>
          <section class="policy-summary wide">
            <header>
              <strong>策略摘要</strong>
              <span>面向运维的业务化说明，原始接口参数保留在下方预览中。</span>
            </header>
            <div class="summary-grid">
              <article v-for="item in buildStrategySummary(selectedStrategy)" :key="item.label">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>
          </section>
          <details class="json-preview-block wide">
            <summary>接口参数预览</summary>
            <label>
              <span>初始身份策略</span>
              <textarea :value="selectedStrategy.defaultOrgRolePolicyJson || ''" rows="3" readonly />
            </label>
            <label>
              <span>初始数据池容量策略</span>
              <textarea :value="selectedStrategy.poolSizePolicyJson || ''" rows="3" readonly />
            </label>
            <label>
              <span>初始数据校验策略</span>
              <textarea :value="selectedStrategy.validationPolicyJson || ''" rows="3" readonly />
            </label>
            <label>
              <span>过期策略</span>
              <textarea :value="selectedStrategy.expirePolicyJson || ''" rows="3" readonly />
            </label>
            <label>
              <span>结果校验策略</span>
              <textarea :value="selectedStrategy.resultCheckPolicyJson || ''" rows="3" readonly />
            </label>
            <label>
              <span>归档策略</span>
              <textarea :value="selectedStrategy.archivePolicyJson || ''" rows="3" readonly />
            </label>
          </details>
        </div>

        <form
          v-else
          class="edit-form"
          @submit.prevent="dialogMode === 'create' ? createLocalStrategy() : updateStrategy()"
        >
          <label v-if="dialogMode === 'create'">
            <span>原平台系统</span>
            <select v-model="connectorSystemId" required>
              <option value="">请选择原平台系统</option>
              <option v-for="system in systems" :key="system.id" :value="system.id">
                {{ system.systemName }} / {{ system.systemCode }}
              </option>
            </select>
          </label>
          <label v-if="dialogMode === 'create'">
            <span>业务模块</span>
            <select
              v-model="businessModuleId"
              required
              @change="strategyForm.moduleName = selectedModule?.moduleName || ''"
            >
              <option value="">请选择业务模块</option>
              <option v-for="module in modules" :key="module.id" :value="module.id">
                {{ module.moduleName }} / {{ module.moduleCode }}
              </option>
            </select>
          </label>
          <label v-if="dialogMode === 'create'">
            <span>教学场景</span>
            <select v-model="sceneType">
              <option v-for="scene in visibleSceneTypes" :key="scene.code" :value="scene.code">
                {{ scene.label }}
              </option>
            </select>
          </label>
          <label>
            <span>策略编码</span>
            <input v-model="strategyForm.strategyCode" />
          </label>
          <label>
            <span>模块名称</span>
            <input v-model="strategyForm.moduleName" required />
          </label>
          <label>
            <span>模板</span>
            <select v-model="strategyForm.templateId" required>
              <option value="">请选择模板</option>
              <option v-for="template in templates" :key="template.id" :value="template.id">
                {{ template.templateName }}
              </option>
              <option
                v-if="
                  strategyForm.templateId &&
                  !templates.some((template) => template.id === strategyForm.templateId)
                "
                :value="strategyForm.templateId"
              >
                当前模板 {{ strategyForm.templateId }}
              </option>
            </select>
          </label>
          <label>
            <span>数据来源</span>
            <select v-model="strategyForm.dataSourceStrategy">
              <option
                v-for="strategy in visibleDataSourceStrategies"
                :key="strategy.code"
                :value="strategy.code"
              >
                {{ strategy.label }}
              </option>
            </select>
          </label>
          <label>
            <span>初始状态</span>
            <input v-model="strategyForm.initExternalStatus" placeholder="如 DRAFT" />
          </label>
          <label>
            <span>完成状态参考</span>
            <input v-model="strategyForm.targetExternalStatus" placeholder="如 SUBMITTED" />
          </label>
          <label>
            <span>分配策略</span>
            <select v-model="strategyForm.sharePolicy">
              <option v-for="policy in visibleSharePolicies" :key="policy.code" :value="policy.code">
                {{ policy.label }}
              </option>
            </select>
          </label>
          <label>
            <span>重建策略</span>
            <select v-model="strategyForm.regeneratePolicy">
              <option v-for="policy in visibleRegeneratePolicies" :key="policy.code" :value="policy.code">
                {{ policy.label }}
              </option>
            </select>
          </label>
          <label>
            <span>锁定策略</span>
            <select v-model="strategyForm.lockPolicy">
              <option v-for="policy in visibleLockPolicies" :key="policy.code" :value="policy.code">
                {{ policy.label }}
              </option>
            </select>
          </label>
          <label>
            <span>准备时机</span>
            <select v-model="strategyForm.prepareTiming">
              <option v-for="timing in visiblePrepareTimings" :key="timing.code" :value="timing.code">
                {{ timing.label }}
              </option>
            </select>
          </label>
          <label class="check-line">
            <input v-model="strategyForm.needPreData" type="checkbox" />
            <span>进入练习、学习或考试前需要创建初始数据</span>
          </label>
          <section class="json-builder wide">
            <header>
              <div>
                <strong>常用策略表单</strong>
                <span>填写业务规则后，系统自动转换为接口需要的参数。</span>
                <em>启用要求：绑定模板，补齐单位要求、角色要求、状态校验；数据池最小值不能大于最大值。</em>
              </div>
            </header>
            <div class="readiness-checklist">
              <article
                v-for="item in strategyEnableChecklist"
                :key="item.label"
                :class="{ done: item.done }"
              >
                <span>{{ item.done ? '已满足' : '待补齐' }}</span>
                <strong>{{ item.label }}</strong>
              </article>
            </div>
            <div class="json-builder-grid">
              <label>
                <span>单位要求</span>
                <select v-model="strategyJsonBuilder.requiredOrg">
                  <option value="required">必填</option>
                  <option value="optional">可选</option>
                  <option value="none">不校验</option>
                </select>
              </label>
              <label>
                <span>角色要求</span>
                <select v-model="strategyJsonBuilder.requiredRole">
                  <option value="required">必填</option>
                  <option value="optional">可选</option>
                  <option value="none">不校验</option>
                </select>
              </label>
              <label>
                <span>最小可用数</span>
                <input v-model.number="strategyJsonBuilder.minReadyCount" type="number" min="1" />
              </label>
              <label>
                <span>最大可用数</span>
                <input v-model.number="strategyJsonBuilder.maxReadyCount" type="number" min="1" />
              </label>
              <label>
                <span>初始状态校验</span>
                <input v-model="strategyJsonBuilder.requiredStatus" placeholder="DRAFT" />
              </label>
              <label>
                <span>过期小时</span>
                <input v-model.number="strategyJsonBuilder.expireHours" type="number" min="1" />
              </label>
              <label>
                <span>归档方式</span>
                <select v-model="strategyJsonBuilder.archiveMode">
                  <option value="MANUAL">手动归档</option>
                  <option value="AFTER_EXPIRE">过期后归档</option>
                  <option value="AFTER_TASK_END">任务结束后归档</option>
                </select>
              </label>
            </div>
          </section>
          <details class="json-preview-block wide">
            <summary>接口参数预览</summary>
            <label>
              <span>初始身份策略</span>
              <textarea :value="strategyForm.defaultOrgRolePolicyJson" rows="3" readonly />
            </label>
            <label>
              <span>初始数据池容量策略</span>
              <textarea :value="strategyForm.poolSizePolicyJson" rows="3" readonly />
            </label>
            <label>
              <span>初始数据校验策略</span>
              <textarea :value="strategyForm.validationPolicyJson" rows="3" readonly />
            </label>
            <label>
              <span>过期策略</span>
              <textarea :value="strategyForm.expirePolicyJson" rows="3" readonly />
            </label>
            <label>
              <span>结果校验策略</span>
              <textarea :value="strategyForm.resultCheckPolicyJson" rows="3" readonly />
            </label>
            <label>
              <span>归档策略</span>
              <textarea :value="strategyForm.archivePolicyJson" rows="3" readonly />
            </label>
          </details>
          <footer class="dialog-actions">
            <button type="button" class="secondary" @click="closeDialog">取消</button>
            <button type="submit" :disabled="loading">
              {{ dialogMode === 'create' ? '提交创建' : '保存' }}
            </button>
          </footer>
        </form>
      </section>
    </div>
  </section>
</template>

<style scoped>
@import './dataPrepareConfig.css';

.context-panel {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  padding: 14px 16px;
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

.dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  background: rgb(15 23 42 / 42%);
  padding: 24px;
}

.dialog-panel {
  width: min(920px, 100%);
  max-height: min(780px, calc(100vh - 48px));
  overflow: auto;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 24px 80px rgb(15 23 42 / 22%);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e3e8f0;
  padding: 18px 20px;
}

.dialog-header p,
.detail-grid span {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.dialog-header h2 {
  margin: 4px 0 0;
  font-size: 20px;
}

.icon-button {
  display: inline-grid;
  width: 36px;
  min-width: 36px;
  min-height: 36px;
  place-items: center;
  border-color: #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 18px;
  font-weight: 800;
  line-height: 1;
  padding: 0;
  transition: border-color 160ms ease, background-color 160ms ease, color 160ms ease, box-shadow 160ms ease;
}

.icon-button:hover {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #2563eb;
}

.icon-button:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.14);
}

.detail-grid {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 10px 14px;
  padding: 18px 22px 20px;
}

.detail-content {
  display: grid;
  gap: 14px;
  padding-bottom: 20px;
}

.edit-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 20px;
}

.edit-form label {
  min-width: 0;
}

.edit-form input,
.edit-form select {
  width: 100%;
  max-width: 100%;
  min-height: 44px;
  box-sizing: border-box;
}

.edit-form select {
  max-width: 360px;
}

.detail-grid strong {
  color: #172033;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.wide {
  grid-column: 1 / -1;
}

.policy-summary {
  display: grid;
  gap: 12px;
  margin: 0 22px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
  padding: 14px;
}

.policy-summary header {
  display: grid;
  gap: 4px;
}

.policy-summary header strong {
  color: #172033;
  font-size: 14px;
}

.policy-summary header span {
  color: #64748b;
  font-size: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.summary-grid article {
  display: grid;
  gap: 5px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  padding: 10px 12px;
}

.summary-grid span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.summary-grid strong {
  color: #172033;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.json-builder {
  display: grid;
  gap: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
  padding: 14px;
}

.json-builder header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.json-builder header div {
  display: grid;
  gap: 3px;
}

.json-builder header strong {
  color: #172033;
  font-size: 14px;
}

.json-builder header span {
  color: #64748b;
  font-size: 12px;
}

.json-builder header em {
  color: #1d4ed8;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  line-height: 1.5;
}

.readiness-checklist {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.readiness-checklist article {
  display: grid;
  gap: 5px;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  background: #fff7ed;
  padding: 10px 12px;
}

.readiness-checklist article.done {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.readiness-checklist span {
  color: #b45309;
  font-size: 12px;
  font-weight: 850;
}

.readiness-checklist article.done span {
  color: #047857;
}

.readiness-checklist strong {
  color: #172033;
  font-size: 13px;
  line-height: 1.45;
}

.json-builder-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.json-preview-block {
  display: grid;
  gap: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
  padding: 14px;
}

.detail-content .json-preview-block {
  margin: 0 22px;
}

.json-preview-block strong {
  color: #172033;
  font-size: 14px;
}

.json-preview-block summary {
  cursor: pointer;
  color: #334155;
  font-size: 13px;
  font-weight: 800;
}

.json-preview-block[open] summary {
  margin-bottom: 10px;
}

.json-preview-block label {
  display: grid;
  gap: 7px;
}

.json-preview-block textarea[readonly] {
  background: #ffffff;
  color: #475569;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
}

.edit-form textarea {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  min-height: 84px;
  resize: vertical;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  color: #172033;
  padding: 10px 11px;
  font: inherit;
}

.check-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
}

.check-line input {
  min-height: auto;
}

.dialog-actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid #e3e8f0;
  padding-top: 16px;
}

@media (max-width: 1180px) {
  .context-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .context-panel,
  .detail-grid,
  .edit-form,
  .readiness-checklist,
  .json-builder-grid,
  .summary-grid,
  .json-preview-block {
    grid-template-columns: 1fr;
  }

  .dialog-backdrop {
    padding: 12px;
  }

  .pagination-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
