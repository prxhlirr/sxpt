import type {
  ActivityEvent,
  BusinessPlatform,
  ExamDataItem,
  ExamSettings,
  GroupPlan,
  LessonPlan,
  PublishedTask,
  StudentTask,
  TrainingState,
  UnitDataPlan
} from '../domain/models';

const relativeDemoTime = (offsetMinutes: number) =>
  new Date(Date.now() + offsetMinutes * 60_000).toISOString();
const DEMO_NOW = relativeDemoTime(0);
const DEMO_START = relativeDemoTime(-60);
const DEMO_END = relativeDemoTime(180);

/**
 * Clean workspace used by the real authenticated application.
 * Business-platform definitions are retained because they are application
 * configuration, while lesson, assignment and learner data start empty.
 */
export function createEmptyTrainingState(): TrainingState {
  return {
    currentRole: 'admin',
    businessPlatforms: createDefaultBusinessPlatforms(),
    lessons: [],
    examSettings: {},
    groupPlans: {},
    unitDataPlans: {},
    dataItems: {},
    publishedTasks: [],
    studentTasks: [],
    activities: []
  };
}

export function createMockTrainingState(): TrainingState {
  const businessPlatforms = createDefaultBusinessPlatforms();
  const lessons = createLessons();
  const lessonId = lessons[0].id;

  return {
    currentRole: 'admin',
    businessPlatforms,
    lessons,
    examSettings: { [lessonId]: createExamSettings(lessonId) },
    groupPlans: { [lessonId]: createGroupPlan(lessonId) },
    unitDataPlans: { [lessonId]: createUnitPlans() },
    dataItems: { [lessonId]: createDataItems(lessonId) },
    publishedTasks: createPublishedTasks(lessonId),
    studentTasks: createStudentTasks(lessonId),
    activities: createActivities()
  };
}

export function createDefaultBusinessPlatforms(): BusinessPlatform[] {
  return [
    {
      id: 'business-platform-purchase',
      code: 'PURCHASE',
      name: '采购业务管理平台',
      baseUrl: 'internal://purchase',
      description: '采购申请、审核与业务档案归集系统。',
      status: 'ENABLED',
      modules: [
        {
          id: 'business-module-purchase-approval',
          code: 'PURCHASE_APPROVAL',
          name: '采购申请与审批',
          path: '/approval',
          description: '发起采购申请，并完成采购业务审核。',
          status: 'ENABLED',
          updatedAt: '2026-07-24T09:00:00.000Z'
        },
        {
          id: 'business-module-purchase-archive',
          code: 'PURCHASE_ARCHIVE',
          name: '采购档案归集',
          path: '/archive',
          description: '检查采购流程材料并完成业务归档。',
          status: 'ENABLED',
          updatedAt: '2026-07-24T09:00:00.000Z'
        }
      ],
      updatedAt: '2026-07-24T09:00:00.000Z'
    },
    {
      id: 'business-platform-expense',
      code: 'EXPENSE',
      name: '财务共享报销平台',
      baseUrl: 'internal://expense',
      description: '费用报销填报、财务复核与付款流程系统。',
      status: 'ENABLED',
      modules: [
        {
          id: 'business-module-expense-reimbursement',
          code: 'EXPENSE_REIMBURSEMENT',
          name: '费用报销与复核',
          path: '/reimbursement',
          description: '填报报销单并完成财务复核。',
          status: 'ENABLED',
          updatedAt: '2026-07-23T09:00:00.000Z'
        }
      ],
      updatedAt: '2026-07-23T09:00:00.000Z'
    },
    {
      id: 'business-platform-contract',
      code: 'CONTRACT',
      name: '合同全生命周期平台',
      baseUrl: 'internal://contract',
      description: '合同起草、审批、签署与归档管理系统。',
      status: 'ENABLED',
      modules: [
        {
          id: 'business-module-contract-filing',
          code: 'CONTRACT_FILING',
          name: '合同备案管理',
          path: '/filing',
          description: '完成合同备案、审批和归档。',
          status: 'ENABLED',
          updatedAt: '2026-07-22T09:00:00.000Z'
        }
      ],
      updatedAt: '2026-07-22T09:00:00.000Z'
    }
  ];
}

