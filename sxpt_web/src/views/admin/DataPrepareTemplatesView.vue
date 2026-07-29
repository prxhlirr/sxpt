<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import TimedToast from '../../components/ui/TimedToast.vue';
import {
  authApi,
  dataPrepareApi,
  type BusinessModule,
  type ConnectorSystem,
  type TeachingDataTemplate,
  type TeachingDataTemplateRequest
} from '../../services/trainingApi';

const PAGE_SIZE = 10;

const session = authApi.getSession();
const tenantId = ref(session?.user.tenantId || 'demo-tenant');
const operatorId = session?.user.userId || 'admin';
const connectorSystemId = ref('');
const systemKeyword = ref('');
const businessModuleId = ref('');
const sceneType = ref('PRACTICE');
const systems = ref<ConnectorSystem[]>([]);
const modules = ref<BusinessModule[]>([]);
const templates = ref<TeachingDataTemplate[]>([]);
const loading = ref(false);
const currentPage = ref(1);
const dialogMode = ref<'none' | 'detail' | 'create' | 'edit'>('none');
const selectedTemplate = ref<TeachingDataTemplate | null>(null);
const createTemplateCode = ref('');
const notice = ref({
  show: false,
  type: 'info' as 'success' | 'error' | 'info',
  message: ''
});

const templateForm = reactive<TeachingDataTemplateRequest>({
  templateName: '',
  sceneType: 'PRACTICE',
  moduleCode: '',
  teachingPointId: '',
  strategyId: '',
  initState: 'DRAFT',
  supportMode: 'PRACTICE',
  configJson: '{"adapter":"local","mode":"demo"}',
  dataSchemaJson: '',
  mockRuleJson: '',
  readonlyFlag: false,
  requestSchemaJson: '',
  requiredOrgRoleJson: '',
  resultCheckSchemaJson: '',
  sensitiveFieldPolicyJson: '',
  updateBy: operatorId
});

function getDefaultTemplateForm(): TeachingDataTemplateRequest {
  return {
    templateName: '本地联调数据模板',
    sceneType: sceneType.value,
    moduleCode: selectedModule.value?.moduleCode || '',
    teachingPointId: '',
    strategyId: '',
    initState: 'DRAFT',
    supportMode: sceneType.value,
    configJson: '{"adapter":"local","mode":"demo"}',
    dataSchemaJson: '',
    mockRuleJson: '',
    readonlyFlag: false,
    requestSchemaJson: '',
    requiredOrgRoleJson: '{"org":"required","role":"required"}',
    resultCheckSchemaJson: '',
    sensitiveFieldPolicyJson: '',
    updateBy: operatorId
  };
}

const selectedModule = computed(() =>
  modules.value.find((item) => item.id === businessModuleId.value)
);

const filteredSystems = computed(() => {
  const keyword = systemKeyword.value.trim().toLowerCase();
  if (!keyword) return systems.value;
  return systems.value.filter((item) =>
    (item.systemName || '').toLowerCase().includes(keyword)
  );
});

const totalPages = computed(() => Math.max(1, Math.ceil(templates.value.length / PAGE_SIZE)));

const pagedTemplates = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE;
  return templates.value.slice(start, start + PAGE_SIZE);
});

onMounted(initialize);

watch(connectorSystemId, async () => {
  businessModuleId.value = '';
  currentPage.value = 1;
  await loadModules();
  await loadTemplates();
  if (dialogMode.value === 'create') {
    templateForm.moduleCode = selectedModule.value?.moduleCode || '';
  }
});

watch([businessModuleId, sceneType], async () => {
  currentPage.value = 1;
  await loadTemplates();
  if (dialogMode.value === 'create') {
    templateForm.moduleCode = selectedModule.value?.moduleCode || '';
  }
});

/**
 * 业务功能：初始化模板管理上下文，让模板绑定到平台、模块和教学场景。
 * 关键流程：加载平台后自动进入第一个模块，减少管理员首次维护时的无效选择。
 */
async function initialize() {
  await run(async () => {
    systems.value = await dataPrepareApi.listConnectorSystems(tenantId.value);
    connectorSystemId.value = systems.value[0]?.id || '';
    await loadModules();
    await loadTemplates();
  }, '模板列表已加载');
}

