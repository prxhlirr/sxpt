<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import TimedToast from '../../components/ui/TimedToast.vue';
import {
  authApi,
  dataPrepareApi,
  type BusinessModule,
  type ConnectorSystem,
  type DataInstanceAllocation,
  type DataPrepareJob,
  type DataRequirement,
  type DataRequirementItem,
  type ModuleDataStrategy,
  type TeachingDataPool,
  type TeachingDataTemplate
} from '../../services/trainingApi';

type ChainStepState = 'done' | 'warning' | 'pending' | 'blocked';
type DetailTab = 'overview' | 'items' | 'pools' | 'jobs' | 'allocations' | 'diagnostics';
type PrimaryActionKey = 'createRequirement' | 'prepare' | 'acquire' | 'diagnostics';
const PAGE_SIZE = 10;

interface ChainStep {
  key: string;
  title: string;
  state: ChainStepState;
  summary: string;
  actionLabel?: string;
  actionKey?: PrimaryActionKey;
}

interface DiagnosticItem {
  level: 'error' | 'warning' | 'info';
  title: string;
  target: string;
  suggestion: string;
}

interface PrimaryAction {
  key: PrimaryActionKey;
  label: string;
  summary: string;
}

const session = authApi.getSession();
const filters = reactive({
  tenantId: session?.user.tenantId || 'demo-tenant',
  connectorSystemId: '',
  businessModuleId: '',
  moduleCode: '',
  templateId: '',
  taskId: 'task_001',
  sceneType: 'PRACTICE'
});

const batchForm = reactive({
  requirementCode: `REQ-${Date.now()}`,
  classId: 'class_001'
});

const prepareForm = reactive({
  requirementId: '',
  requestBatchId: `attempt-${Date.now()}`,
  studentId: 'student_001',
  questionId: 'question_001',
  questionAttemptId: `qa-${Date.now()}`,
  actorType: 'student',
  ownerExternalOrgId: 'org_owner',
  requiredExternalOrgId: 'org_required',
  requiredExternalRoleId: 'role_required',
  requiredActionsJson: '["submit"]',
  scorePointSnapshotJson: '{"score":10}'
});

const requirements = ref<DataRequirement[]>([]);
const items = ref<DataRequirementItem[]>([]);
const jobs = ref<DataPrepareJob[]>([]);
const pools = ref<TeachingDataPool[]>([]);
const allocations = ref<DataInstanceAllocation[]>([]);
const connectorSystems = ref<ConnectorSystem[]>([]);
const businessModules = ref<BusinessModule[]>([]);
const strategies = ref<ModuleDataStrategy[]>([]);
const templates = ref<TeachingDataTemplate[]>([]);
const selectedRequirementId = ref('');
const activeTab = ref<DetailTab>('overview');
const advancedOpen = ref(false);
const loading = ref(false);
const systemKeyword = ref('');
const requirementPage = ref(1);
const notice = ref({
  show: false,
  type: 'info' as 'success' | 'error' | 'info',
  message: ''
});

const selectedConnector = computed(() =>
  connectorSystems.value.find((item) => item.id === filters.connectorSystemId)
);

const filteredConnectorSystems = computed(() => {
  const keyword = systemKeyword.value.trim().toLowerCase();
  if (!keyword) return connectorSystems.value;
  return connectorSystems.value.filter((item) =>
    (item.systemName || '').toLowerCase().includes(keyword)
  );
});

const selectedBusinessModule = computed(() =>
  businessModules.value.find((item) => item.id === filters.businessModuleId)
);

const selectedTemplate = computed(() =>
  templates.value.find((item) => item.id === filters.templateId)
);

const activeStrategy = computed(() =>
  strategies.value.find((item) => item.sceneType === filters.sceneType)
);

const selectedRequirement = computed(() =>
  requirements.value.find((item) => item.id === selectedRequirementId.value)
);

const requirementTotalPages = computed(() =>
  Math.max(1, Math.ceil(requirements.value.length / PAGE_SIZE))
);

const pagedRequirements = computed(() => {
  const start = (requirementPage.value - 1) * PAGE_SIZE;
  return requirements.value.slice(start, start + PAGE_SIZE);
});

const latestJob = computed(() =>
  jobs.value.find(
    (item) =>
      !selectedRequirement.value ||
      item.requestBatchId === prepareForm.requestBatchId ||
      item.taskId === selectedRequirement.value.taskId
  )
);

const visibleJobs = computed(() => {
  if (!selectedRequirement.value) return jobs.value;
  return jobs.value.filter((job) => job.taskId === selectedRequirement.value?.taskId);
});

const selectedPoolReadyCount = computed(() =>
  pools.value.reduce((sum, item) => sum + Number(item.readyCount || 0), 0)
);

const selectedPoolAllocatedCount = computed(() =>
  pools.value.reduce((sum, item) => sum + Number(item.allocatedCount || 0), 0)
);

const selectedPoolFailedCount = computed(() =>
  pools.value.reduce((sum, item) => sum + Number(item.failedCount || 0), 0)
);

const summary = computed(() => {
  const expected = requirements.value.reduce(
    (sum, item) => sum + Number(item.expectedCount || 0),
    0
  );
  const success = requirements.value.reduce(
    (sum, item) => sum + Number(item.successCount || 0),
    0
  );
  const failed = requirements.value.reduce(
    (sum, item) => sum + Number(item.failedCount || 0),
    0
  );
  const ready = pools.value.reduce((sum, item) => sum + Number(item.readyCount || 0), 0);
  return { expected, success, failed, ready };
});

