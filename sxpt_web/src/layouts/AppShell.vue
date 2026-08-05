<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { RouteLocationRaw } from 'vue-router';
import type { SysMenuConfig } from '../api/systemConfig';
import { usersApi, type TeachOrg, type TeachUser, type TeachUserOrg } from '../api/users';
import type { PortalRole } from '../domain/models';
import { authApi, type AuthSession } from '../services/trainingApi';
import { useRuntimeContextStore } from '../stores/runtimeContextStore';
import { useTrainingStore } from '../stores/trainingStore';

interface NavigationItem {
  label: string;
  icon: string;
  to: RouteLocationRaw;
  group?: 'data-prepare' | 'basic-config';
}

interface DemoSwitchUser {
  id: string;
  tenantId: string;
  username: string;
  displayName: string;
  userType: string;
  orgNames: string[];
}

const route = useRoute();
const router = useRouter();
const store = useTrainingStore();
const runtimeContextStore = useRuntimeContextStore();
const DEMO_SWITCH_PASSWORD =
  import.meta.env.VITE_DEMO_SWITCH_PASSWORD ?? 'Sxpt@123456';
const hiddenNavigationMenuCodes = new Set([
  'lesson-editor',
  'exam-setup',
  'group-setup',
  'exam-data',
  'publish-center'
]);
const immersiveRoute = computed(() =>
  ['lesson-editor', 'lesson-recording', 'student-task-runner'].includes(
    String(route.name ?? '')
  )
);

const roleOptions: Array<{
  key: PortalRole;
  label: string;
  shortLabel: string;
  home: string;
}> = [
  { key: 'admin', label: '后台管理', shortLabel: '管', home: '/admin/overview' },
  { key: 'teacher', label: '教师端', shortLabel: '师', home: '/teacher/dashboard' },
  { key: 'student', label: '学生端', shortLabel: '学', home: '/student/tasks' }
];

const navigation = computed<NavigationItem[]>(() => {
  const staticItems = buildStaticNavigation();
  const runtimeItems = buildRuntimeNavigation(runtimeContextStore.getVisibleMenus());
  return runtimeItems.length > 0 ? runtimeItems : staticItems;
});
const currentSession = ref<AuthSession | null>(authApi.getSession());
const demoUsers = ref<DemoSwitchUser[]>([]);
const accountMenuOpen = ref(false);
const accountSwitcherRoot = ref<HTMLElement | null>(null);
const accountDirectoryLoading = ref(false);
const accountSwitchingUserId = ref('');
const accountErrorMessage = ref('');

onMounted(loadRuntimeMenus);
onMounted(loadDemoUserDirectory);
onMounted(() => document.addEventListener('pointerdown', closeAccountMenuOnOutsideClick));
onBeforeUnmount(() =>
  document.removeEventListener('pointerdown', closeAccountMenuOnOutsideClick)
);

watch(
  () => store.state.currentRole,
  () => {
    void loadRuntimeMenus();
  }
);

watch(
  () => runtimeContextStore.state.context?.user.userId,
  () => {
    currentSession.value = authApi.getSession();
    void loadDemoUserDirectory();
  }
);

function buildStaticNavigation(): NavigationItem[] {
  if (store.state.currentRole === 'teacher') {
    return [
      { label: '教学工作台', icon: '01', to: '/teacher/dashboard' },
      { label: '评阅反馈', icon: '02', to: '/teacher/review' },
      { label: '批次准备', icon: '03', to: '/teacher/data-prepare', group: 'data-prepare' }
    ];
  }
  if (store.state.currentRole === 'student') {
    return [
      { label: '我的任务', icon: '01', to: '/student/tasks' },
      { label: '成绩反馈', icon: '02', to: '/student/results' }
    ];
  }
  return [
    { label: '运营总览', icon: '01', to: '/admin/overview' },
    { label: '用户管理', icon: '02', to: '/admin/basic/users', group: 'basic-config' },
    { label: '角色管理', icon: '03', to: '/admin/basic/roles', group: 'basic-config' },
    { label: '单位管理', icon: '04', to: '/admin/basic/orgs', group: 'basic-config' },
    { label: '用户角色', icon: '05', to: '/admin/basic/user-roles', group: 'basic-config' },
    { label: '用户单位', icon: '06', to: '/admin/basic/user-orgs', group: 'basic-config' },
    { label: '菜单管理', icon: '07', to: '/admin/basic/menus', group: 'basic-config' },
    { label: '权限管理', icon: '08', to: '/admin/basic/permissions', group: 'basic-config' },
    { label: '角色权限', icon: '09', to: '/admin/basic/role-permissions', group: 'basic-config' },
    { label: '字典配置', icon: '10', to: '/admin/basic/dict-items', group: 'basic-config' },
    { label: '教案管理', icon: '11', to: '/admin/lessons' },
    { label: '平台接入', icon: '16', to: '/admin/data-prepare/systems', group: 'data-prepare' },
    { label: '业务模块', icon: '17', to: '/admin/data-prepare/modules', group: 'data-prepare' },
    { label: '模板管理', icon: '18', to: '/admin/data-prepare/templates', group: 'data-prepare' },
    { label: '策略管理', icon: '19', to: '/admin/data-prepare/strategies', group: 'data-prepare' },
    { label: '批次准备', icon: '20', to: '/admin/data-prepare', group: 'data-prepare' }
  ];
}

