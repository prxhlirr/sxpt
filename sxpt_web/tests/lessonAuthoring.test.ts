import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const fromRoot = (path: string) => resolve(process.cwd(), path);
const source = (path: string) => readFileSync(fromRoot(path), 'utf8');

describe('后台教案编排页面', () => {
  it('提供教案列表、编排工作台和录制回看三个路由页面源码', () => {
    [
      'src/views/admin/LessonListView.vue',
      'src/views/admin/LessonEditorView.vue',
      'src/views/admin/RecordingPreviewView.vue'
    ].forEach((path) => expect(existsSync(fromRoot(path)), path).toBe(true));
  });

  it('教案列表支持筛选、指标、创建、复制、查看、编排和发布', () => {
    const list = source('src/views/admin/LessonListView.vue');

    expect(list).toContain('store.createLesson');
    expect(list).toContain('store.duplicateLesson');
    expect(list).toContain('store.publishLesson');
    expect(list).toContain('查看录制');
    expect(list).toContain('教案编排');
    expect(list).toContain('业务模块');
    expect(list).toContain('状态筛选');
  });

  it('业务配置菜单使用顶层悬浮定位，不撑高或裁切表格', () => {
    const list = source('src/views/admin/LessonListView.vue');

    expect(list).toContain('<Teleport to="body">');
    expect(list).toContain('class="lesson-actions-popover"');
    expect(list).toContain('position: fixed');
    expect(list).toContain('z-index: 3000');
    expect(list).toContain('getBoundingClientRect()');
    expect(list).not.toContain('<details class="lesson-actions-menu">');
  });

  it('编排阶段来自 stages 动态循环，且支持完整阶段操作和字段配置', () => {
    const editor = source('src/views/admin/LessonEditorView.vue');

    expect(editor).toMatch(/v-for="[^"]*stage[^"]*stages/);
    expect(editor).toContain('store.addStage');
    expect(editor).toContain('store.updateStage');
    expect(editor).toContain('store.removeStage');
    expect(editor).toContain('store.moveStage');
    expect(editor).toContain('groupKey');
    expect(editor).toContain('completionMethod');
    expect(editor).toContain('visibility');
    expect(editor).not.toMatch(/阶段\s*[AB]\b/);
  });

  it('展示录制页面、动作、选择器、时长与逐步讲解信息', () => {
    const editor = source('src/views/admin/LessonEditorView.vue');
    const preview = source('src/views/admin/RecordingPreviewView.vue');
    const playback = source('src/components/lesson/LessonPlaybackPlayer.vue');

    ['pageTitle', 'actionLabel', 'selector', 'durationSeconds'].forEach((field) => {
      expect(editor).toContain(field);
      expect(playback).toContain(field);
    });
    expect(playback).toContain('上一步');
    expect(playback).toContain('下一步');
    expect(preview).toContain('<LessonPlaybackPlayer');
    expect(preview).toContain("'返回编辑'");
  });

  it('录制时使用可收起抽屉和边缘工具坞，元素选择提示可换角落', () => {
    const editor = source('src/views/admin/LessonEditorView.vue');

    expect(editor).toContain('const showStagePanel = ref(false)');
    expect(editor).toContain('toggleStagePanel');
    expect(editor).toContain('toggleConfigPanel');
    expect(editor).toContain('closeAuthoringDrawers();');
    expect(editor).toContain('aria-label="编排快捷工具坞"');
    expect(editor).toContain('cyclePickerToolbarPosition');
    expect(editor).toContain('换个角落');
  });

  it('发布中心可直接进入教师讲解并返回发布中心', () => {
    const publishCenter = source('src/views/admin/PublishCenterView.vue');
    const preview = source('src/views/admin/RecordingPreviewView.vue');

    expect(publishCenter).toContain('function startLecture()');
    expect(publishCenter).toContain("name: 'lesson-recording'");
    expect(publishCenter).toContain("query: { from: 'publish' }");
    expect(publishCenter).toContain('@click="startLecture"');
    expect(publishCenter).toContain('点击开始讲解');
    expect(preview).toContain("route.query.from === 'publish'");
    expect(preview).toContain("'返回发布中心'");
  });

  it('已完成过讲解的教案再次进入时仍可切换教学点和节点', () => {
    const preview = source('src/views/admin/RecordingPreviewView.vue');

    expect(preview).toContain('@next="move(1)"');
    expect(preview).toContain('@select-step="selectStep"');
    expect(preview).toContain('@select-stage="selectTeachingPoint"');
    expect(preview).not.toContain(':action-disabled="Boolean(lesson.lectureCompletedAt)"');
  });

  it('编排页调用基础信息保存和发布，并提供明显的考试设置入口', () => {
    const editor = source('src/views/admin/LessonEditorView.vue');

    expect(editor).toContain('store.updateLesson');
    expect(editor).toContain('store.publishLesson');
    expect(editor).toContain('发布校验');
    expect(editor).toContain('下一步：考试设置');
    expect(editor).toContain("name: 'exam-setup'");
  });

  it('直接进入编排页时同步后端平台和业务模块', () => {
    const editor = source('src/views/admin/LessonEditorView.vue');

    expect(editor).toContain('await store.syncBusinessPlatforms()');
    expect(editor).toContain('basicForm.businessPlatformModuleId');
  });
});