const diagnostics = computed<DiagnosticItem[]>(() => {
  const result: DiagnosticItem[] = [];
  if (!selectedConnector.value) {
    result.push({
      level: 'error',
      title: '原平台缺失',
      target: 'connector_system',
      suggestion: '请管理员先到“平台接入”维护可用原平台'
    });
  }
  if (selectedConnector.value && !selectedBusinessModule.value) {
    result.push({
      level: 'error',
      title: '业务模块缺失',
      target: 'business_module',
      suggestion: '请管理员先到“业务模块”维护当前平台下的业务模块'
    });
  }
  if (selectedBusinessModule.value && !selectedTemplate.value) {
    result.push({
      level: 'error',
      title: '数据模板缺失',
      target: 'teaching_data_template',
      suggestion: '请管理员先到“模板管理”维护当前模块和场景的数据模板'
    });
  }
  if (selectedTemplate.value && !activeStrategy.value) {
    result.push({
      level: 'error',
      title: '启用策略缺失',
      target: 'module_data_strategy',
      suggestion: '请管理员先到“策略管理”启用当前模块和场景的数据策略'
    });
  }
  if (!selectedRequirement.value) {
    result.push({
      level: 'info',
      title: '尚未选择批次',
      target: 'data_requirement',
      suggestion: '创建或选择一个数据准备批次后，可以查看完整证据链'
    });
  }
  if (selectedRequirement.value && items.value.length === 0) {
    result.push({
      level: 'warning',
      title: '批次尚未生成明细',
      target: selectedRequirement.value.id,
      suggestion: '触发准备后会生成学生、题目、单位和角色维度的需求明细'
    });
  }
  if (
    selectedRequirement.value &&
    items.value.some((item) => item.itemStatus === 'READY') &&
    pools.value.length === 0
  ) {
    result.push({
      level: 'error',
      title: '数据实例未入池',
      target: selectedRequirement.value.id,
      suggestion: '请重新触发准备，或检查后端入池任务是否完成'
    });
  }
  if (items.value.some((item) => item.validationStatus === 'NOT_CHECKED')) {
    result.push({
      level: 'warning',
      title: '存在未校验明细',
      target: 'data_requirement_item',
      suggestion: '确认触发准备后是否执行了原平台状态校验'
    });
  }
  return result;
});

const chainSteps = computed<ChainStep[]>(() => [
  {
    key: 'connector',
    title: '原平台',
    state: selectedConnector.value ? 'done' : 'blocked',
    summary: selectedConnector.value?.systemName || '未选择原平台'
  },
  {
    key: 'module',
    title: '业务模块',
    state: selectedBusinessModule.value ? 'done' : selectedConnector.value ? 'blocked' : 'pending',
    summary: selectedBusinessModule.value?.moduleName || '当前平台暂无模块'
  },
  {
    key: 'template',
    title: '数据模板',
    state: selectedTemplate.value ? 'done' : selectedBusinessModule.value ? 'blocked' : 'pending',
    summary: selectedTemplate.value?.templateName || '当前模块暂无模板'
  },
  {
    key: 'strategy',
    title: '启用策略',
    state: activeStrategy.value ? 'done' : selectedTemplate.value ? 'blocked' : 'pending',
    summary: activeStrategy.value?.strategyCode || '当前场景暂无启用策略'
  },
  {
    key: 'requirement',
    title: '数据批次',
    state: selectedRequirement.value ? 'done' : activeStrategy.value ? 'blocked' : 'pending',
    summary: selectedRequirement.value?.requirementCode || '尚未创建批次',
    actionLabel: selectedRequirement.value ? undefined : '创建批次',
    actionKey: selectedRequirement.value ? undefined : 'createRequirement'
  },
  {
    key: 'prepare',
    title: '准备任务',
    state: latestJob.value
      ? statusToStepState(latestJob.value.jobStatus)
      : selectedRequirement.value
        ? 'blocked'
        : 'pending',
    summary: latestJob.value?.jobStatus ? statusText(latestJob.value.jobStatus) : '尚未触发准备',
    actionLabel: latestJob.value ? undefined : '触发准备',
    actionKey: latestJob.value ? undefined : 'prepare'
  },
  {
    key: 'pool',
    title: '数据池',
    state:
      selectedPoolReadyCount.value > 0
        ? 'done'
        : pools.value.length > 0
          ? 'warning'
          : selectedRequirement.value
            ? 'blocked'
            : 'pending',
    summary:
      pools.value.length > 0
        ? `${pools.value.length} 个池，${selectedPoolReadyCount.value} 条可领取`
        : '尚未形成可领取数据池',
    actionLabel: selectedPoolReadyCount.value > 0 ? '领取验证' : undefined,
    actionKey: selectedPoolReadyCount.value > 0 ? 'acquire' : undefined
  }
]);

const primaryAction = computed<PrimaryAction>(() => {
  const nextStep = chainSteps.value.find((step) => step.actionKey);
  if (nextStep?.actionKey) {
    return {
      key: nextStep.actionKey,
      label: nextStep.actionLabel || '继续',
      summary: nextStep.summary
    };
  }
  if (diagnostics.value.some((item) => item.level === 'error')) {
    return {
      key: 'diagnostics',
      label: '查看诊断',
      summary: '当前链路存在前置配置缺口'
    };
  }
  return {
    key: 'prepare',
    label: '重新触发准备',
    summary: '链路完整，可重新生成一个 attempt 进行验证'
  };
});

