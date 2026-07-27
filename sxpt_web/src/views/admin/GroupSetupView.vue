<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import PageHeader from '../../components/ui/PageHeader.vue';
import WorkflowStepper from '../../components/exam/WorkflowStepper.vue';
import {
  deriveRoleGroups,
  upsertMembership
} from '../../components/exam/workflow';
import type {
  GroupMember,
  GroupPlan,
  RoleGroup
} from '../../domain/models';
import { useTrainingStore } from '../../stores/trainingStore';

interface EditableRole extends RoleGroup {
  responsibility: string;
}

interface DemoStudent {
  studentId: string;
  studentName: string;
  unitId: string;
  unitName: string;
  defaultAccount: string;
}

const route = useRoute();
const router = useRouter();
const store = useTrainingStore();
const lessonId = String(route.params.lessonId);
const lesson = computed(() => store.getLesson(lessonId));
const saved = store.state.groupPlans[lessonId];
const configurationLocked = computed(() =>
  store.state.publishedTasks.some((task) => task.lessonId === lessonId)
);

const defaultStudents: DemoStudent[] = [
  {
    studentId: 'student-001',
    studentName: '张敏',
    unitId: 'unit-east',
    unitName: '东区运营中心',
    defaultAccount: 'zhangmin'
  },
  {
    studentId: 'student-002',
    studentName: '李晨',
    unitId: 'unit-east',
    unitName: '东区运营中心',
    defaultAccount: 'lichen'
  },
  {
    studentId: 'student-003',
    studentName: '王雪',
    unitId: 'unit-east',
    unitName: '东区运营中心',
    defaultAccount: 'wangxue'
  },
  {
    studentId: 'student-004',
    studentName: '赵宇',
    unitId: 'unit-west',
    unitName: '西区业务中心',
    defaultAccount: 'zhaoyu'
  },
  {
    studentId: 'student-005',
    studentName: '陈佳',
    unitId: 'unit-west',
    unitName: '西区业务中心',
    defaultAccount: 'chenjia'
  },
  {
    studentId: 'student-006',
    studentName: '周宁',
    unitId: 'unit-west',
    unitName: '西区业务中心',
    defaultAccount: 'zhouning'
  }
];

const allStudents = computed<DemoStudent[]>(() => {
  const byId = new Map(defaultStudents.map((student) => [student.studentId, student]));
  (saved?.members ?? []).forEach((member) => {
    byId.set(member.studentId, {
      studentId: member.studentId,
      studentName: member.studentName,
      unitId: member.unitId,
      unitName: member.unitName,
      defaultAccount: member.accountId
    });
  });
  return [...byId.values()];
});

const roles = ref<EditableRole[]>(
  deriveRoleGroups(lesson.value?.stages ?? [], saved?.roles ?? []).map((role) => ({
    ...role,
    responsibility:
      (role as EditableRole).responsibility ||
      `负责${role.name}相关业务阶段的流程操作与结果提交`
  }))
);
const members = ref<GroupMember[]>(
  (saved?.members ?? []).map((member) => ({ ...member }))
);
const newRole = reactive({
  key: '',
  name: '',
  color: '#6d5dfc',
  responsibility: ''
});
const showRoleCreator = ref(false);
const feedback = ref('');
const saving = ref(false);

