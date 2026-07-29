<script setup lang="ts">
import { computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { RouteLocationRaw } from 'vue-router';
import type { PortalRole } from '../domain/models';
import { useTrainingStore } from '../stores/trainingStore';

interface NavigationItem {
  label: string;
  icon: string;
  to: RouteLocationRaw;
  group?: 'data-prepare';
}

const route = useRoute();
const store = useTrainingStore();

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
    { label: '教案管理', icon: '02', to: '/admin/lessons' },
    {
      label: '教案编排',
      icon: '03',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/editor`
        : '/admin/lessons'
    },
    {
      label: '考试设置',
      icon: '04',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/exam`
        : '/admin/lessons'
    },
    {
      label: '分组设置',
      icon: '05',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/groups`
        : '/admin/lessons'
    },
    {
      label: '考试数据',
      icon: '06',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/data`
        : '/admin/lessons'
    },
    { label: '平台接入', icon: '07', to: '/admin/data-prepare/systems', group: 'data-prepare' },
    { label: '业务模块', icon: '08', to: '/admin/data-prepare/modules', group: 'data-prepare' },
    { label: '模板管理', icon: '09', to: '/admin/data-prepare/templates', group: 'data-prepare' },
    { label: '策略管理', icon: '10', to: '/admin/data-prepare/strategies', group: 'data-prepare' },
    { label: '批次准备', icon: '11', to: '/admin/data-prepare', group: 'data-prepare' },
    {
      label: '发布中心',
      icon: '12',
      to: lessonId
        ? `/admin/lessons/${encodeURIComponent(lessonId)}/publish`
        : '/admin/lessons'
    }
  ];
});

const currentRole = computed(
  () => roleOptions.find((role) => role.key === store.state.currentRole) ?? roleOptions[0]
);
</script>

<template>
  <div
    class="app-shell"
    :class="{ 'app-shell--immersive': route.name === 'lesson-editor' }"
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
          :class="{ 'data-prepare-nav': item.group === 'data-prepare' }"
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
        :class="{ 'app-content--immersive': route.name === 'lesson-editor' }"
      >
        <RouterView :key="route.fullPath" />
      </main>
    </section>
  </div>
</template>
