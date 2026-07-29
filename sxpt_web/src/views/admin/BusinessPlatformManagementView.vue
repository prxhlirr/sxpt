<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import PageHeader from '../../components/ui/PageHeader.vue';
import type {
  BusinessPlatform,
  BusinessPlatformModule
} from '../../domain/models';
import { useTrainingStore } from '../../stores/trainingStore';

const store = useTrainingStore();
const editorMode = ref<'platform' | 'module'>('platform');
const selectedPlatformId = ref('');
const selectedModuleId = ref('');
const expandedPlatformIds = ref(
  store.state.businessPlatforms.map((platform) => platform.id)
);
const feedback = ref('');
const feedbackTone = ref<'success' | 'danger'>('success');

const platformForm = reactive({
  code: '',
  name: '',
  baseUrl: '',
  description: '',
  status: 'ENABLED' as BusinessPlatform['status']
});
const moduleForm = reactive({
  code: '',
  name: '',
  path: '',
  description: '',
  status: 'ENABLED' as BusinessPlatformModule['status']
});

const selectedPlatform = computed(() =>
  store.getBusinessPlatform(selectedPlatformId.value)
);
const selectedModule = computed(() =>
  store.getBusinessPlatformModule(
    selectedPlatformId.value,
    selectedModuleId.value
  )
);
const enabledCount = computed(
  () =>
    store.state.businessPlatforms.filter(
      (platform) => platform.status === 'ENABLED'
    ).length
);
const moduleCount = computed(() =>
  store.state.businessPlatforms.reduce(
    (total, platform) => total + platform.modules.length,
    0
  )
);
const referencedCount = (platformId: string) =>
  store.state.lessons.filter(
    (lesson) => lesson.businessPlatformId === platformId
  ).length;
const referencedModuleCount = (moduleId: string) =>
  store.state.lessons.filter(
    (lesson) => lesson.businessPlatformModuleId === moduleId
  ).length;

function isExpanded(platformId: string) {
  return expandedPlatformIds.value.includes(platformId);
}

function toggleExpanded(platformId: string) {
  expandedPlatformIds.value = isExpanded(platformId)
    ? expandedPlatformIds.value.filter((id) => id !== platformId)
    : [...expandedPlatformIds.value, platformId];
}

function ensureExpanded(platformId: string) {
  if (!isExpanded(platformId)) {
    expandedPlatformIds.value = [...expandedPlatformIds.value, platformId];
  }
}

function newPlatform() {
  editorMode.value = 'platform';
  selectedPlatformId.value = '';
  selectedModuleId.value = '';
  platformForm.code = '';
  platformForm.name = '';
  platformForm.baseUrl = '';
  platformForm.description = '';
  platformForm.status = 'ENABLED';
  feedback.value = '';
}

function editPlatform(platform: BusinessPlatform) {
  editorMode.value = 'platform';
  selectedPlatformId.value = platform.id;
  selectedModuleId.value = '';
  platformForm.code = platform.code;
  platformForm.name = platform.name;
  platformForm.baseUrl = platform.baseUrl;
  platformForm.description = platform.description;
  platformForm.status = platform.status;
  feedback.value = '';
}

function newModule(platform: BusinessPlatform) {
  editorMode.value = 'module';
  selectedPlatformId.value = platform.id;
  selectedModuleId.value = '';
  moduleForm.code = '';
  moduleForm.name = '';
  moduleForm.path = '';
  moduleForm.description = '';
  moduleForm.status = 'ENABLED';
  ensureExpanded(platform.id);
  feedback.value = '';
}

function editModule(
  platform: BusinessPlatform,
  businessModule: BusinessPlatformModule
) {
  editorMode.value = 'module';
  selectedPlatformId.value = platform.id;
  selectedModuleId.value = businessModule.id;
  moduleForm.code = businessModule.code;
  moduleForm.name = businessModule.name;
  moduleForm.path = businessModule.path;
  moduleForm.description = businessModule.description;
  moduleForm.status = businessModule.status;
  ensureExpanded(platform.id);
  feedback.value = '';
}

async function savePlatform() {
  try {
    const payload = {
      code: platformForm.code,
      name: platformForm.name,
      baseUrl: platformForm.baseUrl,
      description: platformForm.description,
      status: platformForm.status
    };
    const saved = selectedPlatform.value
      ? await store.updateBusinessPlatformRemote(
          selectedPlatform.value.id,
          payload
        )
      : await store.createBusinessPlatformRemote(payload);
    editPlatform(saved);
    ensureExpanded(saved.id);
    feedbackTone.value = 'success';
    feedback.value = `“${saved.name}”已保存。`;
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value =
      error instanceof Error ? error.message : '业务平台保存失败';
  }
}

