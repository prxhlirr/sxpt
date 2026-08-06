<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import TimedToast from '../../components/ui/TimedToast.vue';
import { usersApi, type StudentDirectoryItem, type TeachOrg } from '../../api/users';
import { authApi, dataPrepareApi } from '../../services/trainingApi';
import type {
  BusinessModule,
  ConnectorSystem,
  DataPrepareJob,
  DataPrepareParticipant,
  DataPreparePreflightCheck,
  DataPreparePreflightResult,
  DataRequirement,
  ModuleDataStrategy,
  OriginOrg,
  OriginRole,
  TeachingDataTemplate
} from '../../services/trainingApi';

type ToastTone = 'success' | 'error' | 'info';
type PrepareScopeMode = 'single' | 'class';
type StudentSelectionFilter = 'all' | 'selected' | 'unselected';

const session = authApi.getSession();
const loading = ref(false);
const refreshing = ref(false);
const toasts = ref<Array<{ id: number; message: string; tone: ToastTone }>>([]);
let toastId = 0;

const connectorSystems = ref<ConnectorSystem[]>([]);
const businessModules = ref<BusinessModule[]>([]);
const originOrgs = ref<OriginOrg[]>([]);
const originRoles = ref<OriginRole[]>([]);
const teachOrgs = ref<TeachOrg[]>([]);
const studentDirectory = ref<StudentDirectoryItem[]>([]);
const selectedStudentIds = ref<string[]>([]);
const strategies = ref<ModuleDataStrategy[]>([]);
const templates = ref<TeachingDataTemplate[]>([]);
const requirements = ref<DataRequirement[]>([]);
const jobs = ref<DataPrepareJob[]>([]);
const preflightResult = ref<DataPreparePreflightResult | null>(null);

const form = reactive({
  tenantId: session?.user.tenantId || 'demo-tenant',
  connectorSystemId: '',
  businessModuleId: '',
  moduleCode: '',
  templateId: '',
  prepareScopeMode: 'single' as PrepareScopeMode,
  taskId: 'task_001',
  classId: '',
  sceneType: 'PRACTICE',
  requestBatchId: `batch-${Date.now()}`,
  studentId: '',
  questionId: 'question_001',
  questionAttemptId: `attempt-${Date.now()}`,
  actorType: 'student',
  ownerExternalOrgId: 'org_owner',
  requiredExternalOrgId: 'org_required',
  requiredExternalRoleId: 'role_required',
  requiredActionCode: 'submit',
  scoreValue: 10,
  studentKeyword: '',
  studentSelectionFilter: 'all' as StudentSelectionFilter,
  pastedStudentIds: ''
});

const selectedConnector = computed(() =>
  connectorSystems.value.find((item) => item.id === form.connectorSystemId)
);
const selectedModule = computed(() =>
  businessModules.value.find((item) => item.id === form.businessModuleId)
);
const activeStrategy = computed(() =>
  strategies.value.find((item) => item.sceneType === form.sceneType && isEnabledStatus(item.status))
);
const selectedTemplate = computed(() =>
  templates.value.find((item) => item.id === (activeStrategy.value?.templateId || form.templateId))
);
const latestRequirement = computed(() => requirements.value[0]);
const latestJob = computed(() => jobs.value[0]);
const classOptions = computed(() =>
  teachOrgs.value.filter((org) => org.orgType === 'CLASS')
);
const selectedClass = computed(() =>
  classOptions.value.find((org) => org.id === form.classId)
);
const selectedRequiredOrg = computed(() =>
  originOrgs.value.find((item) => externalOrgId(item) === form.requiredExternalOrgId)
);
const selectedRequiredRole = computed(() =>
  originRoles.value.find((item) => externalRoleId(item) === form.requiredExternalRoleId)
);
const classStudents = computed(() => {
  const classId = form.classId.trim();
  if (!classId) return [];
  return studentDirectory.value.filter((student) => student.unitId === classId);
});
const singleStudentOptions = computed(() =>
  form.classId.trim() ? classStudents.value : studentDirectory.value
);
const filteredClassStudents = computed(() => {
  const keyword = form.studentKeyword.trim().toLowerCase();
  return classStudents.value.filter((student) => {
    const checked = isStudentChecked(student.studentId);
    if (form.studentSelectionFilter === 'selected' && !checked) return false;
    if (form.studentSelectionFilter === 'unselected' && checked) return false;
    if (!keyword) return true;
    return [student.studentName, student.username, student.studentNo, student.studentId]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword));
  });
});
const pastedStudentIds = computed(() => parseStudentIds(form.pastedStudentIds));
const batchStudentIds = computed(() => {
  if (form.prepareScopeMode === 'single') {
    return form.studentId.trim() ? [form.studentId.trim()] : [];
  }
  return uniqueStrings(selectedStudentIds.value.length > 0 ? selectedStudentIds.value : pastedStudentIds.value);
});
const participantCount = computed(() => batchStudentIds.value.length);
const generatedRequiredActionsJson = computed(() =>
  stringifyJson([form.requiredActionCode.trim()].filter(Boolean))
);
const generatedScorePointSnapshotJson = computed(() =>
  stringifyJson({
    score: Number(form.scoreValue) || 0,
    action: form.requiredActionCode.trim() || 'submit'
  })
);
const generatedParticipantPreviewJson = computed(() =>
  stringifyJson({
    participantCount: participantCount.value,
    previewItems: buildParticipants().slice(0, 5).map((participant) => ({
      ...participant,
      dataScopeJson: JSON.parse(participant.dataScopeJson),
      requiredActionsJson: JSON.parse(participant.requiredActionsJson),
      scorePointSnapshotJson: JSON.parse(participant.scorePointSnapshotJson)
    }))
  })
);

