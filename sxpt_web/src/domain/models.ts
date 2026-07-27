export type PortalRole = 'admin' | 'teacher' | 'student';
export type LessonStatus = 'DRAFT' | 'RECORDED' | 'PUBLISHED' | 'ARCHIVED';
export type RunMode = 'LEARNING' | 'PRACTICE' | 'EXAM';
export type CompletionMethod =
  | 'click'
  | 'business_check'
  | 'submission'
  | 'manual'
  | 'mixed';
export type DataItemStatus =
  | 'GENERATING'
  | 'READY'
  | 'GENERATION_FAILED'
  | 'DISABLED'
  | 'IN_USE';

export interface CaptureRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface RecordedStep {
  id: string;
  title: string;
  pageTitle: string;
  actionLabel: string;
  selector: string;
  durationSeconds: number;
  note: string;
  kind?: 'action' | 'guide';
  actionType?: 'click' | 'input' | 'select' | 'submit' | 'guide';
  url?: string;
  rect?: CaptureRect;
  selectorCandidates?: string[];
  valueMasked?: string;
  teachingText?: string;
  practiceHint?: string;
  examGoal?: string;
  required?: boolean;
  failurePolicy?: 'stop' | 'retry' | 'skip';
  recordedViewport?: {
    width: number;
    height: number;
  };
}

export interface LessonStage {
  id: string;
  stageKey: string;
  name: string;
  groupKey: string;
  description: string;
  required: boolean;
  score: number;
  completionMethod: CompletionMethod;
  visibility: Record<RunMode, boolean>;
  recordedSteps: RecordedStep[];
}

export interface LessonPlan {
  id: string;
  code: string;
  title: string;
  moduleName: string;
  description: string;
  version: number;
  status: LessonStatus;
  teacherName: string;
  tags: string[];
  objectiveMaxScore: number;
  subjectiveMaxScore: number;
  stages: LessonStage[];
  updatedAt: string;
  publishedAt?: string;
}

export interface ExamSettings {
  lessonId: string;
  batchName: string;
  mode: RunMode;
  startAt: string;
  endAt: string;
  durationMinutes: number;
  allowRetry: boolean;
  maxAttempts: number;
  objectiveWeight: number;
  subjectiveWeight: number;
  showProgress: boolean;
  randomizeData: boolean;
  submissionFields: Array<{
    key: string;
    label: string;
    required: boolean;
  }>;
}

export interface RoleGroup {
  key: string;
  name: string;
  color: string;
  stageIds: string[];
}

export interface GroupMember {
  id: string;
  studentId: string;
  studentName: string;
  unitId: string;
  unitName: string;
  groupKey: string;
  accountId: string;
}

export interface GroupPlan {
  lessonId: string;
  roles: RoleGroup[];
  members: GroupMember[];
}

export interface UnitDataPlan {
  unitId: string;
  unitName: string;
  formalCount: number;
  spareCount: number;
  scenario: string;
  simulateFailure: boolean;
}

export interface DataAuditEvent {
  id: string;
  type: string;
  reason: string;
  at: string;
}

export interface ExamDataItem {
  id: string;
  lessonId: string;
  unitId: string;
  unitName: string;
  kind: 'FORMAL' | 'SPARE';
  status: DataItemStatus;
  revision: number;
  maskedReference: string;
  replacementOf?: string;
  failureReason?: string;
  assignedStudentTaskIds?: string[];
  audit: DataAuditEvent[];
}

export interface PublishedTask {
  id: string;
  lessonId: string;
  title: string;
  mode: RunMode;
  status: 'SCHEDULED' | 'RUNNING' | 'FINISHED';
  startAt: string;
  endAt: string;
  assignedCount: number;
  groupCount: number;
  dataCount: number;
  completedCount: number;
}

export interface StudentTask {
  id: string;
  publishedTaskId: string;
  lessonId: string;
  studentId: string;
  studentName: string;
  title: string;
  mode: RunMode;
  groupKey: string;
  groupKeys: string[];
  unitId: string;
  unitName: string;
  dataItemId: string;
  attemptNumber: number;
  submissionValues: Record<string, string>;
  status: 'TODO' | 'DOING' | 'SUBMITTED' | 'GRADED';
  currentStageIndex: number;
  completedStageIds: string[];
  objectiveScore?: number;
  subjectiveScore?: number;
  comment?: string;
  startedAt?: string;
  submittedAt?: string;
  gradedAt?: string;
}

export interface ActivityEvent {
  id: string;
  type: string;
  title: string;
  detail: string;
  at: string;
}

export interface TrainingState {
  currentRole: PortalRole;
  lessons: LessonPlan[];
  examSettings: Record<string, ExamSettings>;
  groupPlans: Record<string, GroupPlan>;
  unitDataPlans: Record<string, UnitDataPlan[]>;
  dataItems: Record<string, ExamDataItem[]>;
  publishedTasks: PublishedTask[];
  studentTasks: StudentTask[];
  activities: ActivityEvent[];
}

/**
 * The Mock repository only relies on the Web Storage subset it actually uses,
 * making it injectable in Vitest, SSR previews and a browser.
 */
export interface StorageLike {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}
