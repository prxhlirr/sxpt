import { describe, expect, it, vi } from 'vitest';
import type {
  SaveWorkflowDraftRequest,
  WorkflowStageInput
} from '../../types/workflow';
import type { AuthenticatedApiClient } from './authenticatedApiClient';
import { createWorkflowDefinitionApi } from './workflowDefinitionApi';

const rawDraft = {
  id: 'workflow-1',
  tenantId: 'tenant-1',
  taskId: 'task-1',
  versionNo: 1,
  versionStatus: 'DRAFT',
  name: '采购审批',
  serialPolicy: 'SERIAL_ONLY',
  teachingDataTemplateId: 'template-1',
  dataScenarioVersion: 'scenario-v1',
  dataGenerationDefaultsJson: '{"priority":"normal"}',
  objectiveMaxScore: '80.00',
  subjectiveMaxScore: '20.00',
  lockVersion: 4,
  roleGroups: [
    {
      id: 'group-submit',
      groupKey: 'SUBMITTER',
      name: '提交组',
      color: '#2563eb',
      sequenceNo: 1
    }
  ],
  stages: [
    {
      id: 'stage-submit',
      stageKey: 'SUBMIT',
      name: '提交申请',
      sequenceNo: 1,
      roleGroupId: 'group-submit',
      externalRoleId: 'business-submitter',
      externalOrgId: 'org-1',
      launchConfigJson: '{"path":"/purchase/apply"}',
      stageMaxScore: '80.00',
      timeLimitSeconds: 900,
      reconnectGraceSeconds: 60,
      allowSameLearnerNextDefault: false,
      allowBatchOverride: true,
      releasePolicy: 'BEFORE_FIRST_EVENT',
      reattemptPolicy: 'REPLACEMENT_DATA',
      steps: [
        {
          id: 'binding-1',
          taskStepId: 'task-step-1',
          recordedSegmentId: 'action-draft-1',
          recordedAssetVersion: 1,
          sequenceNo: 1,
          stepKind: 'REQUIRED',
          completionMethod: 'click',
          failurePolicy: 'stop',
          score: '80.00',
          matcherJson: '{"selector":"#submit"}'
        }
      ]
    }
  ],
  rubricItems: [
    {
      id: 'rubric-1',
      itemKey: 'QUALITY',
      name: '办理质量',
      sequenceNo: 1,
      maxScore: '20.00',
      required: true
    }
  ]
};

function clientWith(
  overrides: Partial<AuthenticatedApiClient>
): AuthenticatedApiClient {
  return {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    createClientRequestId: vi.fn(() => 'request-1'),
    ...overrides
  };
}

function draftRequest(
  patch: Partial<SaveWorkflowDraftRequest> = {}
): SaveWorkflowDraftRequest {
  return {
    clientRequestId: 'save-intent-1',
    expectedLockVersion: 4,
    name: '采购审批',
    teachingDataTemplateId: 'template-1',
    dataScenarioVersion: 'scenario-v1',
    dataGenerationDefaults: { priority: 'normal' },
    objectiveMaxScore: 80,
    subjectiveMaxScore: 20,
    roleGroups: rawDraft.roleGroups,
    stages: [
      {
        id: 'stage-submit',
        stageKey: 'SUBMIT',
        name: '提交申请',
        sequenceNo: 1,
        roleGroupId: 'group-submit',
        externalRoleId: 'business-submitter',
        externalOrgId: 'org-1',
        launchConfig: { path: '/purchase/apply' },
        stageMaxScore: 80,
        timeLimitSeconds: 900,
        reconnectGraceSeconds: 60,
        allowSameLearnerNextDefault: false,
        allowBatchOverride: true,
        releasePolicy: 'BEFORE_FIRST_EVENT',
        reattemptPolicy: 'REPLACEMENT_DATA',
        steps: [
          {
            id: 'binding-1',
            taskStepId: 'task-step-1',
            recordedSegmentId: 'action-draft-1',
            recordedAssetVersion: 1,
            sequenceNo: 1,
            stepKind: 'REQUIRED',
            completionMethod: 'click',
            failurePolicy: 'stop',
            score: 80,
            matcher: { selector: '#submit' }
          }
        ]
      }
    ],
    rubricItems: [
      {
        id: 'rubric-1',
        itemKey: 'QUALITY',
        name: '办理质量',
        sequenceNo: 1,
        maxScore: 20,
        required: true
      }
    ],
    ...patch
  };
}

