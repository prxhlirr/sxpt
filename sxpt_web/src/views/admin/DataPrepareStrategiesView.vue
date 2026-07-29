<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import TimedToast from '../../components/ui/TimedToast.vue';
import {
  authApi,
  dataPrepareApi,
  type BusinessModule,
  type ConnectorSystem,
  type ModuleDataStrategy,
  type ModuleDataStrategyRequest,
  type TeachingDataTemplate
} from '../../services/trainingApi';

const PAGE_SIZE = 10;

const session = authApi.getSession();
const tenantId = ref(session?.user.tenantId || 'demo-tenant');
const operatorId = session?.user.userId || 'admin';
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
  dataSourceStrategy: 'CREATE',
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

function getDefaultStrategyForm(): ModuleDataStrategyRequest {
  return {
    strategyCode: `LOCAL_STRATEGY_${Date.now()}`,
    moduleName: selectedModule.value?.moduleName || '',
    templateId: templateId.value,
    needPreData: true,
    dataSourceStrategy: 'CREATE',
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

/**
 * 业务功能：初始化策略管理页面的上下文数据，确保策略始终挂在真实平台、模块和模板之下。
 * 关键流程：按平台、模块、场景逐层加载，避免用户编辑策略时引用已经失效的业务边界。
 */
async function initialize() {
  await run(async () => {
    systems.value = await dataPrepareApi.listConnectorSystems(tenantId.value);
    connectorSystemId.value = systems.value[0]?.id || '';
    await loadModules();
    await loadTemplates();
    await loadStrategies();
  }, '策略列表已加载');
}

/**
 * 业务功能：读取当前平台下的业务模块，作为策略归属的第一层约束。
 * 关键流程：平台切换后清空旧模块和旧模板，防止策略编辑错绑旧上下文。
 */
async function loadModules() {
  modules.value = [];
  if (!connectorSystemId.value) return;
  modules.value = await dataPrepareApi.listBusinessModules({
    tenantId: tenantId.value,
    connectorSystemId: connectorSystemId.value
  });
  businessModuleId.value = modules.value[0]?.id || '';
}

/**
 * 业务功能：读取当前模块和场景下可绑定的数据模板，保证启用策略时具备造数结构。
 * 关键流程：策略启用依赖模板，因此没有模板时只允许查看和编辑已有策略，不建议新增策略。
 */
async function loadTemplates() {
  templates.value = [];
  if (!connectorSystemId.value || !selectedModule.value) return;
  templates.value = await dataPrepareApi.listTemplates({
    tenantId: tenantId.value,
    connectorSystemId: connectorSystemId.value,
    moduleCode: selectedModule.value.moduleCode,
    sceneType: sceneType.value
  });
  templateId.value = templates.value[0]?.id || '';
}

/**
 * 业务功能：读取当前模块下的策略列表，供管理员核对老师可消费的准备规则。
 * 关键流程：后端接口返回启用策略，前端再按当前页面上下文展示关键字段和操作入口。
 */
async function loadStrategies() {
  strategies.value = [];
  if (!connectorSystemId.value || !businessModuleId.value) return;
  strategies.value = await dataPrepareApi.listActiveStrategies({
    tenantId: tenantId.value,
    connectorSystemId: connectorSystemId.value,
    businessModuleId: businessModuleId.value
  });
  normalizePage();
}

/**
 * 业务功能：创建并启用本地联调策略，让老师创建批次时可以直接消费已验证规则。
 * 关键流程：创建前固定当前模块和模板快照，避免异步过程中选择变化造成错配。
 */
function openCreateStrategyDialog() {
  fillStrategyForm(getDefaultStrategyForm());
  dialogMode.value = 'create';
}

async function createLocalStrategy() {
  const module = selectedModule.value;
  if (!connectorSystemId.value || !module || !strategyForm.templateId) {
    notify('error', '请先选择原平台、业务模块和数据模板');
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
    const enabled = await dataPrepareApi.enableModuleDataStrategy(created.id);
    strategies.value = [enabled, ...strategies.value];
    currentPage.value = 1;
    closeDialog();
  }, '已创建并启用本地联调策略');
}

/**
 * 业务功能：打开策略详情弹窗，展示后端真实详情而不是列表缓存。
 * 关键流程：按 ID 重新查询详情，确保版本、模板和策略 JSON 字段与数据库一致。
 */
async function showStrategyDetail(strategy: ModuleDataStrategy) {
  await run(async () => {
    selectedStrategy.value = await dataPrepareApi.getModuleDataStrategy(strategy.id);
    dialogMode.value = 'detail';
  }, '策略详情已加载');
}

/**
 * 业务功能：打开策略编辑弹窗，允许管理员维护影响学生实例分配的核心规则。
 * 关键流程：先读取详情再回填表单，避免用列表简略字段覆盖完整策略配置。
 */
async function editStrategy(strategy: ModuleDataStrategy) {
  await run(async () => {
    const detail = await dataPrepareApi.getModuleDataStrategy(strategy.id);
    selectedStrategy.value = detail;
    fillStrategyForm(detail);
    dialogMode.value = 'edit';
  }, '策略编辑信息已加载');
}

/**
 * 业务功能：提交策略编辑，更新生成、共享、锁定、模板和组织角色策略。
 * 关键流程：只提交后端允许更新的策略字段，并在本地替换对应列表行。
 */
async function updateStrategy() {
  if (!selectedStrategy.value) return;
  if (!strategyForm.moduleName?.trim() || !strategyForm.templateId?.trim()) {
    notify('error', '请填写模块名称并选择模板');
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
  }, '策略已更新');
}