/**
 * 业务功能：读取当前平台下业务模块，为模板维护提供模块归属。
 * 关键流程：平台切换后重置模块选择，避免模板被误绑定到旧模块。
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
 * 业务功能：读取当前模块和场景下的数据模板，供策略选择和批次创建使用。
 * 关键流程：只有平台和模块都明确时才查询，保证模板列表不跨上下文展示。
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
  normalizePage();
}

/**
 * 业务功能：创建本地联调模板，用于定义当前模块在指定场景下要生成什么数据。
 * 关键流程：先固定模块快照，再发起创建请求，避免异步期间选择变化导致模板错绑。
 */
function openCreateTemplateDialog() {
  createTemplateCode.value = `LOCAL_TEMPLATE_${Date.now()}`;
  fillTemplateForm(getDefaultTemplateForm());
  dialogMode.value = 'create';
}

async function createLocalTemplate() {
  const module = selectedModule.value;
  if (!connectorSystemId.value || !module) {
    notify('error', '请先选择原平台和业务模块');
    return;
  }
  if (!createTemplateCode.value.trim() || !templateForm.templateName.trim()) {
    notify('error', '请填写模板编码和模板名称');
    return;
  }
  await run(async () => {
    sceneType.value = templateForm.sceneType;
    const created = await dataPrepareApi.createTemplate({
      tenantId: tenantId.value,
      connectorSystemId: connectorSystemId.value,
      templateCode: createTemplateCode.value.trim(),
      templateName: templateForm.templateName.trim(),
      sceneType: templateForm.sceneType,
      moduleCode: module.moduleCode,
      teachingPointId: templateForm.teachingPointId?.trim(),
      strategyId: templateForm.strategyId?.trim(),
      initState: templateForm.initState?.trim(),
      supportMode: templateForm.supportMode?.trim(),
      configJson: templateForm.configJson?.trim()
    });
    templates.value = [created, ...templates.value];
    currentPage.value = 1;
    closeDialog();
  }, '已创建本地联调模板');
}

/**
 * 业务功能：打开模板详情弹窗，展示后端真实模板配置。
 * 关键流程：按 ID 重新查询详情，避免列表字段缺失导致配置证据不完整。
 */
async function showTemplateDetail(template: TeachingDataTemplate) {
  await run(async () => {
    selectedTemplate.value = await dataPrepareApi.getTemplate(template.id);
    dialogMode.value = 'detail';
  }, '模板详情已加载');
}

/**
 * 业务功能：打开模板编辑弹窗，允许管理员维护数据结构和组织角色要求。
 * 关键流程：先读取详情再回填，避免编辑时丢失 JSON 配置。
 */
async function editTemplate(template: TeachingDataTemplate) {
  await run(async () => {
    const detail = await dataPrepareApi.getTemplate(template.id);
    selectedTemplate.value = detail;
    fillTemplateForm(detail);
    dialogMode.value = 'edit';
  }, '模板编辑信息已加载');
}

/**
 * 业务功能：保存模板编辑结果。
 * 关键流程：提交后端允许编辑的字段，并用返回结果替换列表当前行。
 */
async function updateTemplate() {
  if (!selectedTemplate.value) return;
  if (!templateForm.templateName.trim() || !templateForm.moduleCode.trim()) {
    notify('error', '请填写模板名称和模块编码');
    return;
  }
  await run(async () => {
    const updated = await dataPrepareApi.updateTemplate(
      selectedTemplate.value!.id,
      buildTemplateUpdateRequest()
    );
    templates.value = templates.value.map((item) => (item.id === updated.id ? updated : item));
    selectedTemplate.value = updated;
    closeDialog();
  }, '模板已更新');
}

/**
 * 业务功能：启用或停用数据模板。
 * 关键流程：调用后端真实状态接口，再同步替换当前行，避免只改前端状态。
 */
async function toggleTemplateStatus(template: TeachingDataTemplate) {
  await run(async () => {
    const updated =
      template.status === 'ACTIVE'
        ? await dataPrepareApi.disableTemplate(template.id)
        : await dataPrepareApi.enableTemplate(template.id);
    templates.value = templates.value.map((item) => (item.id === updated.id ? updated : item));
  }, template.status === 'ACTIVE' ? '模板已停用' : '模板已启用');
}

/**
 * 业务功能：将模板详情转换为编辑表单。
 * 关键流程：保留所有 JSON 配置字段，避免只编辑名称时破坏模板规则。
 */
