import { describe, expect, it } from 'vitest';
import examSetupSource from '../src/views/admin/ExamSetupView.vue?raw';
import groupSetupSource from '../src/views/admin/GroupSetupView.vue?raw';
import examDataSource from '../src/views/admin/ExamDataView.vue?raw';
import publishCenterSource from '../src/views/admin/PublishCenterView.vue?raw';
import type {
  ExamDataItem,
  ExamSettings,
  GroupMember,
  LessonStage,
  RoleGroup
} from '../src/domain/models';
import {
  buildPublishChecklist,
  deriveRoleGroups,
  getDataActions,
  summarizeExamData,
  upsertMembership,
  validateExamDraft
} from '../src/components/exam/workflow';

const stage = (id: string, groupKey: string, name: string): LessonStage => ({
  id,
  stageKey: id,
  name,
  groupKey,
  description: `${name}业务`,
  required: true,
  score: 20,
  completionMethod: 'click',
  visibility: {
    LEARNING: true,
    PRACTICE: true,
    EXAM: true
  },
  recordedSteps: []
});

const validSettings = (): ExamSettings => ({
  lessonId: 'lesson-1',
  batchName: '2026 年采购业务考试',
  mode: 'EXAM',
  startAt: '2026-08-01T09:00',
  endAt: '2026-08-01T11:00',
  durationMinutes: 90,
  allowRetry: true,
  maxAttempts: 2,
  objectiveWeight: 80,
  subjectiveWeight: 20,
  showProgress: true,
  randomizeData: false,
  submissionFields: [{ key: 'businessNo', label: '业务单号', required: true }]
});

const dataItem = (
  id: string,
  kind: ExamDataItem['kind'],
  status: ExamDataItem['status']
): ExamDataItem => ({
  id,
  lessonId: 'lesson-1',
  unitId: 'unit-1',
  unitName: '第一事业部',
  kind,
  status,
  revision: 1,
  maskedReference: `NO-${id}`,
  audit: []
});

describe('考试链路业务规则', () => {
  it('从教案阶段动态衍生任意数量角色组，并保留已有角色配置', () => {
    const stages = [
      stage('stage-1', 'maker', '经办'),
      stage('stage-2', 'reviewer', '审核'),
      stage('stage-3', 'archiver', '归档')
    ];
    const existing: RoleGroup[] = [
      {
        key: 'reviewer',
        name: '复核专员',
        color: '#16a34a',
        stageIds: ['stage-2']
      }
    ];

    const roles = deriveRoleGroups(stages, existing);

    expect(roles).toHaveLength(3);
    expect(roles.map((role) => role.key)).toEqual(['maker', 'reviewer', 'archiver']);
    expect(roles.find((role) => role.key === 'reviewer')?.name).toBe('复核专员');
    expect(roles.find((role) => role.key === 'maker')?.stageIds).toEqual(['stage-1']);
  });

  it('允许同一学员加入多个角色组，但不会重复加入同一个角色', () => {
    const original: GroupMember[] = [];
    const maker = upsertMembership(original, {
      studentId: 'student-1',
      studentName: '张敏',
      unitId: 'unit-1',
      unitName: '第一事业部',
      groupKey: 'maker',
      accountId: 'zhangmin'
    });
    const multiRole = upsertMembership(maker, {
      studentId: 'student-1',
      studentName: '张敏',
      unitId: 'unit-1',
      unitName: '第一事业部',
      groupKey: 'reviewer',
      accountId: 'zhangmin-review'
    });
    const duplicate = upsertMembership(multiRole, {
      studentId: 'student-1',
      studentName: '张敏',
      unitId: 'unit-1',
      unitName: '第一事业部',
      groupKey: 'reviewer',
      accountId: 'zhangmin-new'
    });

    expect(multiRole).toHaveLength(2);
    expect(new Set(multiRole.map((member) => member.groupKey))).toEqual(
      new Set(['maker', 'reviewer'])
    );
    expect(duplicate).toHaveLength(2);
    expect(
      duplicate.find((member) => member.groupKey === 'reviewer')?.accountId
    ).toBe('zhangmin-new');
  });

  it('要求主客观评分权重合计为 100，并校验时间和动态提交字段', () => {
    expect(validateExamDraft(validSettings())).toEqual([]);
    expect(
      validateExamDraft({
        ...validSettings(),
        objectiveWeight: 70,
        subjectiveWeight: 20,
        endAt: '2026-08-01T08:00',
        submissionFields: [{ key: '', label: '', required: true }]
      })
    ).toEqual(
      expect.arrayContaining([
        '客观评分与主观评分权重合计必须为 100%',
        '结束时间必须晚于开始时间',
        '提交字段的标识和名称不能为空'
      ])
    );
  });

  it('按数据状态给出合法操作，并计算可用率和正式数据数量', () => {
    expect(getDataActions(dataItem('failed', 'FORMAL', 'GENERATION_FAILED'))).toEqual([
      'retry'
    ]);
    expect(getDataActions(dataItem('spare', 'SPARE', 'READY'))).toEqual([
      'promote',
      'replace',
      'disable'
    ]);
    expect(getDataActions(dataItem('formal', 'FORMAL', 'READY'))).toEqual([
      'replace',
      'disable'
    ]);
    expect(getDataActions(dataItem('used', 'FORMAL', 'IN_USE'))).toEqual([]);

    const summary = summarizeExamData([
      dataItem('ready-formal', 'FORMAL', 'READY'),
      dataItem('ready-spare', 'SPARE', 'READY'),
      dataItem('failed', 'FORMAL', 'GENERATION_FAILED'),
      dataItem('disabled', 'FORMAL', 'DISABLED')
    ]);
    expect(summary).toMatchObject({
      total: 4,
      ready: 2,
      readyFormal: 1,
      failed: 1,
      disabled: 1,
      availability: 50
    });
  });

  it('发布检查能明确阻断原因，也能在五步齐备时允许发布', () => {
    const blocked = buildPublishChecklist({
      lessonPublished: true,
      examConfigured: false,
      groupsValid: false,
      dataReady: false,
      alreadyPublished: false
    });
    expect(blocked.ready).toBe(false);
    expect(blocked.items).toHaveLength(5);
    expect(blocked.reasons).toEqual(
      expect.arrayContaining([
        '考试参数尚未完成配置',
        '分组未覆盖全部考试教学点或尚无成员',
        '正式考试数据不足，需完成生成或补齐'
      ])
    );

    const ready = buildPublishChecklist({
      lessonPublished: true,
      examConfigured: true,
      groupsValid: true,
      dataReady: true,
      alreadyPublished: false
    });
    expect(ready.ready).toBe(true);
    expect(ready.reasons).toEqual([]);
    expect(ready.items.every((item) => item.passed)).toBe(true);
  });
});

