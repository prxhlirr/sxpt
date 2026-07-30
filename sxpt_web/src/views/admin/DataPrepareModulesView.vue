<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import TimedToast from '../../components/ui/TimedToast.vue';
import {
  authApi,
  dataPrepareApi,
  isDataPrepareConfigIncompleteError,
  type BusinessModule,
  type BusinessModuleProcessActor,
  type BusinessModuleProcessActorRequest,
  type BusinessModuleProcessStep,
  type BusinessModuleProcessStepRequest,
  type BusinessModuleRequest,
  type ConnectorSystem
} from '../../services/trainingApi';

type ModuleDialogMode = 'none' | 'detail' | 'create' | 'edit';
type ChainDialogMode = 'none' | 'step-create' | 'step-edit' | 'actor-create' | 'actor-edit';
type ModuleDetailTab = 'info' | 'chain';

const PAGE_SIZE = 10;

const session = authApi.getSession();
const tenantId = ref(session?.user.tenantId || 'demo-tenant');
const connectorSystemId = ref('');
const systemKeyword = ref('');
const systems = ref<ConnectorSystem[]>([]);
const modules = ref<BusinessModule[]>([]);
const selectedModule = ref<BusinessModule | null>(null);
const processSteps = ref<BusinessModuleProcessStep[]>([]);
const processActors = ref<BusinessModuleProcessActor[]>([]);
const selectedStep = ref<BusinessModuleProcessStep | null>(null);
const selectedActor = ref<BusinessModuleProcessActor | null>(null);
const dialogMode = ref<ModuleDialogMode>('none');
const detailTab = ref<ModuleDetailTab>('info');
const chainDialogMode = ref<ChainDialogMode>('none');
const loading = ref(false);
const currentPage = ref(1);
const notice = ref({
  show: false,
  type: 'info' as 'success' | 'error' | 'info',
  message: ''
});

const moduleForm = reactive<BusinessModuleRequest>({
  moduleName: '',
  moduleCode: '',
  externalModuleId: '',
  entryUrl: '',
  moduleType: 'BUSINESS',
  supportScenes: 'PRACTICE,EXAM',
  needPreData: true,
  defaultInitialStatus: 'DRAFT',
  defaultTargetStatus: 'SUBMITTED',
  capabilityCodesJson: '["DATA_CREATE","DATA_VALIDATE"]',
  defaultTemplateId: '',
  remark: ''
});

const stepForm = reactive<BusinessModuleProcessStepRequest>({
  stepNo: 1,
  stepCode: 'PURCHASE_CREATE',
  stepName: '采购申请填报',
  stepType: 'FILL',
  initExternalStatus: 'DRAFT',
  targetExternalStatus: 'SUBMITTED',
  completionRuleJson: '{"action":"submit"}',
  remark: ''
});

const actorForm = reactive<BusinessModuleProcessActorRequest>({
  actorNo: 1,
  actorRelation: 'PRIMARY',
  actorType: 'STUDENT',
  requiredOrgType: 'APPLY_ORG',
  requiredOrgCode: 'ORG_APPLY',
  requiredOrgName: '申请单位',
  requiredRoleCode: 'ROLE_APPLICANT',
  requiredRoleName: '采购申请人',
  isRequired: true,
  assignmentRule: 'CURRENT_STUDENT',
  remark: ''
});

function getDefaultModuleForm(): BusinessModuleRequest {
  const stamp = Date.now();
  return {
    moduleName: '本地联调-采购申请',
    moduleCode: `record_apply_${stamp}`,
    externalModuleId: `local-record-apply-${stamp}`,
    entryUrl: '/local-origin/record_apply',
    moduleType: 'BUSINESS',
    supportScenes: 'PRACTICE,EXAM',
    needPreData: true,
    defaultInitialStatus: 'DRAFT',
    defaultTargetStatus: 'SUBMITTED',
    capabilityCodesJson: '["DATA_CREATE","DATA_VALIDATE"]',
    defaultTemplateId: '',
    remark: '本地联调业务模块，用于老师创建练习或考试数据准备批次'
  };
}

const selectedSystem = computed(() =>
  systems.value.find((item) => item.id === connectorSystemId.value)
);

const filteredSystems = computed(() => {
  const keyword = systemKeyword.value.trim().toLowerCase();
  if (!keyword) return systems.value;
  return systems.value.filter((item) =>
    (item.systemName || '').toLowerCase().includes(keyword)
  );
});

const preDataCount = computed(
  () => modules.value.filter((item) => item.needPreData).length
);

const totalPages = computed(() => Math.max(1, Math.ceil(modules.value.length / PAGE_SIZE)));

const pagedModules = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE;
  return modules.value.slice(start, start + PAGE_SIZE);
});

const isDialogOpen = computed(() => dialogMode.value !== 'none');
const isChainDialogOpen = computed(() => chainDialogMode.value !== 'none');
const moduleEnableChecklist = computed(() => {
  const activeSteps = processSteps.value.filter((step) => step.status === 'ACTIVE');
  const activeActors = processActors.value.filter((actor) => actor.status === 'ACTIVE');
  return [
    {
      label: '至少维护 1 个启用的标准步骤',
      done: activeSteps.length > 0
    },
    {
      label: selectedStep.value
        ? `当前步骤“${selectedStep.value.stepName}”至少维护 1 个启用参与方`
        : '选择一个标准步骤后维护启用参与方',
      done: Boolean(selectedStep.value) && activeActors.length > 0
    },
    {
      label: activeSteps.length > 1
        ? '逐个点击左侧启用步骤，确认每个步骤都有启用参与方'
        : '启用多个步骤时，需要逐个确认参与方',
      done: activeSteps.length <= 1 ? activeSteps.length === 1 && activeActors.length > 0 : false
    }
  ];
});

onMounted(initialize);

watch(connectorSystemId, async () => {
  currentPage.value = 1;
  await loadModules();
});

/**
 * 业务功能：初始化平台和模块上下文，确保业务模块始终归属到明确的原平台。
 * 关键流程：先加载平台，默认选中第一个平台，再按平台读取模块列表。
 */
async function initialize() {
  await run(async () => {
    systems.value = await dataPrepareApi.listConnectorSystems(tenantId.value);
    connectorSystemId.value = systems.value[0]?.id || '';
    await loadModules();
  }, '业务模块已加载');
}