async function saveModule() {
  const platform = selectedPlatform.value;
  if (!platform) {
    feedbackTone.value = 'danger';
    feedback.value = '请先选择模块所属的业务平台。';
    return;
  }
  try {
    const payload = {
      code: moduleForm.code,
      name: moduleForm.name,
      path: moduleForm.path,
      description: moduleForm.description,
      status: moduleForm.status
    };
    const saved = selectedModule.value
      ? await store.updateBusinessPlatformModuleRemote(
          platform.id,
          selectedModule.value.id,
          payload
        )
      : await store.createBusinessPlatformModuleRemote(platform.id, payload);
    editModule(platform, saved);
    feedbackTone.value = 'success';
    feedback.value = `“${platform.name} / ${saved.name}”已保存。`;
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '平台模块保存失败';
  }
}

async function togglePlatform(platform: BusinessPlatform) {
  try {
    const status = platform.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    await store.setBusinessPlatformStatusRemote(platform.id, status);
    feedbackTone.value = 'success';
    feedback.value = `“${platform.name}”已${status === 'ENABLED' ? '启用' : '停用'}。`;
    if (
      editorMode.value === 'platform' &&
      selectedPlatformId.value === platform.id
    ) {
      editPlatform(platform);
    }
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '状态更新失败';
  }
}

async function toggleModule(
  platform: BusinessPlatform,
  businessModule: BusinessPlatformModule
) {
  try {
    const status =
      businessModule.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    await store.updateBusinessPlatformModuleRemote(
      platform.id,
      businessModule.id,
      { status }
    );
    feedbackTone.value = 'success';
    feedback.value = `“${businessModule.name}”已${status === 'ENABLED' ? '启用' : '停用'}。`;
    if (selectedModuleId.value === businessModule.id) {
      editModule(platform, businessModule);
    }
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '状态更新失败';
  }
}

async function removePlatform(platform: BusinessPlatform) {
  if (!window.confirm(`确认删除业务平台“${platform.name}”及其模块吗？`)) return;
  try {
    if (store.remote.enabled) {
      await store.setBusinessPlatformStatusRemote(platform.id, 'DISABLED');
      feedbackTone.value = 'success';
      feedback.value =
        `后端暂未提供业务平台删除接口，“${platform.name}”已改为停用。`;
      editPlatform(platform);
      return;
    }
    store.removeBusinessPlatform(platform.id);
    if (selectedPlatformId.value === platform.id) newPlatform();
    feedbackTone.value = 'success';
    feedback.value = `“${platform.name}”已删除。`;
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '删除失败';
  }
}

async function removeModule(
  platform: BusinessPlatform,
  businessModule: BusinessPlatformModule
) {
  if (
    !window.confirm(
      `确认删除“${platform.name}”下的模块“${businessModule.name}”吗？`
    )
  ) {
    return;
  }
  try {
    await store.removeBusinessPlatformModuleRemote(
      platform.id,
      businessModule.id
    );
    if (selectedModuleId.value === businessModule.id) newModule(platform);
    feedbackTone.value = 'success';
    feedback.value = `“${businessModule.name}”已删除。`;
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = error instanceof Error ? error.message : '删除失败';
  }
}

function moduleUrl(
  platform: BusinessPlatform,
  businessModule: BusinessPlatformModule
) {
  if (/^[a-z][a-z\d+.-]*:\/\//i.test(businessModule.path)) {
    return businessModule.path;
  }
  if (platform.baseUrl.startsWith('internal://')) {
    return `${platform.baseUrl.replace(/\/$/, '')}/${businessModule.path.replace(/^\//, '')}`;
  }
  try {
    return new URL(businessModule.path, platform.baseUrl).toString();
  } catch {
    return `${platform.baseUrl.replace(/\/$/, '')}/${businessModule.path.replace(/^\//, '')}`;
  }
}