const blockingMessage = computed(() => {
  if (!form.connectorSystemId) return '请先选择原平台系统。';
  if (!form.businessModuleId || !form.moduleCode) return '请先选择业务模块。';
  if (!form.sceneType) return '请先选择教学场景。';
  if (!selectedTemplate.value) return '当前模块/场景尚未配置可用数据模板。';
  if (!activeStrategy.value) return '当前模块/场景尚未配置启用状态的数据策略。';
  if (!form.taskId.trim()) return '请填写教学任务 ID。';
  if (!form.classId.trim()) return '请选择班级。';
  if (form.prepareScopeMode === 'single' && !form.studentId.trim()) return '请选择学生。';
  if (form.prepareScopeMode === 'class' && participantCount.value === 0) {
    return '当前班级未匹配到学生，请确认班级成员，或在高级兜底区粘贴学生列表。';
  }
  if (!form.questionId.trim()) return '请填写题目 ID。';
  if (!form.requiredExternalOrgId.trim()) return '请选择目标单位。';
  if (!form.requiredExternalRoleId.trim()) return '请选择目标角色。';
  return '';
});
const preflightBlockingMessage = computed(() => {
  if (!form.connectorSystemId) return '请先选择原平台系统。';
  if (!form.businessModuleId || !form.moduleCode) return '请先选择业务模块。';
  if (!form.sceneType) return '请先选择教学场景。';
  if (!form.taskId.trim()) return '请填写教学任务 ID。';
  return '';
});
const canGenerate = computed(() => !loading.value && !blockingMessage.value);
const canPreflight = computed(() => !loading.value && !preflightBlockingMessage.value);
const preflightBlockingChecks = computed(() =>
  preflightResult.value?.checks.filter((item) => item.status === 'FAIL') || []
);

/**
 * 页面初始化流程：先加载原平台系统，再按默认系统联动加载模块、模板、策略和最近生成结果。
 * 这样页面打开后即可展示当前主链路是否具备造数前置配置。
 */
async function initializePage() {
  await run(async () => {
    await loadConnectorSystems();
    await Promise.all([loadTeachClassOptions(), loadStudentDirectory()]);
    await Promise.all([loadBusinessModules(), loadOriginAccessOptions()]);
    await Promise.all([loadTemplates(), loadStrategies(), refreshRecent()]);
  }, true);
}

/**
 * 加载已启用的原平台系统，并在用户未选择时自动选中第一项。
 * 数据生成必须绑定原平台系统，因此该列表是页面主链路的入口。
 */
async function loadConnectorSystems() {
  connectorSystems.value = await dataPrepareApi.listConnectorSystems(form.tenantId);
  if (!form.connectorSystemId && connectorSystems.value.length > 0) {
    form.connectorSystemId = connectorSystems.value[0].id;
  }
}

/**
 * 根据原平台系统加载业务模块，并同步 moduleCode。
 * 后端触发造数接口同时要求 businessModuleId 和 moduleCode，前端在选择模块时统一维护。
 */
async function loadBusinessModules() {
  if (!form.connectorSystemId) {
    businessModules.value = [];
    return;
  }
  businessModules.value = await dataPrepareApi.listBusinessModules({
    tenantId: form.tenantId,
    connectorSystemId: form.connectorSystemId
  });
  if (!businessModules.value.some((item) => item.id === form.businessModuleId)) {
    const first = businessModules.value[0];
    form.businessModuleId = first?.id || '';
    form.moduleCode = first?.moduleCode || '';
  }
}

/**
 * 业务功能：加载教学平台班级列表，供批次准备按班级选择学生范围。
 * 关键流程：班级是教学平台主数据，页面只展示 CLASS 类型组织，避免运维手工录入 classId 导致学生范围不可追溯。
 */
async function loadTeachClassOptions() {
  teachOrgs.value = await usersApi.listOrgs(form.tenantId);
  if (!form.classId && classOptions.value.length > 0) {
    form.classId = classOptions.value[0].id;
  }
}

/**
 * 业务功能：加载教学平台学生目录，支撑按班级一次性生成多名学生的数据。
 * 关键流程：当前后端学生目录已返回学生所属 unitId，页面先按 classId 做本地过滤；没有匹配时仍允许粘贴学生 ID 作为过渡入口。
 */
async function loadStudentDirectory() {
  studentDirectory.value = await usersApi.listStudents();
  applyDefaultStudentSelection();
}

/**
 * 业务功能：加载原平台可用单位和角色，供批次准备页面下拉选择。
 * 关键流程：造数请求最终需要 externalOrgId 和 externalRoleId，页面用原平台备案数据自动回填，避免运维手工猜 ID。
 */
async function loadOriginAccessOptions() {
  if (!form.connectorSystemId) {
    originOrgs.value = [];
    originRoles.value = [];
    return;
  }
  const [orgResult, roleResult] = await Promise.all([
    dataPrepareApi.listOriginOrgs({
      tenantId: form.tenantId,
      connectorSystemId: form.connectorSystemId,
      activeOnly: true
    }),
    dataPrepareApi.listOriginRoles({
      tenantId: form.tenantId,
      connectorSystemId: form.connectorSystemId,
      activeOnly: true
    })
  ]);
  originOrgs.value = orgResult;
  originRoles.value = roleResult;
  applyDefaultOriginAccessOptions();
}

/**
 * 业务功能：在运维没有明确选择时自动使用第一组可用单位和角色。
 * 关键流程：默认值只在当前值无效时覆盖，避免切换刷新时把用户已经选好的业务身份冲掉。
 */
