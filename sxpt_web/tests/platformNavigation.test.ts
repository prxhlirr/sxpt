import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const source = (path: string) =>
  readFileSync(resolve(process.cwd(), path), 'utf8');

describe('登录后的平台入口与身份导航', () => {
  it('沉浸式业务路由隐藏平台顶栏并让内容占满视口', () => {
    const shell = source('src/layouts/AppShell.vue');
    const styles = source('src/styles.css');

    expect(shell).toContain('<header class="topbar">');
    expect(shell).toContain("'app-shell--immersive': immersiveRoute");
    expect(styles).toContain('.app-shell--immersive > .app-workspace > .topbar');
    expect(styles).toContain('.app-shell--immersive .app-content--immersive');
  });

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
    const api = source('src/services/trainingApi.ts');

    expect(portal).toContain('import.meta.env.DEV');
    expect(portal).toContain('store.initializeAuthenticatedWorkspace(session)');
    expect(portal).toContain('authApi.useDevelopmentSession(role)');
    expect(api).toContain('api/v1/auth/login');
    expect(api).toContain('DEVELOPMENT_LOGIN_PROFILES');
    expect(api).toContain("username: 'teacher02'");
    expect(api).toContain("username: 'teacher01'");
    expect(api).toContain("username: 'student01'");
    expect(api).not.toContain('token: `dev-${role}-token`');
    expect(api).not.toContain('dev-${role}');
    expect(portal).toContain("enterDevelopmentPortal('admin')");
    expect(portal).toContain("enterDevelopmentPortal('teacher')");
    expect(portal).toContain("enterDevelopmentPortal('student')");
    expect(portal).toContain('开发环境快捷入口');
  });

  it('业务接口遇到未授权时清理会话并回到登录页', () => {
    const api = source('src/services/trainingApi.ts');

    expect(api).toContain('response.status === 401');
    expect(api).toContain('handleUnauthorizedSession()');
    expect(api).toContain('登录已失效，请重新登录');
    expect(api).toContain('window.location.assign(`/login?redirect=');
  });

  it('全局右上角提供演示用户切换并按新身份刷新菜单上下文', () => {
    const shell = source('src/layouts/AppShell.vue');

    expect(shell).toContain('class="account-switcher"');
    expect(shell).toContain('usersApi.listUsers');
    expect(shell).toContain('usersApi.listOrgs');
    expect(shell).toContain('usersApi.listUserOrgs');
    expect(shell).toContain('VITE_DEMO_SWITCH_PASSWORD');
    expect(shell).toContain('authApi.login({');
    expect(shell).toContain('ref="accountSwitcherRoot"');
    expect(shell).toContain("document.addEventListener('pointerdown', closeAccountMenuOnOutsideClick)");
    expect(shell).toContain("document.removeEventListener('pointerdown', closeAccountMenuOnOutsideClick)");
    expect(shell).toContain('function closeAccountMenuOnOutsideClick(event: PointerEvent)');
    expect(shell).toContain('accountMenuOpen.value = false');
    expect(shell).toContain('store.initializeAuthenticatedWorkspace(session)');
    expect(shell).toContain('runtimeContextStore.resetRuntimeContext()');
    expect(shell).toContain('runtimeContextStore.loadRuntimeContext({ force: true })');
    expect(shell).toContain('router.replace(authApi.getHomePath(session))');
    expect(shell).not.toContain('class="environment-badge"');
    expect(shell).not.toContain('class="icon-button"');
    expect(shell).not.toContain('class="today"');
  });

  it('路由按身份拦截后台和各端页面', () => {
    const router = source('src/router.ts');

    expect(router).toContain('router.beforeEach');
    expect(router).toContain("if (to.path === '/login')");
    expect(router).toContain('authApi.logout()');
    expect(router).toContain('clearAuthenticatedWorkspace()');
    expect(router).toContain('resetRuntimeContext()');
    expect(router).toContain('authApi.canAccess(requiredRole, session)');
    expect(router).toContain('authApi.getPortalRole(session)');
    expect(router).toContain("roles: ['admin', 'teacher']");
  });

  it('教师侧栏提供教学工作台和批次准备，管理员侧栏保留数据准备二级入口', () => {
    const shell = source('src/layouts/AppShell.vue');

    expect(shell).toContain("{ label: '教学工作台'");
    expect(shell).toContain("{ label: '批次准备'");
    expect(shell).toContain("{ label: '平台接入'");
    expect(shell).toContain("{ label: '业务模块'");
    expect(shell).toContain("{ label: '模板管理'");
    expect(shell).toContain("{ label: '策略管理'");
    expect(shell).toContain("'lesson-editor'");
    expect(shell).toContain('hiddenNavigationMenuCodes.has(menu.menuCode)');
    expect(shell).not.toContain("label: '教案编排'");
    expect(shell).not.toContain("label: '考试设置'");
    expect(shell).not.toContain("label: '分组设置'");
    expect(shell).not.toContain("label: '考试数据'");
    expect(shell).not.toContain("label: '发布中心'");
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

  it('业务模块详情提供原平台字典维护入口并服务参与方下拉', () => {
    const modules = source('src/views/admin/DataPrepareModulesView.vue');

    expect(modules).toContain("type ModuleDetailTab = 'info' | 'chain' | 'dictionary'");
    expect(modules).toContain('openOriginDictionaryTab');
    expect(modules).toContain('原平台字典');
    expect(modules).toContain('openCreateOriginOrgForm');
    expect(modules).toContain('openCreateOriginRoleForm');
    expect(modules).toContain('dataPrepareApi.createOriginOrg');
    expect(modules).toContain('dataPrepareApi.createOriginRole');
    expect(modules).toContain('dataPrepareApi.listOriginOrgs');
    expect(modules).toContain('dataPrepareApi.listOriginRoles');
    expect(modules).toContain('v-for="org in originOrgs"');
    expect(modules).toContain('v-for="role in originRoles"');
  });
});
