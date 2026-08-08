import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  DATA_PREPARE_CONFIG_INCOMPLETE_CODE,
  TrainingApiRequestError,
  authApi,
  dataPrepareApi,
  isDataPrepareConfigIncompleteError,
  trainingWorkspaceApi,
  type AuthSession
} from './trainingApi';
import { createMockTrainingState } from '../data/mockSeed';

afterEach(() => {
  authApi.logout();
  vi.unstubAllGlobals();
});

function createSession(role: 'teacher' | 'student'): AuthSession {
  return {
    token: `token-${role}`,
    tokenType: 'Bearer',
    expiresIn: 3600,
    issuedAt: Date.now(),
    user: {
      userId: `${role}-001`,
      tenantId: 'tenant-001',
      username: `${role}01`,
      displayName: role === 'teacher' ? '张老师' : '测试学生',
      userType: role.toUpperCase(),
      roles: [role],
      orgIds: []
    }
  };
}

describe('登录用户工作区接口', () => {
  it('教师从完整教学工作区接口加载数据', async () => {
    const state = createMockTrainingState();
    state.currentRole = 'teacher';
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        success: true,
        code: 200,
        message: 'OK',
        result: state,
        timestamp: Date.now()
      })
    });
    vi.stubGlobal('fetch', fetchMock);

    const result = await trainingWorkspaceApi.load(createSession('teacher'));

    expect(result?.currentRole).toBe('teacher');
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('api/v1/training/workspace'),
      expect.objectContaining({ headers: expect.any(Headers) })
    );
    expect(fetchMock.mock.calls[0][0]).not.toContain('/student/');
  });

  it('学生只向个人工作区接口保存学习进度', async () => {
    const state = createMockTrainingState();
    state.currentRole = 'student';
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        success: true,
        code: 200,
        message: 'OK',
        result: state,
        timestamp: Date.now()
      })
    });
    vi.stubGlobal('fetch', fetchMock);

    await trainingWorkspaceApi.save(createSession('student'), state);

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('api/v1/student/training/workspace'),
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify(state),
        headers: expect.any(Headers)
      })
    );
  });
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
          token: 'token-teacher-admin',
          tokenType: 'Bearer',
          expiresIn: 3600,
          user: {
            userId: 'user-demo-teacher-02',
            tenantId: 'demo-tenant',
            username: 'teacher02',
            displayName: '孙专家',
            userType: 'TEACHER',
            roles: ['admin', 'teacher'],
            orgIds: ['org-demo-class-b']
          }
        },
        timestamp: Date.now()
      })
    });
    vi.stubGlobal('fetch', fetchMock);

    const session = await authApi.useDevelopmentSession('admin');

    expect(session.user.username).toBe('teacher02');
    expect(authApi.getPortalRole(session)).toBe('admin');
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('api/v1/auth/login'),
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('"username":"teacher02"')
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

  it('点击数据准备时会向教学平台 API 添加认证和 JSON 请求头', async () => {
    const session = createSession('teacher');
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          success: true,
          code: 200,
          message: 'OK',
          result: session,
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

    await authApi.login({
      tenantId: 'tenant_001',
      username: 'teacher01',
      password: 'Sxpt@123456',
      loginType: 'PASSWORD'
    });

    await dataPrepareApi.triggerTaskPrepare('task_001', {
      connectorSystemId: 'system_001',
      businessModuleId: 'module_001',
      moduleCode: 'OA_DOC',
      sceneType: 'PRACTICE',
      requestBatchId: 'attempt_001',
      participants: []
    });

    const headers = fetchMock.mock.calls[1][1].headers as Headers;
    expect(headers.get('Content-Type')).toBe('application/json');
    expect(headers.get('Authorization')).toBe('Bearer token-teacher');
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

  it('会提交原平台角色和组织字典维护请求', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({
          success: true,
          code: 200,
          message: 'OK',
          result: {},
          timestamp: Date.now()
        })
      });
    vi.stubGlobal('fetch', fetchMock);

    await dataPrepareApi.createOriginRole({
      tenantId: 'tenant_001',
      connectorSystemId: 'system_001',
      roleCode: 'OA_APPLICANT',
      roleName: 'OA 申请人'
    });
    await dataPrepareApi.updateOriginRole('role_001', {
      tenantId: 'tenant_001',
      connectorSystemId: 'system_001',
      roleCode: 'OA_APPLICANT',
      roleName: 'OA 申请人'
    });
    await dataPrepareApi.disableOriginRole('role_001');
    await dataPrepareApi.createOriginOrg({
      tenantId: 'tenant_001',
      connectorSystemId: 'system_001',
      orgCode: 'OA_DEPT',
      orgName: 'OA 部门'
    });
    await dataPrepareApi.updateOriginOrg('org_001', {
      tenantId: 'tenant_001',
      connectorSystemId: 'system_001',
      orgCode: 'OA_DEPT',
      orgName: 'OA 部门'
    });
    await dataPrepareApi.enableOriginOrg('org_001');

    expect(fetchMock.mock.calls[0][0]).toContain('api/v1/connector/origin-roles/create');
    expect(fetchMock.mock.calls[1][0]).toContain('api/v1/connector/origin-roles/role_001/update');
    expect(fetchMock.mock.calls[2][0]).toContain('api/v1/connector/origin-roles/role_001/disable');
    expect(fetchMock.mock.calls[3][0]).toContain('api/v1/connector/origin-orgs/create');
    expect(fetchMock.mock.calls[4][0]).toContain('api/v1/connector/origin-orgs/org_001/update');
    expect(fetchMock.mock.calls[5][0]).toContain('api/v1/connector/origin-orgs/org_001/enable');
  });
});

describe('教师编排单点启动接口', () => {
  it('请求后端新建编排数据实例和启动令牌', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        success: true,
        code: 200,
        message: 'OK',
        result: {
          tenantId: 'tenant-001',
          launchContextId: 'launch-001',
          launchToken: 'ctx-token',
          dataInstanceId: 'instance-001',
          redirectUrl: 'https://oa.example.com/purchase/apply',
          launchUrl: 'https://oa.example.com/sso?tenantId=tenant-001'
        },
        timestamp: Date.now()
      })
    });
    vi.stubGlobal('fetch', fetchMock);

    const result = await dataPrepareApi.createAuthoringLaunch({
      lessonId: 'lesson-001',
      connectorSystemId: 'system-001',
      businessModuleId: 'module-001'
    });

    expect(result.dataInstanceId).toBe('instance-001');
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('api/v1/teaching-data/authoring-launches/create'),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          lessonId: 'lesson-001',
          connectorSystemId: 'system-001',
          businessModuleId: 'module-001'
        })
      })
    );
  });
});