function createLessons(): LessonPlan[] {
  return [
    {
      id: 'lesson-purchase-v3',
      code: 'CGSQ-001',
      title: '采购申请全流程实训教案',
      moduleName: '采购业务管理',
      businessPlatformId: 'business-platform-purchase',
      businessPlatformModuleId: 'business-module-purchase-approval',
      description: '覆盖采购申请、部门审核与档案归集的三角色串行业务。',
      version: 3,
      status: 'PUBLISHED',
      teacherName: '王老师',
      tags: ['采购', '多角色', '考试'],
      objectiveMaxScore: 80,
      subjectiveMaxScore: 20,
      stages: [
        {
          id: 'stage-purchase-create',
          stageKey: 'purchase-create',
          name: '创建并提交采购申请',
          groupKey: 'handler',
          description: '经办人完成采购申请的填写和提交。',
          required: true,
          score: 35,
          completionMethod: 'mixed',
          visibility: { LEARNING: true, PRACTICE: true, EXAM: true },
          recordedSteps: [
            {
              id: 'record-purchase-create',
              title: '新建采购申请',
              pageTitle: '采购申请管理',
              actionLabel: '新建',
              selector: '[data-action="create"]',
              durationSeconds: 16,
              note: '进入新建申请页面'
            },
            {
              id: 'record-purchase-submit',
              title: '提交采购申请',
              pageTitle: '采购申请编辑',
              actionLabel: '提交',
              selector: '[data-action="submit"]',
              durationSeconds: 42,
              note: '填写必填项并提交'
            }
          ]
        },
        {
          id: 'stage-purchase-review',
          stageKey: 'purchase-review',
          name: '审核采购申请',
          groupKey: 'reviewer',
          description: '审核人复核申请内容并给出审批结论。',
          required: true,
          score: 30,
          completionMethod: 'business_check',
          visibility: { LEARNING: true, PRACTICE: true, EXAM: true },
          recordedSteps: [
            {
              id: 'record-purchase-open-todo',
              title: '打开审核待办',
              pageTitle: '审核待办',
              actionLabel: '办理',
              selector: '[data-action="handle"]',
              durationSeconds: 18,
              note: '进入当前待办'
            },
            {
              id: 'record-purchase-approve',
              title: '审核通过',
              pageTitle: '采购申请审核',
              actionLabel: '通过',
              selector: '[data-action="approve"]',
              durationSeconds: 28,
              note: '核对内容后提交审核结论'
            }
          ]
        },
        {
          id: 'stage-purchase-archive',
          stageKey: 'purchase-archive',
          name: '归集业务档案',
          groupKey: 'archiver',
          description: '归档人员检查流程材料并完成归档。',
          required: true,
          score: 15,
          completionMethod: 'submission',
          visibility: { LEARNING: true, PRACTICE: true, EXAM: true },
          recordedSteps: [
            {
              id: 'record-purchase-archive',
              title: '确认并归档',
              pageTitle: '采购档案',
              actionLabel: '归档',
              selector: '[data-action="archive"]',
              durationSeconds: 24,
              note: '确认材料完整后归档'
            }
          ]
        }
      ],
      updatedAt: '2026-07-24T09:30:00.000Z',
      publishedAt: '2026-07-24T09:30:00.000Z'
    },
    {
      id: 'lesson-expense-v1',
      code: 'FYBX-002',
      title: '费用报销业务办理教案',
      moduleName: '财务报销',
      businessPlatformId: 'business-platform-expense',
      businessPlatformModuleId: 'business-module-expense-reimbursement',
      description: '录制已完成，等待校验后发布。',
      version: 1,
      status: 'RECORDED',
      teacherName: '赵老师',
      tags: ['报销', '财务'],
      objectiveMaxScore: 80,
      subjectiveMaxScore: 20,
      stages: [
        {
          id: 'stage-expense-create',
          stageKey: 'expense-create',
          name: '填报费用报销单',
          groupKey: 'applicant',
          description: '申请人录入票据信息并提交。',
          required: true,
          score: 45,
          completionMethod: 'submission',
          visibility: { LEARNING: true, PRACTICE: true, EXAM: true },
          recordedSteps: [
            {
              id: 'record-expense-create',
              title: '填写报销单',
              pageTitle: '费用报销',
              actionLabel: '提交',
              selector: '[data-action="submit-expense"]',
              durationSeconds: 58,
              note: '录入票据及费用明细'
            }
          ]
        },
        {
          id: 'stage-expense-review',
          stageKey: 'expense-review',
          name: '财务复核',
          groupKey: 'finance',
          description: '财务人员复核票据和预算。',
          required: true,
          score: 35,
          completionMethod: 'business_check',
          visibility: { LEARNING: true, PRACTICE: true, EXAM: true },
          recordedSteps: [
            {
              id: 'record-expense-review',
              title: '复核报销单',
              pageTitle: '财务待办',
              actionLabel: '审核通过',
              selector: '[data-action="approve-expense"]',
              durationSeconds: 36,
              note: '核验票据并提交审核'
            }
          ]
        }
      ],
      updatedAt: '2026-07-23T07:20:00.000Z'
    },
    {
      id: 'lesson-contract-draft',
      code: 'HTBA-003',
      title: '合同备案教案',
      moduleName: '合同管理',
      businessPlatformId: 'business-platform-contract',
      businessPlatformModuleId: 'business-module-contract-filing',
      description: '正在配置教学点和录制内容。',
      version: 1,
      status: 'DRAFT',
      teacherName: '陈老师',
      tags: ['合同', '草稿'],
      objectiveMaxScore: 80,
      subjectiveMaxScore: 20,
      stages: [
        {
          id: 'stage-contract-draft',
          stageKey: 'contract-draft',
          name: '提交合同备案',
          groupKey: 'operator',
          description: '待补充录制步骤。',
          required: true,
          score: 80,
          completionMethod: 'mixed',
          visibility: { LEARNING: true, PRACTICE: true, EXAM: true },
          recordedSteps: []
        }
      ],
      updatedAt: '2026-07-22T11:00:00.000Z'
    }
  ];
}

