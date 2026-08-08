import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createDefaultBusinessPlatforms } from '../data/mockSeed';
import type { RecordedStep } from '../domain/models';
import {
  MAX_RECORDED_STEP_SNAPSHOT_JSON_LENGTH,
  backendTrainingApi,
  buildPublishedDataPrepareSnapshot,
  distributedScore,
  findOrganizationByIdOrCode,
  mapBusinessModule,
  mapConnectorSystem,
  mapPracticeStepActionType,
  mapStepActionType,
  safeCode,
  serializeRecordedStepSnapshot,
  toLocalDateTime
} from './backendTrainingApi';

const connectorApiMock = vi.hoisted(() => ({
  listSystems: vi.fn(),
  createSystem: vi.fn(),
  disableSystem: vi.fn(),
  updateSystem: vi.fn(),
  enableSystem: vi.fn()
}));

const dataPrepareApiMock = vi.hoisted(() => ({
  listActiveBusinessModules: vi.fn(),
  listBusinessModules: vi.fn(),
  listAllBusinessModules: vi.fn(),
  listActiveStrategies: vi.fn(),
  listTemplates: vi.fn(),
  listActiveTemplatesByModuleScene: vi.fn(),
  createRequirement: vi.fn(),
  prepareAndExecute: vi.fn(),
  listPools: vi.fn(),
  acquireDataInstance: vi.fn()
}));

vi.mock('../api/connector', () => ({
  connectorApi: connectorApiMock
}));

vi.mock('./trainingApi', () => ({
  dataPrepareApi: dataPrepareApiMock
}));

