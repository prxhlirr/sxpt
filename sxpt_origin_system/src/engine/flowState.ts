import type {
  LessonFlow,
  LessonNode,
  NodeStatus,
  RunnerMode,
  StudentSession,
  TaskSubmission
} from '../types/domain';

const terminalStatuses: NodeStatus[] = ['success', 'failed', 'manual_pending'];

export function createInitialSession(
  lessonFlow: LessonFlow,
  mode: RunnerMode
): StudentSession {
  const nodeResults = lessonFlow.nodes.reduce<Record<string, NodeStatus>>(
    (result, node, index) => {
      result[node.id] = index === 0 ? 'available' : 'pending';
      return result;
    },
    {}
  );

  return {
    id: `session-${lessonFlow.id}-${mode}`,
    lessonFlowId: lessonFlow.id,
    mode,
    nodeResults,
    events: []
  };
}

export function getCurrentNode(
  lessonFlow: LessonFlow,
  session: StudentSession
): LessonNode | undefined {
  return lessonFlow.nodes.find((node) => session.nodeResults[node.id] === 'available');
}

export function completeCurrentNode(
  lessonFlow: LessonFlow,
  session: StudentSession
): StudentSession {
  const currentNode = getCurrentNode(lessonFlow, session);

  if (!currentNode) {
    return session;
  }

  return markNodeSuccess(lessonFlow, session, currentNode.id, 'node_complete');
}

export function submitExam(
  lessonFlow: LessonFlow,
  session: StudentSession,
  submission: TaskSubmission
): StudentSession {
  const currentNode = getCurrentNode(lessonFlow, session);
  const targetNode =
    currentNode?.completionMethod === 'submission'
      ? currentNode
      : lessonFlow.nodes.find((node) => node.completionMethod === 'submission');

  if (!targetNode) {
    return {
      ...session,
      submission
    };
  }

  return {
    ...markNodeSuccess(lessonFlow, session, targetNode.id, 'submission'),
    submission
  };
}

export function calculateScore(
  lessonFlow: LessonFlow,
  session: StudentSession
): number {
  return lessonFlow.nodes
    .filter((node) => node.required)
    .filter((node) => session.nodeResults[node.id] === 'success')
    .reduce((total, node) => total + node.score, 0);
}

function markNodeSuccess(
  lessonFlow: LessonFlow,
  session: StudentSession,
  nodeId: string,
  eventType: 'node_complete' | 'submission'
): StudentSession {
  const completedIndex = lessonFlow.nodes.findIndex((node) => node.id === nodeId);
  const nextNode = lessonFlow.nodes
    .slice(completedIndex + 1)
    .find((node) => !terminalStatuses.includes(session.nodeResults[node.id]));

  const nodeResults = {
    ...session.nodeResults,
    [nodeId]: 'success' as NodeStatus
  };

  if (nextNode && nodeResults[nextNode.id] === 'pending') {
    nodeResults[nextNode.id] = 'available';
  }

  return {
    ...session,
    nodeResults,
    events: [
      ...session.events,
      {
        id: `event-${session.events.length + 1}`,
        type: eventType,
        url: '',
        nodeId,
        timestamp: new Date().toISOString()
      }
    ]
  };
}