function applyDefaultOriginAccessOptions() {
  if (!originOrgs.value.some((item) => externalOrgId(item) === form.requiredExternalOrgId)) {
    form.requiredExternalOrgId = externalOrgId(originOrgs.value[0]);
  }
  if (!originOrgs.value.some((item) => externalOrgId(item) === form.ownerExternalOrgId)) {
    form.ownerExternalOrgId = form.requiredExternalOrgId || externalOrgId(originOrgs.value[0]);
  }
  if (!originRoles.value.some((item) => externalRoleId(item) === form.requiredExternalRoleId)) {
    form.requiredExternalRoleId = externalRoleId(originRoles.value[0]);
  }
}

/**
 * 业务功能：根据当前班级自动选择学生范围。
 * 关键流程：批量模式默认全选班级学生；单学生模式默认选第一个可用学生，主流程不再要求运维复制内部 studentId。
 */
function applyDefaultStudentSelection() {
  const availableStudents = singleStudentOptions.value;
  if (!availableStudents.some((student) => student.studentId === form.studentId)) {
    form.studentId = availableStudents[0]?.studentId || '';
  }
  selectedStudentIds.value = classStudents.value.map((student) => student.studentId);
}

function toggleAllClassStudents(checked: boolean) {
  selectedStudentIds.value = checked ? classStudents.value.map((student) => student.studentId) : [];
}

function toggleFilteredClassStudents(checked: boolean) {
  const filteredIds = filteredClassStudents.value.map((student) => student.studentId);
  if (checked) {
    selectedStudentIds.value = uniqueStrings([...selectedStudentIds.value, ...filteredIds]);
    return;
  }
  selectedStudentIds.value = selectedStudentIds.value.filter((studentId) => !filteredIds.includes(studentId));
}

function isStudentChecked(studentId: string) {
  return selectedStudentIds.value.includes(studentId);
}

function toggleStudent(studentId: string, checked: boolean) {
  if (checked) {
    selectedStudentIds.value = uniqueStrings([...selectedStudentIds.value, studentId]);
    return;
  }
  selectedStudentIds.value = selectedStudentIds.value.filter((item) => item !== studentId);
}

/**
 * 加载当前模块下的数据模板。
 * 若策略绑定了模板，以策略为准；否则用用户选择的模板作为兜底。
 */
async function loadTemplates() {
  if (!form.businessModuleId) {
    templates.value = [];
    form.templateId = '';
    return;
  }
  templates.value = await dataPrepareApi.listTemplates({
    tenantId: form.tenantId,
    connectorSystemId: form.connectorSystemId,
    moduleCode: form.moduleCode,
    sceneType: form.sceneType
  });
  if (!templates.value.some((item) => item.id === form.templateId)) {
    form.templateId = templates.value[0]?.id || '';
  }
}

/**
 * 加载当前模块和场景下的数据策略。
 * 策略决定数据准备服务如何选择模板、规则和后续执行方式。
 */
async function loadStrategies() {
  if (!form.businessModuleId) {
    strategies.value = [];
    return;
  }
  strategies.value = await dataPrepareApi.listModuleDataStrategies({
    tenantId: form.tenantId,
    connectorSystemId: form.connectorSystemId,
    businessModuleId: form.businessModuleId,
  });
}

/**
 * 刷新最近需求和任务结果。
 * 页面只保留最近批次证据，避免把运维诊断信息重新塞回生成入口。
 */
async function refreshRecent() {
  if (!form.connectorSystemId && !form.businessModuleId && !form.taskId) return;
  refreshing.value = true;
  try {
    const [requirementResult, jobResult] = await Promise.all([
      dataPrepareApi.listRequirements({
        tenantId: form.tenantId,
        taskId: form.taskId,
        sceneType: form.sceneType
      }),
      dataPrepareApi.listJobs({
        tenantId: form.tenantId,
        taskId: form.taskId,
        sceneType: form.sceneType
      })
    ]);
    requirements.value = requirementResult.slice(0, 5);
    jobs.value = jobResult.slice(0, 5);
  } finally {
    refreshing.value = false;
  }
}

/**
 * 触发数据准备前置链路自检。
 * 造数失败通常不是按钮问题，而是平台能力、模板策略或流程参与方缺口；这里先把这些运行前事实暴露出来。
 */
async function runPreflight() {
  if (preflightBlockingMessage.value) {
    notify(preflightBlockingMessage.value, 'error');
    return;
  }
  await run(async () => {
    preflightResult.value = await dataPrepareApi.preflightTaskPrepare({
      tenantId: form.tenantId,
      taskId: form.taskId.trim(),
      connectorSystemId: form.connectorSystemId,
      businessModuleId: form.businessModuleId,
      moduleCode: form.moduleCode,
      sceneType: form.sceneType
    });
    if (preflightResult.value.ready) {
      notify('链路自检通过，可以触发数据生成。', 'success');
    } else {
      notify(`链路自检发现 ${preflightResult.value.failCount} 个阻塞项，请先处理失败节点。`, 'error');
    }
  });
}

/**
 * 业务功能：组装本次批次准备的全部参与者请求。
 * 关键流程：单学生模式生成一条参与者；班级模式按班级学生目录或粘贴学生 ID 生成多条参与者，确保后端一次请求即可创建 50 人级别的数据需求。
 */
function buildParticipants(): DataPrepareParticipant[] {
  return batchStudentIds.value.map((studentId) => buildParticipantForStudent(studentId));
}

/**
 * 业务功能：为指定学生构造一条原平台数据需求参与者。
 * 关键流程：每个学生必须有独立 questionAttemptId，避免 50 人批量造数时因作答 ID 相同触发幂等冲突或结果覆盖。
 */
