import type {
  ExamDataItem,
  ExamSettings,
  GroupMember,
  LessonStage,
  RoleGroup
} from '../../domain/models';

const ROLE_COLORS = [
  '#6d5dfc',
  '#2e75f0',
  '#12a879',
  '#e6962b',
  '#e35662',
  '#7b61a8'
];

export type DataAction = 'retry' | 'disable' | 'promote' | 'replace';

export interface ExamDataSummary {
  total: number;
  ready: number;
  readyFormal: number;
  readySpare: number;
  failed: number;
  disabled: number;
  inUse: number;
  availability: number;
}

export interface PublishReadinessInput {
  lessonPublished: boolean;
  examConfigured: boolean;
  groupsValid: boolean;
  dataReady: boolean;
  alreadyPublished: boolean;
}

export interface PublishChecklistItem {
  key: 'lesson' | 'exam' | 'groups' | 'data' | 'task';
  label: string;
  detail: string;
  passed: boolean;
  routeName: string;
}

export function deriveRoleGroups(
  stages: LessonStage[],
  existing: RoleGroup[] = []
): RoleGroup[] {
  const existingByKey = new Map(existing.map((role) => [role.key, role]));
  const orderedKeys = [
    ...new Set([
      ...stages.map((item) => item.groupKey.trim()).filter(Boolean),
      ...existing.map((role) => role.key.trim()).filter(Boolean)
    ])
  ];

  return orderedKeys.map((key, index) => {
    const configured = existingByKey.get(key);
    const mappedStageIds = stages
      .filter((item) => item.groupKey === key)
      .map((item) => item.id);
    return {
      key,
      name: configured?.name || key,
      color: configured?.color || ROLE_COLORS[index % ROLE_COLORS.length],
      stageIds: [...new Set([...mappedStageIds, ...(configured?.stageIds ?? [])])]
    };
  });
}

export function upsertMembership(
  members: GroupMember[],
  membership: Omit<GroupMember, 'id'> & { id?: string }
): GroupMember[] {
  const matchIndex = members.findIndex(
    (member) =>
      member.studentId === membership.studentId &&
      member.groupKey === membership.groupKey &&
      member.unitId === membership.unitId
  );
  const next: GroupMember = {
    ...membership,
    id:
      membership.id ||
      members[matchIndex]?.id ||
      `membership-${membership.studentId}-${membership.groupKey}-${membership.unitId}`
  };

  if (matchIndex < 0) return [...members, next];
  return members.map((member, index) => (index === matchIndex ? next : member));
}

export function validateExamDraft(settings: ExamSettings): string[] {
  const errors: string[] = [];
  if (!settings.batchName.trim()) errors.push('考试批次名称不能为空');
  if (!settings.startAt || !settings.endAt) {
    errors.push('考试开始和结束时间不能为空');
  } else if (new Date(settings.endAt).getTime() <= new Date(settings.startAt).getTime()) {
    errors.push('结束时间必须晚于开始时间');
  }
  if (!Number.isFinite(settings.durationMinutes) || settings.durationMinutes <= 0) {
    errors.push('考试时长必须大于 0 分钟');
  }
  if (
    settings.allowRetry &&
    (!Number.isInteger(settings.maxAttempts) || settings.maxAttempts < 1)
  ) {
    errors.push('允许重答时，最大作答次数至少为 1');
  }
  if (settings.objectiveWeight + settings.subjectiveWeight !== 100) {
    errors.push('客观评分与主观评分权重合计必须为 100%');
  }
  if (
    settings.objectiveWeight < 0 ||
    settings.subjectiveWeight < 0 ||
    settings.objectiveWeight > 100 ||
    settings.subjectiveWeight > 100
  ) {
    errors.push('评分权重必须在 0% 到 100% 之间');
  }
  if (
    settings.submissionFields.some(
      (field) => !field.key.trim() || !field.label.trim()
    )
  ) {
    errors.push('提交字段的标识和名称不能为空');
  }
  const keys = settings.submissionFields.map((field) => field.key.trim()).filter(Boolean);
  if (new Set(keys).size !== keys.length) errors.push('提交字段标识不能重复');
  return errors;
}

export function getDataActions(item: ExamDataItem): DataAction[] {
  if (item.status === 'IN_USE' || item.status === 'DISABLED') return [];
  if (item.status === 'GENERATION_FAILED') return ['retry'];
  if (item.status !== 'READY') return [];
  return item.kind === 'SPARE'
    ? ['promote', 'replace', 'disable']
    : ['replace', 'disable'];
}

export function summarizeExamData(items: ExamDataItem[]): ExamDataSummary {
  const ready = items.filter((item) => item.status === 'READY').length;
  return {
    total: items.length,
    ready,
    readyFormal: items.filter(
      (item) => item.status === 'READY' && item.kind === 'FORMAL'
    ).length,
    readySpare: items.filter(
      (item) => item.status === 'READY' && item.kind === 'SPARE'
    ).length,
    failed: items.filter((item) => item.status === 'GENERATION_FAILED').length,
    disabled: items.filter((item) => item.status === 'DISABLED').length,
    inUse: items.filter((item) => item.status === 'IN_USE').length,
    availability: items.length ? Math.round((ready / items.length) * 100) : 0
  };
}

export function buildPublishChecklist(input: PublishReadinessInput): {
  items: PublishChecklistItem[];
  reasons: string[];
  ready: boolean;
} {
  const prerequisites =
    input.lessonPublished &&
    input.examConfigured &&
    input.groupsValid &&
    input.dataReady;
  const items: PublishChecklistItem[] = [
    {
      key: 'lesson',
      label: '教案已发布',
      detail: input.lessonPublished
        ? '教案版本已锁定，可用于考试'
        : '请先完成教案编排、录制检查并发布',
      passed: input.lessonPublished,
      routeName: 'lesson-editor'
    },
    {
      key: 'exam',
      label: '考试已配置',
      detail: input.examConfigured
        ? '考试时间、评分和提交字段已配置'
        : '请补齐考试时间、评分权重及提交要求',
      passed: input.examConfigured,
      routeName: 'exam-setup'
    },
    {
      key: 'groups',
      label: '分组有效',
      detail: input.groupsValid
        ? '角色覆盖全部阶段且已分配学员'
        : '请配置角色、阶段映射和学员业务账号',
      passed: input.groupsValid,
      routeName: 'group-setup'
    },
    {
      key: 'data',
      label: '数据已就绪',
      detail: input.dataReady
        ? '各单位正式数据可覆盖当前角色组'
        : '请生成足量正式业务数据并处理失败项',
      passed: input.dataReady,
      routeName: 'exam-data'
    },
    {
      key: 'task',
      label: input.alreadyPublished ? '任务已发布' : '任务可发布',
      detail: input.alreadyPublished
        ? '学生任务已生成，可进入教学监控'
        : prerequisites
          ? '全部前置条件通过，可生成学生任务'
          : '完成前四项后方可发布考试任务',
      passed: prerequisites,
      routeName: 'publish-center'
    }
  ];
  const reasons: string[] = [];
  if (!input.lessonPublished) reasons.push('教案尚未发布');
  if (!input.examConfigured) reasons.push('考试参数尚未完成配置');
  if (!input.groupsValid) reasons.push('分组未覆盖全部考试阶段或尚无成员');
  if (!input.dataReady) reasons.push('正式考试数据不足，需完成生成或补齐');

  return {
    items,
    reasons,
    ready: prerequisites && !input.alreadyPublished
  };
}
