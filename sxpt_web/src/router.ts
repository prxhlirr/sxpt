import { createRouter, createWebHistory } from 'vue-router';
import type { PortalRole } from './domain/models';
import AppShell from './layouts/AppShell.vue';
import { useRuntimeContextStore } from './stores/runtimeContextStore';
import { useTrainingStore } from './stores/trainingStore';
import { authApi } from './services/trainingApi';

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
      path: '/login',
      name: 'login',
      component: () => import('./views/LoginView.vue'),
      meta: { title: '登录', public: true }
    },
    {
      path: '/',
      component: AppShell,
      children: [
        {
          path: '',
          redirect: '/platforms'
        },
        {
          path: 'admin/overview',
          name: 'admin-overview',
          component: () => import('./views/admin/AdminOverviewView.vue'),
          meta: { title: '运营总览', role: 'admin' }
        },
        {
          path: 'admin/basic/users',
          name: 'basic-users',
          component: () => import('./views/admin/BasicConfigView.vue'),
          props: { section: 'users' },
          meta: { title: '用户管理', role: 'admin' }
        },
        {
          path: 'admin/basic/roles',
          name: 'basic-roles',
          component: () => import('./views/admin/BasicConfigView.vue'),
          props: { section: 'roles' },
          meta: { title: '角色管理', role: 'admin' }
        },
        {
          path: 'admin/basic/orgs',
          name: 'basic-orgs',
          component: () => import('./views/admin/BasicConfigView.vue'),
          props: { section: 'orgs' },
          meta: { title: '单位管理', role: 'admin' }
        },
        {
          path: 'admin/basic/user-roles',
          name: 'basic-user-roles',
          component: () => import('./views/admin/BasicConfigView.vue'),
          props: { section: 'userRoles' },
          meta: { title: '用户角色', role: 'admin' }
        },
        {
          path: 'admin/basic/user-orgs',
          name: 'basic-user-orgs',
          component: () => import('./views/admin/BasicConfigView.vue'),
          props: { section: 'userOrgs' },
          meta: { title: '用户单位', role: 'admin' }
        },
        {
          path: 'admin/basic/menus',
          name: 'basic-menus',
          component: () => import('./views/admin/BasicConfigView.vue'),
          props: { section: 'menus' },
          meta: { title: '菜单管理', role: 'admin' }
        },
        {
          path: 'admin/basic/permissions',
          name: 'basic-permissions',
          component: () => import('./views/admin/BasicConfigView.vue'),
          props: { section: 'permissions' },
          meta: { title: '权限管理', role: 'admin' }
        },
        {
          path: 'admin/basic/role-permissions',
          name: 'basic-role-permissions',
          component: () => import('./views/admin/BasicConfigView.vue'),
          props: { section: 'rolePermissions' },
          meta: { title: '角色权限', role: 'admin' }
        },
        {
          path: 'admin/basic/dict-items',
          name: 'basic-dict-items',
          component: () => import('./views/admin/BasicConfigView.vue'),
          props: { section: 'dictItems' },
          meta: { title: '字典配置', role: 'admin' }
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
          path: 'admin/data-prepare',
          name: 'data-prepare',
          component: () => import('./views/admin/DataPrepareView.vue'),
          meta: { title: '批次准备', roles: ['admin', 'teacher'] }
        },
        {
          path: 'admin/data-prepare/systems',
          name: 'data-prepare-systems',
          component: () => import('./views/admin/DataPrepareSystemsView.vue'),
          meta: { title: '平台接入', roles: ['admin', 'teacher'] }
        },
        {
          path: 'admin/data-prepare/modules',
          name: 'data-prepare-modules',
          component: () => import('./views/admin/DataPrepareModulesView.vue'),
          meta: { title: '业务模块', roles: ['admin', 'teacher'] }
        },
        {
          path: 'admin/data-prepare/templates',
          name: 'data-prepare-templates',
          component: () => import('./views/admin/DataPrepareTemplatesView.vue'),
          meta: { title: '模板管理', roles: ['admin', 'teacher'] }
        },
        {
          path: 'admin/data-prepare/strategies',
          name: 'data-prepare-strategies',
          component: () => import('./views/admin/DataPrepareStrategiesView.vue'),
          meta: { title: '策略管理', roles: ['admin', 'teacher'] }
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
          meta: { title: '评阅反馈', role: 'teacher' }
        },
        {
          path: 'teacher/data-prepare',
          name: 'teacher-data-prepare',
          component: () => import('./views/admin/DataPrepareView.vue'),
          meta: { title: '批次准备', role: 'teacher' }
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

router.beforeEach(async (to) => {
  const session = authApi.getSession();
  if (to.meta.public || to.path === '/platforms') {
    return session && to.path === '/login' ? '/platforms' : true;
  }
  if (!session) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    };
  }
  const store = useTrainingStore();
  const runtimeContextStore = useRuntimeContextStore();
  const sessionRole = authApi.getPortalRole(session);
  if (store.state.currentRole !== sessionRole) {
    store.setRole(sessionRole);
    runtimeContextStore.resetRuntimeContext();
  }
  if (runtimeContextStore.state.context?.user.userId !== session.user.userId) {
    runtimeContextStore.resetRuntimeContext();
  }
  const requiredRole = (to.meta.roles ?? to.meta.role) as
    | PortalRole
    | PortalRole[]
    | undefined;
  if (!authApi.canAccess(requiredRole, session)) {
    return {
      name: 'platforms',
      query: { access: 'denied', target: to.fullPath }
    };
  }
  await runtimeContextStore.loadRuntimeContext();
  if (!runtimeContextStore.canAccessRoute(to.path)) {
    return {
      name: 'platforms',
      query: { access: 'denied', target: to.fullPath }
    };
  }
  return true;
});

router.afterEach((to) => {
  const pageTitle = String(to.meta.title ?? '业务实训平台');
  document.title = `${pageTitle} - 业务实训平台`;
});
