import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import { createTrainingStore } from '../src/stores/trainingStore';
import { createMockTrainingState } from '../src/data/mockSeed';
import {
  createMemoryStorage,
  TRAINING_STORAGE_KEY
} from '../src/services/trainingApi';

const source = (path: string) =>
  readFileSync(resolve(process.cwd(), path), 'utf8');

describe('业务平台维护、教案绑定与录制加载', () => {
  it('自动迁移升级前没有业务平台字段的浏览器数据', () => {
    const legacy = createMockTrainingState() as unknown as Record<string, unknown>;
    delete legacy.businessPlatforms;
    for (const lesson of legacy.lessons as Array<Record<string, unknown>>) {
      delete lesson.businessPlatformId;
      delete lesson.businessPlatformModuleId;
    }
    const storage = createMemoryStorage({
      [TRAINING_STORAGE_KEY]: JSON.stringify(legacy)
    });

    const store = createTrainingStore({ storage });

    expect(store.state.businessPlatforms.length).toBeGreaterThan(0);
    expect(
      store.state.lessons.every((lesson) => Boolean(lesson.businessPlatformId))
    ).toBe(true);
    expect(
      store.state.businessPlatforms.every((platform) =>
        Array.isArray(platform.modules)
      )
    ).toBe(true);
    expect(
      store.state.lessons.every((lesson) =>
        Boolean(lesson.businessPlatformModuleId)
      )
    ).toBe(true);
  });

  it('自动迁移已有平台但尚无下属模块的浏览器数据', () => {
    const legacy = createMockTrainingState() as unknown as Record<string, unknown>;
    for (const platform of legacy.businessPlatforms as Array<
      Record<string, unknown>
    >) {
      delete platform.modules;
    }
    for (const lesson of legacy.lessons as Array<Record<string, unknown>>) {
      delete lesson.businessPlatformModuleId;
    }
    const storage = createMemoryStorage({
      [TRAINING_STORAGE_KEY]: JSON.stringify(legacy)
    });

    const store = createTrainingStore({ storage });

    expect(
      store.state.businessPlatforms.every(
        (platform) => platform.modules.length > 0
      )
    ).toBe(true);
    expect(
      store.state.lessons.every((lesson) =>
        Boolean(
          store.getBusinessPlatformModule(
            lesson.businessPlatformId,
            lesson.businessPlatformModuleId
          )
        )
      )
    ).toBe(true);
  });

  it('管理员可以维护业务平台，已绑定的平台不能直接删除', () => {
    let sequence = 0;
    const store = createTrainingStore({
      storage: createMemoryStorage(),
      now: () => '2026-07-27T10:00:00.000Z',
      idFactory: (prefix) => `${prefix}-test-${++sequence}`
    });

    const platform = store.createBusinessPlatform({
      code: 'CRM',
      name: '客户关系业务平台',
      baseUrl: 'https://crm.example.test',
      description: '客户业务实训'
    });
    const businessModule = store.createBusinessPlatformModule(platform.id, {
      code: 'CUSTOMER_APPROVAL',
      name: '客户审批',
      path: '/customer-approval',
      description: '客户业务审批'
    });
    const lesson = store.createLesson({
      code: 'CRM-LESSON',
      title: '客户业务教案',
      moduleName: '客户管理',
      businessPlatformId: platform.id,
      businessPlatformModuleId: businessModule.id
    });

    expect(lesson.businessPlatformId).toBe(platform.id);
    expect(lesson.businessPlatformModuleId).toBe(businessModule.id);
    expect(store.getBusinessPlatform(platform.id)?.baseUrl).toBe(
      'https://crm.example.test'
    );
    expect(() => store.removeBusinessPlatform(platform.id)).toThrow(
      '已被教案使用'
    );
    expect(() =>
      store.removeBusinessPlatformModule(platform.id, businessModule.id)
    ).toThrow('已被教案使用');
  });

  it('业务平台停用后不允许新教案继续绑定', () => {
    const store = createTrainingStore({ storage: createMemoryStorage() });
    const platform = store.state.businessPlatforms[0];

    store.updateBusinessPlatform(platform.id, { status: 'DISABLED' });

    expect(() =>
      store.createLesson({
        title: '不可绑定平台的教案',
        businessPlatformId: platform.id
      })
    ).toThrow('请选择已启用的业务平台');
  });

  it('后台提供业务平台维护页和管理员专属路由', () => {
    const router = source('src/router.ts');
    const view = source('src/views/admin/BusinessPlatformManagementView.vue');

    expect(router).toContain("name: 'business-platforms'");
    expect(router).toContain("meta: { title: '业务平台管理', role: 'admin' }");
    expect(view).toContain('store.createBusinessPlatform');
    expect(view).toContain('store.updateBusinessPlatform');
    expect(view).toContain('store.removeBusinessPlatform');
    expect(view).toContain('store.createBusinessPlatformModule');
    expect(view).toContain('store.updateBusinessPlatformModule');
    expect(view).toContain('store.removeBusinessPlatformModule');
    expect(view).toContain('platform.modules');
    expect(view).toContain('新增模块');
  });

  it('新建教案必须选择已启用业务平台', () => {
    const list = source('src/views/admin/LessonListView.vue');

    expect(list).toContain('enabledBusinessPlatforms');
    expect(list).toContain('createForm.businessPlatformId');
    expect(list).toContain('createForm.businessPlatformModuleId');
    expect(list).toContain('enabledBusinessPlatformModules');
    expect(list).toContain('请选择录制时打开的业务平台');
    expect(list).toContain('请选择录制时进入的平台模块');
    expect(list).not.toContain('createForm.objectiveMaxScore');
    expect(list).not.toContain('createForm.subjectiveMaxScore');
  });

  it('录制工作区按教案绑定地址加载外部业务平台', () => {
    const editor = source('src/views/admin/LessonEditorView.vue');

    expect(editor).toContain('effectiveBusinessPlatformUrl');
    expect(editor).toContain('businessPlatformModule');
    expect(editor).toContain('configured-business-frame');
    expect(editor).toContain('useEmbeddedBusinessSimulation');
    expect(editor).toContain('单独打开业务模块');
    expect(editor).toContain("payload.type !== 'BUSINESS_ACTION'");
    expect(editor).toContain("window.addEventListener('message'");
  });
});
