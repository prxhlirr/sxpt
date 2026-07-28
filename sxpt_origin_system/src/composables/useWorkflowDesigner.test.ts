import { describe, expect, it, vi } from 'vitest';
import { TrainingApiError } from '../types/trainingApi';
import type {
  TaskWorkflowDraft,
  TaskWorkflowVersion
} from '../types/workflow';
import type { WorkflowDefinitionApi } from '../api/v1/workflowDefinitionApi';
import {
  createWorkflowDesigner,
  WorkflowDesignerError
} from './useWorkflowDesigner';

function draftFixture(): TaskWorkflowDraft {
  return {
    id: 'workflow-1',
    tenantId: 'tenant-1',
    taskId: 'task-1',
    versionNo: 1,
    versionStatus: 'DRAFT',
    name: '采购协同审批',
    serialPolicy: 'SERIAL_ONLY',
    teachingDataTemplateId: 'template-1',
    dataScenarioVersion: 'scenario-v1',
    dataGenerationDefaults: { priority: 'normal' },
    objectiveMaxScore: 90,
    subjectiveMaxScore: 10,
    lockVersion: 4,
    roleGroups: [
      {
        id: 'group-submit',
        groupKey: 'SUBMIT',
        name: '提交组',
        sequenceNo: 1
      },
      {
        id: 'group-review',
        groupKey: 'REVIEW',
        name: '复核组',
        sequenceNo: 2
      },
      {
        id: 'group-approve',
        groupKey: 'APPROVE',
        name: '审批组',
        sequenceNo: 3
      }
    ],
    stages: [
      stage('submit', 'group-submit', 1),
      stage('review', 'group-review', 2),
      stage('approve', 'group-approve', 3)
    ],
    rubricItems: [
      {
        id: 'rubric-1',
        itemKey: 'QUALITY',
        name: '办理质量',
        sequenceNo: 1,
        maxScore: 10,
        required: true
      }
    ]
  };
}

function stage(id: string, roleGroupId: string, sequenceNo: number) {
  return {
    id,
    stageKey: id.toUpperCase(),
    name: `${id} stage`,
    sequenceNo,
    roleGroupId,
    externalRoleId: `business-${id}`,
    externalOrgId: 'org-1',
    launchConfig: { path: `/${id}` },
    stageMaxScore: 30,
    reconnectGraceSeconds: 60,
    allowSameLearnerNextDefault: false,
    allowBatchOverride: true,
    releasePolicy: 'BEFORE_FIRST_EVENT' as const,
    reattemptPolicy: 'REPLACEMENT_DATA' as const,
    steps: [
      {
        id: `${id}-step`,
        taskStepId: `${id}-task-step`,
        recordedSegmentId: `${id}-segment`,
        recordedAssetVersion: 1,
        sequenceNo: 1,
        stepKind: 'REQUIRED' as const,
        completionMethod: 'click',
        failurePolicy: 'stop',
        score: 30,
        matcher: { selector: `#${id}` }
      }
    ]
  };
}

function published(draft: TaskWorkflowDraft): TaskWorkflowVersion {
  return {
    ...draft,
    versionStatus: 'PUBLISHED',
    snapshotDigest: 'digest-1',
    publishedBy: 'teacher-1',
    publishedTime: '2026-07-23T10:00:00'
  };
}

function apiWith(
  overrides: Partial<WorkflowDefinitionApi> = {}
): WorkflowDefinitionApi {
  const draft = draftFixture();
  return {
    getDraft: vi.fn().mockResolvedValue(draft),
    saveDraft: vi.fn().mockResolvedValue({ ...draft, lockVersion: 5 }),
    validateDraft: vi.fn().mockResolvedValue({ valid: true, issues: [] }),
    publish: vi.fn().mockResolvedValue(published(draft)),
    getVersion: vi.fn().mockResolvedValue(published(draft)),
    createClientRequestId: vi.fn()
      .mockReturnValueOnce('save-intent-1')
      .mockReturnValueOnce('publish-intent-1'),
    ...overrides
  };
}

