import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const source = (path: string) =>
  readFileSync(resolve(process.cwd(), path), 'utf8');

describe('登录后的平台入口与身份导航', () => {
  it('根入口先进入双平台选择页，并支持配置精品课堂外链', () => {
    const router = source('src/router.ts');
    const portal = source('src/views/PlatformPortalView.vue');

    expect(router).toContain("path: '/platforms'");
    expect(router).toContain("redirect: '/platforms'");
    expect(portal).toContain('精品课堂');
    expect(portal).toContain('实训平台');
    expect(portal).toContain('VITE_PREMIUM_CLASSROOM_URL');
  });

  it('实训平台按照登录身份进入管理员、教师或学生首页', () => {
    const portal = source('src/views/PlatformPortalView.vue');

    expect(portal).toContain("destination: '/admin/overview'");
    expect(portal).toContain("destination: '/teacher/dashboard'");
    expect(portal).toContain("destination: '/student/tasks'");
  });

  it('仅在开发环境提供管理员、教师和学生身份快捷入口', () => {
    const portal = source('src/views/PlatformPortalView.vue');

    expect(portal).toContain('import.meta.env.DEV');
    expect(portal).toContain('store.setRole(role)');
    expect(portal).toContain("enterDevelopmentPortal('admin')");
    expect(portal).toContain("enterDevelopmentPortal('teacher')");
    expect(portal).toContain("enterDevelopmentPortal('student')");
    expect(portal).toContain('开发环境快捷入口');
  });

  it('路由按身份拦截后台和各端页面', () => {
    const router = source('src/router.ts');

    expect(router).toContain('router.beforeEach');
    expect(router).toContain('allowedRoles.includes(role)');
    expect(router).toContain("roles: ['admin', 'teacher']");
  });

  it('教师侧栏提供录制教案，管理员侧栏不再展开五步配置菜单', () => {
    const shell = source('src/layouts/AppShell.vue');

    expect(shell).toContain("{ label: '录制教案'");
    expect(shell).not.toContain("{ label: '考试设置'");
    expect(shell).not.toContain("{ label: '分组设置'");
    expect(shell).not.toContain("{ label: '考试数据'");
    expect(shell).not.toContain("{ label: '发布中心'");
    expect(shell).not.toContain('switchRole');
  });

  it('教案管理操作中集中提供完整业务配置入口', () => {
    const list = source('src/views/admin/LessonListView.vue');

    expect(list).toContain('业务配置');
    expect(list).toContain("name: 'lesson-editor'");
    expect(list).toContain("name: 'exam-setup'");
    expect(list).toContain("name: 'group-setup'");
    expect(list).toContain("name: 'exam-data'");
    expect(list).toContain("name: 'publish-center'");
  });
});
