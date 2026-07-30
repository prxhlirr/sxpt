<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import type { RouteLocationRaw } from 'vue-router';
import type { SysMenuConfig } from '../api/systemConfig';
import type { PortalRole } from '../domain/models';
import { useRuntimeContextStore } from '../stores/runtimeContextStore';
import { useTrainingStore } from '../stores/trainingStore';

interface NavigationItem {
  label: string;
  icon: string;
  to: RouteLocationRaw;
  group?: 'data-prepare' | 'basic-config';
}

const route = useRoute();
const store = useTrainingStore();
const runtimeContextStore = useRuntimeContextStore();
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

const activeLessonId = computed(() => {
  const routeLessonId = String(route.params.lessonId ?? '');
  return (
    store.state.lessons.find((lesson) => lesson.id === routeLessonId)?.id ??
    store.state.lessons[0]?.id ??
    ''
  );
});

const navigation = computed<NavigationItem[]>(() => {
  const staticItems = buildStaticNavigation();
  const runtimeItems = buildRuntimeNavigation(runtimeContextStore.getVisibleMenus());
  return runtimeItems.length > 0 ? runtimeItems : staticItems;
});

onMounted(loadRuntimeMenus);

watch(
  () => store.state.currentRole,
  () => {
    void loadRuntimeMenus();
  }
);

function buildStaticNavigation(): NavigationItem[] {
  const lessonId = activeLessonId.value;
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
    {
      label: '教案编排',
      icon: '12',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/editor`
        : '/admin/lessons'
    },
    {
      label: '考试设置',
      icon: '13',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/exam`
        : '/admin/lessons'
    },
    {
      label: '分组设置',
      icon: '14',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/groups`
        : '/admin/lessons'
    },
    {
      label: '考试数据',
      icon: '15',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/data`
        : '/admin/lessons'
    },
    { label: '平台接入', icon: '16', to: '/admin/data-prepare/systems', group: 'data-prepare' },
    { label: '业务模块', icon: '17', to: '/admin/data-prepare/modules', group: 'data-prepare' },
    { label: '模板管理', icon: '18', to: '/admin/data-prepare/templates', group: 'data-prepare' },
    { label: '策略管理', icon: '19', to: '/admin/data-prepare/strategies', group: 'data-prepare' },
    { label: '批次准备', icon: '20', to: '/admin/data-prepare', group: 'data-prepare' },
    {
      label: '发布中心',
      icon: '21',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/publish`
      : '/admin/lessons'
    }
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
    .filter((menu) => Boolean(menu.routePath))
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
      <header>
        <div>
          <span class="breadcrumb">业务实训平台 / {{ currentRole.label }}</span>
          <strong>{{ String(route.meta.title ?? '工作台') }}</strong>
        </div>
        <div class="topbar-actions">
          <span class="environment-badge"><i></i> 本地联调环境</span>
          <button class="icon-button" type="button" aria-label="搜索">S</button>
          <button class="icon-button" type="button" aria-label="通知">N</button>
          <span class="today">2026 秋季学期</span>
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
