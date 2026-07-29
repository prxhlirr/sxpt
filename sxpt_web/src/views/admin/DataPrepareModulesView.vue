<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import TimedToast from '../../components/ui/TimedToast.vue';
import {
  authApi,
  dataPrepareApi,
  type BusinessModule,
  type BusinessModuleRequest,
  type ConnectorSystem
} from '../../services/trainingApi';

type ModuleDialogMode = 'none' | 'detail' | 'create' | 'edit';

const PAGE_SIZE = 10;

const session = authApi.getSession();
const tenantId = ref(session?.user.tenantId || 'demo-tenant');
const connectorSystemId = ref('');
const systemKeyword = ref('');
const systems = ref<ConnectorSystem[]>([]);
const modules = ref<BusinessModule[]>([]);
const selectedModule = ref<BusinessModule | null>(null);
const dialogMode = ref<ModuleDialogMode>('none');
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
  modules.value = await dataPrepareApi.listBusinessModules({
    tenantId: tenantId.value,
    connectorSystemId: connectorSystemId.value
  });
  normalizePage();
}

/**
 * 业务功能：创建本地联调业务模块。
 * 关键流程：必须先选择平台，创建后立即插入当前列表，便于继续维护模板和策略。
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
    closeDialog();
  }, '已创建本地联调业务模块');
}

/**
 * 业务功能：打开业务模块详情弹窗。
 * 关键流程：从后端读取最新详情，避免列表缓存导致展示信息过期。
 */
async function showModuleDetail(module: BusinessModule) {
  await run(async () => {
    selectedModule.value = await dataPrepareApi.getBusinessModule(module.id);
    dialogMode.value = 'detail';
  }, '模块详情已加载');
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
  await run(async () => {
    const updated =
      module.status === 'ACTIVE'
        ? await dataPrepareApi.disableBusinessModule(module.id)
        : await dataPrepareApi.enableBusinessModule(module.id);
    modules.value = modules.value.map((item) => (item.id === updated.id ? updated : item));
  }, module.status === 'ACTIVE' ? '业务模块已停用' : '业务模块已启用');
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

function closeDialog() {
  selectedModule.value = null;
  dialogMode.value = 'none';
  closeNotice();
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
      <section class="edit-dialog" role="dialog" aria-modal="true" aria-labelledby="module-dialog-title">
        <header class="dialog-header">
          <div>
            <p>业务模块</p>
            <h2 id="module-dialog-title">
              {{
                dialogMode === 'create'
                  ? '创建本地联调模块'
                  : dialogMode === 'edit'
                    ? '编辑业务模块'
                    : '业务模块详情'
              }}
            </h2>
          </div>
          <button type="button" class="icon-close" aria-label="关闭业务模块弹窗" @click="closeDialog">×</button>
        </header>

        <div v-if="dialogMode === 'detail' && selectedModule" class="detail-grid">
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

.icon-close {
  width: 40px;
  height: 40px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  font-size: 24px;
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
  .edit-form {
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
