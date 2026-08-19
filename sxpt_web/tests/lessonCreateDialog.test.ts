import { renderToString } from '@vue/server-renderer';
import { createSSRApp } from 'vue';
import {
  createMemoryHistory,
  createRouter,
  type RouteRecordRaw
} from 'vue-router';
import { describe, expect, it } from 'vitest';
import LessonListView from '../src/views/admin/LessonListView.vue';

const EmptyRoute = { template: '<div />' };
const routes: RouteRecordRaw[] = [
  { path: '/', name: 'lesson-list', component: EmptyRoute },
  {
    path: '/lessons/:lessonId/edit',
    name: 'lesson-editor',
    component: EmptyRoute
  },
  {
    path: '/lessons/:lessonId/recording',
    name: 'lesson-recording',
    component: EmptyRoute
  },
  {
    path: '/lessons/:lessonId/publish',
    name: 'publish-center',
    component: EmptyRoute
  }
];

async function renderLessonList() {
  const router = createRouter({ history: createMemoryHistory(), routes });
  await router.push('/');
  await router.isReady();

  const app = createSSRApp(LessonListView);
  app.use(router);
  return renderToString(app);
}

describe('新增教案弹框', () => {
  it('按基本信息、业务场景和数据生成方式的顺序展示单页创建流程', async () => {
    const html = await renderLessonList();
    const dialogHtml = html.slice(html.indexOf('<dialog'));

    const basicInfoIndex = dialogHtml.indexOf('01 基本信息');
    const businessContextIndex = dialogHtml.indexOf('02 业务场景');
    const generationSourceIndex = dialogHtml.indexOf('03 数据生成方式');

    expect(basicInfoIndex).toBeGreaterThan(-1);
    expect(businessContextIndex).toBeGreaterThan(basicInfoIndex);
    expect(generationSourceIndex).toBeGreaterThan(businessContextIndex);
    expect(dialogHtml).toContain('快速完成教案基础配置，创建后即可进入编排');
  });
});
