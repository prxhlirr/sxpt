import { createRouter, createWebHistory } from 'vue-router';
import AppShell from './layouts/AppShell.vue';
import type { PortalRole } from './domain/models';
import { useTrainingStore } from './stores/trainingStore';

export const router = createRouter({
  history: createWebHistory(),
  scrollBehavior() {
    return { top: 0 };
  },
  routes: [
    {
      path: '/platforms',
      name: 'platforms',
      component: () => import('./views/PlatformPortalView.vue'),
      meta: { title: '选择平台' }
    },
    {
      path: '/',
      component: AppShell,
      children: [
        { path: '', redirect: '/platforms' },
        {
          path: 'admin/overview',
          name: 'admin-overview',
          component: () => import('./views/admin/AdminOverviewView.vue'),
          meta: { title: '运营总览', role: 'admin' }
        },
        {
          path: 'admin/lessons',
          name: 'lesson-list',
          component: () => import('./views/admin/LessonListView.vue'),
          meta: { title: '教案管理', roles: ['admin', 'teacher'] }
        },
        {
          path: 'admin/business-platforms',
          name: 'business-platforms',
          component: () => import('./views/admin/BusinessPlatformManagementView.vue'),
          meta: { title: '业务平台管理', role: 'admin' }
        },
        {
          path: 'admin/lessons/:lessonId/editor',
          name: 'lesson-editor',
          component: () => import('./views/admin/LessonEditorView.vue'),
          meta: { title: '教案编排', roles: ['admin', 'teacher'] }
        },
        {
          path: 'admin/lessons/:lessonId/recording',
          name: 'lesson-recording',
          component: () => import('./views/admin/RecordingPreviewView.vue'),
          meta: { title: '录制教案回看', roles: ['admin', 'teacher'] }
        },
        {
          path: 'admin/lessons/:lessonId/exam',
          name: 'exam-setup',
          component: () => import('./views/admin/ExamSetupView.vue'),
          meta: { title: '考试设置', roles: ['admin', 'teacher'] }
        },
        {
          path: 'admin/lessons/:lessonId/groups',
          name: 'group-setup',
          component: () => import('./views/admin/GroupSetupView.vue'),
          meta: { title: '分组设置', roles: ['admin', 'teacher'] }
        },
        {
          path: 'admin/lessons/:lessonId/data',
          name: 'exam-data',
          component: () => import('./views/admin/ExamDataView.vue'),
          meta: { title: '考试数据', roles: ['admin', 'teacher'] }
        },
        {
          path: 'admin/lessons/:lessonId/publish',
          name: 'publish-center',
          component: () => import('./views/admin/PublishCenterView.vue'),
          meta: { title: '发布中心', roles: ['admin', 'teacher'] }
        },
        {
          path: 'teacher/dashboard',
          name: 'teacher-dashboard',
          component: () => import('./views/teacher/TeacherDashboardView.vue'),
          meta: { title: '教学工作台', role: 'teacher' }
        },
        {
          path: 'teacher/review',
          name: 'teacher-review',
          component: () => import('./views/teacher/TeacherReviewView.vue'),
          meta: { title: '评阅与反馈', role: 'teacher' }
        },
        {
          path: 'student/tasks',
          name: 'student-tasks',
          component: () => import('./views/student/StudentTasksView.vue'),
          meta: { title: '我的任务', role: 'student' }
        },
        {
          path: 'student/tasks/:taskId',
          name: 'student-task-runner',
          component: () => import('./views/student/StudentTaskRunnerView.vue'),
          meta: { title: '任务办理', role: 'student' }
        },
        {
          path: 'student/results',
          name: 'student-results',
          component: () => import('./views/student/StudentResultsView.vue'),
          meta: { title: '成绩反馈', role: 'student' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      component: () => import('./views/NotFoundView.vue')
    }
  ]
});

router.beforeEach((to) => {
  const store = useTrainingStore();
  const role = store.state.currentRole;
  const allowedRoles = Array.isArray(to.meta.roles)
    ? (to.meta.roles as PortalRole[])
    : to.meta.role
      ? [to.meta.role as PortalRole]
      : [];

  if (allowedRoles.length && !allowedRoles.includes(role)) {
    return {
      name: 'platforms',
      query: { access: 'denied', target: to.fullPath }
    };
  }
  return true;
});

router.afterEach((to) => {
  const pageTitle = String(to.meta.title ?? '业务实训平台');
  document.title = `${pageTitle} · 业务实训平台`;
});
