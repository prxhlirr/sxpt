export type CompletionMethod =
  | 'click'
  | 'business_check'
  | 'submission'
  | 'manual'
  | 'mixed';

export type RunnerMode = 'learning' | 'practice' | 'exam';

export type RecordedActionType = 'click' | 'input' | 'select' | 'submit';

export type NodeStatus =
  | 'pending'
  | 'available'
  | 'clicked'
  | 'success'
  | 'failed'
  | 'manual_pending';

export interface Rect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface RecordedViewport {
  width: number;
  height: number;
  devicePixelRatio: number;
}

export interface BusinessActionPayload {
  actionType: RecordedActionType;
  url: string;
  selector: string;
  selectorCandidates?: string[];
  text: string;
  value?: string;
  rect: Rect;
  timestamp: string;
  clientEventId?: string;
  sdkSessionId?: string;
  sequenceNo?: number;
  stableKey?: string;
  pageTitle?: string;
  inputValueMasked?: string;
  recordedViewport?: RecordedViewport;
}

export interface NodeDef {
  id: string;
  code: string;
  name: string;
  type: string;
  defaultScore: number;
  defaultCompletionMethod: CompletionMethod;
  needBusinessCheck: boolean;
  enabled: boolean;
  category: string;
}

export interface FlowTemplateNode {
  id: string;
  nodeDefId: string;
  name: string;
  required: boolean;
  score: number;
  completionMethod: CompletionMethod;
}

export interface FlowTemplateEdge {
  id: string;
  from: string;
  to: string;
}

export interface FlowTemplate {
  id: string;
  moduleName: string;
  title: string;
  version: string;
  nodes: FlowTemplateNode[];
  edges: FlowTemplateEdge[];
  status: 'draft' | 'published' | 'disabled';
}

export interface NodeBinding {
  url: string;
  selector: string;
  rect: Rect;
  actionText: string;
}

export interface LessonNode {
  id: string;
  nodeDefId: string;
  name: string;
  required: boolean;
  score: number;
  completionMethod: CompletionMethod;
  teachingText: string;
  binding?: NodeBinding;
}

export interface ModePolicy {
  showFullFlow: boolean;
  showTeachingText: boolean;
  showOverlayHint: boolean;
  allowFreeBrowse: boolean;
  enforceOrder: boolean;
  showSubmission: boolean;
  silentRecord: boolean;
}

export interface LessonFlow {
  id: string;
  templateId: string;
  title: string;
  moduleName: string;
  nodes: LessonNode[];
  edges: FlowTemplateEdge[];
  modePolicies: Record<RunnerMode, ModePolicy>;
  publishStatus: 'draft' | 'published';
}

export interface RecordedEvent {
  id: string;
  type: 'business_action' | 'overlay_click' | 'node_complete' | 'submission';
  actionType?: RecordedActionType;
  url: string;
  selector?: string;
  text?: string;
  value?: string;
  nodeId?: string;
  timestamp: string;
}

export type StepPersistenceStatus =
  | 'local'
  | 'reporting'
  | 'reported'
  | 'draft_pending'
  | 'confirmed'
  | 'discarded'
  | 'error';

export interface StepPersistence {
  eventId?: string;
  resourceSnapshotId?: string;
  actionDraftId?: string;
  taskStepId?: string;
  connectorResourceId?: string;
  status: StepPersistenceStatus;
  errorMessage?: string;
}

export interface GuideAnchor {
  mode: 'element' | 'viewport' | 'center';
  selector?: string;
  selectorCandidates?: string[];
  rect?: Rect;
  position?: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';
}

export interface GuideImage {
  src: string;
  name: string;
  mimeType: string;
  width: number;
  height: number;
  storageType: 'inline' | 'remote';
}

export interface RecordedStep {
  id: string;
  nodeId: string;
  order: number;
  title: string;
  actionType: RecordedActionType;
  selector: string;
  rect: Rect;
  text: string;
  value?: string;
  url: string;
  teachingText: string;
  practiceHint: string;
  examGoal: string;
  completionMethod: CompletionMethod;
  required?: boolean;
  advanceMode?: 'manual' | 'auto';
  durationMs?: number;
  failurePolicy?: 'stop' | 'retry' | 'skip';
  persistence?: StepPersistence;
  clientEventId?: string;
  sdkSessionId?: string;
  sequenceNo?: number;
  stableKey?: string;
  selectorCandidates?: string[];
  pageTitle?: string;
  inputValueMasked?: string;
  recordedViewport?: RecordedViewport;
  kind?: 'action' | 'guide';
  anchor?: GuideAnchor;
  guideImage?: GuideImage;
}

export type RecordedActionStep = RecordedStep & { kind?: 'action' };
export type RecordedGuideStep = RecordedStep & {
  kind: 'guide';
  anchor: GuideAnchor;
};

export interface RecordedSegment {
  id: string;
  segmentNo: number;
  title: string;
  actorType?: string;
  externalOrgId?: string;
  externalOrgName?: string;
  externalRoleId?: string;
  externalRoleName?: string;
  targetUrl: string;
  switchReason?: string;
  launchToken?: string;
  expireTime?: string;
  status: 'recording' | 'saving' | 'saved' | 'error';
  steps: RecordedStep[];
}

export interface RecordedFlow {
  id: string;
  title: string;
  businessUrl: string;
  source: 'template' | 'custom';
  steps: RecordedStep[];
  segments?: RecordedSegment[];
  publishStatus: 'draft' | 'published';
}

export interface TaskSubmission {
  businessId: string;
  attachmentName: string;
  description: string;
  submittedAt: string;
}

export interface StudentSession {
  id: string;
  lessonFlowId: string;
  mode: RunnerMode;
  nodeResults: Record<string, NodeStatus>;
  events: RecordedEvent[];
  submission?: TaskSubmission;
}