function fillTemplateForm(template: TeachingDataTemplate | TeachingDataTemplateRequest) {
  templateForm.templateName = template.templateName || '';
  templateForm.sceneType = template.sceneType || sceneType.value;
  templateForm.moduleCode = template.moduleCode || selectedModule.value?.moduleCode || '';
  templateForm.teachingPointId = template.teachingPointId || '';
  templateForm.strategyId = template.strategyId || '';
  templateForm.initState = template.initState || '';
  templateForm.supportMode = template.supportMode || '';
  templateForm.configJson = template.configJson || '';
  templateForm.dataSchemaJson = template.dataSchemaJson || '';
  templateForm.mockRuleJson = template.mockRuleJson || '';
  templateForm.readonlyFlag = template.readonlyFlag ?? false;
  templateForm.requestSchemaJson = template.requestSchemaJson || '';
  templateForm.requiredOrgRoleJson = template.requiredOrgRoleJson || '';
  templateForm.resultCheckSchemaJson = template.resultCheckSchemaJson || '';
  templateForm.sensitiveFieldPolicyJson = template.sensitiveFieldPolicyJson || '';
  templateForm.updateBy = operatorId;
}

/**
 * 业务功能：构造模板更新请求。
 * 关键流程：显式携带全部可编辑字段，使后端合并逻辑可预测。
 */