const examStages = computed(() => lesson.value?.stages ?? []);
const coveredStageIds = computed(
  () => new Set(roles.value.flatMap((role) => role.stageIds))
);
const uniqueLearners = computed(
  () => new Set(members.value.map((member) => member.studentId)).size
);
const multiRoleLearners = computed(() => {
  const counts = new Map<string, Set<string>>();
  members.value.forEach((member) => {
    const groupKeys = counts.get(member.studentId) ?? new Set<string>();
    groupKeys.add(member.groupKey);
    counts.set(member.studentId, groupKeys);
  });
  return [...counts.values()].filter((groupKeys) => groupKeys.size > 1).length;
});
const coveragePercent = computed(() => {
  if (!examStages.value.length) return 0;
  return Math.round(
    (examStages.value.filter((stage) => coveredStageIds.value.has(stage.id)).length /
      examStages.value.length) *
      100
  );
});
const validationErrors = computed(() => {
  const errors: string[] = [];
  if (!roles.value.length) errors.push('至少需要一个业务角色组');
  const keys = roles.value.map((role) => role.key.trim()).filter(Boolean);
  if (keys.length !== roles.value.length) errors.push('角色标识不能为空');
  if (new Set(keys).size !== keys.length) errors.push('角色标识不能重复');
  const uncovered = examStages.value.filter(
    (stage) => !coveredStageIds.value.has(stage.id)
  );
  if (uncovered.length) {
    errors.push(`尚有 ${uncovered.length} 个教案阶段未映射角色`);
  }
  const mappedStageIds = roles.value.flatMap((role) => role.stageIds);
  if (new Set(mappedStageIds).size !== mappedStageIds.length) {
    errors.push('同一教案阶段只能由一个角色负责');
  }
  roles.value.forEach((role) => {
    if (!members.value.some((member) => member.groupKey === role.key)) {
      errors.push(`角色「${role.name}」尚未分配学员`);
    }
  });
  if (members.value.some((member) => !member.accountId.trim())) {
    errors.push('已分组学员的业务账号不能为空');
  }
  const units = [...new Set(members.value.map((member) => member.unitId))];
  units.forEach((unitId) => {
    const unitName =
      members.value.find((member) => member.unitId === unitId)?.unitName ?? unitId;
    const missingRoles = roles.value.filter(
      (role) =>
        !members.value.some(
          (member) => member.unitId === unitId && member.groupKey === role.key
        )
    );
    if (missingRoles.length) {
      errors.push(
        `${unitName}尚未分配：${missingRoles.map((role) => role.name).join('、')}`
      );
    }
  });
  return errors;
});

function slugifyRoleKey(name: string) {
  const ascii = name
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
  return ascii || `role-${roles.value.length + 1}`;
}

function openRoleCreator() {
  newRole.key = `role-${roles.value.length + 1}`;
  newRole.name = '';
  newRole.color = ['#6d5dfc', '#2e75f0', '#12a879', '#e6962b'][
    roles.value.length % 4
  ];
  newRole.responsibility = '';
  showRoleCreator.value = true;
}

function addRole() {
  const key = (newRole.key || slugifyRoleKey(newRole.name)).trim();
  if (!newRole.name.trim() || !key) {
    feedback.value = '请填写角色名称和角色标识。';
    return;
  }
  if (roles.value.some((role) => role.key === key)) {
    feedback.value = '角色标识已存在，请更换后再添加。';
    return;
  }
  roles.value.push({
    key,
    name: newRole.name.trim(),
    color: newRole.color,
    responsibility:
      newRole.responsibility.trim() || `负责${newRole.name.trim()}相关流程`,
    stageIds: []
  });
  showRoleCreator.value = false;
  feedback.value = `已添加角色「${newRole.name.trim()}」，请映射业务阶段并分配成员。`;
}

function removeRole(roleKey: string) {
  const role = roles.value.find((item) => item.key === roleKey);
  roles.value = roles.value.filter((item) => item.key !== roleKey);
  members.value = members.value.filter((member) => member.groupKey !== roleKey);
  feedback.value = `已移除角色「${role?.name ?? roleKey}」及其成员关系。`;
}

function hasStage(role: EditableRole, stageId: string) {
  return role.stageIds.includes(stageId);
}

function toggleStage(role: EditableRole, stageId: string) {
  if (role.stageIds.includes(stageId)) {
    role.stageIds = role.stageIds.filter((id) => id !== stageId);
    return;
  }
  roles.value.forEach((candidate) => {
    candidate.stageIds = candidate.stageIds.filter((id) => id !== stageId);
  });
  role.stageIds = [...role.stageIds, stageId];
}

function findMembership(studentId: string, groupKey: string) {
  return members.value.find(
    (member) => member.studentId === studentId && member.groupKey === groupKey
  );
}

function isMember(studentId: string, groupKey: string) {
  return Boolean(findMembership(studentId, groupKey));
}

function toggleMembership(student: DemoStudent, role: EditableRole) {
  const existing = findMembership(student.studentId, role.key);
  if (existing) {
    members.value = members.value.filter((member) => member.id !== existing.id);
    return;
  }
  members.value = upsertMembership(members.value, {
    studentId: student.studentId,
    studentName: student.studentName,
    unitId: student.unitId,
    unitName: student.unitName,
    groupKey: role.key,
    accountId: student.defaultAccount
  });
}

function updateAccount(student: DemoStudent, role: EditableRole, accountId: string) {
  members.value = upsertMembership(members.value, {
    studentId: student.studentId,
    studentName: student.studentName,
    unitId: student.unitId,
    unitName: student.unitName,
    groupKey: role.key,
    accountId
  });
}