const tabs = computed<Array<{ key: DetailTab; label: string; count?: number }>>(() => [
  { key: 'overview', label: '概览' },
  { key: 'items', label: '需求明细', count: items.value.length },
  { key: 'pools', label: '数据池', count: pools.value.length },
  { key: 'jobs', label: '准备任务', count: visibleJobs.value.length },
  { key: 'allocations', label: '领取记录', count: allocations.value.length },
  { key: 'diagnostics', label: '异常诊断', count: diagnostics.value.length }
]);

onMounted(() => {
  initializePage();
});

watch(
  () => filters.connectorSystemId,
  () => {
    filters.businessModuleId = '';
    filters.moduleCode = '';
    filters.templateId = '';
    strategies.value = [];
    selectedRequirementId.value = '';
    items.value = [];
    pools.value = [];
    loadBusinessModules();
  }
);

watch(
  () => filters.businessModuleId,
  () => {
    const selected = businessModules.value.find(
      (item) => item.id === filters.businessModuleId
    );
    filters.moduleCode = selected?.moduleCode || '';
    filters.templateId = '';
    loadTemplates();
    loadStrategies();
  }
);

watch(
  () => filters.sceneType,
  () => {
    filters.templateId = '';
    loadTemplates();
    loadStrategies();
  }
);

/**
 * 业务功能：初始化老师批次准备页面，拉取平台、模块、模板、策略和历史批次。
 * 关键流程：按原平台到策略的顺序加载，保证后续批次创建前置条件完整。
 */
async function initializePage() {
  await run(async () => {
    await loadConnectorSystems();
    await loadBusinessModules();
    await loadTemplates();
    await loadStrategies();
    await refreshAll();
  }, '数据准备状态已刷新');
}

/**
 * 业务功能：刷新批次、任务、明细和数据池，让老师看到最新准备结果。
 * 关键流程：先刷新批次和任务，再根据当前批次补充明细与数据池。
 */
async function refreshAll() {
  await run(async () => {
    requirements.value = await dataPrepareApi.listRequirements(filters);
    jobs.value = await dataPrepareApi.listJobs(filters);
    if (!selectedRequirementId.value && requirements.value.length > 0) {
      selectRequirement(requirements.value[0].id);
    } else if (selectedRequirementId.value) {
      await loadItems(selectedRequirementId.value);
      await loadPools(selectedRequirementId.value);
    }
  }, '数据准备状态已刷新');
}

async function loadConnectorSystems() {
  connectorSystems.value = await dataPrepareApi.listConnectorSystems(filters.tenantId);
  if (!filters.connectorSystemId && connectorSystems.value.length > 0) {
    filters.connectorSystemId = connectorSystems.value[0].id;
  }
}

async function loadBusinessModules() {
  businessModules.value = [];
  if (!filters.connectorSystemId) return;
  businessModules.value = await dataPrepareApi.listBusinessModules({
    tenantId: filters.tenantId,
    connectorSystemId: filters.connectorSystemId
  });
  if (!filters.businessModuleId && businessModules.value.length > 0) {
    filters.businessModuleId = businessModules.value[0].id;
  }
}

async function loadTemplates() {
  templates.value = [];
  if (!filters.connectorSystemId || !filters.moduleCode) return;
  templates.value = await dataPrepareApi.listTemplates({
    tenantId: filters.tenantId,
    connectorSystemId: filters.connectorSystemId,
    moduleCode: filters.moduleCode,
    sceneType: filters.sceneType
  });
  if (!filters.templateId && templates.value.length > 0) {
    filters.templateId = templates.value[0].id;
  }
}

async function loadStrategies() {
  strategies.value = [];
  if (!filters.connectorSystemId || !filters.businessModuleId) return;
  strategies.value = await dataPrepareApi.listActiveStrategies({
    tenantId: filters.tenantId,
    connectorSystemId: filters.connectorSystemId,
    businessModuleId: filters.businessModuleId
  });
}

/**
 * 业务功能：为当前班级和模块创建数据准备批次。
 * 关键流程：校验平台、模块和启用策略后创建批次，避免生成无法执行的孤立批次。
 */
async function createRequirement() {
  if (!filters.connectorSystemId) {
    notify('error', '请先选择原平台');
    return;
  }
  if (!filters.businessModuleId || !filters.moduleCode) {
    notify('error', '请先选择业务模块');
    return;
  }
  if (!activeStrategy.value) {
    notify('error', '当前业务模块和场景没有启用策略，请管理员先到策略管理维护');
    return;
  }
  await run(async () => {
    const created = await dataPrepareApi.createRequirement({
      tenantId: filters.tenantId,
      requirementCode: batchForm.requirementCode,
      connectorSystemId: filters.connectorSystemId,
      businessModuleId: filters.businessModuleId,
      moduleCode: filters.moduleCode,
      strategyId: activeStrategy.value?.id,
      templateId: activeStrategy.value?.templateId || filters.templateId,
      taskId: filters.taskId,
      classId: batchForm.classId,
      sceneType: filters.sceneType,
      createBy: session?.user.userId || 'admin',
      updateBy: session?.user.userId || 'admin'
    });
    requirements.value = [created, ...requirements.value];
    selectRequirement(created.id);
    prepareForm.requirementId = created.id;
    batchForm.requirementCode = `REQ-${Date.now()}`;
  }, '已创建数据准备批次');
}

/**
 * 业务功能：触发当前批次的数据准备任务，为学生生成并校验可分配的数据实例。
 * 关键流程：将学生、题目、单位、角色信息作为参与者提交，确保生成数据能满足原平台角色约束。
 */
