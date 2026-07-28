import { describe, expect, it } from 'vitest';
import type { LessonFlow, TaskSubmission } from '../types/domain';
import {
  calculateScore,
  completeCurrentNode,
  createInitialSession,
  getCurrentNode,
  submitExam
} from './flowState';

const lessonFlow: LessonFlow = {
  id: 'lesson-purchase',
  templateId: 'template-purchase',
  title: '采购申请实训',
  moduleName: '采购申请',
  publishStatus: 'published',
  modePolicies: {
    learning: {
      showFullFlow: true,
      showTeachingText: true,
      showOverlayHint: true,
      allowFreeBrowse: true,
      enforceOrder: false,
      showSubmission: false,
      silentRecord: false
    },
    practice: {
      showFullFlow: true,
      showTeachingText: false,
      showOverlayHint: true,
      allowFreeBrowse: false,
      enforceOrder: true,
      showSubmission: false,
      silentRecord: false
    },
    exam: {
      showFullFlow: false,
      showTeachingText: false,
      showOverlayHint: false,
      allowFreeBrowse: false,
      enforceOrder: false,
      showSubmission: true,
      silentRecord: true
    }
  },
  nodes: [
    {
      id: 'node-create',
      nodeDefId: 'def-create',
      name: '创建申请',
      required: true,
      score: 20,
      completionMethod: 'click',
      teachingText: '从业务页面创建一张新的采购申请。'
    },
    {
      id: 'node-fill',
      nodeDefId: 'def-fill',
      name: '填写基础信息',
      required: true,
      score: 30,
      completionMethod: 'click',
      teachingText: '填写供应商、金额和申请原因。'
    },
    {
      id: 'node-submit',
      nodeDefId: 'def-submit',
      name: '提交申请',
      required: true,
      score: 50,
      completionMethod: 'submission',
      teachingText: '提交采购申请并记录业务单号。'
    }
  ],
  edges: [
    { id: 'edge-1', from: 'node-create', to: 'node-fill' },
    { id: 'edge-2', from: 'node-fill', to: 'node-submit' }
  ]
};

describe('flowState', () => {
  it('creates a session with the first node available and later nodes pending', () => {
    const session = createInitialSession(lessonFlow, 'learning');

    expect(session.mode).toBe('learning');
    expect(session.nodeResults).toEqual({
      'node-create': 'available',
      'node-fill': 'pending',
      'node-submit': 'pending'
    });
  });

  it('returns the current available node', () => {
    const session = createInitialSession(lessonFlow, 'practice');

    expect(getCurrentNode(lessonFlow, session)?.id).toBe('node-create');
  });

  it('completes the current click node and unlocks the next node', () => {
    const firstSession = createInitialSession(lessonFlow, 'learning');
    const secondSession = completeCurrentNode(lessonFlow, firstSession);

    expect(secondSession.nodeResults['node-create']).toBe('success');
    expect(secondSession.nodeResults['node-fill']).toBe('available');
    expect(secondSession.events.at(-1)).toMatchObject({
      type: 'node_complete',
      nodeId: 'node-create'
    });
  });

  it('submits exam evidence and marks the submission node successful', () => {
    const baseSession = completeCurrentNode(
      lessonFlow,
      completeCurrentNode(lessonFlow, createInitialSession(lessonFlow, 'exam'))
    );
    const submission: TaskSubmission = {
      businessId: 'CG-2026-0001',
      attachmentName: 'result.png',
      description: '已完成采购申请并提交审批。',
      submittedAt: '2026-07-08T09:00:00.000Z'
    };

    const submittedSession = submitExam(lessonFlow, baseSession, submission);

    expect(submittedSession.submission).toEqual(submission);
    expect(submittedSession.nodeResults['node-submit']).toBe('success');
    expect(submittedSession.events.at(-1)).toMatchObject({
      type: 'submission',
      nodeId: 'node-submit'
    });
  });

  it('calculates score from successful required nodes only', () => {
    const session = completeCurrentNode(
      lessonFlow,
      completeCurrentNode(lessonFlow, createInitialSession(lessonFlow, 'learning'))
    );

    expect(calculateScore(lessonFlow, session)).toBe(50);
  });
});
