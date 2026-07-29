import { describe, expect, it } from 'vitest';
import type { RecordedFlow } from '../types/domain';
import type { SaveWorkflowDraftRequest } from '../types/workflow';
import type { RecordedStageBinding } from './captureOrchestrator';
import {
  clearPendingRecordedStageWriteback,
  loadPendingRecordedStageWriteback,
  PUBLISHED_RECORDED_FLOW_KEY,
  loadPublishedRecordedFlow,
  savePendingRecordedStageWriteback,
  savePublishedRecordedFlow
} from './recordingStorage';

class MemoryStorage implements Storage {
  private values = new Map<string, string>();

  get length() {
    return this.values.size;
  }

  clear() {
    this.values.clear();
  }

  getItem(key: string) {
    return this.values.get(key) ?? null;
  }

  key(index: number) {
    return Array.from(this.values.keys())[index] ?? null;
  }

  removeItem(key: string) {
    this.values.delete(key);
  }

  setItem(key: string, value: string) {
    this.values.set(key, value);
  }
}

const flow: RecordedFlow = {
  id: 'recorded-flow',
  title: '发布后的录制流程',
  businessUrl: '/business-demo.html',
  source: 'custom',
  publishStatus: 'draft',
  steps: [
    {
      id: 'step-1',
      nodeId: 'node-1',
      order: 1,
      title: '新建申请',
      actionType: 'click',
      selector: '[data-action="create"]',
      rect: { x: 10, y: 20, width: 100, height: 40 },
      text: '新建申请',
      url: '/business-demo.html',
      teachingText: '点击新建申请。',
      practiceHint: '找到新建申请按钮。',
      examGoal: '独立创建申请。',
      completionMethod: 'click'
    }
  ]
};

const pendingBinding: RecordedStageBinding = {
  workflowDraftId: 'workflow-1',
  stageId: 'stage-1',
  recordedSegmentId: 'segment-1',
  recordedAssetVersion: 1,
  steps: []
};

const pendingRequest: SaveWorkflowDraftRequest = {
  clientRequestId: 'stable-writeback-1',
  expectedLockVersion: 4,
  name: 'workflow',
  teachingDataTemplateId: 'template-1',
  dataScenarioVersion: 'v1',
  dataGenerationDefaults: {},
  objectiveMaxScore: 90,
  subjectiveMaxScore: 10,
  roleGroups: [],
  stages: [],
  rubricItems: []
};

describe('recordingStorage', () => {
  it('restores the exact pending stage binding request after a reload', () => {
    const storage = new MemoryStorage();

    savePendingRecordedStageWriteback({
      workflowDraftId: 'workflow-1',
      stageId: 'stage-1',
      taskId: 'task-1',
      clientRequestId: 'stable-writeback-1',
      binding: pendingBinding,
      request: pendingRequest
    }, storage);

    expect(
      loadPendingRecordedStageWriteback('workflow-1', 'stage-1', storage)
    ).toEqual({
      workflowDraftId: 'workflow-1',
      stageId: 'stage-1',
      taskId: 'task-1',
      clientRequestId: 'stable-writeback-1',
      binding: pendingBinding,
      request: pendingRequest
    });
  });

  it('clears a completed pending stage binding without affecting other stages', () => {
    const storage = new MemoryStorage();
    savePendingRecordedStageWriteback({
      workflowDraftId: 'workflow-1',
      stageId: 'stage-1',
      taskId: 'task-1',
      clientRequestId: 'stable-writeback-1',
      binding: pendingBinding,
      request: pendingRequest
    }, storage);
    savePendingRecordedStageWriteback({
      workflowDraftId: 'workflow-1',
      stageId: 'stage-2',
      taskId: 'task-1',
      clientRequestId: 'stable-writeback-2',
      binding: { ...pendingBinding, stageId: 'stage-2' },
      request: { ...pendingRequest, clientRequestId: 'stable-writeback-2' }
    }, storage);

    clearPendingRecordedStageWriteback('workflow-1', 'stage-1', storage);

    expect(
      loadPendingRecordedStageWriteback('workflow-1', 'stage-1', storage)
    ).toBeUndefined();
    expect(
      loadPendingRecordedStageWriteback('workflow-1', 'stage-2', storage)
        ?.clientRequestId
    ).toBe('stable-writeback-2');
  });

  it('saves a flow as published and loads it back', () => {
    const storage = new MemoryStorage();

    savePublishedRecordedFlow(flow, storage);

    expect(loadPublishedRecordedFlow(storage)).toMatchObject({
      ...flow,
      publishStatus: 'published'
    });
  });

  it('returns undefined when storage has invalid flow json', () => {
    const storage = new MemoryStorage();
    storage.setItem(PUBLISHED_RECORDED_FLOW_KEY, '{bad-json');

    expect(loadPublishedRecordedFlow(storage)).toBeUndefined();
  });

  it('ignores a flow stored under the legacy demo key', () => {
    const storage = new MemoryStorage();
    storage.setItem('sxpt.publishedRecordedFlow', JSON.stringify(flow));

    expect(loadPublishedRecordedFlow(storage)).toBeUndefined();
  });

  it('keeps a guide image when a published flow is loaded again', () => {
    const storage = new MemoryStorage();
    const flowWithImage: RecordedFlow = {
      ...flow,
      steps: [
        {
          ...flow.steps[0],
          kind: 'guide',
          anchor: { mode: 'center' },
          guideImage: {
            src: 'data:image/webp;base64,AAAA',
            name: 'guide.webp',
            mimeType: 'image/webp',
            width: 640,
            height: 360,
            storageType: 'inline'
          }
        }
      ]
    };

    savePublishedRecordedFlow(flowWithImage, storage);

    expect(loadPublishedRecordedFlow(storage)?.steps[0].guideImage).toEqual(
      flowWithImage.steps[0].guideImage
    );
  });

  it('reports a clear message when browser storage quota is exceeded', () => {
    const storage = new MemoryStorage();
    storage.setItem = () => {
      throw new DOMException('quota', 'QuotaExceededError');
    };

    expect(() => savePublishedRecordedFlow(flow, storage)).toThrow(
      '浏览器存储空间不足，无法保存带图片的演示教案，请删除部分图片后重试。'
    );
  });
});