function dateLabel(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

onMounted(async () => {
  if (!store.remote.enabled) return;
  try {
    await store.syncBusinessPlatforms();
    expandedPlatformIds.value = store.state.businessPlatforms.map(
      (platform) => platform.id
    );
    feedbackTone.value = 'success';
    feedback.value = '业务平台已从后端同步。';
  } catch (error) {
    feedbackTone.value = 'danger';
    feedback.value = `后端同步失败，当前保留本地缓存：${
      error instanceof Error ? error.message : '未知错误'
    }`;
  }
});
</script>

<template>
  <div class="page business-platform-page">
    <PageHeader
      eyebrow="BUSINESS PLATFORM"
      title="业务平台管理"
      description="按树结构维护业务平台及其下属模块。教师新建教案时将先选平台，再选择对应模块。"
    >
      <button class="primary" type="button" @click="newPlatform">
        ＋ 新增业务平台
      </button>
    </PageHeader>

    <section class="platform-metrics">
      <span
        ><small>平台总数</small
        ><strong>{{ store.state.businessPlatforms.length }}</strong></span
      >
      <span><small>平台模块</small><strong>{{ moduleCount }}</strong></span>
      <span><small>已启用平台</small><strong>{{ enabledCount }}</strong></span>
      <span
        ><small>已绑定教案</small
        ><strong>{{
          store.state.lessons.filter(
            (lesson) =>
              lesson.businessPlatformId && lesson.businessPlatformModuleId
          ).length
        }}</strong></span
      >
      <span
        ><small>接口状态</small
        ><strong>{{
          store.remote.loading
            ? '同步中'
            : store.remote.enabled
              ? store.remote.lastError
                ? '异常'
                : '已启用'
              : '本地模式'
        }}</strong></span
      >
    </section>

    <div v-if="feedback" class="notice" :class="feedbackTone">
      {{ feedback }}
    </div>

    <section class="platform-layout">
      <div class="card platform-table-card">
        <table>
          <thead>
            <tr>
              <th>平台 / 模块</th>
              <th>访问地址 / 路径</th>
              <th>绑定教案</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <template
              v-for="platform in store.state.businessPlatforms"
              :key="platform.id"
            >
              <tr
                class="platform-row"
                :class="{
                  selected:
                    editorMode === 'platform' &&
                    platform.id === selectedPlatformId
                }"
              >
                <td>
                  <div class="platform-name">
                    <button
                      class="tree-toggle"
                      type="button"
                      :aria-label="isExpanded(platform.id) ? '收起模块' : '展开模块'"
                      @click="toggleExpanded(platform.id)"
                    >
                      {{ isExpanded(platform.id) ? '⌄' : '›' }}
                    </button>
                    <span>{{ platform.code.slice(0, 2) }}</span>
                    <span
                      ><strong>{{ platform.name }}</strong
                      ><small
                        >{{ platform.code }} ·
                        {{ platform.modules.length }} 个模块 ·
                        {{ dateLabel(platform.updatedAt) }}</small
                      ></span
                    >
                  </div>
                </td>
                <td>
                  <a
                    v-if="/^https?:\/\//.test(platform.baseUrl)"
                    class="platform-url"
                    :href="platform.baseUrl"
                    target="_blank"
                    rel="noreferrer"
                  >
                    {{ platform.baseUrl }} ↗
                  </a>
                  <span v-else class="platform-url internal">{{
                    platform.baseUrl
                  }}</span>
                </td>
                <td>{{ referencedCount(platform.id) }} 个</td>
                <td>
                  <span
                    class="platform-status"
                    :class="platform.status.toLowerCase()"
                  >
                    {{ platform.status === 'ENABLED' ? '已启用' : '已停用' }}
                  </span>
                </td>
                <td>
                  <div class="platform-actions">
                    <button type="button" @click="editPlatform(platform)">
                      编辑
                    </button>
                    <button
                      class="module-add-button"
                      type="button"
                      @click="newModule(platform)"
                    >
                      ＋ 新增模块
                    </button>
                    <button type="button" @click="togglePlatform(platform)">
                      {{ platform.status === 'ENABLED' ? '停用' : '启用' }}
                    </button>
                    <button
                      class="danger"
                      type="button"
                      @click="removePlatform(platform)"
                    >
                      删除
                    </button>
                  </div>
                </td>
              </tr>
              <tr
                v-for="businessModule in platform.modules"
                v-show="isExpanded(platform.id)"
                :key="businessModule.id"
                class="module-row"
                :class="{
                  selected:
                    editorMode === 'module' &&
                    businessModule.id === selectedModuleId
                }"
              >
                <td>
                  <div class="module-name">
                    <span class="tree-line" aria-hidden="true">└</span>
                    <span class="module-icon">模</span>
                    <span
                      ><strong>{{ businessModule.name }}</strong
                      ><small
                        >{{ businessModule.code }} ·
                        {{ dateLabel(businessModule.updatedAt) }}</small
                      ></span
                    >
                  </div>
                </td>
                <td>
                  <span class="platform-url module-path">{{
                    moduleUrl(platform, businessModule)
                  }}</span>
                </td>
                <td>{{ referencedModuleCount(businessModule.id) }} 个</td>
                <td>
                  <span
                    class="platform-status"
                    :class="businessModule.status.toLowerCase()"
                  >
                    {{
                      businessModule.status === 'ENABLED' ? '已启用' : '已停用'
                    }}
                  </span>
                </td>
                <td>
                  <div class="platform-actions module-actions">
                    <button
                      type="button"
                      @click="editModule(platform, businessModule)"
                    >
                      编辑模块
                    </button>
                    <button
                      type="button"
                      @click="toggleModule(platform, businessModule)"
                    >
                      {{
                        businessModule.status === 'ENABLED' ? '停用' : '启用'
                      }}
                    </button>
                    <button
                      class="danger"
                      type="button"
                      @click="removeModule(platform, businessModule)"
                    >
                      删除
                    </button>
                  </div>
                </td>
              </tr>
              <tr
                v-if="isExpanded(platform.id) && !platform.modules.length"
                :key="`${platform.id}-empty`"
                class="module-empty-row"
              >
                <td colspan="5">
                  <span>└ 该平台还没有模块。</span>
                  <button type="button" @click="newModule(platform)">
                    立即新增模块
                  </button>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <aside class="card platform-editor">
        <template v-if="editorMode === 'platform'">
          <div class="platform-editor__header">
            <div>
              <span class="eyebrow">{{
                selectedPlatform ? 'EDIT PLATFORM' : 'NEW PLATFORM'
              }}</span>
              <h2>
                {{ selectedPlatform ? '编辑业务平台' : '新增业务平台' }}
              </h2>
            </div>
            <span v-if="selectedPlatform"
              >{{ referencedCount(selectedPlatform.id) }} 个教案使用</span
            >
          </div>
          <label>
            <span>平台名称 *</span>
            <input
              v-model="platformForm.name"
              placeholder="例如：采购业务管理平台"
            />
          </label>
          <label>
            <span>平台编码 *</span>
            <input
              v-model="platformForm.code"
              :disabled="Boolean(selectedPlatform)"
              placeholder="例如：PURCHASE"
            />
            <small v-if="selectedPlatform">平台编码由后端作为稳定标识，保存后不可修改。</small>
          </label>
          <label>
            <span>访问地址 *</span>
            <input
              v-model="platformForm.baseUrl"
              placeholder="https://business.example.com"
            />
            <small
              >外部平台需允许被当前系统嵌入；联调环境也可配置内部地址。</small
            >
          </label>
          <label>
            <span>平台说明</span>
            <textarea v-model="platformForm.description" rows="4" />
          </label>
          <label>
            <span>平台状态</span>
            <select v-model="platformForm.status">
              <option value="ENABLED">启用，可供新教案选择</option>
              <option value="DISABLED">停用，不允许新绑定</option>
            </select>
          </label>
          <div class="editor-actions">
            <button type="button" @click="newPlatform">清空</button>
            <button class="primary" type="button" @click="savePlatform">
              保存业务平台
            </button>
          </div>
        </template>

        <template v-else>
          <div class="platform-editor__header">
            <div>
              <span class="eyebrow">{{
                selectedModule ? 'EDIT MODULE' : 'NEW MODULE'
              }}</span>
              <h2>{{ selectedModule ? '编辑平台模块' : '新增平台模块' }}</h2>
            </div>
            <span>{{ selectedPlatform?.name }}</span>
          </div>
          <div class="module-parent">
            <small>所属平台</small>
            <strong>{{ selectedPlatform?.name }}</strong>
            <span>{{ selectedPlatform?.baseUrl }}</span>
          </div>
          <label>
            <span>模块名称 *</span>
            <input
              v-model="moduleForm.name"
              placeholder="例如：审批采购业务"
            />
          </label>
          <label>
            <span>模块编码 *</span>
            <input
              v-model="moduleForm.code"
              placeholder="例如：PURCHASE_APPROVAL"
            />
          </label>
          <label>
            <span>模块路径 *</span>
            <input v-model="moduleForm.path" placeholder="/approval" />
            <small
              >录制时将打开“平台地址 + 模块路径”；也可填写完整模块地址。</small
            >
          </label>
          <label>
            <span>模块说明</span>
            <textarea v-model="moduleForm.description" rows="4" />
          </label>
          <label>
            <span>模块状态</span>
            <select v-model="moduleForm.status">
              <option value="ENABLED">启用，可供新教案选择</option>
              <option value="DISABLED">停用，不允许新绑定</option>
            </select>
          </label>
          <div class="editor-actions">
            <button
              v-if="selectedPlatform"
              type="button"
              @click="newModule(selectedPlatform)"
            >
              清空
            </button>
            <button class="primary" type="button" @click="saveModule">
              保存平台模块
            </button>
          </div>
        </template>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.business-platform-page {
  display: grid;
  gap: 18px;
}

