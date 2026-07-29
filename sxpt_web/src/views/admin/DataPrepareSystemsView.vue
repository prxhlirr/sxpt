<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import TimedToast from '../../components/ui/TimedToast.vue';
import {
  authApi,
  dataPrepareApi,
  type ConnectorSystem,
  type CreateConnectorSystemRequest,
  type UpdateConnectorSystemRequest
} from '../../services/trainingApi';

type ConnectorSystemForm = Omit<CreateConnectorSystemRequest, 'tenantId' | 'configJson'> & {
  configJson: string;
};

type PlatformDialogMode = 'none' | 'create' | 'detail' | 'edit';

const PAGE_SIZE = 10;

const session = authApi.getSession();
const tenantId = ref(session?.user.tenantId || 'demo-tenant');
const systems = ref<ConnectorSystem[]>([]);
const loading = ref(false);
const systemKeyword = ref('');
const currentPage = ref(1);
const dialogMode = ref<PlatformDialogMode>('none');
const selectedSystem = ref<ConnectorSystem | null>(null);
const notice = reactive({
  show: false,
  type: 'info' as 'success' | 'error' | 'info',
  message: ''
});

const form = reactive<ConnectorSystemForm>({
  systemName: '',
  systemCode: '',
  systemType: 'LOCAL_DEV',
  baseUrl: '',
  authType: 'NONE',
  configJson: ''
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

onMounted(loadSystems);

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
  form.systemType = 'LOCAL_DEV';
  form.baseUrl = 'http://127.0.0.1:8080/local-origin';
  form.authType = 'NONE';
  form.configJson = '{"adapter":"local"}';
  notify('info', '已填充本地联调默认值，确认无误后再保存');
}

/**
 * 业务功能：打开平台详情弹窗。
 * 关键流程：从后端拉取最新详情，避免列表缓存导致展示信息过期。
 */
async function showSystemDetail(system: ConnectorSystem) {
  await run(async () => {
    selectedSystem.value = await dataPrepareApi.getConnectorSystem(system.id);
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
    form.systemName = detail.systemName || '';
    form.systemCode = detail.systemCode || '';
    form.systemType = detail.systemType || 'LOCAL_DEV';
    form.baseUrl = detail.baseUrl || '';
    form.authType = detail.authType || 'NONE';
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
  dialogMode.value = 'none';
  closeNotice();
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
  return '';
}

function validateEditForm() {
  if (!form.systemName.trim()) return '请填写平台名称';
  if (!form.baseUrl.trim()) return '请填写平台地址';
  return '';
}

function resetForm() {
  form.systemName = '';
  form.systemCode = '';
  form.systemType = 'LOCAL_DEV';
  form.baseUrl = '';
  form.authType = 'NONE';
  form.configJson = '';
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

        <div v-if="dialogMode === 'detail' && selectedSystem" class="detail-grid">
          <span>系统名称</span><strong>{{ selectedSystem.systemName }}</strong>
          <span>系统编码</span><strong>{{ selectedSystem.systemCode }}</strong>
          <span>类型</span><strong>{{ selectedSystem.systemType }}</strong>
          <span>认证</span><strong>{{ selectedSystem.authType }}</strong>
          <span>地址</span><strong>{{ selectedSystem.baseUrl }}</strong>
          <span>状态</span><strong>{{ selectedSystem.status || '未设置' }}</strong>
          <span>创建时间</span><strong>{{ selectedSystem.createTime || '-' }}</strong>
          <span>更新时间</span><strong>{{ selectedSystem.updateTime || '-' }}</strong>
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
              <option value="NONE">无需认证</option>
              <option value="TOKEN">Token</option>
              <option value="SSO">单点登录</option>
              <option value="COOKIE">Cookie</option>
            </select>
          </label>
          <label class="field-wide">
            <span>平台地址 <em>*</em></span>
            <input v-model="form.baseUrl" type="text" placeholder="例如：http://127.0.0.1:8080/local-origin" />
          </label>
          <label class="field-wide">
            <span>扩展配置</span>
            <textarea v-model="form.configJson" rows="4" placeholder='例如：{"adapter":"local"}'></textarea>
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
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 10px 14px;
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

@media (max-width: 720px) {
  .page-action {
    width: 100%;
  }

  .query-panel,
  .create-form,
  .detail-grid {
    grid-template-columns: 1fr;
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