async function prepareAndExecute() {
  if (!prepareForm.requirementId && selectedRequirementId.value) {
    prepareForm.requirementId = selectedRequirementId.value;
  }
  if (!prepareForm.requirementId) {
    notify('error', '请先创建或选择一个数据准备批次');
    return;
  }
  if (!prepareForm.requestBatchId.trim()) {
    notify('error', 'requestBatchId 不能为空');
    return;
  }
  const requirement = requirements.value.find(
    (item) => item.id === prepareForm.requirementId
  );
  if (requirement && !requirement.templateId) {
    notify('error', '当前批次没有绑定数据模板，请重新创建批次');
    return;
  }
  await run(async () => {
    await dataPrepareApi.prepareAndExecute({
      triggerType: 'ON_DEMAND',
      generateRequest: {
        requirementId: prepareForm.requirementId,
        requestBatchId: prepareForm.requestBatchId,
        createBy: session?.user.userId || 'admin',
        updateBy: session?.user.userId || 'admin',
        participants: [
          {
            studentId: prepareForm.studentId,
            questionId: prepareForm.questionId,
            questionAttemptId: prepareForm.questionAttemptId,
            actorType: prepareForm.actorType,
            ownerExternalOrgId: prepareForm.ownerExternalOrgId,
            requiredExternalOrgId: prepareForm.requiredExternalOrgId,
            requiredExternalRoleId: prepareForm.requiredExternalRoleId,
            dataScopeJson: JSON.stringify({
              attempt: prepareForm.requestBatchId,
              moduleCode: filters.moduleCode
            }),
            requiredActionsJson: prepareForm.requiredActionsJson,
            scorePointSnapshotJson: prepareForm.scorePointSnapshotJson
          }
        ]
      }
    });
    await refreshAll();
  }, '已触发数据准备');
}

function selectRequirement(requirementId: string) {
  selectedRequirementId.value = requirementId;
  prepareForm.requirementId = requirementId;
  activeTab.value = 'overview';
  loadItems(requirementId);
  loadPools(requirementId);
}

async function loadItems(requirementId: string) {
  items.value = await dataPrepareApi.listRequirementItems({
    tenantId: filters.tenantId,
    requirementId
  });
}

async function loadPools(requirementId: string) {
  pools.value = await dataPrepareApi.listPools({
    tenantId: filters.tenantId,
    requirementId
  });
}

/**
 * 业务功能：模拟学生进入练习或考试时领取数据实例，验证数据池能否正确分配。
 * 关键流程：从可用数据池领取一条实例，并记录学生、attempt 和题目维度的分配关系。
 */
async function acquirePool(pool: TeachingDataPool) {
  await run(async () => {
    const allocation = await dataPrepareApi.acquireDataInstance({
      tenantId: filters.tenantId,
      poolId: pool.id,
      ownerUserId: prepareForm.studentId,
      taskId: pool.taskId,
      allocationScene: pool.sceneType,
      attemptId: prepareForm.requestBatchId,
      questionAttemptId: prepareForm.questionAttemptId,
      createBy: session?.user.userId || 'admin',
      updateBy: session?.user.userId || 'admin'
    });
    allocations.value = [allocation, ...allocations.value];
    await loadPools(pool.requirementId);
  }, '已领取数据实例');
}

function nextAttempt() {
  const stamp = Date.now();
  prepareForm.requestBatchId = `attempt-${stamp}`;
  prepareForm.questionAttemptId = `qa-${stamp}`;
}

async function runPrimaryAction() {
  const key = primaryAction.value.key;
  if (key === 'createRequirement') await createRequirement();
  if (key === 'prepare') await prepareAndExecute();
  if (key === 'acquire') {
    const pool = pools.value.find(
      (item) => item.poolStatus === 'READY' && Number(item.readyCount || 0) > 0
    );
    if (pool) await acquirePool(pool);
  }
  if (key === 'diagnostics') activeTab.value = 'diagnostics';
}

async function run(action: () => Promise<void>, successMessage: string) {
  loading.value = true;
  closeNotice();
  try {
    await action();
    notify('success', successMessage);
  } catch (err) {
    notify('error', err instanceof Error ? err.message : '操作失败');
  } finally {
    loading.value = false;
  }
}

function notify(type: 'success' | 'error' | 'info', text: string) {
  notice.value = { show: true, type, message: text };
}

function closeNotice() {
  notice.value = { ...notice.value, show: false, message: '' };
}

function handleSystemKeywordChange() {
  if (
    filters.connectorSystemId &&
    !filteredConnectorSystems.value.some((item) => item.id === filters.connectorSystemId)
  ) {
    filters.connectorSystemId = filteredConnectorSystems.value[0]?.id || '';
  }
}

function previousRequirementPage() {
  requirementPage.value = Math.max(1, requirementPage.value - 1);
}

function nextRequirementPage() {
  requirementPage.value = Math.min(requirementTotalPages.value, requirementPage.value + 1);
}

function showRequirementDetail(requirement: DataRequirement) {
  selectRequirement(requirement.id);
  activeTab.value = 'overview';
  notify('info', `已切换到批次：${requirement.requirementCode || shortId(requirement.id)}`);
}

/**
 * 业务功能：触发指定批次的数据准备。
 * 关键流程：先选中批次并同步准备表单，再复用统一触发逻辑，保证证据链视图和执行目标一致。
 */
async function prepareRequirement(requirement: DataRequirement) {
  selectRequirement(requirement.id);
  await prepareAndExecute();
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    CREATED: '已创建',
    PREPARING: '准备中',
    READY: '可用',
    SUCCESS: '成功',
    PARTIAL_FAILED: '部分失败',
    FAILED: '失败',
    CANCELLED: '已取消',
    NOT_CHECKED: '未校验',
    PASSED: '校验通过',
    ALLOCATED: '已领取',
    EXHAUSTED: '已耗尽'
  };
  return status ? map[status] || status : '-';
}