function buildTemplateUpdateRequest(): TeachingDataTemplateRequest {
  return {
    templateName: templateForm.templateName.trim(),
    sceneType: templateForm.sceneType,
    moduleCode: templateForm.moduleCode.trim(),
    teachingPointId: templateForm.teachingPointId?.trim(),
    strategyId: templateForm.strategyId?.trim(),
    initState: templateForm.initState?.trim(),
    supportMode: templateForm.supportMode?.trim(),
    configJson: templateForm.configJson?.trim(),
    dataSchemaJson: templateForm.dataSchemaJson?.trim(),
    mockRuleJson: templateForm.mockRuleJson?.trim(),
    readonlyFlag: templateForm.readonlyFlag,
    requestSchemaJson: templateForm.requestSchemaJson?.trim(),
    requiredOrgRoleJson: templateForm.requiredOrgRoleJson?.trim(),
    resultCheckSchemaJson: templateForm.resultCheckSchemaJson?.trim(),
    sensitiveFieldPolicyJson: templateForm.sensitiveFieldPolicyJson?.trim(),
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

function closeDialog() {
  dialogMode.value = 'none';
  selectedTemplate.value = null;
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
  <section class="config-page template-page">
    <TimedToast
      :show="notice.show"
      :type="notice.type"
      :message="notice.message"
      @close="closeNotice"
    />

    <header class="page-heading">
      <div>
        <p>数据准备 / 模板管理</p>
        <h1>系统模块数据模板</h1>
        <p>维护每个业务模块在不同教学场景下需要生成的数据结构、初始状态和单位角色要求。</p>
      </div>
      <button type="button" :disabled="loading || systems.length === 0" @click="openCreateTemplateDialog">
        创建本地联调模板
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
    </section>

    <section class="metrics">
      <article>
        <span>当前模板</span>
        <strong>{{ templates.length }}</strong>
      </article>
      <article>
        <span>教学场景</span>
        <strong>{{ sceneText(sceneType) }}</strong>
      </article>
      <article>
        <span>可选模块</span>
        <strong>{{ modules.length }}</strong>
      </article>
    </section>

    <section class="panel">
      <header class="panel-header">
        <h2>模板列表</h2>
        <span class="helper-text">共 {{ templates.length }} 条，每页 {{ PAGE_SIZE }} 条</span>
      </header>
      <div class="panel-body table-wrap">
        <table>
          <thead>
            <tr>
              <th>模板名称</th>
              <th>模板编码</th>
              <th>模块编码</th>
              <th>场景</th>
              <th>初始状态</th>
              <th>支持模式</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="template in pagedTemplates" :key="template.id">
              <td>{{ template.templateName }}</td>
              <td>{{ template.templateCode }}</td>
              <td>{{ template.moduleCode || '-' }}</td>
              <td>{{ sceneText(template.sceneType) }}</td>
              <td>{{ template.initState || '-' }}</td>
              <td>{{ template.supportMode || '-' }}</td>
              <td>
                <span class="status-badge" :class="statusClass(template.status)">
                  {{ template.status || '未设置' }}
                </span>
              </td>
              <td>
                <div class="row-actions">
                  <button type="button" @click="showTemplateDetail(template)">详情</button>
                  <button type="button" @click="editTemplate(template)">编辑</button>
                  <button type="button" :disabled="loading" @click="toggleTemplateStatus(template)">
                    {{ template.status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="templates.length === 0" class="empty-state">
          <strong>暂无模板</strong>
          <p>模板缺失时，老师无法为该业务模块创建有效的数据准备批次。</p>
        </div>
      </div>
      <footer v-if="templates.length > PAGE_SIZE" class="pagination-bar">
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
            <p>模板 {{ dialogMode === 'detail' ? '详情' : dialogMode === 'create' ? '创建' : '编辑' }}</p>
            <h2>{{ dialogMode === 'create' ? '创建本地联调模板' : selectedTemplate?.templateName || selectedTemplate?.id }}</h2>
          </div>
          <button type="button" class="icon-button" @click="closeDialog">×</button>
        </header>

        <div v-if="dialogMode === 'detail' && selectedTemplate" class="detail-grid">
          <span>模板编码</span><strong>{{ selectedTemplate.templateCode }}</strong>
          <span>模块编码</span><strong>{{ selectedTemplate.moduleCode || '-' }}</strong>
          <span>场景</span><strong>{{ sceneText(selectedTemplate.sceneType) }}</strong>
          <span>初始状态</span><strong>{{ selectedTemplate.initState || '-' }}</strong>
          <span>支持模式</span><strong>{{ selectedTemplate.supportMode || '-' }}</strong>
          <span>只读模板</span><strong>{{ selectedTemplate.readonlyFlag ? '是' : '否' }}</strong>
          <span>状态</span><strong>{{ selectedTemplate.status || '-' }}</strong>
          <span>更新时间</span><strong>{{ selectedTemplate.updateTime || '-' }}</strong>
          <span>模板配置</span><strong>{{ selectedTemplate.configJson || '-' }}</strong>
          <span>数据结构</span><strong>{{ selectedTemplate.dataSchemaJson || '-' }}</strong>
          <span>单位角色要求</span><strong>{{ selectedTemplate.requiredOrgRoleJson || '-' }}</strong>
          <span>结果校验</span><strong>{{ selectedTemplate.resultCheckSchemaJson || '-' }}</strong>
        </div>

        <form
          v-else
          class="edit-form"
          @submit.prevent="dialogMode === 'create' ? createLocalTemplate() : updateTemplate()"
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
              @change="templateForm.moduleCode = selectedModule?.moduleCode || ''"
            >
              <option value="">请选择业务模块</option>
              <option v-for="module in modules" :key="module.id" :value="module.id">
                {{ module.moduleName }} / {{ module.moduleCode }}
              </option>
            </select>
          </label>
          <label v-if="dialogMode === 'create'">
            <span>模板编码</span>
            <input v-model="createTemplateCode" required />
          </label>
          <label>
            <span>模板名称</span>
            <input v-model="templateForm.templateName" required />
          </label>
          <label>
            <span>模块编码</span>
            <input v-model="templateForm.moduleCode" required readonly />
          </label>
          <label>
            <span>教学场景</span>
            <select v-model="templateForm.sceneType">
              <option value="RECORD">备课</option>
              <option value="LEARN">学习</option>
              <option value="PRACTICE">练习</option>
              <option value="EXAM">考试</option>
            </select>
          </label>
          <label>
            <span>初始状态</span>
            <input v-model="templateForm.initState" placeholder="如 DRAFT" />
          </label>
          <label>
            <span>支持模式</span>
            <input v-model="templateForm.supportMode" placeholder="如 PRACTICE,EXAM" />
          </label>
          <label>
            <span>策略 ID</span>
            <input v-model="templateForm.strategyId" />
          </label>
          <label class="check-line">
            <input v-model="templateForm.readonlyFlag" type="checkbox" />
            <span>模板只读</span>
          </label>
          <label class="wide">
            <span>模板配置 JSON</span>
            <textarea v-model="templateForm.configJson" rows="3" />
          </label>
          <label class="wide">
            <span>数据结构 JSON</span>
            <textarea v-model="templateForm.dataSchemaJson" rows="3" />
          </label>
          <label class="wide">
            <span>模拟规则 JSON</span>
            <textarea v-model="templateForm.mockRuleJson" rows="3" />
          </label>
          <label class="wide">
            <span>请求结构 JSON</span>
            <textarea v-model="templateForm.requestSchemaJson" rows="3" />
          </label>
          <label class="wide">
            <span>单位角色要求 JSON</span>
            <textarea v-model="templateForm.requiredOrgRoleJson" rows="3" />
          </label>
          <label class="wide">
            <span>结果校验 JSON</span>
            <textarea v-model="templateForm.resultCheckSchemaJson" rows="3" />
          </label>
          <label class="wide">
            <span>敏感字段策略 JSON</span>
            <textarea v-model="templateForm.sensitiveFieldPolicyJson" rows="3" />
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
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
  width: min(900px, 100%);
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

@media (max-width: 900px) {
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
