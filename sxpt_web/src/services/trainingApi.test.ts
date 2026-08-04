import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  DATA_PREPARE_CONFIG_INCOMPLETE_CODE,
  TrainingApiRequestError,
  authApi,
  dataPrepareApi,
  isDataPrepareConfigIncompleteError
} from './trainingApi';

afterEach(() => {
  authApi.logout();
  vi.unstubAllGlobals();
});

describe('开发快捷登录', () => {
  it('管理端快捷入口使用初始化 SQL 中的真实专家账号登录', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        code: 200,
        message: 'OK',
        result: {
          token: 'token-expert',
          tokenType: 'Bearer',
          expiresIn: 3600,
          user: {
            userId: 'user-demo-expert-01',
            tenantId: 'demo-tenant',
            username: 'expert01',
            displayName: '孙专家',
            userType: 'EXPERT',
            roles: ['expert'],
            orgIds: ['org-demo-expert-group']
          }
        },
        timestamp: Date.now()
      })
    });
    vi.stubGlobal('fetch', fetchMock);

    const session = await authApi.useDevelopmentSession('admin');

    expect(session.user.username).toBe('expert01');
    expect(authApi.getPortalRole(session)).toBe('admin');
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('api/v1/auth/login'),
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('"username":"expert01"')
      })
    );
  });
});

describe('数据准备接口错误识别', () => {
  it('会识别后端返回的数据准备配置不完整错误码', () => {
    const error = new TrainingApiRequestError(
      '数据准备配置不完整',
      DATA_PREPARE_CONFIG_INCOMPLETE_CODE,
      200
    );

    expect(isDataPrepareConfigIncompleteError(error)).toBe(true);
  });

  it('不会把普通参数错误误判为配置不完整', () => {
    const error = new TrainingApiRequestError('参数错误', 400, 200);

    expect(isDataPrepareConfigIncompleteError(error)).toBe(false);
  });
});

describe('按任务触发数据准备接口', () => {
  it('会调用后端单点触发入口并返回批次和准备任务', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        success: true,
        code: 200,
        message: 'OK',
        result: {
          requirement: {
            id: 'requirement_001',
            tenantId: 'tenant_001',
            requirementCode: 'REQ-001',
            connectorSystemId: 'system_001',
            moduleCode: 'OA_DOC',
            taskId: 'task_001',
            sceneType: 'PRACTICE'
          },
          job: {
            id: 'job_001',
            tenantId: 'tenant_001',
            connectorSystemId: 'system_001',
            moduleCode: 'OA_DOC',
            taskId: 'task_001',
            sceneType: 'PRACTICE',
            jobType: 'INITIAL_CREATE',
            idempotencyKey: 'key_001',
            requestBatchId: 'attempt_001'
          },
          requestBatchId: 'attempt_001'
        },
        timestamp: Date.now()
      })
    });
    vi.stubGlobal('fetch', fetchMock);

    const result = await dataPrepareApi.triggerTaskPrepare('task_001', {
      connectorSystemId: 'system_001',
      businessModuleId: 'module_001',
      moduleCode: 'OA_DOC',
      sceneType: 'PRACTICE',
      requestBatchId: 'attempt_001',
      participants: [
        {
          studentId: 'student_001',
          questionId: 'question_001',
          questionAttemptId: 'qa_001',
          actorType: 'student',
          ownerExternalOrgId: 'owner_org',
          requiredExternalOrgId: 'required_org',
          requiredExternalRoleId: 'required_role',
          dataScopeJson: '{}',
          requiredActionsJson: '[]',
          scorePointSnapshotJson: '{}'
        }
      ]
    });

    expect(result.requirement.id).toBe('requirement_001');
    expect(result.job.id).toBe('job_001');
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('api/v1/teaching-data/tasks/task_001/prepare'),
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('"requestBatchId":"attempt_001"')
      })
    );
  });
});

describe('原平台角色组织字典接口', () => {
  it('会按原平台查询启用角色和组织，供模块参与方下拉使用', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          success: true,
          code: 200,
          message: 'OK',
          result: [
            {
              id: 'role_001',
              tenantId: 'tenant_001',
              connectorSystemId: 'system_001',
              roleCode: 'OA_APPLICANT',
              roleName: 'OA 申请人',
              status: 'ACTIVE'
            }
          ],
          timestamp: Date.now()
        })
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          success: true,
          code: 200,
          message: 'OK',
          result: [
            {
              id: 'org_001',
              tenantId: 'tenant_001',
              connectorSystemId: 'system_001',
              orgCode: 'OA_DEPT',
              orgName: 'OA 部门',
              orgType: 'DEPT',
              status: 'ACTIVE'
            }
          ],
          timestamp: Date.now()
        })
      });
    vi.stubGlobal('fetch', fetchMock);

    const roles = await dataPrepareApi.listOriginRoles({
      tenantId: 'tenant_001',
      connectorSystemId: 'system_001',
      activeOnly: true
    });
    const orgs = await dataPrepareApi.listOriginOrgs({
      tenantId: 'tenant_001',
      connectorSystemId: 'system_001',
      activeOnly: true
    });

    expect(roles[0].roleCode).toBe('OA_APPLICANT');
    expect(orgs[0].orgCode).toBe('OA_DEPT');
    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining('api/v1/connector/origin-roles?'),
      expect.any(Object)
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      expect.stringContaining('api/v1/connector/origin-orgs?'),
      expect.any(Object)
    );
    expect(fetchMock.mock.calls[0][0]).toContain('activeOnly=true');
    expect(fetchMock.mock.calls[1][0]).toContain('activeOnly=true');
  });
});