function createExamSettings(lessonId: string): ExamSettings {
  return {
    lessonId,
    batchName: '2026 年第三季度采购业务能力考试',
    mode: 'EXAM',
    startAt: DEMO_START,
    endAt: DEMO_END,
    durationMinutes: 120,
    allowRetry: true,
    maxAttempts: 2,
    objectiveWeight: 80,
    subjectiveWeight: 20,
    showProgress: true,
    randomizeData: false,
    submissionFields: [
      { key: 'businessNo', label: '采购申请单号', required: true },
      { key: 'note', label: '办理说明', required: false }
    ]
  };
}

function createGroupPlan(lessonId: string): GroupPlan {
  return {
    lessonId,
    roles: [
      {
        key: 'handler',
        name: '经办组',
        color: '#2563eb',
        stageIds: ['stage-purchase-create']
      },
      {
        key: 'reviewer',
        name: '审核组',
        color: '#16a34a',
        stageIds: ['stage-purchase-review']
      },
      {
        key: 'archiver',
        name: '归档组',
        color: '#7c3aed',
        stageIds: ['stage-purchase-archive']
      }
    ],
    members: [
      member('membership-1', 'student-001', '张敏', 'unit-east', '东部事业部', 'handler', 'zhangmin'),
      member('membership-2', 'student-002', '李晨', 'unit-east', '东部事业部', 'handler', 'lichen'),
      member('membership-3', 'student-003', '周昕', 'unit-east', '东部事业部', 'reviewer', 'zhouxin'),
      member('membership-4', 'student-004', '刘洋', 'unit-east', '东部事业部', 'reviewer', 'liuyang'),
      member('membership-5', 'student-001', '张敏', 'unit-east', '东部事业部', 'archiver', 'zhangmin'),
      member('membership-6', 'student-002', '李晨', 'unit-east', '东部事业部', 'archiver', 'lichen'),
      member('membership-7', 'student-005', '王蕾', 'unit-north', '北方分公司', 'handler', 'wanglei'),
      member('membership-8', 'student-005', '王蕾', 'unit-north', '北方分公司', 'reviewer', 'wanglei'),
      member('membership-9', 'student-005', '王蕾', 'unit-north', '北方分公司', 'archiver', 'wanglei')
    ]
  };
}