describe('createWorkflowDefinitionApi', () => {
  it('parses JSON fields and decimal scores returned by the backend', async () => {
    const get = vi.fn().mockResolvedValue(rawDraft);
    const api = createWorkflowDefinitionApi(clientWith({ get }));

    const draft = await api.getDraft('task-1');

    expect(get).toHaveBeenCalledWith('/tasks/task-1/workflow-draft');
    expect(draft.dataGenerationDefaults).toEqual({ priority: 'normal' });
    expect(draft.objectiveMaxScore).toBe(80);
    expect(draft.stages[0].launchConfig).toEqual({
      path: '/purchase/apply'
    });
    expect(draft.stages[0].steps[0].matcher).toEqual({
      selector: '#submit'
    });
    expect(draft.rubricItems[0].maxScore).toBe(20);
  });

  it('sends optimistic lock and one stable client request id', async () => {
    const put = vi.fn().mockResolvedValue(rawDraft);
    const api = createWorkflowDefinitionApi(clientWith({ put }));
    const request = draftRequest({ expectedLockVersion: 4 });

    await api.saveDraft('task-1', request);

    expect(put).toHaveBeenCalledWith(
      '/tasks/task-1/workflow-draft',
      expect.objectContaining({
        expectedLockVersion: 4,
        clientRequestId: request.clientRequestId,
        objectiveMaxScore: '80',
        subjectiveMaxScore: '20',
        dataGenerationDefaults: { priority: 'normal' },
        stages: [
          expect.objectContaining({
            stageMaxScore: '80',
            launchConfig: { path: '/purchase/apply' },
            steps: [
              expect.objectContaining({
                score: '80',
                matcher: { selector: '#submit' }
              })
            ]
          })
        ],
        rubricItems: [
          expect.objectContaining({
            maxScore: '20'
          })
        ]
      })
    );
  });

  it('keeps role groups separate from external business identity', () => {
    const stage: WorkflowStageInput = draftRequest().stages[0];

    expect(stage.roleGroupId).not.toBe(stage.externalRoleId);
    expect(stage.externalOrgId).toBe('org-1');
  });

  it('uses the validation, publish, and version resources', async () => {
    const get = vi
      .fn()
      .mockResolvedValueOnce({
        valid: false,
        issues: [
          {
            code: 'MISSING_EXTERNAL_IDENTITY',
            path: 'stages[0].externalRoleId',
            message: '未配置业务角色'
          }
        ]
      })
      .mockResolvedValueOnce({
        ...rawDraft,
        versionStatus: 'PUBLISHED',
        snapshotDigest: 'abc123',
        publishedBy: 'teacher-1',
        publishedTime: '2026-07-23T10:00:00'
      });
    const post = vi.fn().mockResolvedValue({
      ...rawDraft,
      versionStatus: 'PUBLISHED',
      snapshotDigest: 'abc123',
      publishedBy: 'teacher-1',
      publishedTime: '2026-07-23T10:00:00'
    });
    const api = createWorkflowDefinitionApi(clientWith({ get, post }));

    await expect(api.validateDraft('task-1')).resolves.toEqual({
      valid: false,
      issues: [
        expect.objectContaining({ code: 'MISSING_EXTERNAL_IDENTITY' })
      ]
    });
    await expect(
      api.publish('task-1', { clientRequestId: 'publish-intent-1' })
    ).resolves.toEqual(
      expect.objectContaining({
        versionStatus: 'PUBLISHED',
        snapshotDigest: 'abc123'
      })
    );
    await expect(api.getVersion('workflow-1')).resolves.toEqual(
      expect.objectContaining({ snapshotDigest: 'abc123' })
    );

    expect(post).toHaveBeenCalledWith(
      '/tasks/task-1/workflow-publish',
      { clientRequestId: 'publish-intent-1' }
    );
    expect(get).toHaveBeenNthCalledWith(
      2,
      '/workflow-versions/workflow-1'
    );
  });
});