describe('createWorkflowDesigner', () => {
  it('rejects every mutating command while the loaded workflow is published', async () => {
    const publishedDraft = published(draftFixture());
    const designer = createWorkflowDesigner(apiWith(), publishedDraft);
    const before = structuredClone(designer.draft.value);
    const operations = [
      () => designer.addRoleGroup({
        id: 'group-new',
        groupKey: 'NEW',
        name: 'new group',
        sequenceNo: 4
      }),
      () => designer.removeRoleGroup('group-submit'),
      () => designer.addStage(stage('new', 'group-submit', 4)),
      () => designer.removeStage('submit'),
      () => designer.moveStage('submit', 1),
      () => designer.updateStage('submit', { name: 'changed' }),
      () => designer.replaceStageSteps('submit', []),
      () => designer.replaceRubricItems([]),
      () => designer.updateMetadata({ name: 'changed' }),
      () => designer.updateDataTemplate('template-2', 'v2', {})
    ];

    for (const operation of operations) {
      expect(operation).toThrow(
        expect.objectContaining<Partial<WorkflowDesignerError>>({
          code: 'WORKFLOW_READ_ONLY'
        })
      );
    }
    await expect(designer.save()).rejects.toEqual(
      expect.objectContaining({ code: 'WORKFLOW_READ_ONLY' })
    );
    await expect(designer.publish()).rejects.toEqual(
      expect.objectContaining({ code: 'WORKFLOW_READ_ONLY' })
    );
    expect(designer.draft.value).toEqual(before);
    expect(designer.dirty.value).toBe(false);
  });

  it('only becomes editable after continueEditing obtains a new draft', async () => {
    const nextDraft = {
      ...draftFixture(),
      id: 'workflow-2',
      versionNo: 2,
      versionStatus: 'DRAFT' as const
    };
    const getDraft = vi.fn().mockResolvedValue(nextDraft);
    const designer = createWorkflowDesigner(
      apiWith({ getDraft }),
      published(draftFixture())
    );

    expect(designer.isReadOnly.value).toBe(true);

    await designer.continueEditing();

    expect(getDraft).toHaveBeenCalledWith('task-1');
    expect(designer.draft.value).toEqual(nextDraft);
    expect(designer.isReadOnly.value).toBe(false);
    designer.updateMetadata({ name: 'editable again' });
    expect(designer.draft.value?.name).toBe('editable again');
  });

  it('reorders stages to a continuous one-based sequence', () => {
    const designer = createWorkflowDesigner(apiWith(), draftFixture());

    designer.moveStage('review', 0);

    expect(designer.draft.value?.stages.map((item) => item.id)).toEqual([
      'review',
      'submit',
      'approve'
    ]);
    expect(
      designer.draft.value?.stages.map((item) => item.sequenceNo)
    ).toEqual([1, 2, 3]);
    expect(designer.dirty.value).toBe(true);
  });

  it('rejects removal of a role group that is still used by a stage', () => {
    const designer = createWorkflowDesigner(apiWith(), draftFixture());

    expect(() => designer.removeRoleGroup('group-submit')).toThrow(
      expect.objectContaining<Partial<WorkflowDesignerError>>({
        code: 'GROUP_IN_USE'
      })
    );
    expect(designer.dirty.value).toBe(false);
  });

  it('normalizes replacement step ordering and saves the current lock', async () => {
    const saveDraft = vi.fn().mockImplementation(
      async (_taskId, request) => ({
        ...draftFixture(),
        lockVersion: request.expectedLockVersion + 1,
        stages: request.stages
      })
    );
    const api = apiWith({ saveDraft });
    const designer = createWorkflowDesigner(api, draftFixture());
    const originalSteps = designer.draft.value!.stages[0].steps;

    designer.replaceStageSteps('submit', [
      { ...originalSteps[0], id: 'second', sequenceNo: 20 },
      { ...originalSteps[0], id: 'first', sequenceNo: 10 }
    ]);
    await designer.save();

    expect(saveDraft).toHaveBeenCalledWith(
      'task-1',
      expect.objectContaining({
        clientRequestId: 'save-intent-1',
        expectedLockVersion: 4,
        stages: [
          expect.objectContaining({
            steps: [
              expect.objectContaining({ id: 'second', sequenceNo: 1 }),
              expect.objectContaining({ id: 'first', sequenceNo: 2 })
            ]
          }),
          expect.anything(),
          expect.anything()
        ]
      })
    );
    expect(designer.draft.value?.lockVersion).toBe(5);
    expect(designer.dirty.value).toBe(false);
  });

  it('does not replace the local draft after an optimistic lock conflict', async () => {
    const conflict = new TrainingApiError(
      'WORKFLOW_DRAFT_CONFLICT',
      409,
      '/tasks/task-1/workflow-draft'
    );
    const designer = createWorkflowDesigner(
      apiWith({ saveDraft: vi.fn().mockRejectedValue(conflict) }),
      draftFixture()
    );
    designer.updateStage('submit', { name: '本地修改仍需保留' });

    await expect(designer.save()).rejects.toBe(conflict);

    expect(designer.draft.value?.stages[0].name).toBe('本地修改仍需保留');
    expect(designer.draft.value?.lockVersion).toBe(4);
    expect(designer.conflict.value).toBe(true);
    expect(designer.dirty.value).toBe(true);
  });

  it('saves dirty state, validates, and only then publishes', async () => {
    const calls: string[] = [];
    const draft = draftFixture();
    const api = apiWith({
      saveDraft: vi.fn().mockImplementation(async () => {
        calls.push('save');
        return { ...draft, lockVersion: 5 };
      }),
      validateDraft: vi.fn().mockImplementation(async () => {
        calls.push('validate');
        return { valid: true, issues: [] };
      }),
      publish: vi.fn().mockImplementation(async () => {
        calls.push('publish');
        return published({ ...draft, lockVersion: 5 });
      })
    });
    const designer = createWorkflowDesigner(api, draft);
    designer.updateDataTemplate(
      'template-2',
      'scenario-v2',
      { priority: 'urgent' }
    );

    await expect(designer.publish()).resolves.toEqual(
      expect.objectContaining({ versionStatus: 'PUBLISHED' })
    );

    expect(calls).toEqual(['save', 'validate', 'publish']);
    expect(api.publish).toHaveBeenCalledWith('task-1', {
      clientRequestId: 'publish-intent-1'
    });
  });

  it('does not call publish when validation has blockers', async () => {
    const validation = {
      valid: false,
      issues: [
        {
          code: 'MISSING_EXTERNAL_IDENTITY',
          path: 'stages[0].externalRoleId',
          message: '未配置业务角色'
        }
      ]
    };
    const api = apiWith({
      validateDraft: vi.fn().mockResolvedValue(validation)
    });
    const designer = createWorkflowDesigner(api, draftFixture());

    await expect(designer.publish()).rejects.toEqual(
      expect.objectContaining({ code: 'WORKFLOW_INVALID' })
    );

    expect(designer.validation.value).toEqual(validation);
    expect(api.publish).not.toHaveBeenCalled();
  });

  it('retries an uncertain publish with the same id without revalidating', async () => {
    const networkError = new TrainingApiError(
      'network interrupted after submit',
      'REQUEST_FAILED'
    );
    const draft = draftFixture();
    const publish = vi.fn()
      .mockRejectedValueOnce(networkError)
      .mockResolvedValueOnce(published(draft));
    const api = apiWith({ publish });
    const designer = createWorkflowDesigner(api, draft);

    await expect(designer.publish()).rejects.toBe(networkError);
    await expect(designer.publish()).resolves.toEqual(
      expect.objectContaining({ versionStatus: 'PUBLISHED' })
    );

    expect(api.validateDraft).toHaveBeenCalledTimes(1);
    expect(publish).toHaveBeenCalledTimes(2);
    expect(publish).toHaveBeenNthCalledWith(1, 'task-1', {
      clientRequestId: 'save-intent-1'
    });
    expect(publish).toHaveBeenNthCalledWith(2, 'task-1', {
      clientRequestId: 'save-intent-1'
    });
    expect(api.createClientRequestId).toHaveBeenCalledTimes(1);
  });
});