/**
 * 业务功能：读取当前原平台下的业务模块。
 * 关键流程：平台为空时清空模块，避免跨平台残留数据误导维护人员。
 */
async function loadModules() {
  modules.value = [];
  if (!connectorSystemId.value) return;
  modules.value = await dataPrepareApi.listAllBusinessModules({
    tenantId: tenantId.value,
    connectorSystemId: connectorSystemId.value
  });
  normalizePage();
}

/**
 * 业务功能：创建本地联调业务模块。
 * 关键流程：必须先选择平台，创建后立即进入办理链维护，避免管理员创建模块后找不到下一步入口。
 */
function openCreateModuleDialog() {
  if (!connectorSystemId.value) {
    notify('error', '请先选择原平台');
    return;
  }
  selectedModule.value = null;
  fillModuleForm(getDefaultModuleForm());
  dialogMode.value = 'create';
}

/**
 * 业务功能：提交创建本地联调业务模块。
 * 关键流程：管理员在弹窗中确认所有模块字段后再写入数据库，避免按钮点击即产生脏数据。
 */
async function createLocalModule() {
  if (!connectorSystemId.value) {
    notify('error', '请先选择原平台');
    return;
  }
  if (!moduleForm.moduleName?.trim()) {
    notify('error', '请填写模块名称');
    return;
  }
  if (!moduleForm.moduleCode?.trim()) {
    notify('error', '请填写模块编码');
    return;
  }
  if (!moduleForm.entryUrl?.trim()) {
    notify('error', '请填写业务入口地址');
    return;
  }
  if (!moduleForm.supportScenes?.trim()) {
    notify('error', '请填写支持场景');
    return;
  }
  await run(async () => {
    const created = await dataPrepareApi.createBusinessModule({
      tenantId: tenantId.value,
      connectorSystemId: connectorSystemId.value,
      moduleCode: moduleForm.moduleCode?.trim(),
      moduleName: moduleForm.moduleName?.trim(),
      externalModuleId: moduleForm.externalModuleId?.trim(),
      entryUrl: moduleForm.entryUrl?.trim(),
      moduleType: moduleForm.moduleType,
      supportScenes: moduleForm.supportScenes,
      needPreData: moduleForm.needPreData,
      defaultInitialStatus: moduleForm.defaultInitialStatus,
      defaultTargetStatus: moduleForm.defaultTargetStatus,
      capabilityCodesJson: moduleForm.capabilityCodesJson,
      defaultTemplateId: moduleForm.defaultTemplateId,
      remark: moduleForm.remark,
      updateBy: session?.user.userId || 'admin'
    });
    modules.value = [created, ...modules.value];
    currentPage.value = 1;
    selectedModule.value = await dataPrepareApi.getBusinessModule(created.id);
    await loadProcessSteps(selectedModule.value);
    detailTab.value = 'chain';
    dialogMode.value = 'detail';
  }, '已创建本地联调业务模块');
}

/**
 * 业务功能：打开业务模块配置弹窗。
 * 关键流程：从后端读取最新模块详情和办理链，支撑管理员维护标准步骤及步骤参与方。
 */
async function showModuleDetail(module: BusinessModule) {
  await run(async () => {
    selectedModule.value = await dataPrepareApi.getBusinessModule(module.id);
    await loadProcessSteps(selectedModule.value);
    detailTab.value = 'info';
    dialogMode.value = 'detail';
  }, '模块详情已加载');
}

/**
 * 业务功能：直接进入业务模块办理链维护。
 * 关键流程：复用详情加载链路，但用更明确的操作入口承接“模块创建后维护标准办理链”的业务步骤。
 */
async function maintainProcessChain(module: BusinessModule) {
  await run(async () => {
    selectedModule.value = await dataPrepareApi.getBusinessModule(module.id);
    await loadProcessSteps(selectedModule.value);
    detailTab.value = 'chain';
    dialogMode.value = 'detail';
  }, '办理链路已加载');
}

/**
 * 业务功能：加载业务模块标准办理步骤。
 * 关键流程：详情页打开后读取步骤列表，并默认选中第一步加载参与方。
 */
async function loadProcessSteps(module: BusinessModule) {
  processSteps.value = await dataPrepareApi.listBusinessModuleProcessSteps({
    tenantId: tenantId.value,
    businessModuleId: module.id
  });
  selectedStep.value = processSteps.value[0] || null;
  await loadProcessActors(selectedStep.value);
}

/**
 * 业务功能：加载标准步骤下的参与方。
 * 关键流程：未选中步骤时清空参与方，避免上一个步骤的参与方残留。
 */
async function loadProcessActors(step: BusinessModuleProcessStep | null) {
  processActors.value = [];
  selectedStep.value = step;
  if (!step) return;
  processActors.value = await dataPrepareApi.listBusinessModuleProcessActors({
    tenantId: tenantId.value,
    processStepId: step.id
  });
}

function openCreateStepDialog() {
  if (!selectedModule.value) return;
  fillStepForm({
    stepNo: processSteps.value.length + 1,
    stepCode: `STEP_${processSteps.value.length + 1}`,
    stepName: '',
    stepType: 'BUSINESS',
    initExternalStatus: selectedModule.value.defaultInitialStatus || 'DRAFT',
    targetExternalStatus: selectedModule.value.defaultTargetStatus || 'SUBMITTED',
    completionRuleJson: '{}',
    remark: ''
  });
  selectedStep.value = null;
  chainDialogMode.value = 'step-create';
}

function openEditStepDialog(step: BusinessModuleProcessStep) {
  selectedStep.value = step;
  fillStepForm(step);
  chainDialogMode.value = 'step-edit';
}

async function saveProcessStep() {
  if (!selectedModule.value) return;
  if (!stepForm.stepName?.trim()) {
    notify('error', '请填写步骤名称');
    return;
  }
  if (chainDialogMode.value === 'step-create' && !stepForm.stepCode?.trim()) {
    notify('error', '请填写步骤编码');
    return;
  }
  await run(async () => {
    const request = buildStepRequest();
    if (chainDialogMode.value === 'step-create') {
      await dataPrepareApi.createBusinessModuleProcessStep(selectedModule.value!.id, request);
    } else if (selectedStep.value) {
      await dataPrepareApi.updateBusinessModuleProcessStep(selectedStep.value.id, request);
    }
    await loadProcessSteps(selectedModule.value!);
    closeChainDialog();
  }, chainDialogMode.value === 'step-create' ? '标准步骤已创建' : '标准步骤已更新');
}

