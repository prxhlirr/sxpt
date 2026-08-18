import { createSSRApp, type Component } from 'vue';
import { renderToString } from '@vue/server-renderer';
import {
  createMemoryHistory,
  createRouter,
  type RouteRecordRaw
} from 'vue-router';
import { describe, expect, it } from 'vitest';
import AdminOverviewView from '../src/views/admin/AdminOverviewView.vue';
import StudentResultsView from '../src/views/student/StudentResultsView.vue';
import StudentTaskRunnerView from '../src/views/student/StudentTaskRunnerView.vue';
import StudentTasksView from '../src/views/student/StudentTasksView.vue';
import TeacherDashboardView from '../src/views/teacher/TeacherDashboardView.vue';
import TeacherReviewView from '../src/views/teacher/TeacherReviewView.vue';
import { useTrainingStore } from '../src/stores/trainingStore';

async function renderRoute(
  component: Component,
  path: string,
  routePath = path
) {
  const routes: RouteRecordRaw[] = [
    { path: routePath, component },
    {
      path: '/:pathMatch(.*)*',
      component: { template: '<span />' }
    }
  ];
  const router = createRouter({ history: createMemoryHistory(), routes });
  const app = createSSRApp(component);
  app.use(router);
  await router.push(path);
  await router.isReady();
  return renderToString(app);
}

describe('three-portal workflow rendering', () => {
  it('renders the admin five-step delivery chain', async () => {
    const html = await renderRoute(AdminOverviewView, '/admin/overview');
    expect(html).toContain('实训平台运营总览');
    expect(html).toContain('教案编排');
    expect(html).toContain('动态分组');
    expect(html).toContain('考试数据');
    expect(html).toContain('发布任务');
  });

  it('renders teacher monitoring with platform evidence language', async () => {
    const html = await renderRoute(
      TeacherDashboardView,
      '/teacher/dashboard'
    );
    expect(html).toContain('考试全过程监控');
    expect(html).toContain('角色组进度');
    expect(html).toContain('学员实时状态');
  });

  it('renders subjective review and objective score separately', async () => {
    const html = await renderRoute(TeacherReviewView, '/teacher/review');
    expect(html).toContain('主观评分与反馈');
    expect(html).toContain('系统客观分');
    expect(html).toContain('教师主观分');
    expect(html).toContain('result-archive-card');
    expect(html).toContain('开始评阅');
    expect(html).toContain('课程切换');
    expect(html).toContain('全部课程');
  });

  it('renders student tasks with multi-role assignments', async () => {
    const html = await renderRoute(StudentTasksView, '/student/tasks');
    expect(html).toContain('我的业务角色');
    expect(html).toContain('经办组');
    expect(html).toContain('归档组');
  });

  it('merges learning and practice assignments into one lesson card', async () => {
    const store = useTrainingStore();
    const originalTasks = [...store.state.studentTasks];
    const baseTask = originalTasks[0];
    expect(baseTask).toBeTruthy();
    if (!baseTask) return;
    const trainingBase = {
      ...baseTask,
      studentId: baseTask.studentId,
      lessonId: baseTask.lessonId,
      status: 'TODO' as const,
      currentStageIndex: 0,
      completedStageIds: [],
      completedPracticeStepIds: [],
      practiceStepResults: [],
      objectiveScore: undefined,
      subjectiveScore: undefined,
      submittedAt: undefined,
      gradedAt: undefined
    };
    store.state.studentTasks.unshift(
      {
        ...trainingBase,
        id: 'merged-practice-task',
        publishedTaskId: 'merged-practice-publication',
        title: '合并展示教案｜流程练习',
        mode: 'PRACTICE'
      },
      {
        ...trainingBase,
        id: 'merged-learning-task',
        publishedTaskId: 'merged-learning-publication',
        title: '合并展示教案｜流程学习',
        mode: 'LEARNING'
      }
    );

    try {
      const html = await renderRoute(StudentTasksView, '/student/tasks');
      expect(html.split('course-card"').length - 1).toBe(1);
      expect(html).toContain('学习任务');
      expect(html).toContain('练习任务');
      expect(html).toContain('开始学习');
      expect(html).toContain('开始练习');
      expect(html).not.toContain('MY TRAINING DESK');
    } finally {
      store.state.studentTasks.splice(
        0,
        store.state.studentTasks.length,
        ...originalTasks
      );
    }
  });

  it('renders the seeded exam with only a hideable task description overlay', async () => {
    const html = await renderRoute(
      StudentTaskRunnerView,
      '/student/tasks/student-task-east-handler',
      '/student/tasks/:taskId'
    );
    expect(html).toContain('业务系统演示窗口');
    expect(html).toContain('考试任务说明');
    expect(html).toContain('隐藏说明');
    expect(html).not.toContain('当前判定口径');
    expect(html).not.toContain('访问目标页面');
  });

  it('renders combined results and the evidence boundary', async () => {
    const html = await renderRoute(StudentResultsView, '/student/results');
    expect(html).toContain('成绩与教师反馈');
    expect(html).toContain('客观 + 主观');
    expect(html).toContain('无法可靠绑定具体业务数据');
    expect(html).toContain('result-archive-card');
    expect(html).toContain('全部成绩');
    expect(html).toContain('课程切换');
    expect(html).toContain('选择课程');
  });
});