function roleMemberCount(roleKey: string) {
  return members.value.filter((member) => member.groupKey === roleKey).length;
}

function save(next = false) {
  feedback.value = '';
  if (validationErrors.value.length) {
    feedback.value = '分组尚未通过完整性检查，请先处理右侧问题。';
    return;
  }
  saving.value = true;
  try {
    const plan: GroupPlan = {
      lessonId,
      roles: roles.value.map((role) => ({
        ...role,
        stageIds: [...role.stageIds]
      })),
      members: members.value.map((member) => ({ ...member }))
    };
    store.saveGroupPlan(lessonId, plan);
    feedback.value = '分组方案已保存，成员将按所在单位参与对应业务阶段。';
    if (next) {
      void router.push({ name: 'exam-data', params: { lessonId } });
    }
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '分组方案保存失败';
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <main class="page exam-page">
    <PageHeader
      eyebrow="LESSON BUSINESS CHAIN · 03"
      title="分组设置"
      description="依据教案阶段动态建立任意数量的业务角色，完成单位、学员与业务账号映射。"
    >
      <button class="secondary" type="button" @click="router.push({ name: 'exam-setup', params: { lessonId } })">
        上一步
      </button>
      <button
        class="primary"
        type="button"
        :disabled="saving || configurationLocked"
        @click="save(true)"
      >
        保存并配置数据
      </button>
    </PageHeader>

    <WorkflowStepper
      :lesson-id="lessonId"
      current="groups"
      aria-label="教案编排、考试设置、分组设置、数据生成、发布任务"
    />
    <div v-if="configurationLocked" class="notice">
      此教案已有考试任务，角色与成员关系已冻结。请复制教案后创建新版本。
    </div>

    <div v-if="!lesson" class="card empty-state">
      <div><strong>未找到教案</strong><p>请返回教案列表重新选择。</p></div>
    </div>

    <template v-else>
      <section class="metric-strip four">
        <article><span>业务角色</span><strong>{{ roles.length }}</strong><small>可继续动态添加</small></article>
        <article><span>参与学员</span><strong>{{ uniqueLearners }}</strong><small>{{ members.length }} 条角色关系</small></article>
        <article><span>多角色学员</span><strong>{{ multiRoleLearners }}</strong><small>可由一人走完整流程</small></article>
        <article :class="{ warning: coveragePercent < 100 }"><span>阶段覆盖</span><strong>{{ coveragePercent }}%</strong><small>{{ coveredStageIds.size }}/{{ examStages.length }} 个教案阶段</small></article>
      </section>

      <div class="group-layout">
        <section class="group-main">
          <article class="card">
            <div class="card-header">
              <div>
                <h2>业务角色与阶段映射</h2>
                <p>角色来自教案的 groupKey，也可继续增加；每个阶段由一个业务角色负责。</p>
              </div>
              <button class="secondary" type="button" @click="openRoleCreator">＋ 新增角色</button>
            </div>
            <div class="card-body role-list">
              <article v-for="role in roles" :key="role.key" class="role-card" :style="{ '--role-color': role.color }">
                <div class="role-card__head">
                  <span class="role-badge" :style="{ background: role.color }">{{ role.name.slice(0, 1) }}</span>
                  <label>
                    角色名称
                    <input v-model.trim="role.name" />
                  </label>
                  <label>
                    角色标识
                    <input v-model.trim="role.key" disabled />
                  </label>
                  <label class="color-field">
                    颜色
                    <input v-model="role.color" type="color" />
                  </label>
                  <button class="danger icon-action" type="button" aria-label="移除角色" @click="removeRole(role.key)">×</button>
                </div>
                <label class="field">
                  职责说明
                  <input v-model.trim="role.responsibility" placeholder="说明该角色在业务流程中的职责" />
                </label>
                <div class="stage-mapping">
                  <span>负责阶段</span>
                  <label
                    v-for="stage in examStages"
                    :key="stage.id"
                    class="stage-chip"
                    :class="{ selected: hasStage(role, stage.id) }"
                  >
                    <input
                      type="checkbox"
                      :checked="hasStage(role, stage.id)"
                      @change="toggleStage(role, stage.id)"
                    />
                    {{ stage.name }}
                  </label>
                </div>
                <footer>
                  <span>{{ role.stageIds.length }} 个阶段</span>
                  <span>{{ roleMemberCount(role.key) }} 名成员</span>
                </footer>
              </article>
              <div v-if="!roles.length" class="inline-empty">
                当前教案尚未产生角色。请新增角色并将其映射到考试阶段。
              </div>
            </div>
          </article>

          <article class="card member-card">
            <div class="card-header">
              <div>
                <h2>成员分配与业务账号映射</h2>
                <p>勾选角色即可建立关系；同一学员可承担多个角色，并可分别配置业务账号。</p>
              </div>
              <span class="status-pill success">Mock 学员名册</span>
            </div>
            <div class="table-wrap member-matrix">
              <table>
                <thead>
                  <tr>
                    <th>学员 / 单位</th>
                    <th v-for="role in roles" :key="role.key">
                      <span class="role-column-title"><i :style="{ background: role.color }" />{{ role.name }}</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="student in allStudents" :key="student.studentId">
                    <td>
                      <strong>{{ student.studentName }}</strong>
                      <small>{{ student.unitName }} · {{ student.studentId }}</small>
                    </td>
                    <td v-for="role in roles" :key="role.key">
                      <label class="membership-toggle">
                        <input
                          type="checkbox"
                          :checked="isMember(student.studentId, role.key)"
                          @change="toggleMembership(student, role)"
                        />
                        <span>{{ isMember(student.studentId, role.key) ? '已分配' : '未分配' }}</span>
                      </label>
                      <input
                        v-if="isMember(student.studentId, role.key)"
                        class="account-input"
                        :value="findMembership(student.studentId, role.key)?.accountId"
                        aria-label="业务账号"
                        placeholder="业务账号"
                        @input="updateAccount(student, role, ($event.target as HTMLInputElement).value)"
                      />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </article>

          <div v-if="feedback" class="notice" :class="{ danger: validationErrors.length }">{{ feedback }}</div>
          <div class="bottom-actions">
            <button
              class="secondary"
              type="button"
              :disabled="configurationLocked"
              @click="save(false)"
            >
              仅保存分组
            </button>
            <button
              class="primary"
              type="button"
              :disabled="
                validationErrors.length > 0 || saving || configurationLocked
              "
              @click="save(true)"
            >
              下一步：数据生成 →
            </button>
          </div>
        </section>

        <aside class="group-aside">
          <article class="card sticky-card">
            <div class="card-header">
              <div><h2>分组完整性</h2><p>保存和发布前置检查</p></div>
              <span class="summary-dot" :class="{ ready: !validationErrors.length }" />
            </div>
            <div class="card-body role-summary">
              <div v-for="role in roles" :key="role.key">
                <i :style="{ background: role.color }" />
                <span><strong>{{ role.name }}</strong><small>{{ role.stageIds.length }} 阶段 · {{ roleMemberCount(role.key) }} 人</small></span>
              </div>
            </div>
            <div class="validation-panel" :class="{ valid: !validationErrors.length }">
              <strong>{{ validationErrors.length ? `发现 ${validationErrors.length} 个问题` : '分组校验通过' }}</strong>
              <ul v-if="validationErrors.length">
                <li v-for="error in validationErrors" :key="error">{{ error }}</li>
              </ul>
              <p v-else>角色、阶段、成员和账号均已覆盖，可按单位生成考试数据。</p>
            </div>
          </article>
        </aside>
      </div>
    </template>

    <div v-if="showRoleCreator" class="dialog-backdrop" @click.self="showRoleCreator = false">
      <section class="dialog">
        <header class="dialog-header"><h2>新增业务角色</h2><button class="icon-action" type="button" @click="showRoleCreator = false">×</button></header>
        <div class="dialog-body form-grid">
          <label class="field">
            角色名称
            <input v-model.trim="newRole.name" placeholder="例如：经办人员" @blur="!newRole.key && (newRole.key = slugifyRoleKey(newRole.name))" />
          </label>
          <label class="field">
            角色标识
            <input v-model.trim="newRole.key" placeholder="maker" />
          </label>
          <label class="field">
            标识颜色
            <input v-model="newRole.color" type="color" />
          </label>
          <label class="field wide">
            职责说明
            <textarea v-model.trim="newRole.responsibility" rows="3" placeholder="描述该角色负责的业务内容" />
          </label>
        </div>
        <footer class="dialog-footer">
          <button class="secondary" type="button" @click="showRoleCreator = false">取消</button>
          <button class="primary" type="button" @click="addRole">添加角色</button>
        </footer>
      </section>
    </div>
  </main>
</template>