async function toggleStepStatus(step: BusinessModuleProcessStep) {
  if (!selectedModule.value) return;
  await run(async () => {
    if (step.status === 'ACTIVE') {
      await dataPrepareApi.disableBusinessModuleProcessStep(step.id);
    } else {
      await dataPrepareApi.enableBusinessModuleProcessStep(step.id);
    }
    await loadProcessSteps(selectedModule.value!);
  }, step.status === 'ACTIVE' ? '标准步骤已停用' : '标准步骤已启用');
}

function openCreateActorDialog() {
  if (!selectedStep.value) {
    notify('error', '请先选择标准步骤');
    return;
  }
  fillActorForm({
    actorNo: processActors.value.length + 1,
    actorRelation: processActors.value.length === 0 ? 'PRIMARY' : 'REVIEWER',
    actorType: 'STUDENT',
    requiredOrgType: '',
    requiredOrgCode: '',
    requiredOrgName: '',
    requiredRoleCode: '',
    requiredRoleName: '',
    isRequired: true,
    assignmentRule: 'CURRENT_STUDENT',
    remark: ''
  });
  selectedActor.value = null;
  chainDialogMode.value = 'actor-create';
}

function openEditActorDialog(actor: BusinessModuleProcessActor) {
  selectedActor.value = actor;
  fillActorForm(actor);
  chainDialogMode.value = 'actor-edit';
}

async function saveProcessActor() {
  if (!selectedStep.value) return;
  if (!actorForm.actorRelation?.trim()) {
    notify('error', '请填写参与关系');
    return;
  }
  if (!actorForm.actorType?.trim()) {
    notify('error', '请填写参与人类型');
    return;
  }
  if (!actorForm.requiredRoleCode?.trim()) {
    notify('error', '请填写角色编码');
    return;
  }
  await run(async () => {
    const request = buildActorRequest();
    if (chainDialogMode.value === 'actor-create') {
      await dataPrepareApi.createBusinessModuleProcessActor(selectedStep.value!.id, request);
    } else if (selectedActor.value) {
      await dataPrepareApi.updateBusinessModuleProcessActor(selectedActor.value.id, request);
    }
    await loadProcessActors(selectedStep.value!);
    closeChainDialog();
  }, chainDialogMode.value === 'actor-create' ? '步骤参与方已创建' : '步骤参与方已更新');
}

async function toggleActorStatus(actor: BusinessModuleProcessActor) {
  if (!selectedStep.value) return;
  await run(async () => {
    if (actor.status === 'ACTIVE') {
      await dataPrepareApi.disableBusinessModuleProcessActor(actor.id);
    } else {
      await dataPrepareApi.enableBusinessModuleProcessActor(actor.id);
    }
    await loadProcessActors(selectedStep.value!);
  }, actor.status === 'ACTIVE' ? '步骤参与方已停用' : '步骤参与方已启用');
}

/**
 * 业务功能：打开业务模块编辑弹窗。
 * 关键流程：取后端最新详情后回填表单，模块编码只展示不修改。
 */
async function editModule(module: BusinessModule) {
  await run(async () => {
    const detail = await dataPrepareApi.getBusinessModule(module.id);
    selectedModule.value = detail;
    fillModuleForm(detail);
    dialogMode.value = 'edit';
  }, '模块编辑信息已加载');
}

/**
 * 业务功能：保存业务模块修改。
 * 关键流程：调用后端更新接口并替换当前行，保持当前分页位置不变。
 */
async function updateModule() {
  if (!selectedModule.value) return;
  if (!moduleForm.moduleName?.trim()) {
    notify('error', '请填写模块名称');
    return;
  }
  if (!moduleForm.entryUrl?.trim()) {
    notify('error', '请填写业务入口地址');
    return;
  }

  await run(async () => {
    const updated = await dataPrepareApi.updateBusinessModule(selectedModule.value!.id, {
      tenantId: selectedModule.value!.tenantId,
      connectorSystemId: selectedModule.value!.connectorSystemId,
      moduleCode: selectedModule.value!.moduleCode,
      moduleName: moduleForm.moduleName?.trim(),
      externalModuleId: moduleForm.externalModuleId?.trim(),
      entryUrl: moduleForm.entryUrl?.trim(),
      moduleType: moduleForm.moduleType,
      supportScenes: moduleForm.supportScenes,
      needPreData: moduleForm.needPreData,
      defaultInitialStatus: moduleForm.defaultInitialStatus,
      defaultTargetStatus: moduleForm.defaultTargetStatus,
      capabilityCodesJson: moduleForm.capabilityCodesJson,
      defaultTemplateId: moduleForm.defaultTemplateId,
      remark: moduleForm.remark,
      updateBy: session?.user.userId || 'admin'
    });
    modules.value = modules.value.map((item) => (item.id === updated.id ? updated : item));
    closeDialog();
  }, '业务模块已更新');
}

/**
 * 业务功能：启用或停用业务模块。
 * 关键流程：调用后端真实启停用接口并替换当前行。
 */
async function toggleModuleStatus(module: BusinessModule) {
  if (module.status !== 'ACTIVE') {
    loading.value = true;
    closeNotice();
    try {
      const updated = await dataPrepareApi.enableBusinessModule(module.id);
      modules.value = modules.value.map((item) => (item.id === updated.id ? updated : item));
      notify('success', '业务模块已启用');
    } catch (err) {
      notify('error', moduleEnableErrorMessage(err));
      await openModuleChainAfterEnableFailure(module);
    } finally {
      loading.value = false;
    }
    return;
  }
  await run(async () => {
    const updated = await dataPrepareApi.disableBusinessModule(module.id);
    modules.value = modules.value.map((item) => (item.id === updated.id ? updated : item));
  }, '业务模块已停用');
}