function member(
  id: string,
  studentId: string,
  studentName: string,
  unitId: string,
  unitName: string,
  groupKey: string,
  accountId: string
) {
  return { id, studentId, studentName, unitId, unitName, groupKey, accountId };
}

function createUnitPlans(): UnitDataPlan[] {
  return [
    {
      unitId: 'unit-east',
      unitName: '东部事业部',
      formalCount: 2,
      spareCount: 1,
      scenario: '采购标准审批场景',
      simulateFailure: false
    },
    {
      unitId: 'unit-north',
      unitName: '北方分公司',
      formalCount: 1,
      spareCount: 1,
      scenario: '采购加急审批场景',
      simulateFailure: true
    }
  ];
}

function createDataItems(lessonId: string): ExamDataItem[] {
  return [
    dataItem('data-east-1', lessonId, 'unit-east', '东部事业部', 'FORMAL', 'IN_USE', 1),
    dataItem('data-east-2', lessonId, 'unit-east', '东部事业部', 'FORMAL', 'IN_USE', 2),
    dataItem('data-east-spare-1', lessonId, 'unit-east', '东部事业部', 'SPARE', 'READY', 3),
    dataItem('data-north-1', lessonId, 'unit-north', '北方分公司', 'FORMAL', 'IN_USE', 1),
    {
      ...dataItem(
        'data-north-spare-1',
        lessonId,
        'unit-north',
        '北方分公司',
        'SPARE',
        'GENERATION_FAILED',
        2
      ),
      failureReason: 'Mock：业务造数接口返回可重试错误'
    }
  ];
}

function dataItem(
  id: string,
  lessonId: string,
  unitId: string,
  unitName: string,
  kind: ExamDataItem['kind'],
  status: ExamDataItem['status'],
  index: number
): ExamDataItem {
  const assignedStudentTaskIds: Record<string, string[]> = {
    'data-east-1': ['student-task-east-handler', 'student-task-east-reviewer'],
    'data-east-2': ['student-task-east-handler-2', 'student-task-east-reviewer-2'],
    'data-north-1': ['student-task-north']
  };
  return {
    id,
    lessonId,
    unitId,
    unitName,
    kind,
    status,
    revision: 1,
    maskedReference: `CG-2026-${unitId === 'unit-east' ? 'E' : 'N'}***${index}`,
    assignedStudentTaskIds: assignedStudentTaskIds[id] ?? [],
    audit: [
      {
        id: `audit-${id}-1`,
        type: status === 'GENERATION_FAILED' ? 'DATA_GENERATION_FAILED' : 'DATA_GENERATION_READY',
        reason:
          status === 'GENERATION_FAILED'
            ? 'Mock：演示部分失败'
            : 'Mock：业务接口生成完成',
        at: '2026-07-24T10:00:00.000Z'
      }
    ]
  };
}

function createPublishedTasks(lessonId: string): PublishedTask[] {
  return [
    {
      id: 'published-task-purchase-q3',
      lessonId,
      title: '2026 年第三季度采购业务能力考试',
      mode: 'EXAM',
      status: 'RUNNING',
      startAt: DEMO_START,
      endAt: DEMO_END,
      assignedCount: 5,
      groupCount: 3,
      dataCount: 3,
      completedCount: 2
    }
  ];
}

