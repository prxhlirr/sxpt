<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import type { PortalRole } from '../domain/models';
import { useTrainingStore } from '../stores/trainingStore';

const route = useRoute();
const store = useTrainingStore();
const immersiveRoute = computed(() =>
  ['lesson-editor', 'lesson-recording', 'student-task-runner'].includes(
    String(route.name ?? '')
  )
);

const identityProfiles: Record<
  PortalRole,
  {
    label: string;
    shortLabel: string;
    name: string;
    accountLabel: string;
  }
> = {
  admin: {
    label: '后台管理',
    shortLabel: '管',
    name: '薛管理员',
    accountLabel: '平台管理员'
  },
  teacher: {
    label: '教师端',
    shortLabel: '教',
    name: '薛老师',
    accountLabel: '授课教师'
  },
  student: {
    label: '学生端',
    shortLabel: '学',
    name: '林同学',
    accountLabel: '参训学员'
  }
};

const navigation = computed(() => {
  if (store.state.currentRole === 'teacher') {
    return [
      { label: '教学工作台', icon: '⌂', to: '/teacher/dashboard' },
      { label: '录制教案', icon: '⌘', to: '/admin/lessons' },
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
    { label: '业务平台', icon: '◎', to: '/admin/business-platforms' }
  ];
});

const currentRole = computed(
  () => identityProfiles[store.state.currentRole]
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
        <span class="avatar">{{ currentRole.name.slice(0, 1) }}</span>
        <span>
          <strong>{{ currentRole.name }}</strong>
          <small>{{ currentRole.accountLabel }}</small>
        </span>
        <button type="button" aria-label="更多账号操作">•••</button>
      </footer>
    </aside>

    <section class="app-workspace">
      <header class="topbar">
        <div>
          <span class="breadcrumb">业务实训平台 / {{ currentRole.label }}</span>
          <strong>{{ String(route.meta.title ?? '工作台') }}</strong>
        </div>
        <div class="topbar-actions">
          <RouterLink class="platform-home-link" to="/platforms">平台首页</RouterLink>
          <span class="environment-badge"><i></i> 实训环境</span>
          <button class="icon-button" type="button" aria-label="搜索">⌕</button>
          <button class="icon-button" type="button" aria-label="通知">♢</button>
          <span class="today">2026 · 秋季学期</span>
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
