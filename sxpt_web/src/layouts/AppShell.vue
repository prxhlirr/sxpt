<script setup lang="ts">
import { computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { PortalRole } from '../domain/models';
import { useTrainingStore } from '../stores/trainingStore';

const route = useRoute();
const router = useRouter();
const store = useTrainingStore();

const roleOptions: Array<{
  key: PortalRole;
  label: string;
  shortLabel: string;
  home: string;
}> = [
  { key: 'admin', label: '后台管理', shortLabel: '管', home: '/admin/overview' },
  { key: 'teacher', label: '教师端', shortLabel: '教', home: '/teacher/dashboard' },
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

const navigation = computed(() => {
  const lessonId = activeLessonId.value;
  if (store.state.currentRole === 'teacher') {
    return [
      { label: '教学工作台', icon: '⌂', to: '/teacher/dashboard' },
      { label: '评阅与反馈', icon: '✓', to: '/teacher/review' }
    ];
  }
  if (store.state.currentRole === 'student') {
    return [
      { label: '我的任务', icon: '▣', to: '/student/tasks' },
      { label: '成绩反馈', icon: '★', to: '/student/results' }
    ];
  }
  return [
    { label: '运营总览', icon: '⌂', to: '/admin/overview' },
    { label: '教案管理', icon: '▤', to: '/admin/lessons' },
    {
      label: '教案编排',
      icon: '⌘',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/editor`
        : '/admin/lessons'
    },
    {
      label: '考试设置',
      icon: '◫',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/exam`
        : '/admin/lessons'
    },
    {
      label: '分组设置',
      icon: '♟',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/groups`
        : '/admin/lessons'
    },
    {
      label: '考试数据',
      icon: '◈',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/data`
        : '/admin/lessons'
    },
    {
      label: '发布中心',
      icon: '↗',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/publish`
        : '/admin/lessons'
    }
  ];
});

const currentRole = computed(() =>
  roleOptions.find((item) => item.key === store.state.currentRole)
);

watch(
  () => route.path,
  (path) => {
    const role: PortalRole = path.startsWith('/student')
      ? 'student'
      : path.startsWith('/teacher')
        ? 'teacher'
        : 'admin';
    if (store.state.currentRole !== role) store.setRole(role);
  },
  { immediate: true }
);

async function switchRole(role: PortalRole) {
  const option = roleOptions.find((item) => item.key === role);
  if (!option) return;
  store.setRole(role);
  await router.push(option.home);
}
</script>

<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <RouterLink class="brand" to="/admin/overview">
        <span class="brand-mark">SX</span>
        <span>
          <strong>实训云台</strong>
          <small>TRAINING OS</small>
        </span>
      </RouterLink>

      <div class="role-switcher" aria-label="切换门户">
        <button
          v-for="role in roleOptions"
          :key="role.key"
          type="button"
          :class="{ active: store.state.currentRole === role.key }"
          @click="switchRole(role.key)"
        >
          <span>{{ role.shortLabel }}</span>
          {{ role.label }}
        </button>
      </div>

      <p class="nav-caption">{{ currentRole?.label }}功能</p>
      <nav class="main-nav">
        <RouterLink
          v-for="item in navigation"
          :key="item.label"
          :to="item.to"
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
        <small>教案 → 考试 → 分组 → 数据 → 发布</small>
      </section>

      <footer class="sidebar-footer">
        <span class="avatar">薛</span>
        <span><strong>薛老师</strong><small>平台管理员</small></span>
        <button type="button" aria-label="更多账号操作">•••</button>
      </footer>
    </aside>

    <section class="app-workspace">
      <header class="topbar">
        <div>
          <span class="breadcrumb">业务实训平台 / {{ currentRole?.label }}</span>
          <strong>{{ String(route.meta.title ?? '工作台') }}</strong>
        </div>
        <div class="topbar-actions">
          <span class="environment-badge"><i></i> MOCK 演示环境</span>
          <button class="icon-button" type="button" aria-label="搜索">⌕</button>
          <button class="icon-button" type="button" aria-label="通知">♢</button>
          <span class="today">2026 · 秋季学期</span>
        </div>
      </header>
      <main
        class="app-content"
        :class="{ 'app-content--immersive': route.name === 'lesson-editor' }"
      >
        <RouterView :key="route.fullPath" />
      </main>
    </section>
  </div>
</template>