/**
 * 业务功能：启用失败后直接定位到办理链维护页签。
 * 关键流程：后端启用闸门已经拒绝半成品模块，前端随即加载详情并打开办理链，减少管理员排查路径。
 */
async function openModuleChainAfterEnableFailure(module: BusinessModule) {
  try {
    selectedModule.value = await dataPrepareApi.getBusinessModule(module.id);
    await loadProcessSteps(selectedModule.value);
    detailTab.value = 'chain';
    dialogMode.value = 'detail';
  } catch {
    // 失败提示已经展示，详情加载失败不再覆盖原始启用错误。
  }
}

/**
 * 业务功能：把后端通用参数错误翻译成业务人员可执行的配置提示。
 * 关键流程：启用业务模块失败大概率来自办理链不完整，因此保留原始错误并补充下一步操作。
 */
function moduleEnableErrorMessage(err: unknown) {
  const rawMessage = err instanceof Error ? err.message : '操作失败';
  const normalized = rawMessage.trim();
  if (isDataPrepareConfigIncompleteError(err)) {
    return '业务模块启用失败：标准办理链还不完整。请先维护至少一个启用的标准步骤，并为每个启用步骤维护至少一个启用参与方。';
  }
  return `${normalized}。请确认该模块已维护启用的标准步骤和步骤参与方。`;
}

function fillModuleForm(module: BusinessModuleRequest) {
  moduleForm.moduleName = module.moduleName || '';
  moduleForm.moduleCode = module.moduleCode || '';
  moduleForm.externalModuleId = module.externalModuleId || '';
  moduleForm.entryUrl = module.entryUrl || '';
  moduleForm.moduleType = module.moduleType || 'BUSINESS';
  moduleForm.supportScenes = module.supportScenes || 'PRACTICE,EXAM';
  moduleForm.needPreData = Boolean(module.needPreData);
  moduleForm.defaultInitialStatus = module.defaultInitialStatus || '';
  moduleForm.defaultTargetStatus = module.defaultTargetStatus || '';
  moduleForm.capabilityCodesJson = module.capabilityCodesJson || '';
  moduleForm.defaultTemplateId = module.defaultTemplateId || '';
  moduleForm.remark = module.remark || '';
}

function fillStepForm(step: BusinessModuleProcessStepRequest) {
  stepForm.stepNo = step.stepNo || 1;
  stepForm.stepCode = step.stepCode || '';
  stepForm.stepName = step.stepName || '';
  stepForm.stepType = step.stepType || 'BUSINESS';
  stepForm.initExternalStatus = step.initExternalStatus || '';
  stepForm.targetExternalStatus = step.targetExternalStatus || '';
  stepForm.completionRuleJson = step.completionRuleJson || '{}';
  stepForm.remark = step.remark || '';
}

function fillActorForm(actor: BusinessModuleProcessActorRequest) {
  actorForm.actorNo = actor.actorNo || 1;
  actorForm.actorRelation = actor.actorRelation || 'PRIMARY';
  actorForm.actorType = actor.actorType || 'STUDENT';
  actorForm.requiredOrgType = actor.requiredOrgType || '';
  actorForm.requiredOrgCode = actor.requiredOrgCode || '';
  actorForm.requiredOrgName = actor.requiredOrgName || '';
  actorForm.requiredRoleCode = actor.requiredRoleCode || '';
  actorForm.requiredRoleName = actor.requiredRoleName || '';
  actorForm.isRequired = actor.isRequired ?? true;
  actorForm.assignmentRule = actor.assignmentRule || 'CURRENT_STUDENT';
  actorForm.remark = actor.remark || '';
}

function buildStepRequest(): BusinessModuleProcessStepRequest {
  return {
    tenantId: tenantId.value,
    connectorSystemId: selectedModule.value?.connectorSystemId,
    businessModuleId: selectedModule.value?.id,
    moduleCode: selectedModule.value?.moduleCode,
    stepNo: Number(stepForm.stepNo || 1),
    stepCode: stepForm.stepCode?.trim(),
    stepName: stepForm.stepName?.trim(),
    stepType: stepForm.stepType?.trim(),
    initExternalStatus: stepForm.initExternalStatus?.trim(),
    targetExternalStatus: stepForm.targetExternalStatus?.trim(),
    completionRuleJson: stepForm.completionRuleJson,
    remark: stepForm.remark,
    createBy: session?.user.userId || 'admin',
    updateBy: session?.user.userId || 'admin'
  };
}

function buildActorRequest(): BusinessModuleProcessActorRequest {
  return {
    tenantId: tenantId.value,
    processStepId: selectedStep.value?.id,
    actorNo: Number(actorForm.actorNo || 1),
    actorRelation: actorForm.actorRelation?.trim(),
    actorType: actorForm.actorType?.trim(),
    requiredOrgType: actorForm.requiredOrgType?.trim(),
    requiredOrgCode: actorForm.requiredOrgCode?.trim(),
    requiredOrgName: actorForm.requiredOrgName?.trim(),
    requiredRoleCode: actorForm.requiredRoleCode?.trim(),
    requiredRoleName: actorForm.requiredRoleName?.trim(),
    isRequired: actorForm.isRequired,
    assignmentRule: actorForm.assignmentRule?.trim(),
    remark: actorForm.remark,
    createBy: session?.user.userId || 'admin',
    updateBy: session?.user.userId || 'admin'
  };
}

function closeDialog() {
  selectedModule.value = null;
  processSteps.value = [];
  processActors.value = [];
  selectedStep.value = null;
  selectedActor.value = null;
  detailTab.value = 'info';
  dialogMode.value = 'none';
  chainDialogMode.value = 'none';
  closeNotice();
}