function buildParticipantForStudent(studentId: string): DataPrepareParticipant {
  const trimmedStudentId = studentId.trim();
  return {
    studentId: trimmedStudentId,
    questionId: form.questionId.trim(),
    questionAttemptId: buildAttemptId(trimmedStudentId),
    actorType: form.actorType.trim(),
    ownerExternalOrgId: form.ownerExternalOrgId.trim(),
    requiredExternalOrgId: form.requiredExternalOrgId.trim(),
    requiredExternalRoleId: form.requiredExternalRoleId.trim(),
    dataScopeJson: '{}',
    requiredActionsJson: generatedRequiredActionsJson.value,
    scorePointSnapshotJson: generatedScorePointSnapshotJson.value
  };
}

function buildAttemptId(studentId: string) {
  if (form.prepareScopeMode === 'single') {
    return form.questionAttemptId.trim();
  }
  return `attempt-${form.taskId.trim()}-${form.questionId.trim()}-${studentId}-${Date.now()}`;
}

/**
 * 触发教学任务数据准备。
 * 该方法只走后端正式入口，不在前端伪造业务数据，确保测试链路与生产链路一致。
 */
async function generateData() {
  if (blockingMessage.value) {
    notify(blockingMessage.value, 'error');
    return;
  }
  await run(async () => {
    const requestBatchId = form.requestBatchId.trim() || `batch-${Date.now()}`;
    const result = await dataPrepareApi.triggerTaskPrepare(form.taskId.trim(), {
      requirementCode: `REQ-${Date.now()}`,
      connectorSystemId: form.connectorSystemId,
      businessModuleId: form.businessModuleId,
      moduleCode: form.moduleCode,
      strategyId: activeStrategy.value?.id,
      templateId: activeStrategy.value?.templateId || form.templateId,
      classId: form.classId.trim(),
      sceneType: form.sceneType,
      triggerType: 'ON_DEMAND',
      requestBatchId,
      idempotencyKey: `task-prepare:${form.taskId.trim()}:${form.sceneType}:${requestBatchId}`,
      participants: buildParticipants(),
      remark: '管理端页面触发数据生成'
    });
    notifyPrepareResult(result.job);
    form.requestBatchId = `batch-${Date.now()}`;
    form.questionAttemptId = `attempt-${Date.now()}`;
    await refreshRecent();
  });
}

/**
 * 统一异步执行包装。
 * 保证加载态、错误提示和页面初始化错误都通过同一条路径处理。
 */
async function run(action: () => Promise<void>, silent = false) {
  loading.value = true;
  try {
    await action();
  } catch (error) {
    const message = error instanceof Error ? error.message : '操作失败';
    if (!silent) {
      notify(message, 'error');
    } else {
      notify(`页面初始化失败：${message}`, 'error');
    }
  } finally {
    loading.value = false;
  }
}

function onModuleChange() {
  const next = selectedModule.value;
  form.moduleCode = next?.moduleCode || '';
}

function externalOrgId(org?: OriginOrg) {
  return org?.externalOrgId || org?.orgCode || '';
}

function externalRoleId(role?: OriginRole) {
  return role?.externalRoleId || role?.roleCode || '';
}

function parseStudentIds(value: string) {
  return uniqueStrings(
    value
      .split(/[\n,，;；\s]+/)
      .map((item) => item.trim())
      .filter(Boolean)
  );
}

function uniqueStrings(values: string[]) {
  return Array.from(new Set(values));
}

function stringifyJson(value: unknown) {
  return JSON.stringify(value, null, 2);
}

function notify(message: string, tone: ToastTone = 'info') {
  toasts.value.push({ id: ++toastId, message, tone });
}

/**
 * 根据后端任务终态提示生成结果。
 * 触发接口成功只代表请求进入后端，真正是否造数成功必须以 DataPrepareJob.jobStatus 为准。
 */
function notifyPrepareResult(job?: DataPrepareJob) {
  const status = job?.jobStatus;
  if (status === 'SUCCESS') {
    notify('数据生成成功。', 'success');
    return;
  }
  if (status === 'PARTIAL_FAILED' || status === 'PARTIAL_SUCCESS') {
    notify(`数据部分生成成功：${job?.successCount ?? 0} 成功，${job?.failedCount ?? 0} 失败。`, 'info');
    return;
  }
  if (status === 'FAILED') {
    notify(`数据生成失败：${jobFailureMessage(job)}`, 'error');
    return;
  }
  notify('数据生成任务已提交，请刷新查看执行结果。', 'info');
}

/**
 * 提取任务失败原因。
 * 后端当前可能把错误放在 errorMessage，也可能放在 resultJson.errorMessage，因此前端做一次兼容读取。
 */
function jobFailureMessage(job?: DataPrepareJob) {
  const directMessage = job?.errorMessage?.trim();
  if (directMessage) return directMessage;
  const resultMessage = readResultJsonMessage(job?.resultJson);
  return resultMessage || '后端未返回明确失败原因';
}

function readResultJsonMessage(resultJson?: string) {
  if (!resultJson) return '';
  try {
    const parsed = JSON.parse(resultJson) as { errorMessage?: unknown };
    return typeof parsed.errorMessage === 'string' ? parsed.errorMessage.trim() : '';
  } catch {
    return '';
  }
}

function preflightStatusTone(value?: string) {
  if (value === 'PASS') return 'success';
  if (value === 'WARN') return 'warning';
  if (value === 'FAIL') return 'danger';
  return 'neutral';
}

function preflightStatusText(value?: string) {
  const map: Record<string, string> = {
    PASS: '通过',
    WARN: '警告',
    FAIL: '失败'
  };
  return value ? map[value] || value : '-';
}