/**
 * 业务功能：加载后端配置的当前用户可见菜单。
 * 关键流程：后端菜单存在时接管导航；加载失败时 store 会进入兼容模式，由静态菜单兜底。
 */
async function loadRuntimeMenus() {
  await runtimeContextStore.loadRuntimeContext({ force: true });
}

/**
 * 业务功能：将后端菜单配置转换为侧边栏导航项。
 * 关键流程：只转换可路由菜单；分组由路由前缀推导，避免菜单表额外承担前端展示细节。
 */
function buildRuntimeNavigation(menus: SysMenuConfig[]): NavigationItem[] {
  return menus
    .filter(
      (menu) =>
        Boolean(menu.routePath) && !hiddenNavigationMenuCodes.has(menu.menuCode)
    )
    .map((menu, index) => ({
      label: menu.menuName,
      icon: menu.icon || String(menu.sortNo || index + 1).padStart(2, '0'),
      to: menu.routePath as string,
      group: resolveNavigationGroup(menu.routePath)
    }));
}

function resolveNavigationGroup(routePath?: string): NavigationItem['group'] {
  if (routePath?.startsWith('/admin/basic/')) return 'basic-config';
  if (routePath?.includes('/data-prepare')) return 'data-prepare';
  return undefined;
}

const currentRole = computed(
  () => roleOptions.find((role) => role.key === store.state.currentRole) ?? roleOptions[0]
);
const currentUserDisplayName = computed(
  () =>
    runtimeContextStore.state.context?.user.displayName ||
    currentSession.value?.user.displayName ||
    currentSession.value?.user.username ||
    '未登录用户'
);
const currentUserUnitName = computed(() => {
  const orgNames = runtimeContextStore.state.context?.orgs
    .map((org) => org.orgName)
    .filter(Boolean) ?? [];
  return orgNames.length > 0 ? orgNames.join(' / ') : '未绑定单位';
});
const currentUserInitial = computed(() =>
  currentUserDisplayName.value.trim().slice(0, 1) || '用'
);

/**
 * 业务功能：加载演示用户目录，供页面右上角快速切换登录用户。
 * 关键流程：复用现有用户、单位、用户单位关系接口，在前端组装展示文本；接口异常不阻塞业务页面，只隐藏下拉错误数据。
 */
async function loadDemoUserDirectory() {
  const session = authApi.getSession();
  if (!session?.user.tenantId || accountDirectoryLoading.value) return;
  accountDirectoryLoading.value = true;
  accountErrorMessage.value = '';
  try {
    const [users, orgs, userOrgs] = await Promise.all([
      usersApi.listUsers(session.user.tenantId),
      usersApi.listOrgs(session.user.tenantId),
      usersApi.listUserOrgs(session.user.tenantId)
    ]);
    demoUsers.value = buildDemoSwitchUsers(users, orgs, userOrgs);
  } catch (error) {
    accountErrorMessage.value =
      error instanceof Error ? error.message : '演示用户列表加载失败';
    demoUsers.value = [];
  } finally {
    accountDirectoryLoading.value = false;
  }
}

/**
 * 业务功能：把用户主数据与组织关系合并为下拉展示模型。
 * 关键流程：按用户 ID 聚合单位名称，并按角色优先级与姓名排序，保证演示切换列表稳定可扫读。
 */
function buildDemoSwitchUsers(
  users: TeachUser[],
  orgs: TeachOrg[],
  userOrgs: TeachUserOrg[]
): DemoSwitchUser[] {
  const orgNameById = new Map(orgs.map((org) => [org.id, org.orgName]));
  const orgNamesByUserId = new Map<string, string[]>();
  userOrgs.forEach((relation) => {
    const orgName = orgNameById.get(relation.orgId);
    if (!orgName) return;
    const names = orgNamesByUserId.get(relation.userId) ?? [];
    if (!names.includes(orgName)) names.push(orgName);
    orgNamesByUserId.set(relation.userId, names);
  });
  return users
    .filter((user) => user.status === 'ACTIVE')
    .map((user) => ({
      id: user.id,
      tenantId: user.tenantId,
      username: user.username,
      displayName: user.realName || user.username,
      userType: user.userType,
      orgNames: orgNamesByUserId.get(user.id) ?? []
    }))
    .sort((left, right) => {
      const roleDelta = getUserTypeSortNo(left.userType) - getUserTypeSortNo(right.userType);
      if (roleDelta !== 0) return roleDelta;
      return left.displayName.localeCompare(right.displayName, 'zh-CN');
    });
}