describe('后台考试链路页面', () => {
  it('提供考试、分组、数据和发布四个完整页面', () => {
    [
      examSetupSource,
      groupSetupSource,
      examDataSource,
      publishCenterSource
    ].forEach((page) => expect(page).toContain('<template>'));
  });

  it('每个页面都呈现完整业务 stepper，且按顺序串接下一步', () => {
    const pages = [
      examSetupSource,
      groupSetupSource,
      examDataSource
    ];

    pages.forEach((page) => {
      expect(page).toContain('WorkflowStepper');
      expect(page).toContain('教案编排');
      expect(page).toContain('考试设置');
      expect(page).toContain('分组设置');
      expect(page).toContain('数据生成');
      expect(page).toContain('发布任务');
    });
    expect(pages[0]).toContain('store.saveExamSettings');
    expect(pages[1]).toContain('store.saveGroupPlan');
    expect(pages[2]).toContain('store.generateData');
    expect(publishCenterSource).not.toContain('WorkflowStepper');
    expect(publishCenterSource).not.toContain('store.publishExam');
  });

  it('分组界面不写死 A/B，支持角色增删、成员多角色和账号映射', () => {
    const page = groupSetupSource;

    expect(page).toMatch(/v-for="[^"]*role[^"]*roles/);
    expect(page).toContain('addRole');
    expect(page).toContain('removeRole');
    expect(page).toContain('upsertMembership');
    expect(page).toContain('accountId');
    expect(page).toContain('同一学员可承担多个角色');
    expect(page).not.toMatch(/A\s*组|B\s*组/);
  });

  it('数据页面连通生成、筛选、审计和全部状态操作，操作要求填写原因', () => {
    const page = examDataSource;

    [
      'store.generateData',
      'store.retryData',
      'store.disableData',
      'store.promoteData',
      'store.replaceData',
      '操作原因',
      '审计详情',
      'simulateFailure'
    ].forEach((keyword) => expect(page).toContain(keyword));
  });

  it('发布中心只保留教师讲解与学习练习发布，并自动准备练习数据', () => {
    const page = publishCenterSource;

    expect(page).toContain('教师讲解');
    expect(page).toContain('发布学生学习与练习');
    expect(page).toContain('store.publishLearningAndPracticeRemote');
    expect(page).toContain('批次准备的数据生成操作');
    expect(page).toContain("name: 'student-tasks'");
    expect(page).not.toContain('buildPublishChecklist');
    expect(page).not.toContain('store.publishExam');
  });
});