function preflightNodeText(value: string) {
  const map: Record<string, string> = {
    CONNECTOR_SYSTEM: '原平台系统',
    DATA_CREATE_CAPABILITY: '造数能力',
    BUSINESS_MODULE: '业务模块',
    DATA_TEMPLATE: '数据模板',
    MODULE_DATA_STRATEGY: '数据策略',
    PROCESS_STEPS: '流程步骤',
    PROCESS_ACTORS: '步骤参与方'
  };
  return map[value] || value;
}

function preflightEvidenceText(check: DataPreparePreflightCheck) {
  const evidence = check.evidence || {};
  const entries = Object.entries(evidence).filter(([, value]) => Boolean(value));
  if (entries.length === 0) return '';
  return entries.map(([key, value]) => `${key}: ${value}`).join(' / ');
}

function closeToast(id: number) {
  toasts.value = toasts.value.filter((item) => item.id !== id);
}

function sceneText(value: string) {
  const map: Record<string, string> = {
    PRACTICE: '练习',
    EXAM: '考试',
    TEACHING: '教学'
  };
  return map[value] || value;
}

function statusText(value?: string) {
  const map: Record<string, string> = {
    PENDING: '待处理',
    RUNNING: '执行中',
    SUCCESS: '成功',
    PARTIAL_SUCCESS: '部分成功',
    PARTIAL_FAILED: '部分失败',
    FAILED: '失败',
    CREATED: '已创建',
    PREPARED: '已准备'
  };
  return value ? map[value] || value : '暂无';
}

function isEnabledStatus(value?: string) {
  return !value || value === 'ENABLED' || value === 'ACTIVE';
}

function statusTone(value?: string) {
  if (value === 'SUCCESS' || value === 'PREPARED') return 'success';
  if (value === 'FAILED') return 'danger';
  if (value === 'PARTIAL_FAILED' || value === 'PARTIAL_SUCCESS') return 'warning';
  if (value === 'RUNNING' || value === 'PENDING') return 'warning';
  return 'neutral';
}

function formatTime(value?: string) {
  if (!value) return '-';
  return new Date(value).toLocaleString();
}

function shortId(value?: string) {
  if (!value) return '-';
  return value.length > 18 ? `${value.slice(0, 8)}...${value.slice(-6)}` : value;
}

watch(
  () => form.connectorSystemId,
  async () => {
    preflightResult.value = null;
    form.businessModuleId = '';
    form.moduleCode = '';
    form.templateId = '';
    await run(async () => {
      await Promise.all([loadBusinessModules(), loadOriginAccessOptions()]);
      await Promise.all([loadTemplates(), loadStrategies(), refreshRecent()]);
    }, true);
  }
);

watch(
  () => form.businessModuleId,
  async () => {
    preflightResult.value = null;
    onModuleChange();
    form.templateId = '';
    await run(async () => {
      await Promise.all([loadTemplates(), loadStrategies(), refreshRecent()]);
    }, true);
  }
);

watch(
  () => form.sceneType,
  async () => {
    preflightResult.value = null;
    form.templateId = '';
    await run(async () => {
      await Promise.all([loadTemplates(), loadStrategies(), refreshRecent()]);
    }, true);
  }
);

watch(
  () => form.classId,
  () => {
    applyDefaultStudentSelection();
  }
);

watch(
  () => form.prepareScopeMode,
  () => {
    applyDefaultStudentSelection();
  }
);

onMounted(initializePage);
</script>