function getUserTypeSortNo(userType: string): number {
  const normalized = userType.toUpperCase();
  if (normalized.includes('ADMIN') || normalized.includes('EXPERT')) return 1;
  if (normalized.includes('TEACHER')) return 2;
  if (normalized.includes('STUDENT')) return 3;
  return 9;
}

function formatUserType(userType: string): string {
  const normalized = userType.toUpperCase();
  if (normalized.includes('EXPERT')) return '专家';
  if (normalized.includes('TEACHER')) return '教师';
  if (normalized.includes('STUDENT')) return '学生';
  if (normalized.includes('ADMIN')) return '管理员';
  return userType || '用户';
}

function isCurrentSwitchUser(user: DemoSwitchUser): boolean {
  return currentSession.value?.user.userId === user.id;
}

function toggleAccountMenu() {
  accountMenuOpen.value = !accountMenuOpen.value;
}

/**
 * 业务功能：点击页面其它区域时自动收起演示用户下拉。
 * 关键流程：只判断点击目标是否仍在账号切换容器内，避免影响页面其它按钮的正常交互。
 */
function closeAccountMenuOnOutsideClick(event: PointerEvent) {
  const target = event.target;
  if (!(target instanceof Node)) return;
  if (accountSwitcherRoot.value?.contains(target)) return;
  accountMenuOpen.value = false;
}

/**
 * 业务功能：演示环境下选择目标用户并重新登录，刷新该用户权限菜单。
 * 关键流程：调用正式登录接口换取目标用户 token，随后重置运行时上下文并跳转到新身份默认首页。
 */
async function switchDemoUser(user: DemoSwitchUser) {
  if (isCurrentSwitchUser(user) || accountSwitchingUserId.value) return;
  accountMenuOpen.value = false;
  accountSwitchingUserId.value = user.id;
  accountErrorMessage.value = '';
  try {
    const session = await authApi.login({
      loginType: 'PASSWORD',
      tenantId: user.tenantId,
      username: user.username,
      password: DEMO_SWITCH_PASSWORD
    });
    currentSession.value = session;
    await store.initializeAuthenticatedWorkspace(session);
    runtimeContextStore.resetRuntimeContext();
    await runtimeContextStore.loadRuntimeContext({ force: true });
    await router.replace(authApi.getHomePath(session));
  } catch (error) {
    accountErrorMessage.value =
      error instanceof Error ? error.message : '演示用户切换失败';
  } finally {
    accountSwitchingUserId.value = '';
  }
}
</script>