/**
 * 业务功能：启用或停用模块数据策略。
 * 关键流程：启用由后端校验模板、模块和平台能力，前端只负责同步当前行状态。
 */
async function toggleStrategyStatus(strategy: ModuleDataStrategy) {
  await run(async () => {
    const updated =
      strategy.status === 'ACTIVE'
        ? await dataPrepareApi.disableModuleDataStrategy(strategy.id)
        : await dataPrepareApi.enableModuleDataStrategy(strategy.id);
    strategies.value = strategies.value.map((item) => (item.id === updated.id ? updated : item));
  }, strategy.status === 'ACTIVE' ? '策略已停用' : '策略已启用');
}

/**
 * 业务功能：将策略详情转换为表单字段。
 * 关键流程：保留后端已有 JSON 配置，避免编辑非 JSON 字段时丢失组织角色、校验和归档策略。
 */
function fillStrategyForm(strategy: ModuleDataStrategy | ModuleDataStrategyRequest) {
  strategyForm.strategyCode = strategy.strategyCode || '';
  strategyForm.moduleName = strategy.moduleName || '';
  strategyForm.templateId = strategy.templateId || '';
  strategyForm.needPreData = strategy.needPreData ?? true;
  strategyForm.dataSourceStrategy = strategy.dataSourceStrategy || 'CREATE';
  strategyForm.initExternalStatus = strategy.initExternalStatus || '';
  strategyForm.targetExternalStatus = strategy.targetExternalStatus || '';
  strategyForm.defaultOrgRolePolicyJson = strategy.defaultOrgRolePolicyJson || '';
  strategyForm.sharePolicy = strategy.sharePolicy || 'ATTEMPT_EXCLUSIVE';
  strategyForm.regeneratePolicy = strategy.regeneratePolicy || 'ON_ATTEMPT';
  strategyForm.lockPolicy = strategy.lockPolicy || 'NONE';
  strategyForm.prepareTiming = strategy.prepareTiming || 'ON_DEMAND';
  strategyForm.poolSizePolicyJson = strategy.poolSizePolicyJson || '';
  strategyForm.validationPolicyJson = strategy.validationPolicyJson || '';
  strategyForm.expirePolicyJson = strategy.expirePolicyJson || '';
  strategyForm.resultCheckPolicyJson = strategy.resultCheckPolicyJson || '';
  strategyForm.archivePolicyJson = strategy.archivePolicyJson || '';
  strategyForm.updateBy = operatorId;
}