<template>
  <section class="data-generate-page">
    <TimedToast
      v-for="toast in toasts"
      :key="toast.id"
      show
      :message="toast.message"
      :type="toast.tone"
      @close="closeToast(toast.id)"
    />

    <header class="page-header">
      <div>
        <p class="eyebrow">Data Prepare</p>
        <h1>数据生成</h1>
        <p class="subtitle">面向教学任务触发原平台数据准备，并回看最近批次结果。</p>
      </div>
      <button class="ghost-button" type="button" :disabled="refreshing" @click="refreshRecent">
        {{ refreshing ? '刷新中' : '刷新结果' }}
      </button>
    </header>

    <main class="workspace">
      <form class="generate-panel" @submit.prevent="generateData">
        <section class="form-section">
          <div class="section-heading">
            <h2>生成范围</h2>
            <span>{{ selectedConnector?.systemName || '未选择系统' }}</span>
          </div>

          <label>
            <span>原平台系统</span>
            <select v-model="form.connectorSystemId">
              <option value="">请选择</option>
              <option v-for="system in connectorSystems" :key="system.id" :value="system.id">
                {{ system.systemName }}（{{ system.systemCode }}）
              </option>
            </select>
          </label>

          <label>
            <span>业务模块</span>
            <select v-model="form.businessModuleId">
              <option value="">请选择</option>
              <option v-for="module in businessModules" :key="module.id" :value="module.id">
                {{ module.moduleName }}（{{ module.moduleCode }}）
              </option>
            </select>
          </label>

          <div class="field-grid">
            <label>
              <span>教学场景</span>
              <select v-model="form.sceneType">
                <option value="PRACTICE">练习</option>
                <option value="EXAM">考试</option>
                <option value="TEACHING">教学</option>
              </select>
            </label>
            <label>
              <span>模板</span>
              <select v-model="form.templateId" :disabled="Boolean(activeStrategy?.templateId)">
                <option value="">请选择</option>
                <option v-for="template in templates" :key="template.id" :value="template.id">
                  {{ template.templateName }}
                </option>
              </select>
            </label>
          </div>

          <div class="field-grid">
            <label>
              <span>生成对象</span>
              <select v-model="form.prepareScopeMode">
                <option value="single">单个学生</option>
                <option value="class">按班级批量</option>
              </select>
            </label>
            <label>
              <span>教学任务 ID</span>
              <input v-model.trim="form.taskId" type="text" />
            </label>
          </div>

          <div class="field-grid">
            <label>
              <span>班级</span>
              <select v-model="form.classId">
                <option value="">请选择</option>
                <option v-for="org in classOptions" :key="org.id" :value="org.id">
                  {{ org.orgName }}（{{ org.orgCode }}）
                </option>
              </select>
            </label>
            <label>
              <span>题目 ID</span>
              <input v-model.trim="form.questionId" type="text" />
            </label>
          </div>
        </section>

        <section class="form-section">
          <div class="section-heading">
            <h2>参与者</h2>
            <span>{{ participantCount }} 名学生</span>
          </div>
          <div v-if="form.prepareScopeMode === 'single'" class="field-grid">
            <label>
              <span>学生</span>
              <select v-model="form.studentId">
                <option value="">请选择</option>
                <option v-for="student in singleStudentOptions" :key="student.studentId" :value="student.studentId">
                  {{ student.studentName || student.username }}（{{ student.studentNo || student.username }}）
                </option>
              </select>
            </label>
            <label>
              <span>作答 ID</span>
              <input v-model.trim="form.questionAttemptId" type="text" />
            </label>
          </div>

          <div v-else class="batch-students">
            <div class="batch-summary">
              <strong>{{ selectedClass?.orgName || '未选择班级' }}：共 {{ classStudents.length }} 人，已选 {{ participantCount }} 人</strong>
              <span>默认全选当前班级学生；学生目录为空时，可在高级兜底区粘贴学生 ID。</span>
            </div>
            <div class="student-filter-bar">
              <input
                v-model.trim="form.studentKeyword"
                type="search"
                placeholder="搜索姓名、学号或账号"
              />
              <select v-model="form.studentSelectionFilter">
                <option value="all">全部学生</option>
                <option value="selected">只看已选</option>
                <option value="unselected">只看未选</option>
              </select>
            </div>
            <div class="student-bulk-actions">
              <button type="button" class="ghost-button" @click="toggleAllClassStudents(true)">
                全选全班
              </button>
              <button type="button" class="ghost-button" @click="toggleAllClassStudents(false)">
                清空全班
              </button>
              <button type="button" class="ghost-button" @click="toggleFilteredClassStudents(true)">
                选择当前筛选
              </button>
              <button type="button" class="ghost-button" @click="toggleFilteredClassStudents(false)">
                清空当前筛选
              </button>
            </div>
            <div v-if="classStudents.length" class="student-table-wrap">
              <table class="student-table">
                <thead>
                  <tr>
                    <th>选择</th>
                    <th>姓名</th>
                    <th>学号</th>
                    <th>账号</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="student in filteredClassStudents" :key="student.studentId">
                    <td>
                      <input
                        type="checkbox"
                        :checked="isStudentChecked(student.studentId)"
                        @change="toggleStudent(student.studentId, ($event.target as HTMLInputElement).checked)"
                      />
                    </td>
                    <td>{{ student.studentName || '-' }}</td>
                    <td>{{ student.studentNo || '-' }}</td>
                    <td>{{ student.username || '-' }}</td>
                  </tr>
                </tbody>
              </table>
              <p v-if="filteredClassStudents.length === 0" class="empty-text">
                当前筛选条件下没有学生
              </p>
            </div>
            <p v-else class="empty-text">当前班级暂无学生，请先在系统管理维护班级成员。</p>
          </div>

          <div class="field-grid">
            <label>
              <span>目标单位</span>
              <select v-model="form.requiredExternalOrgId">
                <option value="">请选择</option>
                <option v-for="org in originOrgs" :key="org.id" :value="externalOrgId(org)">
                  {{ org.orgName }}（{{ externalOrgId(org) }}）
                </option>
              </select>
            </label>
            <label>
              <span>目标角色</span>
              <select v-model="form.requiredExternalRoleId">
                <option value="">请选择</option>
                <option v-for="role in originRoles" :key="role.id" :value="externalRoleId(role)">
                  {{ role.roleName }}（{{ externalRoleId(role) }}）
                </option>
              </select>
            </label>
          </div>
          <div class="field-grid">
            <label>
              <span>要求动作</span>
              <select v-model="form.requiredActionCode">
                <option value="submit">提交</option>
                <option value="save">保存</option>
                <option value="approve">审批</option>
                <option value="reject">驳回</option>
              </select>
            </label>
            <label>
              <span>得分值</span>
              <input v-model.number="form.scoreValue" type="number" min="0" />
            </label>
          </div>
        </section>

        <details class="advanced-section">
          <summary>高级参数与 JSON 预览</summary>
          <div class="field-grid">
            <label>
              <span>批次 ID</span>
              <input v-model.trim="form.requestBatchId" type="text" />
            </label>
            <label>
              <span>作答 ID</span>
              <input v-model.trim="form.questionAttemptId" type="text" />
            </label>
          </div>
          <div class="field-grid">
            <label>
              <span>参与者类型</span>
              <select v-model="form.actorType">
                <option value="student">学生</option>
                <option value="teacher">教师</option>
                <option value="operator">业务经办人</option>
                <option value="approver">审批人</option>
              </select>
            </label>
            <label>
              <span>归属单位</span>
              <select v-model="form.ownerExternalOrgId">
                <option value="">请选择</option>
                <option v-for="org in originOrgs" :key="org.id" :value="externalOrgId(org)">
                  {{ org.orgName }}（{{ externalOrgId(org) }}）
                </option>
              </select>
            </label>
          </div>
          <label>
            <span>要求动作 JSON（系统生成）</span>
            <textarea :value="generatedRequiredActionsJson" rows="3" readonly />
          </label>
          <label>
            <span>得分点快照 JSON（系统生成）</span>
            <textarea :value="generatedScorePointSnapshotJson" rows="4" readonly />
          </label>
          <label>
            <span>批量学生 ID 兜底</span>
            <textarea
              v-model.trim="form.pastedStudentIds"
              rows="5"
              placeholder="仅在班级学生目录为空时使用"
            />
          </label>
          <label>
            <span>参与者请求预览</span>
            <textarea :value="generatedParticipantPreviewJson" rows="10" readonly />
          </label>
        </details>

        <p v-if="blockingMessage" class="blocking-message">{{ blockingMessage }}</p>

        <footer class="form-actions">
          <button class="ghost-button" type="button" :disabled="!canPreflight" @click="runPreflight">
            链路自检
          </button>
          <button class="primary-button" type="submit" :disabled="!canGenerate">
            {{ loading ? '提交中' : '生成数据' }}
          </button>
        </footer>
      </form>

      <aside class="result-panel">
        <section class="result-summary">
          <div>
            <span class="metric-label">最近批次</span>
            <strong>{{ shortId(latestJob?.requestBatchId || latestRequirement?.requirementCode) }}</strong>
          </div>
          <div>
            <span class="metric-label">任务状态</span>
            <strong :class="['status', statusTone(latestJob?.jobStatus)]">
              {{ statusText(latestJob?.jobStatus) }}
            </strong>
          </div>
          <div>
            <span class="metric-label">成功 / 失败</span>
            <strong>{{ latestJob?.successCount ?? 0 }} / {{ latestJob?.failedCount ?? 0 }}</strong>
          </div>
        </section>

        <section class="context-strip">
          <div>
            <span>模块</span>
            <strong>{{ selectedModule?.moduleName || '-' }}</strong>
          </div>
          <div>
            <span>场景</span>
            <strong>{{ sceneText(form.sceneType) }}</strong>
          </div>
          <div>
            <span>策略</span>
            <strong>{{ activeStrategy?.strategyCode || activeStrategy?.moduleName || '-' }}</strong>
          </div>
          <div>
            <span>模板</span>
            <strong>{{ selectedTemplate?.templateName || '-' }}</strong>
          </div>
          <div>
            <span>单位角色</span>
            <strong>{{ selectedRequiredOrg?.orgName || '-' }} / {{ selectedRequiredRole?.roleName || '-' }}</strong>
          </div>
          <div>
            <span>生成对象</span>
            <strong>{{ form.prepareScopeMode === 'class' ? `批量 ${participantCount} 人` : '单个学生' }}</strong>
          </div>
        </section>

        <section v-if="preflightResult" class="list-section preflight-section">
          <div class="section-heading">
            <h2>链路自检</h2>
            <span>
              {{ preflightResult.passCount }} 通过 / {{ preflightResult.warnCount }} 警告 / {{ preflightResult.failCount }} 失败
            </span>
          </div>
          <p :class="['preflight-summary', preflightResult.ready ? 'success-text' : 'danger-text']">
            {{ preflightResult.summary }}
          </p>
          <ul class="preflight-list">
            <li v-for="check in preflightResult.checks" :key="check.nodeCode">
              <div>
                <strong>{{ preflightNodeText(check.nodeCode) }}</strong>
                <span>{{ check.message }}</span>
                <small v-if="preflightEvidenceText(check)">{{ preflightEvidenceText(check) }}</small>
              </div>
              <span :class="['badge', preflightStatusTone(check.status)]">
                {{ preflightStatusText(check.status) }}
              </span>
            </li>
          </ul>
          <p v-if="preflightBlockingChecks.length" class="blocking-message">
            优先处理：{{ preflightBlockingChecks.map((item) => preflightNodeText(item.nodeCode)).join('、') }}
          </p>
        </section>

        <section class="list-section">
          <div class="section-heading">
            <h2>最近任务</h2>
            <span>{{ jobs.length }} 条</span>
          </div>
          <ul v-if="jobs.length" class="compact-list">
            <li v-for="job in jobs" :key="job.id">
              <div>
                <strong>{{ shortId(job.requestBatchId) }}</strong>
                <span>{{ formatTime(job.createTime) }}</span>
                <span v-if="job.jobStatus === 'FAILED'" class="job-error">
                  {{ jobFailureMessage(job) }}
                </span>
              </div>
              <span :class="['badge', statusTone(job.jobStatus)]">{{ statusText(job.jobStatus) }}</span>
            </li>
          </ul>
          <p v-else class="empty-text">暂无任务记录</p>
        </section>

        <section class="list-section">
          <div class="section-heading">
            <h2>最近需求</h2>
            <span>{{ requirements.length }} 条</span>
          </div>
          <ul v-if="requirements.length" class="compact-list">
            <li v-for="item in requirements" :key="item.id">
              <div>
                <strong>{{ item.requirementCode }}</strong>
                <span>{{ formatTime(item.createTime) }}</span>
              </div>
              <span :class="['badge', statusTone(item.requirementStatus)]">
                {{ statusText(item.requirementStatus) }}
              </span>
            </li>
          </ul>
          <p v-else class="empty-text">暂无需求记录</p>
        </section>
      </aside>
    </main>
  </section>