function statusTone(status?: string) {
  if (!status) return 'muted';
  if (['READY', 'SUCCESS', 'PASSED'].includes(status)) return 'success';
  if (['PREPARING', 'CREATED', 'NOT_CHECKED'].includes(status)) return 'info';
  if (['PARTIAL_FAILED', 'EXHAUSTED'].includes(status)) return 'warning';
  if (['FAILED', 'CANCELLED'].includes(status)) return 'danger';
  if (['ALLOCATED'].includes(status)) return 'allocated';
  return 'muted';
}

function statusToStepState(status?: string): ChainStepState {
  if (['SUCCESS', 'READY', 'PASSED'].includes(status || '')) return 'done';
  if (['PARTIAL_FAILED', 'EXHAUSTED'].includes(status || '')) return 'warning';
  if (['FAILED', 'CANCELLED'].includes(status || '')) return 'blocked';
  return 'pending';
}

function sceneText(value?: string) {
  const map: Record<string, string> = {
    RECORD: '备课',
    LEARN: '学习',
    PRACTICE: '练习',
    EXAM: '考试'
  };
  return value ? map[value] || value : '-';
}

function shortId(value?: string) {
  if (!value) return '-';
  if (value.length <= 14) return value;
  return `${value.slice(0, 8)}...${value.slice(-4)}`;
}

function formatTime(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}
</script>