function createStudentTasks(lessonId: string): StudentTask[] {
  return [
    {
      id: 'student-task-east-handler',
      publishedTaskId: 'published-task-purchase-q3',
      lessonId,
      studentId: 'student-001',
      studentName: '张敏',
      title: '采购申请全流程实训教案 · 东部事业部',
      mode: 'EXAM',
      groupKey: 'handler',
      groupKeys: ['handler', 'archiver'],
      unitId: 'unit-east',
      unitName: '东部事业部',
      dataItemId: 'data-east-1',
      attemptNumber: 1,
      submissionValues: {},
      status: 'DOING',
      currentStageIndex: 2,
      completedStageIds: ['stage-purchase-create'],
      startedAt: relativeDemoTime(-25)
    },
    {
      id: 'student-task-east-reviewer',
      publishedTaskId: 'published-task-purchase-q3',
      lessonId,
      studentId: 'student-003',
      studentName: '周昕',
      title: '采购申请全流程实训教案 · 东部事业部',
      mode: 'EXAM',
      groupKey: 'reviewer',
      groupKeys: ['reviewer'],
      unitId: 'unit-east',
      unitName: '东部事业部',
      dataItemId: 'data-east-1',
      attemptNumber: 1,
      submissionValues: {
        businessNo: 'CG-2026-E***1',
        note: '已完成采购审核'
      },
      status: 'SUBMITTED',
      currentStageIndex: 2,
      completedStageIds: ['stage-purchase-review'],
      objectiveScore: 80,
      startedAt: relativeDemoTime(-35),
      submittedAt: relativeDemoTime(-12)
    },
    {
      id: 'student-task-east-handler-2',
      publishedTaskId: 'published-task-purchase-q3',
      lessonId,
      studentId: 'student-002',
      studentName: '李晨',
      title: '采购申请全流程实训教案 · 东部事业部',
      mode: 'EXAM',
      groupKey: 'handler',
      groupKeys: ['handler', 'archiver'],
      unitId: 'unit-east',
      unitName: '东部事业部',
      dataItemId: 'data-east-2',
      attemptNumber: 1,
      submissionValues: {},
      status: 'TODO',
      currentStageIndex: 0,
      completedStageIds: []
    },
    {
      id: 'student-task-east-reviewer-2',
      publishedTaskId: 'published-task-purchase-q3',
      lessonId,
      studentId: 'student-004',
      studentName: '刘洋',
      title: '采购申请全流程实训教案 · 东部事业部',
      mode: 'EXAM',
      groupKey: 'reviewer',
      groupKeys: ['reviewer'],
      unitId: 'unit-east',
      unitName: '东部事业部',
      dataItemId: 'data-east-2',
      attemptNumber: 1,
      submissionValues: {},
      status: 'TODO',
      currentStageIndex: 0,
      completedStageIds: []
    },
    {
      id: 'student-task-north',
      publishedTaskId: 'published-task-purchase-q3',
      lessonId,
      studentId: 'student-005',
      studentName: '王蕾',
      title: '采购申请全流程实训教案 · 北方分公司',
      mode: 'EXAM',
      groupKey: 'handler',
      groupKeys: ['handler', 'reviewer', 'archiver'],
      unitId: 'unit-north',
      unitName: '北方分公司',
      dataItemId: 'data-north-1',
      attemptNumber: 1,
      submissionValues: {
        businessNo: 'CG-2026-N***1',
        note: '完整完成三角色业务流程'
      },
      status: 'GRADED',
      currentStageIndex: 3,
      completedStageIds: [
        'stage-purchase-create',
        'stage-purchase-review',
        'stage-purchase-archive'
      ],
      objectiveScore: 80,
      subjectiveScore: 18,
      comment: '业务流程完整，审核说明清晰。',
      startedAt: relativeDemoTime(-70),
      submittedAt: relativeDemoTime(-32),
      gradedAt: relativeDemoTime(-10)
    }
  ];
}

function createActivities(): ActivityEvent[] {
  return [
    activity(
      'activity-1',
      'STUDENT_TASK_GRADED',
      '王蕾的考试已完成评阅',
      '客观分 80，主观分 18'
    ),
    activity(
      'activity-2',
      'STUDENT_TASK_SUBMITTED',
      '周昕已提交审核教学点',
      '等待教师主观评分'
    ),
    activity(
      'activity-3',
      'EXAM_PUBLISHED',
      '采购业务考试已发布',
      '2 个单位、3 个角色组、3 条正式业务数据'
    ),
    activity(
      'activity-4',
      'LESSON_PUBLISHED',
      '采购申请教案 V3 已发布',
      '已冻结三个串行教学点'
    )
  ];
}

function activity(
  id: string,
  type: string,
  title: string,
  detail: string
): ActivityEvent {
  return { id, type, title, detail, at: DEMO_NOW };
}