</template>

<style scoped>
.data-generate-page {
  min-height: 100%;
  padding: 24px;
  background: #f6f7f9;
  color: #20242c;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  max-width: 1240px;
  margin: 0 auto 20px;
}

.eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 700;
  color: #5b6b84;
  text-transform: uppercase;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  font-size: 28px;
  font-weight: 760;
  line-height: 1.25;
}

h2 {
  font-size: 15px;
  font-weight: 720;
}

.subtitle {
  margin-top: 8px;
  color: #657083;
  font-size: 14px;
}

.workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  gap: 18px;
  max-width: 1240px;
  margin: 0 auto;
}

.generate-panel,
.result-panel {
  background: #ffffff;
  border: 1px solid #dfe4ec;
  border-radius: 8px;
  box-shadow: 0 12px 28px rgba(31, 41, 55, 0.06);
}

.generate-panel {
  padding: 20px;
}

.result-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 18px;
}

.form-section {
  padding-bottom: 18px;
  border-bottom: 1px solid #edf0f5;
}

.form-section + .form-section {
  padding-top: 18px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-heading span {
  color: #6b7280;
  font-size: 12px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-bottom: 14px;
  color: #3f4653;
  font-size: 13px;
  font-weight: 650;
}

input,
select,
textarea {
  width: 100%;
  min-height: 38px;
  border: 1px solid #cfd6e1;
  border-radius: 6px;
  padding: 8px 10px;
  background: #ffffff;
  color: #20242c;
  font: inherit;
  font-weight: 500;
}

textarea {
  resize: vertical;
  line-height: 1.5;
}

input:focus,
select:focus,
textarea:focus {
  outline: 2px solid rgba(35, 99, 235, 0.18);
  border-color: #2363eb;
}

select:disabled {
  background: #f2f4f7;
  color: #737b89;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.advanced-section {
  margin-top: 18px;
  padding: 14px 0 0;
}

.advanced-section summary {
  cursor: pointer;
  color: #334155;
  font-size: 13px;
  font-weight: 720;
}

.advanced-section[open] summary {
  margin-bottom: 14px;
}

.batch-students {
  margin-bottom: 14px;
}

.batch-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #f8fbff;
}