describe('后端训练接口映射', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    dataPrepareApiMock.listAllBusinessModules.mockResolvedValue([]);
  });

  it('把后端 ACTIVE 平台映射为前端 ENABLED 并保留本地模块', () => {
    const mapped = mapConnectorSystem(
      {
        id: 'server-platform',
        tenantId: 'tenant-1',
        systemCode: 'PURCHASE',
        systemName: '采购平台',
        systemType: 'BUSINESS',
        baseUrl: 'https://business.example.com',
        authType: 'NONE',
        status: 'ACTIVE',
        createTime: '2026-07-28T10:00:00',
        updateTime: '2026-07-28T11:00:00'
      },
      {
        id: 'local-platform',
        code: 'PURCHASE',
        name: '旧名称',
        baseUrl: 'internal://purchase',
        description: '本地维护的说明',
        status: 'ENABLED',
        updatedAt: '2026-07-27T10:00:00',
        modules: [
          {
            id: 'module-1',
            code: 'APPROVAL',
            name: '采购审批',
            path: '/approval',
            description: '',
            status: 'ENABLED',
            updatedAt: '2026-07-27T10:00:00'
          }
        ]
      }
    );

    expect(mapped).toMatchObject({
      id: 'server-platform',
      code: 'PURCHASE',
      name: '采购平台',
      status: 'ENABLED',
      description: '本地维护的说明'
    });
    expect(mapped.modules).toHaveLength(1);
  });

  it('同步已接入平台时读取后端业务模块，使 OA 可以用于教案编排', async () => {
    connectorApiMock.listSystems.mockResolvedValue([
      {
        id: 'origin-oa-system',
        tenantId: 'demo-tenant',
        systemCode: 'OA_DEMO',
        systemName: 'OA 协同办公系统',
        systemType: 'OA',
        baseUrl: 'http://127.0.0.1:5174/oa',
        authType: 'LAUNCH_TOKEN',
        status: 'ACTIVE',
        createTime: '2026-08-04T10:00:00',
        updateTime: '2026-08-04T11:00:00'
      }
    ]);
    dataPrepareApiMock.listAllBusinessModules.mockResolvedValue([
      {
        id: 'module-oa-approval',
        tenantId: 'demo-tenant',
        connectorSystemId: 'origin-oa-system',
        moduleCode: 'OA_APPROVAL',
        moduleName: 'OA 审批',
        entryUrl: '/oa/approvals',
        remark: 'OA 审批流程',
        status: 'ACTIVE',
        updateTime: '2026-08-04T11:00:00'
      }
    ]);

    const platforms = await backendTrainingApi.listBusinessPlatforms([]);

    expect(dataPrepareApiMock.listAllBusinessModules).toHaveBeenCalledWith({
      tenantId: 'demo-tenant',
      connectorSystemId: 'origin-oa-system'
    });
    expect(platforms).toEqual([
      expect.objectContaining({
        code: 'OA_DEMO',
        status: 'ENABLED',
        modules: [
          expect.objectContaining({
            id: 'module-oa-approval',
            code: 'OA_APPROVAL',
            path: '/oa/approvals',
            status: 'ENABLED'
          })
        ]
      })
    ]);
  });

  it('把停用的后端业务模块映射为不可选模块', () => {
    expect(
      mapBusinessModule({
        id: 'module-disabled',
        tenantId: 'demo-tenant',
        connectorSystemId: 'origin-oa-system',
        moduleCode: 'OA_ARCHIVE',
        moduleName: 'OA 归档',
        entryUrl: '/oa/archive',
        status: 'DISABLED'
      })
    ).toMatchObject({
      code: 'OA_ARCHIVE',
      path: '/oa/archive',
      status: 'DISABLED'
    });
  });

  it('发布任务时按不区分大小写的组织编码复用已有班级', () => {
    const existing = findOrganizationByIdOrCode(
      [
        {
          id: 'org-1',
          orgCode: 'DEMO-CLASS'
        }
      ],
      'demo-class',
      'DEMO-CLASS'
    );

    expect(existing?.id).toBe('org-1');
  });

  it('长节点编码保留稳定哈希后缀，避免前缀相同的步骤发生碰撞', () => {
    const first = safeCode(
      'PRACTICE-record-stage-9b0a7b17-3e46-44e7-8e3a-15eb4da133de-1785399069998',
      64
    );
    const second = safeCode(
      'PRACTICE-record-stage-9b0a7b17-3e46-44e7-8e3a-15eb4da133de-1785399070798',
      64
    );

    expect(first).toHaveLength(64);
    expect(second).toHaveLength(64);
    expect(first).not.toBe(second);
    expect(safeCode('demo-class', 64)).toBe('DEMO-CLASS');
  });

  it.each([
    ['click', 'CLICK'],
    ['submit', 'CLICK'],
    ['input', 'INPUT'],
    ['select', 'SELECT'],
    ['guide', 'VERIFY_STATE']
  ] as const)('把 %s 步骤转换为后端 %s 事件', (actionType, expected) => {
    expect(
      mapStepActionType({ actionType } as Pick<RecordedStep, 'actionType'> as RecordedStep)
    ).toBe(expected);
  });

  it('练习中的元素说明节点按点击轨迹上报', () => {
    expect(
      mapPracticeStepActionType({
        kind: 'guide',
        actionType: 'guide',
        selector: '#query'
      } as RecordedStep)
    ).toBe('CLICK');
  });

  it('按 LocalDateTime 格式发送时间，不携带时区后缀', () => {
    expect(toLocalDateTime(new Date('2026-07-28T12:34:56.789Z'))).toBe(
      '2026-07-28T12:34:56'
    );
  });

  it('把评价总分精确分配到所有步骤且不丢失小数余数', () => {
    const scores = Array.from({ length: 3 }, (_, index) =>
      distributedScore(10, 3, index)
    );

    expect(scores).toEqual([3.34, 3.33, 3.33]);
    expect(scores.reduce((total, score) => total + score, 0)).toBe(10);
  });

  it('允许同步超过旧版 32 KB 限制的真实业务页面快照', () => {
    const snapshotJson = serializeRecordedStepSnapshot({
      id: 'record-large-snapshot',
      title: '提交采购审批',
      pageTitle: '采购审批',
      actionLabel: '提交',
      selector: '[data-action="submit"]',
      selectorCandidates: [],
      durationSeconds: 8,
      note: '提交采购审批',
      pageSnapshot: {
        version: 1,
        format: 'DOM',
        pageUrl: 'internal://purchase/approval',
        pageTitle: '采购审批',
        capturedAt: '2026-07-29T00:00:00.000Z',
        html: `<main>${'业务内容'.repeat(12_000)}</main>`,
        cssText: '.page { display: grid; }',
        viewport: { width: 1440, height: 900 }
      }
    });

    expect(snapshotJson.length).toBeGreaterThan(32_768);
    expect(snapshotJson.length).toBeLessThan(
      MAX_RECORDED_STEP_SNAPSHOT_JSON_LENGTH
    );
    expect(JSON.parse(snapshotJson).pageSnapshot.html).toContain('业务内容');
  });

  it('发布数据准备快照应冻结系统模块模板策略和学生范围', () => {
    const snapshot = buildPublishedDataPrepareSnapshot(
      'tenant-1',
      'teacher-1',
      {
        id: 'lesson-1',
        code: 'lesson-code',
        title: '采购审批',
        moduleName: '采购模块',
        businessPlatformId: 'platform-1',
        businessPlatformModuleId: 'module-1',
        description: '',
        version: 3,
        status: 'PUBLISHED',
        teacherName: '王老师',
        tags: [],
        objectiveMaxScore: 80,
        subjectiveMaxScore: 20,
        updatedAt: '2026-07-30T00:00:00',
        teachingPointId: 'point-1',
        stages: [
          {
            id: 'stage-1',
            stageKey: 'submit',
            name: '提交申请',
            groupKey: 'buyer',
            description: '提交采购申请',
            required: true,
            score: 10,
            completionMethod: 'business_check',
            visibility: {
              LEARNING: true,
              PRACTICE: true,
              EXAM: true
            },
            recordedSteps: []
          }
        ]
      },
      {
        id: 'platform-1',
        code: 'PURCHASE',
        name: '采购平台',
        baseUrl: 'https://purchase.example.com',
        description: '',
        status: 'ENABLED',
        modules: [],
        updatedAt: '2026-07-30T00:00:00'
      },
      {
        id: 'module-1',
        tenantId: 'tenant-1',
        connectorSystemId: 'platform-1',
        moduleCode: 'purchase_apply',
        moduleName: '采购申请',
        entryUrl: '/apply',
        moduleType: 'BUSINESS',
        status: 'ACTIVE'
      },
      {
        id: 'template-1',
        tenantId: 'tenant-1',
        connectorSystemId: 'platform-1',
        templateCode: 'TPL_PURCHASE_PRACTICE',
        templateName: '采购练习初始数据',
        sceneType: 'PRACTICE',
        moduleCode: 'purchase_apply',
        initState: 'DRAFT',
        supportMode: 'INITIAL_ONLY',
        configJson: '{"mode":"initial"}',
        updateTime: '2026-07-30T00:00:00'
      },
      {
        id: 'strategy-1',
        tenantId: 'tenant-1',
        connectorSystemId: 'platform-1',
        businessModuleId: 'module-1',
        moduleCode: 'purchase_apply',
        moduleName: '采购申请',
        sceneType: 'PRACTICE',
        templateId: 'template-1',
        dataSourceStrategy: 'CREATE',
        sharePolicy: 'ATTEMPT_EXCLUSIVE',
        regeneratePolicy: 'ON_ATTEMPT',
        lockPolicy: 'NONE',
        strategyCode: 'STRATEGY_PURCHASE_PRACTICE',
        strategyVersion: 2
      },
      {
        id: 'published-1',
        lessonId: 'lesson-1',
        title: '采购练习',
        mode: 'PRACTICE',
        status: 'RUNNING',
        startAt: '2026-07-30T09:00:00',
        endAt: '2026-07-30T10:00:00',
        assignedCount: 1,
        groupCount: 1,
        dataCount: 0,
        completedCount: 0,
        remoteTaskId: 'task-1'
      },
      [
        {
          id: 'student-task-1',
          publishedTaskId: 'published-1',
          lessonId: 'lesson-1',
          studentId: 'student-1',
          studentName: '学生一',
          title: '采购练习',
          mode: 'PRACTICE',
          groupKey: 'buyer',
          groupKeys: ['buyer'],
          unitId: 'unit-1',
          unitName: '采购科',
          dataItemId: 'question-1',
          attemptNumber: 1,
          submissionValues: {},
          status: 'TODO',
          currentStageIndex: 0,
          completedStageIds: []
        }
      ]
    );

    expect(snapshot).toMatchObject({
      source: 'publishTeachingTask',
      snapshotVersion: 1,
      platform: { id: 'platform-1', code: 'PURCHASE' },
      module: { id: 'module-1', code: 'purchase_apply' },
      template: { id: 'template-1', code: 'TPL_PURCHASE_PRACTICE' },
      strategy: { id: 'strategy-1', version: 2 },
      publishedTask: { remoteTaskId: 'task-1', mode: 'PRACTICE' },
      studentScope: {
        totalTaskCount: 1,
        uniqueStudentCount: 1,
        unitCount: 1,
        groupKeys: ['buyer']
      }
    });
    expect(snapshot.studentScope.students[0]).toMatchObject({
      studentId: 'student-1',
      unitName: '采购科'
    });
  });

  it('同步时自动在后端补齐缺失的内置业务平台并保留模块', async () => {
    const purchase = createDefaultBusinessPlatforms()[0];
    connectorApiMock.listSystems.mockResolvedValue([
      {
        id: 'demo-connector',
        tenantId: 'demo-tenant',
        systemCode: 'DEMO',
        systemName: '演示系统',
        systemType: 'LOCAL_DEMO',
        baseUrl: 'http://localhost:5173/biz-local',
        authType: 'NONE',
        status: 'ACTIVE',
        createTime: '2026-07-28T10:00:00',
        updateTime: '2026-07-28T10:00:00'
      }
    ]);
    connectorApiMock.createSystem.mockImplementation(async (request) => ({
      id: 'purchase-server',
      tenantId: request.tenantId,
      systemCode: request.systemCode,
      systemName: request.systemName,
      systemType: request.systemType,
      baseUrl: request.baseUrl,
      authType: request.authType,
      status: 'ACTIVE',
      createTime: '2026-07-28T12:00:00',
      updateTime: '2026-07-28T12:00:00'
    }));

    const platforms = await backendTrainingApi.listBusinessPlatforms([
      purchase
    ]);

    expect(connectorApiMock.createSystem).toHaveBeenCalledWith(
      expect.objectContaining({
        tenantId: 'demo-tenant',
        systemCode: 'PURCHASE',
        baseUrl: 'internal://purchase'
      })
    );
    expect(
      JSON.parse(connectorApiMock.createSystem.mock.calls[0][0].configJson)
        .modules
    ).toHaveLength(2);
    expect(platforms.find((platform) => platform.code === 'PURCHASE')).toMatchObject(
      {
        id: 'purchase-server',
        modules: purchase.modules
      }
    );
  });

  it('发布任务自动准备数据时只读取已启用模板', async () => {
    dataPrepareApiMock.listActiveBusinessModules.mockResolvedValue([
      {
        id: 'server-module-1',
        tenantId: 'demo-tenant',
        connectorSystemId: 'platform-1',
        moduleCode: 'PURCHASE_APPLY',
        moduleName: '采购申请',
        entryUrl: '/apply',
        moduleType: 'BUSINESS',
        status: 'ACTIVE'
      }
    ]);
    dataPrepareApiMock.listActiveStrategies.mockResolvedValue([
      {
        id: 'strategy-1',
        tenantId: 'demo-tenant',
        connectorSystemId: 'platform-1',
        businessModuleId: 'server-module-1',
        moduleCode: 'PURCHASE_APPLY',
        moduleName: '采购申请',
        sceneType: 'PRACTICE',
        templateId: 'template-disabled-1',
        dataSourceStrategy: 'CREATE',
        sharePolicy: 'ATTEMPT_EXCLUSIVE',
        regeneratePolicy: 'ON_ATTEMPT',
        lockPolicy: 'NONE',
        strategyCode: 'STRATEGY_PURCHASE_PRACTICE',
        strategyVersion: 1
      }
    ]);
    dataPrepareApiMock.listActiveTemplatesByModuleScene.mockResolvedValue([
      {
        id: 'template-active-1',
        tenantId: 'demo-tenant',
        connectorSystemId: 'platform-1',
        templateCode: 'TPL_PURCHASE_PRACTICE',
        templateName: '采购练习初始数据',
        sceneType: 'PRACTICE',
        moduleCode: 'PURCHASE_APPLY',
        initState: 'DRAFT',
        supportMode: 'INITIAL_ONLY',
        configJson: '{"mode":"initial"}',
        status: 'ACTIVE'
      }
    ]);
    dataPrepareApiMock.createRequirement.mockResolvedValue({
      id: 'requirement-1'
    });
    dataPrepareApiMock.prepareAndExecute.mockResolvedValue({
      id: 'job-1',
      jobStatus: 'SUCCESS'
    });
    dataPrepareApiMock.listPools.mockResolvedValue([
      {
        id: 'pool-1',
        questionId: safeCode(
          'training-practice-lesson-32120f48-24a2-485d-bf37-c6faf4101ec6-user-demo-student-01',
          64
        ),
        poolStatus: 'READY',
        readyCount: 1
      }
    ]);
    dataPrepareApiMock.acquireDataInstance.mockResolvedValue({
      id: 'allocation-1'
    });

    const allocatedCount =
      await backendTrainingApi.prepareInitialDataForPublishedTask(
        {
          id: 'lesson-1',
          code: 'lesson-code',
          title: '采购申请练习',
          moduleName: '采购申请',
          businessPlatformId: 'platform-1',
          businessPlatformModuleId: 'local-module-1',
          description: '',
          version: 1,
          status: 'PUBLISHED',
          teacherName: 'teacher',
          tags: [],
          objectiveMaxScore: 80,
          subjectiveMaxScore: 20,
          updatedAt: '2026-07-30T00:00:00',
          teachingPointId: 'point-1',
          stages: []
        } as any,
        {
          id: 'platform-1',
          code: 'PURCHASE',
          name: '采购平台',
          baseUrl: 'https://purchase.example.com',
          description: '',
          status: 'ENABLED',
          modules: [
            {
              id: 'local-module-1',
              code: 'PURCHASE_APPLY',
              name: '采购申请',
              path: '/apply',
              description: '',
              status: 'ENABLED',
              updatedAt: '2026-07-30T00:00:00'
            }
          ],
          updatedAt: '2026-07-30T00:00:00'
        },
        {
          id: 'published-1',
          lessonId: 'lesson-1',
          title: '采购申请练习',
          mode: 'PRACTICE',
          status: 'RUNNING',
          startAt: '2026-07-30T09:00:00',
          endAt: '2026-07-30T10:00:00',
          assignedCount: 1,
          groupCount: 1,
          dataCount: 0,
          completedCount: 0,
          remoteTaskId: 'task-1'
        } as any,
        [
          {
            id: 'student-task-1',
            publishedTaskId: 'published-1',
            lessonId: 'lesson-1',
            studentId: 'student-1',
            studentName: '学生一',
            title: '采购申请练习',
            mode: 'PRACTICE',
            groupKey: 'buyer',
            groupKeys: ['buyer'],
            unitId: 'unit-1',
            unitName: '采购科',
            dataItemId:
              'training-practice-lesson-32120f48-24a2-485d-bf37-c6faf4101ec6-user-demo-student-01',
            attemptNumber: 1,
            submissionValues: {},
            status: 'TODO',
            currentStageIndex: 0,
            completedStageIds: []
          }
        ] as any
      );

    expect(allocatedCount).toBe(1);
    expect(dataPrepareApiMock.listTemplates).not.toHaveBeenCalled();
    expect(dataPrepareApiMock.listActiveTemplatesByModuleScene).toHaveBeenCalledWith({
      tenantId: 'demo-tenant',
      connectorSystemId: 'platform-1',
      moduleCode: 'PURCHASE_APPLY',
      sceneType: 'PRACTICE'
    });
    expect(dataPrepareApiMock.createRequirement).toHaveBeenCalledWith(
      expect.objectContaining({ templateId: 'template-active-1' })
    );
    const participant =
      dataPrepareApiMock.prepareAndExecute.mock.calls[0][0].generateRequest
        .participants[0];
    expect(participant.questionId.length).toBeLessThanOrEqual(64);
    expect(participant.questionAttemptId.length).toBeLessThanOrEqual(64);
    const allocationRequest =
      dataPrepareApiMock.acquireDataInstance.mock.calls[0][0];
    expect(allocationRequest.attemptId.length).toBeLessThanOrEqual(64);
    expect(allocationRequest.questionAttemptId.length).toBeLessThanOrEqual(64);
    expect(dataPrepareApiMock.acquireDataInstance).toHaveBeenCalledWith(
      expect.objectContaining({
        poolId: 'pool-1',
        ownerUserId: 'student-1',
        taskId: 'task-1'
      })
    );
  });
});