function closeChainDialog() {
  selectedActor.value = null;
  chainDialogMode.value = 'none';
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

function handleSystemKeywordChange() {
  if (
    connectorSystemId.value &&
    !filteredSystems.value.some((item) => item.id === connectorSystemId.value)
  ) {
    connectorSystemId.value = filteredSystems.value[0]?.id || '';
  }
}

function notify(type: 'success' | 'error' | 'info', text: string) {
  notice.value = { show: true, type, message: text };
}

function closeNotice() {
  notice.value = { ...notice.value, show: false, message: '' };
}

function booleanText(value?: boolean) {
  return value ? '需要' : '不需要';
}

function statusClass(status?: string) {
  return status === 'ACTIVE' ? 'success' : 'info';
}

function relationText(value?: string) {
  const map: Record<string, string> = {
    PRIMARY: '主办',
    COOPERATE: '协办',
    APPROVER: '审核',
    REVIEWER: '复核',
    COUNTERSIGN: '会签',
    CC: '抄送'
  };
  return value ? map[value] || value : '-';
}
</script>

<template>
  <section class="config-page module-page">
    <TimedToast
      :show="notice.show"
      :type="notice.type"
      :message="notice.message"
      @close="closeNotice"
    />

    <header class="page-heading">
      <div>
        <p>数据准备 / 业务模块</p>
        <h1>原平台业务模块维护</h1>
      </div>
      <button type="button" :disabled="loading || !connectorSystemId" @click="openCreateModuleDialog">
        创建本地联调模块
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
            {{ system.systemName }} / {{ system.systemCode }}
          </option>
        </select>
      </label>
      <p>{{ selectedSystem?.systemName || '未选择原平台' }}</p>
    </section>

    <section class="metrics">
      <article>
        <span>当前模块</span>
        <strong>{{ modules.length }}</strong>
      </article>
      <article>
        <span>需要预置数据</span>
        <strong>{{ preDataCount }}</strong>
      </article>
      <article>
        <span>可用平台</span>
        <strong>{{ systems.length }}</strong>
      </article>
    </section>

    <section class="summary-card">
      <span>当前原平台</span>
      <strong>{{ selectedSystem?.systemName || '未选择' }}</strong>
      <p>业务模块决定老师在批次准备页能选择哪些原平台业务，也决定后续模板和策略的归属。</p>
    </section>

    <section class="panel">
      <header class="panel-header">
        <h2>模块列表</h2>
        <span class="helper-text">共 {{ modules.length }} 条，每页 {{ PAGE_SIZE }} 条</span>
      </header>
      <div class="panel-body table-wrap">
        <table>
          <thead>
            <tr>
              <th>模块名称</th>
              <th>模块编码</th>
              <th>类型</th>
              <th>支持场景</th>
              <th>预置数据</th>
              <th>初始状态</th>
              <th>目标状态</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="module in pagedModules" :key="module.id">
              <td>{{ module.moduleName }}</td>
              <td>{{ module.moduleCode }}</td>
              <td>{{ module.moduleType || '-' }}</td>
              <td>{{ module.supportScenes || '-' }}</td>
              <td>{{ booleanText(module.needPreData) }}</td>
              <td>{{ module.defaultInitialStatus || '-' }}</td>
              <td>{{ module.defaultTargetStatus || '-' }}</td>
              <td>
                <span class="status-badge" :class="statusClass(module.status)">
                  {{ module.status || '未设置' }}
                </span>
              </td>
              <td>
                <div class="row-actions">
                  <button type="button" @click="showModuleDetail(module)">详情</button>
                  <button type="button" @click="maintainProcessChain(module)">办理链</button>
                  <button type="button" @click="editModule(module)">编辑</button>
                  <button type="button" :disabled="loading" @click="toggleModuleStatus(module)">
                    {{ module.status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="modules.length === 0" class="empty-state">
          <strong>暂无业务模块</strong>
          <p>业务模块定义老师后续可以选择哪个原平台业务来准备数据。</p>
        </div>
      </div>
      <footer v-if="modules.length > PAGE_SIZE" class="pagination-bar">
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
        class="edit-dialog"
        :class="{ 'module-chain-dialog': dialogMode === 'detail' && detailTab === 'chain' }"
        role="dialog"
        aria-modal="true"
        aria-labelledby="module-dialog-title"
      >
        <header class="dialog-header">
          <div>
            <p>业务模块</p>
            <h2 id="module-dialog-title">
              {{
                dialogMode === 'create'
                  ? '创建本地联调模块'
                  : dialogMode === 'edit'
                    ? '编辑业务模块'
                    : '模块配置 / 标准办理链'
              }}
            </h2>
          </div>
          <button type="button" class="icon-close" aria-label="关闭业务模块弹窗" @click="closeDialog">×</button>
        </header>

        <template v-if="dialogMode === 'detail' && selectedModule">
          <div class="detail-tabs" role="tablist" aria-label="业务模块配置视图">
            <button
              type="button"
              :class="{ active: detailTab === 'info' }"
              role="tab"
              :aria-selected="detailTab === 'info'"
              @click="detailTab = 'info'"
            >
              模块信息
            </button>
            <button
              type="button"
              :class="{ active: detailTab === 'chain' }"
              role="tab"
              :aria-selected="detailTab === 'chain'"
              @click="detailTab = 'chain'"
            >
              办理链路
            </button>
          </div>

          <div v-if="detailTab === 'info'" class="detail-grid">
            <span>模块名称</span><strong>{{ selectedModule.moduleName }}</strong>
            <span>模块编码</span><strong>{{ selectedModule.moduleCode }}</strong>
            <span>入口地址</span><strong>{{ selectedModule.entryUrl }}</strong>
            <span>类型</span><strong>{{ selectedModule.moduleType || '-' }}</strong>
            <span>支持场景</span><strong>{{ selectedModule.supportScenes || '-' }}</strong>
            <span>预置数据</span><strong>{{ booleanText(selectedModule.needPreData) }}</strong>
            <span>初始状态</span><strong>{{ selectedModule.defaultInitialStatus || '-' }}</strong>
            <span>目标状态</span><strong>{{ selectedModule.defaultTargetStatus || '-' }}</strong>
            <span>状态</span><strong>{{ selectedModule.status || '未设置' }}</strong>
            <span>备注</span><strong>{{ selectedModule.remark || '-' }}</strong>
          </div>

          <section v-if="detailTab === 'chain'" class="process-chain">
            <header class="chain-header">
              <div>
                <span>办理链配置</span>
                <h3>标准步骤与步骤参与方</h3>
                <p>正式练习和考试从初始数据开始，办理链用于定义第一步进入原平台时的单位、角色，以及后续过程追踪依据。</p>
                <p class="chain-rule">启用要求：至少 1 个启用标准步骤，且每个启用步骤至少 1 个启用参与方。</p>
              </div>
              <button type="button" class="secondary-action" @click="openCreateStepDialog">
                新增标准步骤
              </button>
            </header>

            <section class="readiness-checklist" aria-label="业务模块启用检查清单">
              <article
                v-for="item in moduleEnableChecklist"
                :key="item.label"
                :class="{ done: item.done }"
              >
                <span>{{ item.done ? '已满足' : '待补齐' }}</span>
                <strong>{{ item.label }}</strong>
              </article>
            </section>

            <div class="chain-layout">
              <article class="chain-list">
                <header>
                  <strong>标准办理步骤</strong>
                  <small>{{ processSteps.length }} 个步骤</small>
                </header>
                <button
                  v-for="step in processSteps"
                  :key="step.id"
                  type="button"
                  class="step-card"
                  :class="{ active: selectedStep?.id === step.id }"
                  @click="loadProcessActors(step)"
                >
                  <span>{{ step.stepNo }}</span>
                  <div>
                    <strong>{{ step.stepName }}</strong>
                    <small>{{ step.stepCode }} / {{ step.stepType || 'BUSINESS' }}</small>
                  </div>
                  <em :class="statusClass(step.status)">{{ step.status || '未设置' }}</em>
                </button>
                <div v-if="processSteps.length === 0" class="chain-empty">
                  <strong>暂无标准步骤</strong>
                  <p>先定义这个模块办理一条数据需要经过哪些步骤。</p>
                </div>
              </article>

              <article class="chain-detail">
                <header>
                  <div>
                    <strong>{{ selectedStep?.stepName || '未选择步骤' }}</strong>
                    <small>{{ selectedStep?.stepCode || '选择左侧步骤后维护参与方' }}</small>
                  </div>
                  <div class="chain-actions">
                    <button
                      v-if="selectedStep"
                      type="button"
                      class="secondary-action"
                      @click="openEditStepDialog(selectedStep)"
                    >
                      编辑步骤
                    </button>
                    <button
                      v-if="selectedStep"
                      type="button"
                      class="secondary-action"
                      @click="toggleStepStatus(selectedStep)"
                    >
                      {{ selectedStep.status === 'ACTIVE' ? '停用步骤' : '启用步骤' }}
                    </button>
                    <button
                      type="button"
                      class="primary-action"
                      :disabled="!selectedStep"
                      @click="openCreateActorDialog"
                    >
                      新增参与方
                    </button>
                  </div>
                </header>
                <div class="actor-table">
                  <table v-if="processActors.length > 0">
                    <thead>
                      <tr>
                        <th>序号</th>
                        <th>关系</th>
                        <th>单位</th>
                        <th>角色</th>
                        <th>分配规则</th>
                        <th>状态</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="actor in processActors" :key="actor.id">
                        <td>{{ actor.actorNo }}</td>
                        <td>{{ relationText(actor.actorRelation) }}</td>
                        <td>{{ actor.requiredOrgName || actor.requiredOrgCode || actor.requiredOrgType || '-' }}</td>
                        <td>{{ actor.requiredRoleName || actor.requiredRoleCode || '-' }}</td>
                        <td>{{ actor.assignmentRule || '-' }}</td>
                        <td>
                          <span class="status-badge" :class="statusClass(actor.status)">
                            {{ actor.status || '未设置' }}
                          </span>
                        </td>
                        <td>
                          <div class="row-actions">
                            <button type="button" @click="openEditActorDialog(actor)">编辑</button>
                            <button type="button" @click="toggleActorStatus(actor)">
                              {{ actor.status === 'ACTIVE' ? '停用' : '启用' }}
                            </button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                  <div v-else class="chain-empty">
                    <strong>暂无步骤参与方</strong>
                    <p>参与方决定学生进入原平台时使用哪个单位和角色。</p>
                  </div>
                </div>
              </article>
            </div>
          </section>
        </template>

        <div v-else class="edit-form">
          <label v-if="dialogMode === 'create'" class="field-wide">
            <span>原平台系统 <em>*</em></span>
            <select v-model="connectorSystemId">
              <option value="">请选择原平台系统</option>
              <option v-for="system in systems" :key="system.id" :value="system.id">
                {{ system.systemName }} / {{ system.systemCode }}
              </option>
            </select>
            <small>业务模块必须挂在一个明确的原平台系统下，后续模板、策略和批次都会沿用这个边界。</small>
          </label>
          <label>
            <span>模块名称 <em>*</em></span>
            <input v-model="moduleForm.moduleName" type="text" />
          </label>
          <label>
            <span>模块编码</span>
            <input v-model="moduleForm.moduleCode" type="text" :readonly="dialogMode === 'edit'" />
            <small v-if="dialogMode === 'create'">建议使用稳定英文编码，创建后不要随意变更。</small>
          </label>
          <label>
            <span>原平台模块 ID</span>
            <input v-model="moduleForm.externalModuleId" type="text" />
          </label>
          <label class="field-wide">
            <span>入口地址 <em>*</em></span>
            <input v-model="moduleForm.entryUrl" type="text" />
          </label>
          <label>
            <span>模块类型</span>
            <input v-model="moduleForm.moduleType" type="text" />
          </label>
          <label>
            <span>支持场景</span>
            <input v-model="moduleForm.supportScenes" type="text" />
          </label>
          <label>
            <span>初始状态</span>
            <input v-model="moduleForm.defaultInitialStatus" type="text" />
          </label>
          <label>
            <span>目标状态</span>
            <input v-model="moduleForm.defaultTargetStatus" type="text" />
          </label>
          <label>
            <span>默认模板 ID</span>
            <input v-model="moduleForm.defaultTemplateId" type="text" />
          </label>
          <label class="check-field">
            <input v-model="moduleForm.needPreData" type="checkbox" />
            <span>需要预置数据</span>
          </label>
          <label class="field-wide">
            <span>能力编码 JSON</span>
            <textarea v-model="moduleForm.capabilityCodesJson" rows="3"></textarea>
          </label>
          <label class="field-wide">
            <span>备注</span>
            <textarea v-model="moduleForm.remark" rows="3"></textarea>
          </label>
        </div>

        <footer class="dialog-actions">
          <span></span>
          <div>
            <button type="button" class="secondary-action" @click="closeDialog">关闭</button>
            <button
              v-if="dialogMode === 'create' || dialogMode === 'edit'"
              type="button"
              class="primary-action"
              :disabled="loading"
              @click="dialogMode === 'create' ? createLocalModule() : updateModule()"
            >
              {{ dialogMode === 'create' ? '提交创建' : '保存修改' }}
            </button>
          </div>
        </footer>
      </section>
    </div>

    <div
      v-if="isChainDialogOpen"
      class="modal-overlay modal-overlay-nested"
      role="presentation"
      @click.self="closeChainDialog"
    >
      <section class="edit-dialog chain-dialog" role="dialog" aria-modal="true">
        <header class="dialog-header">
          <div>
            <p>办理链配置</p>
            <h2>
              {{
                chainDialogMode === 'step-create'
                  ? '新增标准步骤'
                  : chainDialogMode === 'step-edit'
                    ? '编辑标准步骤'
                    : chainDialogMode === 'actor-create'
                      ? '新增步骤参与方'
                      : '编辑步骤参与方'
              }}
            </h2>
          </div>
          <button type="button" class="icon-close" aria-label="关闭办理链弹窗" @click="closeChainDialog">×</button>
        </header>

        <div v-if="chainDialogMode === 'step-create' || chainDialogMode === 'step-edit'" class="edit-form">
          <label>
            <span>步骤顺序 <em>*</em></span>
            <input v-model.number="stepForm.stepNo" type="number" min="1" :readonly="chainDialogMode === 'step-edit'" />
          </label>
          <label>
            <span>步骤编码 <em>*</em></span>
            <input v-model="stepForm.stepCode" type="text" :readonly="chainDialogMode === 'step-edit'" />
          </label>
          <label>
            <span>步骤名称 <em>*</em></span>
            <input v-model="stepForm.stepName" type="text" />
          </label>
          <label>
            <span>步骤类型</span>
            <select v-model="stepForm.stepType">
              <option value="FILL">填报</option>
              <option value="APPROVE">审批</option>
              <option value="REVIEW">复核</option>
              <option value="COUNTERSIGN">会签</option>
              <option value="ARCHIVE">归档</option>
              <option value="BUSINESS">业务处理</option>
            </select>
          </label>
          <label>
            <span>进入状态</span>
            <input v-model="stepForm.initExternalStatus" type="text" />
          </label>
          <label>
            <span>完成状态</span>
            <input v-model="stepForm.targetExternalStatus" type="text" />
          </label>
          <label class="field-wide">
            <span>完成规则</span>
            <textarea v-model="stepForm.completionRuleJson" rows="3"></textarea>
          </label>
          <label class="field-wide">
            <span>备注</span>
            <textarea v-model="stepForm.remark" rows="3"></textarea>
          </label>
        </div>

        <div v-else class="edit-form">
          <label>
            <span>参与方顺序 <em>*</em></span>
            <input v-model.number="actorForm.actorNo" type="number" min="1" :readonly="chainDialogMode === 'actor-edit'" />
          </label>
          <label>
            <span>参与关系 <em>*</em></span>
            <select v-model="actorForm.actorRelation">
              <option value="PRIMARY">主办</option>
              <option value="COOPERATE">协办</option>
              <option value="APPROVER">审核</option>
              <option value="REVIEWER">复核</option>
              <option value="COUNTERSIGN">会签</option>
              <option value="CC">抄送</option>
            </select>
          </label>
          <label>
            <span>参与人类型</span>
            <select v-model="actorForm.actorType">
              <option value="STUDENT">学生</option>
              <option value="TEACHER">教师</option>
              <option value="SYSTEM">系统</option>
              <option value="ORIGIN_USER">原平台用户</option>
            </select>
          </label>
          <label>
            <span>分配规则</span>
            <select v-model="actorForm.assignmentRule">
              <option value="CURRENT_STUDENT">当前学生</option>
              <option value="GROUP_MEMBER">小组成员</option>
              <option value="TEACHER">教师</option>
              <option value="SYSTEM">系统</option>
            </select>
          </label>
          <label>
            <span>单位类型</span>
            <input v-model="actorForm.requiredOrgType" type="text" />
          </label>
          <label>
            <span>单位编码</span>
            <input v-model="actorForm.requiredOrgCode" type="text" />
          </label>
          <label>
            <span>单位名称</span>
            <input v-model="actorForm.requiredOrgName" type="text" />
          </label>
          <label>
            <span>角色编码 <em>*</em></span>
            <input v-model="actorForm.requiredRoleCode" type="text" />
          </label>
          <label>
            <span>角色名称</span>
            <input v-model="actorForm.requiredRoleName" type="text" />
          </label>
          <label class="check-field">
            <input v-model="actorForm.isRequired" type="checkbox" />
            <span>必须参与</span>
          </label>
          <label class="field-wide">
            <span>备注</span>
            <textarea v-model="actorForm.remark" rows="3"></textarea>
          </label>
        </div>

        <footer class="dialog-actions">
          <span></span>
          <div>
            <button type="button" class="secondary-action" @click="closeChainDialog">关闭</button>
            <button
              type="button"
              class="primary-action"
              :disabled="loading"
              @click="
                chainDialogMode === 'step-create' || chainDialogMode === 'step-edit'
                  ? saveProcessStep()
                  : saveProcessActor()
              "
            >
              保存
            </button>
          </div>
        </footer>
      </section>
    </div>
  </section>
</template>

<style scoped>
@import './dataPrepareConfig.css';

.context-panel {
  display: grid;
  grid-template-columns: minmax(220px, 320px) minmax(260px, 420px) minmax(0, 1fr);
  align-items: end;
  gap: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  padding: 14px 16px;
}

.context-panel label {
  display: grid;
  gap: 6px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.context-panel input,
.context-panel select {
  min-height: 40px;
  border: 1px solid #d7e0ec;
  border-radius: 6px;
  background: #fff;
  color: #172033;
  padding: 0 11px;
}

.context-panel p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.row-actions,
.pagination-bar div,
.dialog-actions > div {
  display: flex;
  gap: 8px;
}

.row-actions {
  flex-wrap: wrap;
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

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.46);
}

.edit-dialog {
  width: min(760px, 100%);
  max-height: min(86vh, 720px);
  overflow: auto;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.16);
}

.module-chain-dialog {
  width: min(1040px, 100%);
}

.dialog-header,
.dialog-actions {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
}

.dialog-header {
  border-bottom: 1px solid #edf2f7;
}

.dialog-actions {
  border-top: 1px solid #edf2f7;
  background: #fbfdff;
}

.icon-close {
  display: inline-grid;
  width: 36px;
  height: 36px;
  min-width: 36px;
  place-items: center;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  cursor: pointer;
  font-size: 18px;
  font-weight: 800;
  line-height: 1;
  padding: 0;
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

.dialog-header p {
  margin: 0 0 4px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.dialog-header h2 {
  margin: 0;
  font-size: 20px;
}

.detail-tabs {
  display: inline-flex;
  gap: 4px;
  margin: 16px 22px 0;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
  padding: 4px;
}

.detail-tabs button {
  min-height: 34px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font-size: 13px;
  font-weight: 850;
  padding: 0 14px;
}

.detail-tabs button.active {
  background: #fff;
  color: #2563eb;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}

.detail-tabs button:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.detail-grid,
.edit-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 20px 22px 22px;
}

.detail-grid {
  grid-template-columns: 120px minmax(0, 1fr);
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

.process-chain {
  margin: 0 22px 22px;
  border: 1px solid #dbe6f2;
  border-radius: 8px;
  background: #f8fbff;
  overflow: hidden;
}

.chain-header,
.chain-list header,
.chain-detail > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chain-header {
  border-bottom: 1px solid #e5edf7;
  background: #fff;
  padding: 16px 18px;
}

.chain-header span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.chain-header h3 {
  margin: 4px 0 0;
  color: #172033;
  font-size: 18px;
}

.chain-rule {
  display: inline-flex;
  margin: 10px 0 0;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.5;
  padding: 7px 10px;
}

.readiness-checklist {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding: 0 22px 16px;
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

.chain-layout {
  display: grid;
  grid-template-columns: minmax(220px, 320px) minmax(0, 1fr);
  gap: 0;
}

.chain-list,
.chain-detail {
  min-width: 0;
  padding: 16px;
}

.chain-list {
  border-right: 1px solid #e5edf7;
  background: #fbfdff;
}

.chain-list header,
.chain-detail > header {
  margin-bottom: 12px;
}

.chain-list header strong,
.chain-detail > header strong {
  color: #172033;
  font-size: 14px;
}

.chain-list header small,
.chain-detail > header small {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.step-card {
  width: 100%;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  border: 1px solid #dbe6f2;
  border-radius: 8px;
  background: #fff;
  color: inherit;
  cursor: pointer;
  margin-bottom: 8px;
  padding: 10px;
  text-align: left;
}

.step-card.active {
  border-color: #93c5fd;
  background: #eff6ff;
}

.step-card > span {
  display: inline-grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 8px;
  background: #e0ecff;
  color: #2563eb;
  font-size: 13px;
  font-weight: 900;
}

.step-card strong,
.step-card small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-card strong {
  color: #172033;
  font-size: 13px;
}

.step-card small {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.step-card em {
  border-radius: 999px;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
  padding: 4px 8px;
}

.chain-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.actor-table {
  overflow: auto;
  border: 1px solid #e5edf7;
  border-radius: 8px;
  background: #fff;
}

.actor-table table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

.actor-table th,
.actor-table td {
  border-bottom: 1px solid #edf2f7;
  padding: 12px 14px;
  color: #334155;
  font-size: 13px;
  text-align: left;
}

.actor-table th {
  background: #f8fafc;
  color: #64748b;
  font-weight: 900;
}

.actor-table tbody tr:last-child td {
  border-bottom: 0;
}

.chain-empty {
  display: grid;
  place-items: center;
  min-height: 150px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #fff;
  padding: 18px;
  text-align: center;
}

.chain-empty strong {
  color: #172033;
  font-size: 14px;
}

.chain-empty p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.modal-overlay-nested {
  z-index: 3200;
  background: rgba(15, 23, 42, 0.34);
}

.chain-dialog {
  width: min(680px, 100%);
}

.edit-form label {
  display: grid;
  align-content: start;
  gap: 8px;
  min-width: 0;
  color: #1f2a44;
  font-size: 13px;
  font-weight: 750;
}

.edit-form input,
.edit-form select,
.edit-form textarea {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fbfdff;
  color: #172033;
  font: inherit;
  min-height: 44px;
  outline: none;
  padding: 0 12px;
  transition: border-color 160ms ease, background-color 160ms ease, box-shadow 160ms ease;
}

.edit-form select {
  max-width: 360px;
}

.edit-form .field-wide select {
  max-width: 480px;
}

.edit-form input:focus,
.edit-form select:focus,
.edit-form textarea:focus {
  border-color: #2563eb;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.edit-form input[readonly] {
  border-color: #e2e8f0;
  background: #f8fafc;
  color: #64748b;
  cursor: not-allowed;
}

.edit-form textarea {
  min-height: 88px;
  padding: 11px 12px;
  resize: vertical;
  line-height: 1.55;
}

.edit-form small {
  margin: -2px 0 0;
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.5;
}

.edit-form em {
  color: #dc2626;
  font-style: normal;
}

.field-wide {
  grid-column: 1 / -1;
}

.check-field {
  align-items: center;
  align-self: end;
  grid-template-columns: 18px minmax(0, 1fr);
  min-height: 44px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fbfdff;
  padding: 0 12px;
}

.check-field input {
  width: 16px;
  min-height: 16px;
  accent-color: #2563eb;
}

.secondary-action,
.primary-action {
  min-height: 40px;
  border-radius: 6px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.secondary-action {
  border: 1px solid #d7e0ec;
  background: #fff;
  color: #334155;
}

.primary-action {
  border: 1px solid #2f6df6;
  background: #2f6df6;
  color: #fff;
}

@media (max-width: 720px) {
  .context-panel,
  .detail-grid,
  .edit-form,
  .readiness-checklist {
    grid-template-columns: 1fr;
  }

  .pagination-bar,
  .dialog-actions,
  .dialog-actions > div {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