.batch-summary strong {
  color: #1f2937;
  font-size: 13px;
}

.batch-summary span {
  color: #64748b;
  font-size: 12px;
}

.student-filter-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px;
  gap: 10px;
  margin-bottom: 10px;
}

.student-bulk-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  margin: 0 0 10px;
}

.student-table-wrap {
  max-height: 320px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #ffffff;
}

.student-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.student-table th,
.student-table td {
  padding: 9px 10px;
  border-bottom: 1px solid #eef2f7;
  color: #475569;
  font-size: 12px;
  text-align: left;
}

.student-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8fafc;
  color: #334155;
  font-weight: 720;
}

.student-table th:first-child,
.student-table td:first-child {
  width: 58px;
  text-align: center;
}

.student-table td {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.student-table input {
  width: 16px;
  min-height: 16px;
  padding: 0;
}

.blocking-message {
  margin-top: 16px;
  padding: 10px 12px;
  border: 1px solid #f5c2c7;
  border-radius: 6px;
  background: #fff1f2;
  color: #b42318;
  font-size: 13px;
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 18px;
}

button {
  min-height: 38px;
  border: 0;
  border-radius: 6px;
  padding: 0 14px;
  cursor: pointer;
  font: inherit;
  font-weight: 720;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.primary-button {
  background: #2363eb;
  color: #ffffff;
}

.ghost-button {
  border: 1px solid #cfd6e1;
  background: #ffffff;
  color: #334155;
}

.result-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.result-summary > div,
.context-strip > div {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #e4e8ef;
  border-radius: 6px;
  background: #fafbfc;
}

.metric-label,
.context-strip span {
  color: #6b7280;
  font-size: 12px;
}

.result-summary strong,
.context-strip strong {
  overflow: hidden;
  color: #20242c;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.context-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.list-section {
  border-top: 1px solid #edf0f5;
  padding-top: 16px;
}

.compact-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.compact-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f2f6;
}

.compact-list li:last-child {
  border-bottom: 0;
}

.compact-list div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.compact-list strong {
  overflow: hidden;
  color: #20242c;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.compact-list span {
  color: #737b89;
  font-size: 12px;
}

.compact-list .job-error {
  color: #b42318;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preflight-section {
  display: grid;
  gap: 10px;
}

.preflight-summary {
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 700;
}

.success-text {
  background: #ecfdf3;
  color: #067647;
}

.danger-text {
  background: #fff1f2;
  color: #b42318;
}

.preflight-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.preflight-list li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: start;
  border-bottom: 1px solid #f0f2f6;
  padding: 8px 0;
}

.preflight-list li:last-child {
  border-bottom: 0;
}

.preflight-list div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.preflight-list strong {
  color: #20242c;
  font-size: 13px;
}

.preflight-list span,
.preflight-list small {
  color: #737b89;
  font-size: 12px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.badge,
.status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 54px;
  border-radius: 999px;
  padding: 4px 8px;
  font-size: 12px;
  font-weight: 760;
}

.success {
  background: #ecfdf3;
  color: #067647;
}

.danger {
  background: #fff1f2;
  color: #b42318;
}

.warning {
  background: #fff7ed;
  color: #b45309;
}

.neutral {
  background: #eef2f7;
  color: #475569;
}

.empty-text {
  padding: 12px 0;
  color: #737b89;
  font-size: 13px;
}

@media (max-width: 980px) {
  .workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .data-generate-page {
    padding: 16px;
  }

  .page-header,
  .field-grid,
  .result-summary,
  .context-strip,
  .student-filter-bar {
    grid-template-columns: 1fr;
  }

  .page-header {
    display: grid;
  }

  .ghost-button,
  .primary-button {
    width: 100%;
  }
}
</style>
