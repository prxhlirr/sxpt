import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createDefaultBusinessPlatforms } from '../data/mockSeed';
import type { RecordedStep } from '../domain/models';
import {
  MAX_RECORDED_STEP_SNAPSHOT_JSON_LENGTH,
  backendTrainingApi,
  distributedScore,
  mapConnectorSystem,
  mapStepActionType,
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

vi.mock('../api/connector', () => ({
  connectorApi: connectorApiMock
}));

describe('后端训练接口映射', () => {
  beforeEach(() => {
    vi.clearAllMocks();
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
});
