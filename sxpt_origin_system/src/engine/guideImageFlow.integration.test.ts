import { createSSRApp, h } from 'vue';
import { renderToString } from '@vue/server-renderer';
import { describe, expect, it } from 'vitest';
import TeachingOverlay from '../components/TeachingOverlay.vue';
import type { RecordedFlow } from '../types/domain';
import {
  createCaptureWorkspace,
  insertGuideStep,
  patchStep
} from './captureWorkspace';
import {
  loadPublishedRecordedFlow,
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
  id: 'flow-guide-image',
  title: '说明图片演示',
  businessUrl: '/business-sdk-demo.html',
  source: 'custom',
  publishStatus: 'draft',
  steps: [],
  segments: [
    {
      id: 'segment-1',
      segmentNo: 1,
      title: '备案第 1 段',
      targetUrl: '/business-sdk-demo.html',
      status: 'recording',
      steps: []
    }
  ]
};

describe('guide image flow', () => {
  it('survives guide editing and local publishing before rendering for a learner', async () => {
    const storage = new MemoryStorage();
    let workspace = insertGuideStep(createCaptureWorkspace(flow));
    const guideId = workspace.selectedStepId!;
    workspace = patchStep(workspace, guideId, {
      title: '查看示意图',
      teachingText: '请根据示意图继续操作。',
      guideImage: {
        src: 'data:image/webp;base64,AAAA',
        name: 'guide.webp',
        mimeType: 'image/webp',
        width: 640,
        height: 360,
        storageType: 'inline'
      }
    });

    savePublishedRecordedFlow(workspace.flow, storage);
    const learnedStep = loadPublishedRecordedFlow(storage)?.steps[0];
    const html = await renderToString(
      createSSRApp({
        render: () =>
          h(TeachingOverlay, {
            step: learnedStep,
            visible: true,
            showHint: true
          })
      })
    );

    expect(learnedStep?.guideImage?.name).toBe('guide.webp');
    expect(html).toContain('data:image/webp;base64,AAAA');
    expect(html).toContain('查看示意图说明图片');
  });
});