.business-platform-page :deep(.page-header) {
  margin-bottom: 0;
}

.platform-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 170px));
  gap: 12px;
}

.platform-metrics > span {
  display: grid;
  gap: 5px;
  border: 1px solid #e4e8ef;
  border-radius: 12px;
  padding: 14px 16px;
  background: #fff;
}

.platform-metrics small {
  color: #838ea0;
  font-size: 10px;
}

.platform-metrics strong {
  font-size: 22px;
}

.platform-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  align-items: start;
  gap: 16px;
}

.platform-table-card {
  overflow-x: auto;
}

.platform-table-card table {
  min-width: 860px;
}

.platform-table-card tr.selected td {
  background: #f7f5ff;
}

.platform-row td {
  border-top: 1px solid #e3e7ef;
}

.platform-name,
.module-name {
  display: flex;
  min-width: 240px;
  align-items: center;
  gap: 9px;
}

.tree-toggle {
  display: grid;
  width: 24px;
  min-height: 24px;
  place-items: center;
  border: 0;
  padding: 0;
  color: #647085;
  background: transparent;
  font-size: 19px;
}

.platform-name > span:nth-child(2),
.module-icon {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  border-radius: 9px;
  color: #6654d8;
  background: #eeeaff;
  font-size: 9px;
  font-weight: 900;
}