/**
 * 业务功能：构造策略更新请求。
 * 关键流程：后端更新接口按 null 覆盖字段，因此前端显式携带表单中的全部可编辑字段。
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
    notify('error', err instanceof Error ? err.message : '操作失败');
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

function sceneText(value: string) {
  const map: Record<string, string> = {
    RECORD: '备课',
    LEARN: '学习',
    PRACTICE: '练习',
    EXAM: '考试'
  };
  return map[value] || value;
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
        <p>数据准备 / 策略管理</p>
        <h1>模块数据准备策略</h1>
        <p>定义模块在备课、学习、练习和考试中的造数、共享、锁定与分配规则。</p>
      </div>
      <button type="button" :disabled="loading || systems.length === 0" @click="openCreateStrategyDialog">
        创建并启用策略
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
          <option value="RECORD">备课</option>
          <option value="LEARN">学习</option>
          <option value="PRACTICE">练习</option>
          <option value="EXAM">考试</option>
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
        <span>启用策略</span>
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
              <th>共享</th>
              <th>重生成</th>
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
              <td>{{ strategy.dataSourceStrategy }}</td>
              <td>{{ strategy.sharePolicy }}</td>
              <td>{{ strategy.regeneratePolicy }}</td>
              <td>{{ strategy.lockPolicy }}</td>
              <td>{{ strategy.prepareTiming || '-' }}</td>
              <td>
                <span class="status-badge" :class="statusClass(strategy.status)">
                  {{ strategy.status || '未设置' }}
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
          <strong>暂无启用策略</strong>
          <p>缺少策略时，老师无法为该模块和场景创建数据准备批次。</p>
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
            <p>策略 {{ dialogMode === 'detail' ? '详情' : dialogMode === 'create' ? '创建' : '编辑' }}</p>
            <h2>{{ dialogMode === 'create' ? '创建并启用策略' : selectedStrategy?.strategyCode || selectedStrategy?.id }}</h2>
          </div>
          <button type="button" class="icon-button" @click="closeDialog">×</button>
        </header>

        <div v-if="dialogMode === 'detail' && selectedStrategy" class="detail-grid">
          <span>模块</span><strong>{{ selectedStrategy.moduleName }}</strong>
          <span>模块编码</span><strong>{{ selectedStrategy.moduleCode }}</strong>
          <span>场景</span><strong>{{ sceneText(selectedStrategy.sceneType) }}</strong>
          <span>模板 ID</span><strong>{{ selectedStrategy.templateId || '-' }}</strong>
          <span>数据来源</span><strong>{{ selectedStrategy.dataSourceStrategy }}</strong>
          <span>共享策略</span><strong>{{ selectedStrategy.sharePolicy }}</strong>
          <span>重生成策略</span><strong>{{ selectedStrategy.regeneratePolicy }}</strong>
          <span>锁定策略</span><strong>{{ selectedStrategy.lockPolicy }}</strong>
          <span>准备时机</span><strong>{{ selectedStrategy.prepareTiming || '-' }}</strong>
          <span>需要预生成</span><strong>{{ selectedStrategy.needPreData ? '是' : '否' }}</strong>
          <span>策略版本</span><strong>{{ selectedStrategy.strategyVersion || 0 }}</strong>
          <span>状态</span><strong>{{ selectedStrategy.status || '-' }}</strong>
          <span>组织角色策略</span><strong>{{ selectedStrategy.defaultOrgRolePolicyJson || '-' }}</strong>
          <span>校验策略</span><strong>{{ selectedStrategy.validationPolicyJson || '-' }}</strong>
          <span>结果校验策略</span><strong>{{ selectedStrategy.resultCheckPolicyJson || '-' }}</strong>
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
              <option value="RECORD">备课</option>
              <option value="LEARN">学习</option>
              <option value="PRACTICE">练习</option>
              <option value="EXAM">考试</option>
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
              <option value="CREATE">CREATE</option>
              <option value="MOCK_GENERATE">MOCK_GENERATE</option>
              <option value="QUERY_EXISTING">QUERY_EXISTING</option>
            </select>
          </label>
          <label>
            <span>初始状态</span>
            <input v-model="strategyForm.initExternalStatus" placeholder="如 DRAFT" />
          </label>
          <label>
            <span>目标状态</span>
            <input v-model="strategyForm.targetExternalStatus" placeholder="如 SUBMITTED" />
          </label>
          <label>
            <span>共享策略</span>
            <select v-model="strategyForm.sharePolicy">
              <option value="ATTEMPT_EXCLUSIVE">ATTEMPT_EXCLUSIVE</option>
              <option value="QUESTION_EXCLUSIVE">QUESTION_EXCLUSIVE</option>
              <option value="STUDENT_EXCLUSIVE">STUDENT_EXCLUSIVE</option>
              <option value="SHARED_READONLY">SHARED_READONLY</option>
            </select>
          </label>
          <label>
            <span>重生成策略</span>
            <select v-model="strategyForm.regeneratePolicy">
              <option value="ON_ATTEMPT">ON_ATTEMPT</option>
              <option value="ON_FAILURE">ON_FAILURE</option>
              <option value="NEVER">NEVER</option>
            </select>
          </label>
          <label>
            <span>锁定策略</span>
            <select v-model="strategyForm.lockPolicy">
              <option value="NONE">NONE</option>
              <option value="ON_ALLOCATE">ON_ALLOCATE</option>
              <option value="ON_EXAM_START">ON_EXAM_START</option>
            </select>
          </label>
          <label>
            <span>准备时机</span>
            <select v-model="strategyForm.prepareTiming">
              <option value="ON_DEMAND">ON_DEMAND</option>
              <option value="ON_PUBLISH">ON_PUBLISH</option>
              <option value="BEFORE_START">BEFORE_START</option>
            </select>
          </label>
          <label class="check-line">
            <input v-model="strategyForm.needPreData" type="checkbox" />
            <span>进入练习或考试前需要预生成数据</span>
          </label>
          <label class="wide">
            <span>组织角色策略 JSON</span>
            <textarea v-model="strategyForm.defaultOrgRolePolicyJson" rows="3" />
          </label>
          <label class="wide">
            <span>池容量策略 JSON</span>
            <textarea v-model="strategyForm.poolSizePolicyJson" rows="3" />
          </label>
          <label class="wide">
            <span>校验策略 JSON</span>
            <textarea v-model="strategyForm.validationPolicyJson" rows="3" />
          </label>
          <label class="wide">
            <span>过期策略 JSON</span>
            <textarea v-model="strategyForm.expirePolicyJson" rows="3" />
          </label>
          <label class="wide">
            <span>结果校验策略 JSON</span>
            <textarea v-model="strategyForm.resultCheckPolicyJson" rows="3" />
          </label>
          <label class="wide">
            <span>归档策略 JSON</span>
            <textarea v-model="strategyForm.archivePolicyJson" rows="3" />
          </label>
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
  .edit-form {
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