<template>
  <div
    class="app-shell"
    :class="{ 'app-shell--immersive': immersiveRoute }"
  >
    <aside class="app-sidebar">
      <RouterLink class="brand" to="/platforms">
        <span class="brand-mark">SX</span>
        <span>
          <strong>实训云台</strong>
          <small>TRAINING OS</small>
        </span>
      </RouterLink>

      <div class="identity-card" aria-label="当前登录身份">
        <span>{{ currentRole.shortLabel }}</span>
        <div>
          <small>当前身份</small>
          <strong>{{ currentRole.label }}</strong>
        </div>
        <RouterLink to="/platforms" title="返回平台选择">↗</RouterLink>
      </div>

      <p class="nav-caption">{{ currentRole.label }}功能</p>
      <nav class="main-nav">
        <RouterLink
          v-for="item in navigation"
          :key="item.label"
          :to="item.to"
          :class="{
            'data-prepare-nav': item.group === 'data-prepare',
            'basic-config-nav': item.group === 'basic-config'
          }"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <section class="sidebar-flow">
        <span class="sidebar-flow__label">当前业务链</span>
        <strong>采购审批实训</strong>
        <div class="mini-progress">
          <span style="width: 72%"></span>
        </div>
        <small>教案 / 考试 / 分组 / 数据 / 发布</small>
      </section>

      <footer class="sidebar-footer">
        <span class="avatar">师</span>
        <span><strong>教师账号</strong><small>平台管理员</small></span>
        <button type="button" aria-label="更多账号操作">···</button>
      </footer>
    </aside>

    <section class="app-workspace">
      <header class="topbar">
        <div>
          <span class="breadcrumb">业务实训平台 / {{ currentRole.label }}</span>
          <strong>{{ String(route.meta.title ?? '工作台') }}</strong>
        </div>
        <div class="topbar-actions">
          <div ref="accountSwitcherRoot" class="account-switcher">
            <button
              class="account-trigger"
              type="button"
              :aria-expanded="accountMenuOpen"
              aria-label="切换演示用户"
              @click="toggleAccountMenu"
            >
              <span class="account-avatar">{{ currentUserInitial }}</span>
              <span class="account-summary">
                <strong>{{ currentUserDisplayName }}</strong>
                <small>{{ currentUserUnitName }}</small>
              </span>
              <span class="account-caret">⌄</span>
            </button>
            <section v-if="accountMenuOpen" class="account-menu">
              <header>
                <strong>演示用户</strong>
                <button
                  type="button"
                  :disabled="accountDirectoryLoading"
                  @click="loadDemoUserDirectory"
                >
                  刷新
                </button>
              </header>
              <p v-if="accountErrorMessage" class="account-message">
                {{ accountErrorMessage }}
              </p>
              <p v-else-if="accountDirectoryLoading" class="account-message">
                正在加载用户...
              </p>
              <template v-else>
                <button
                  v-for="user in demoUsers"
                  :key="user.id"
                  class="account-option"
                  type="button"
                  :class="{ active: isCurrentSwitchUser(user) }"
                  :disabled="isCurrentSwitchUser(user) || Boolean(accountSwitchingUserId)"
                  @click="switchDemoUser(user)"
                >
                  <span>{{ user.displayName.slice(0, 1) || '用' }}</span>
                  <span>
                    <strong>{{ user.displayName }}</strong>
                    <small>{{ user.orgNames.join(' / ') || '未绑定单位' }}</small>
                  </span>
                  <em>
                    {{ accountSwitchingUserId === user.id ? '登录中' : formatUserType(user.userType) }}
                  </em>
                </button>
              </template>
            </section>
          </div>
        </div>
      </header>
      <main
        class="app-content"
        :class="{ 'app-content--immersive': immersiveRoute }"
      >
        <RouterView :key="route.fullPath" />
      </main>
    </section>
  </div>
</template>

<style scoped>
.account-switcher {
  position: relative;
}

.account-trigger {
  display: grid;
  grid-template-columns: 32px minmax(116px, 176px) 14px;
  min-height: 42px;
  align-items: center;
  gap: 9px;
  border-color: #dde5ef;
  border-radius: 8px;
  padding: 4px 9px 4px 5px;
  background: #fff;
  color: #263449;
  box-shadow: none;
}

.account-trigger:hover {
  border-color: #b7c7df;
  box-shadow: 0 8px 18px rgb(30 41 59 / 8%);
}

.account-avatar,
.account-option > span:first-child {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 8px;
  background: #e8f3ef;
  color: #0f766e;
  font-size: 12px;
  font-weight: 900;
}

.account-summary,
.account-option > span:nth-child(2) {
  display: grid;
  min-width: 0;
  gap: 2px;
  text-align: left;
}

.account-summary strong,
.account-option strong {
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-summary small,
.account-option small {
  overflow: hidden;
  color: #718096;
  font-size: 10px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-caret {
  color: #7b8797;
  font-size: 13px;
}

.account-menu {
  position: absolute;
  z-index: 60;
  top: calc(100% + 9px);
  right: 0;
  display: grid;
  width: min(360px, 88vw);
  max-height: min(520px, calc(100vh - 96px));
  overflow-y: auto;
  border: 1px solid #dfe6ef;
  border-radius: 8px;
  background: #fff;
  padding: 8px;
  box-shadow: 0 22px 55px rgb(15 23 42 / 16%);
}

.account-menu header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 4px 8px;
}

.account-menu header strong {
  font-size: 13px;
}

.account-menu header button {
  min-height: 28px;
  border-color: #dfe6ef;
  border-radius: 7px;
  padding: 0 10px;
  color: #536174;
  font-size: 11px;
}

.account-message {
  margin: 4px;
  border: 1px solid #eef1f5;
  border-radius: 7px;
  padding: 10px;
  color: #66758a;
  background: #fafbfc;
  font-size: 12px;
}

.account-option {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  width: 100%;
  min-height: 54px;
  align-items: center;
  justify-content: initial;
  gap: 10px;
  border: 0;
  border-radius: 7px;
  padding: 7px;
  background: transparent;
  box-shadow: none;
}

.account-option:hover:not(:disabled) {
  background: #f5f8fb;
  box-shadow: none;
  transform: none;
}

.account-option.active {
  background: #eef7f4;
}

.account-option em {
  color: #718096;
  font-size: 10px;
  font-style: normal;
  font-weight: 800;
}

@media (max-width: 820px) {
  .account-trigger {
    grid-template-columns: 32px 14px;
  }

  .account-summary {
    display: none;
  }
}
</style>