.platform-name > span:last-child,
.module-name > span:last-child {
  display: grid;
  gap: 4px;
}

.platform-name strong,
.module-name strong {
  font-size: 12px;
}

.platform-name small,
.module-name small {
  color: #8a94a5;
  font-size: 9px;
}

.module-row td {
  background: #fbfcfe;
}

.module-row:hover td {
  background: #f7f8fc;
}

.module-name {
  padding-left: 34px;
}

.tree-line {
  color: #b6becb;
  font-family: monospace;
  font-size: 17px;
}

.module-icon {
  color: #397b70;
  background: #e7f5f1;
}

.module-empty-row td {
  padding-left: 74px;
  color: #8c96a6;
  background: #fbfcfe;
  font-size: 10px;
}

.module-empty-row button {
  min-height: 30px;
  margin-left: 10px;
  padding: 0 10px;
  font-size: 9px;
}

.platform-url {
  display: block;
  max-width: 260px;
  overflow: hidden;
  color: #5f50d4;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.platform-url.internal,
.module-path {
  color: #788497;
  font-family: "Cascadia Code", Consolas, monospace;
}

.module-path {
  color: #397b70;
}

.platform-status {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  border-radius: 999px;
  padding: 0 8px;
  font-size: 9px;
  font-weight: 800;
}

.platform-status.enabled {
  color: #08775f;
  background: #e5f8f2;
}

.platform-status.disabled {
  color: #7c8798;
  background: #edf0f4;
}

.platform-actions {
  display: flex;
  min-width: 260px;
  gap: 5px;
}

.platform-actions button {
  min-height: 30px;
  padding: 0 8px;
  font-size: 9px;
}

.platform-actions .module-add-button {
  border-color: #d8d3ff;
  color: #5d4fd0;
  background: #f7f5ff;
}

.module-actions {
  min-width: 205px;
}

.platform-editor {
  position: sticky;
  top: 90px;
  display: grid;
  gap: 13px;
  padding: 17px;
}

.platform-editor__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid #e8ebf1;
  padding-bottom: 13px;
}

.platform-editor__header h2 {
  margin: 0;
  font-size: 16px;
}

.platform-editor__header > span {
  max-width: 130px;
  color: #7e899b;
  font-size: 9px;
  text-align: right;
}

.platform-editor label small {
  color: #8c96a7;
  font-size: 9px;
  font-weight: 500;
  line-height: 1.5;
}

.module-parent {
  display: grid;
  gap: 4px;
  border-radius: 10px;
  padding: 11px 12px;
  background: #f5f7fa;
}

.module-parent small,
.module-parent span {
  overflow: hidden;
  color: #8a94a4;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.module-parent strong {
  color: #4b5668;
  font-size: 11px;
}

.editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 7px;
  border-top: 1px solid #e8ebf1;
  padding-top: 13px;
}

@media (max-width: 1100px) {
  .platform-layout {
    grid-template-columns: 1fr;
  }

  .platform-editor {
    position: static;
  }
}

@media (max-width: 720px) {
  .platform-metrics {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