<template>
  <section class="data-prepare-page">
    <TimedToast
      :show="notice.show"
      :type="notice.type"
      :message="notice.message"
      @close="closeNotice"
    />

    <header class="page-heading">
      <div>
        <p>数据准备 / 批次准备</p>
        <h1>给班级生成可练习的数据</h1>
        <span>选择原平台业务模块，创建批次，触发准备，然后验证学生是否能领取到正确的数据实例。</span>
      </div>
      <div class="heading-actions">
        <button type="button" class="button ghost" :disabled="loading" @click="nextAttempt">
          新 attempt
        </button>
        <button type="button" class="button primary" :disabled="loading" @click="runPrimaryAction">
          {{ primaryAction.label }}
        </button>
      </div>
    </header>

    <section class="context-bar">
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
        <select v-model="filters.connectorSystemId">
          <option value="">请选择原平台</option>
          <option v-for="system in filteredConnectorSystems" :key="system.id" :value="system.id">
            {{ system.systemName }} / {{ shortId(system.id) }}
          </option>
        </select>
      </label>
      <label>
        <span>业务模块</span>
        <select v-model="filters.businessModuleId">
          <option value="">请选择业务模块</option>
          <option v-for="module in businessModules" :key="module.id" :value="module.id">
            {{ module.moduleName }} / {{ module.moduleCode }}
          </option>
        </select>
      </label>
      <label>
        <span>场景</span>
        <select v-model="filters.sceneType">
          <option value="RECORD">备课</option>
          <option value="LEARN">学习</option>
          <option value="PRACTICE">练习</option>
          <option value="EXAM">考试</option>
        </select>
      </label>
      <label>
        <span>任务</span>
        <input v-model="filters.taskId" />
      </label>
    </section>

    <section class="chain-section">
      <article
        v-for="(step, index) in chainSteps"
        :key="step.key"
        class="chain-step"
        :class="step.state"
      >
        <span class="step-index">{{ index + 1 }}</span>
        <div>
          <strong>{{ step.title }}</strong>
          <p>{{ step.summary }}</p>
        </div>
      </article>
    </section>

    <section class="metrics">
      <article>
        <span>批次数</span>
        <strong>{{ requirements.length }}</strong>
      </article>
      <article>
        <span>预计数据</span>
        <strong>{{ summary.expected }}</strong>
      </article>
      <article>
        <span>准备成功</span>
        <strong>{{ summary.success }}</strong>
      </article>
      <article>
        <span>可领取</span>
        <strong>{{ summary.ready }}</strong>
      </article>
    </section>

    <section class="setup-panel">
      <header>
        <div>
          <h2>创建与触发</h2>
        </div>
        <button
          type="button"
          class="button ghost"
          @click="advancedOpen = !advancedOpen"
        >
          {{ advancedOpen ? '收起参数' : '展开参数' }}
        </button>
      </header>

      <div class="compact-form">
        <label>
          <span>批次编码</span>
          <input v-model="batchForm.requirementCode" />
        </label>
        <label>
          <span>班级</span>
          <input v-model="batchForm.classId" />
        </label>
        <label>
          <span>学生</span>
          <input v-model="prepareForm.studentId" />
        </label>
        <label>
          <span>题目</span>
          <input v-model="prepareForm.questionId" />
        </label>
      </div>

      <div v-if="advancedOpen" class="advanced-form">
        <label>
          <span>模板</span>
          <select v-model="filters.templateId">
            <option value="">自动使用策略模板</option>
            <option v-for="template in templates" :key="template.id" :value="template.id">
              {{ template.templateName }}
            </option>
          </select>
        </label>
        <label>
          <span>模块编码</span>
          <input v-model="filters.moduleCode" readonly />
        </label>
        <label>
          <span>requestBatchId</span>
          <input v-model="prepareForm.requestBatchId" />
        </label>
        <label>
          <span>题目 attempt</span>
          <input v-model="prepareForm.questionAttemptId" />
        </label>
        <label>
          <span>单位</span>
          <input v-model="prepareForm.requiredExternalOrgId" />
        </label>
        <label>
          <span>角色</span>
          <input v-model="prepareForm.requiredExternalRoleId" />
        </label>
        <label>
          <span>动作约束</span>
          <input v-model="prepareForm.requiredActionsJson" />
        </label>
        <label>
          <span>评分点快照</span>
          <input v-model="prepareForm.scorePointSnapshotJson" />
        </label>
      </div>

      <div class="setup-actions">
        <button type="button" class="button" :disabled="loading" @click="createRequirement">
          创建批次
        </button>
        <button type="button" class="button primary" :disabled="loading" @click="prepareAndExecute">
          触发准备
        </button>
      </div>
    </section>

    <section class="main-workspace">
      <aside class="requirement-list">
        <header>
          <h2>批次列表</h2>
          <span>{{ requirements.length }}</span>
        </header>

        <article
          v-for="requirement in pagedRequirements"
          :key="requirement.id"
          class="requirement-item"
          :class="{ active: requirement.id === selectedRequirementId }"
        >
          <button type="button" class="requirement-main" @click="selectRequirement(requirement.id)">
            <span class="badge" :class="statusTone(requirement.requirementStatus)">
              {{ statusText(requirement.requirementStatus) }}
            </span>
            <strong>{{ requirement.requirementCode || shortId(requirement.id) }}</strong>
            <small>{{ sceneText(requirement.sceneType) }} / {{ formatTime(requirement.createTime) }}</small>
            <em>{{ requirement.successCount || 0 }} / {{ requirement.expectedCount || 0 }}</em>
          </button>
          <div class="requirement-actions">
            <button type="button" @click="showRequirementDetail(requirement)">详情</button>
            <button type="button" :disabled="loading" @click="prepareRequirement(requirement)">
              触发准备
            </button>
          </div>
        </article>

        <div v-if="requirements.length === 0" class="empty-state">
          <strong>暂无批次</strong>
          <p>当前模块策略完整后，可以创建第一批数据准备需求。</p>
        </div>

        <footer v-if="requirements.length > PAGE_SIZE" class="requirement-pagination">
          <span>第 {{ requirementPage }} / {{ requirementTotalPages }} 页</span>
          <div>
            <button type="button" :disabled="requirementPage === 1" @click="previousRequirementPage">
              上一页
            </button>
            <button
              type="button"
              :disabled="requirementPage === requirementTotalPages"
              @click="nextRequirementPage"
            >
              下一页
            </button>
          </div>
        </footer>
      </aside>

      <section class="detail-panel">
        <header class="detail-heading">
          <div>
            <h2>{{ selectedRequirement?.requirementCode || '未选择批次' }}</h2>
            <p>{{ selectedRequirement?.id || primaryAction.summary }}</p>
          </div>
          <span class="badge" :class="statusTone(selectedRequirement?.requirementStatus)">
            {{ statusText(selectedRequirement?.requirementStatus) }}
          </span>
        </header>

        <nav class="tabs">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
            <span v-if="tab.count !== undefined">{{ tab.count }}</span>
          </button>
        </nav>

        <div v-if="activeTab === 'overview'" class="overview-grid">
          <article>
            <span>需求明细</span>
            <strong>{{ items.length }}</strong>
          </article>
          <article>
            <span>数据池</span>
            <strong>{{ pools.length }}</strong>
          </article>
          <article>
            <span>可领取</span>
            <strong>{{ selectedPoolReadyCount }}</strong>
          </article>
          <article>
            <span>已领取</span>
            <strong>{{ selectedPoolAllocatedCount }}</strong>
          </article>
          <article>
            <span>失败</span>
            <strong>{{ selectedPoolFailedCount }}</strong>
          </article>
          <article>
            <span>最近任务</span>
            <strong>{{ statusText(latestJob?.jobStatus) }}</strong>
          </article>
        </div>

        <div v-if="activeTab === 'items'" class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>学生</th>
                <th>题目</th>
                <th>requestItemId</th>
                <th>单位</th>
                <th>角色</th>
                <th>状态</th>
                <th>原平台数据</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in items" :key="item.id">
                <td>{{ item.studentId }}</td>
                <td>{{ item.questionId || '-' }}</td>
                <td>{{ shortId(item.requestItemId) }}</td>
                <td>{{ item.requiredExternalOrgId }}</td>
                <td>{{ item.requiredExternalRoleId }}</td>
                <td>
                  <span class="badge" :class="statusTone(item.itemStatus)">
                    {{ statusText(item.itemStatus) }}
                  </span>
                  <span class="badge soft" :class="statusTone(item.validationStatus)">
                    {{ statusText(item.validationStatus) }}
                  </span>
                </td>
                <td>{{ item.externalBusinessId || item.failureReason || '-' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="items.length === 0" class="empty-state">
            <strong>暂无明细</strong>
            <p>触发准备后会生成学生和题目维度的数据需求。</p>
          </div>
        </div>

        <div v-if="activeTab === 'pools'" class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>题目</th>
                <th>模块</th>
                <th>状态</th>
                <th>总数</th>
                <th>可用</th>
                <th>已分配</th>
                <th>失败</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="pool in pools" :key="pool.id">
                <td>{{ pool.questionId || '-' }}</td>
                <td>{{ pool.moduleCode }}</td>
                <td>
                  <span class="badge" :class="statusTone(pool.poolStatus)">
                    {{ statusText(pool.poolStatus) }}
                  </span>
                </td>
                <td>{{ pool.totalCount || 0 }}</td>
                <td>{{ pool.readyCount || 0 }}</td>
                <td>{{ pool.allocatedCount || 0 }}</td>
                <td>{{ pool.failedCount || 0 }}</td>
                <td>
                  <button
                    type="button"
                    class="button tiny"
                    :disabled="loading || pool.poolStatus !== 'READY' || !pool.readyCount"
                    @click="acquirePool(pool)"
                  >
                    领取
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="pools.length === 0" class="empty-state">
            <strong>暂无数据池</strong>
            <p>准备成功并校验后，实例会进入题目维度的数据池。</p>
          </div>
        </div>

        <div v-if="activeTab === 'jobs'" class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>任务 ID</th>
                <th>批次号</th>
                <th>状态</th>
                <th>成功</th>
                <th>失败</th>
                <th>幂等键</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="job in visibleJobs" :key="job.id">
                <td>{{ shortId(job.id) }}</td>
                <td>{{ job.requestBatchId }}</td>
                <td>
                  <span class="badge" :class="statusTone(job.jobStatus)">
                    {{ statusText(job.jobStatus) }}
                  </span>
                </td>
                <td>{{ job.successCount || 0 }}</td>
                <td>{{ job.failedCount || 0 }}</td>
                <td>{{ shortId(job.idempotencyKey) }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="visibleJobs.length === 0" class="empty-state">
            <strong>暂无准备任务</strong>
            <p>点击触发准备后会生成任务记录。</p>
          </div>
        </div>

        <div v-if="activeTab === 'allocations'" class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>学生</th>
                <th>实例</th>
                <th>场景</th>
                <th>状态</th>
                <th>领取时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="allocation in allocations" :key="allocation.id">
                <td>{{ allocation.ownerUserId }}</td>
                <td>{{ shortId(allocation.dataInstanceId) }}</td>
                <td>{{ sceneText(allocation.allocationScene) }}</td>
                <td>
                  <span class="badge" :class="statusTone(allocation.allocationStatus)">
                    {{ statusText(allocation.allocationStatus) }}
                  </span>
                </td>
                <td>{{ formatTime(allocation.allocateTime) }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="allocations.length === 0" class="empty-state">
            <strong>暂无领取记录</strong>
            <p>数据池 READY 后，可以领取一个实例验证学生分配链路。</p>
          </div>
        </div>

        <div v-if="activeTab === 'diagnostics'" class="diagnostic-list">
          <article
            v-for="item in diagnostics"
            :key="`${item.title}-${item.target}`"
            :class="item.level"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.target }}</span>
            <p>{{ item.suggestion }}</p>
          </article>
          <div v-if="diagnostics.length === 0" class="empty-state">
            <strong>未发现异常</strong>
            <p>当前链路配置和批次状态没有明显阻塞项。</p>
          </div>
        </div>
      </section>
    </section>
  </section>
</template>

<style scoped>
.data-prepare-page {
  --surface: #ffffff;
  --surface-soft: #f8fafc;
  --border: #e2e8f0;
  --border-strong: #cbd5e1;
  --text: #172033;
  --muted: #64748b;
  --blue: #2563eb;
  --blue-soft: #eff6ff;
  --green: #0f7a55;
  --green-soft: #ecfdf5;
  --red: #b42318;
  --red-soft: #fef3f2;
  --amber: #a16207;
  --amber-soft: #fffbeb;
  --violet: #6d5bd0;
  display: grid;
  gap: 18px;
  color: var(--text);
}

.page-heading,
.context-bar,
.chain-section,
.setup-panel,
.main-workspace,
.metrics article,
.detail-panel,
.requirement-list {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 20px;
}

.page-heading p,
.page-heading span,
.setup-panel p,
.detail-heading p,
.empty-state p,
.chain-step p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.page-heading h1,
.setup-panel h2,
.requirement-list h2,
.detail-heading h2 {
  margin: 4px 0 0;
  font-size: 22px;
  letter-spacing: 0;
}

.heading-actions,
.setup-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.button {
  min-height: 40px;
  border: 1px solid var(--border-strong);
  border-radius: 6px;
  background: #fff;
  color: #334155;
  cursor: pointer;
  font-weight: 800;
  padding: 0 14px;
}

.button.primary {
  border-color: var(--blue);
  background: var(--blue);
  color: #fff;
}

.button.ghost {
  background: #fff;
}

.button.tiny {
  min-height: 32px;
  padding: 0 10px;
}

.button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.notice {
  margin: 0;
  border-radius: 6px;
  padding: 11px 13px;
  font-size: 13px;
}

.notice.success {
  border: 1px solid #bbf7d0;
  background: var(--green-soft);
  color: var(--green);
}

.notice.error {
  border: 1px solid #fecaca;
  background: var(--red-soft);
  color: var(--red);
}

.context-bar {
  display: grid;
  grid-template-columns: minmax(120px, 0.8fr) minmax(220px, 1.4fr) minmax(220px, 1.4fr) minmax(120px, 0.8fr) minmax(150px, 1fr);
  gap: 12px;
  padding: 16px;
}

label {
  display: grid;
  gap: 6px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

input,
select {
  min-height: 40px;
  border: 1px solid var(--border-strong);
  border-radius: 6px;
  background: #fff;
  color: var(--text);
  padding: 0 11px;
}

.chain-section {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  overflow: hidden;
}

.chain-step {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 8px;
  min-height: 88px;
  border-right: 1px solid var(--border);
  padding: 13px;
}

.chain-step:last-child {
  border-right: 0;
}

.chain-step strong {
  display: block;
  margin-bottom: 4px;
}

.step-index {
  display: inline-grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: #eef2f7;
  color: #475569;
  font-size: 12px;
  font-weight: 850;
}

.chain-step.done .step-index {
  background: var(--green-soft);
  color: var(--green);
}

.chain-step.warning .step-index {
  background: var(--amber-soft);
  color: var(--amber);
}

.chain-step.blocked .step-index {
  background: var(--red-soft);
  color: var(--red);
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metrics article,
.overview-grid article {
  padding: 16px;
}

.metrics span,
.overview-grid span {
  display: block;
  color: var(--muted);
  font-size: 13px;
}

.metrics strong,
.overview-grid strong {
  display: block;
  margin-top: 4px;
  font-size: 28px;
  font-weight: 850;
}

.setup-panel {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.setup-panel header,
.detail-heading,
.requirement-list header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.compact-form,
.advanced-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.advanced-form {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding-top: 4px;
}

.main-workspace {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  align-items: stretch;
  overflow: hidden;
}

.requirement-list {
  display: grid;
  align-content: start;
  gap: 8px;
  border: 0;
  border-right: 1px solid var(--border);
  border-radius: 0;
  background: var(--surface-soft);
  padding: 14px;
}

.requirement-item {
  display: grid;
  gap: 6px;
  width: 100%;
  border: 1px solid transparent;
  border-radius: 8px;
  background: #fff;
  color: var(--text);
  padding: 12px;
  text-align: left;
}

.requirement-item.active {
  border-color: #93c5fd;
  background: #f8fbff;
}

.requirement-main {
  display: grid;
  gap: 6px;
  width: 100%;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  padding: 0;
  text-align: left;
}

.requirement-item strong,
.requirement-item small,
.requirement-item em {
  overflow-wrap: anywhere;
}

.requirement-item small {
  color: var(--muted);
}

.requirement-item em {
  color: #334155;
  font-style: normal;
  font-weight: 850;
}

.requirement-actions,
.requirement-pagination,
.requirement-pagination div {
  display: flex;
  gap: 6px;
}

.requirement-actions {
  flex-wrap: wrap;
  padding-top: 4px;
}

.requirement-actions button,
.requirement-pagination button {
  min-height: 30px;
  border: 1px solid var(--border-strong);
  border-radius: 6px;
  background: #fff;
  color: var(--blue);
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
  padding: 0 9px;
}

.requirement-pagination {
  align-items: center;
  justify-content: space-between;
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
  padding-top: 4px;
}

.requirement-pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.detail-panel {
  display: grid;
  align-content: start;
  gap: 14px;
  border: 0;
  border-radius: 0;
  padding: 16px;
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 8px;
}

.tabs button {
  min-height: 36px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #334155;
  cursor: pointer;
  font-weight: 800;
  padding: 0 10px;
}

.tabs button.active {
  border-color: #bfdbfe;
  background: var(--blue-soft);
  color: var(--blue);
}

.tabs span {
  margin-left: 6px;
  color: var(--muted);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.overview-grid article {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 800px;
  border-collapse: collapse;
}

th,
td {
  border-bottom: 1px solid var(--border);
  padding: 12px 14px;
  text-align: left;
  vertical-align: top;
  font-size: 13px;
}

th {
  background: #f8fafc;
  color: #475569;
  font-weight: 850;
  white-space: nowrap;
}

td {
  color: #334155;
  overflow-wrap: anywhere;
}

tbody tr:hover {
  background: #f9fbff;
}

.badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  border-radius: 999px;
  background: #eef2f7;
  color: #475569;
  font-size: 12px;
  font-weight: 800;
  margin-right: 4px;
  padding: 0 9px;
  white-space: nowrap;
}

.badge.success {
  background: var(--green-soft);
  color: var(--green);
}

.badge.info {
  background: var(--blue-soft);
  color: var(--blue);
}

.badge.warning {
  background: var(--amber-soft);
  color: var(--amber);
}

.badge.danger {
  background: var(--red-soft);
  color: var(--red);
}

.badge.allocated {
  background: #f5f3ff;
  color: var(--violet);
}

.badge.soft {
  opacity: 0.85;
}

.empty-state {
  display: grid;
  gap: 4px;
  border: 1px dashed var(--border-strong);
  border-radius: 8px;
  background: #fbfdff;
  padding: 22px;
}

.empty-state strong {
  color: var(--text);
}

.diagnostic-list {
  display: grid;
  gap: 10px;
}

.diagnostic-list article {
  display: grid;
  grid-template-columns: minmax(150px, 0.8fr) minmax(120px, 0.7fr) minmax(0, 1fr);
  gap: 10px;
  border: 1px solid var(--border);
  border-left-width: 4px;
  border-radius: 8px;
  padding: 12px;
}

.diagnostic-list article.error {
  border-left-color: var(--red);
}

.diagnostic-list article.warning {
  border-left-color: var(--amber);
}

.diagnostic-list article.info {
  border-left-color: var(--blue);
}

.diagnostic-list span {
  color: var(--muted);
  overflow-wrap: anywhere;
}

.diagnostic-list p {
  margin: 0;
}

@media (max-width: 1180px) {
  .context-bar,
  .compact-form,
  .advanced-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chain-section {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chain-step {
    border-right: 0;
    border-bottom: 1px solid var(--border);
  }

  .main-workspace {
    grid-template-columns: 1fr;
  }

  .requirement-list {
    border-right: 0;
    border-bottom: 1px solid var(--border);
  }

  .overview-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-heading,
  .setup-panel header,
  .detail-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .heading-actions,
  .setup-actions {
    flex-direction: column;
  }

  .context-bar,
  .compact-form,
  .advanced-form,
  .metrics,
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .chain-section {
    grid-template-columns: 1fr;
  }

  .diagnostic-list article {
    grid-template-columns: 1fr;
  }
}
</style>
