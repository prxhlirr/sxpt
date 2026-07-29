<script setup lang="ts">
import { ref } from 'vue';
import type { WorkflowRoleGroupInput } from '../../types/workflow';

const props = defineProps<{
  groups: WorkflowRoleGroupInput[];
  readonly?: boolean;
}>();

const emit = defineEmits<{
  add: [group: WorkflowRoleGroupInput];
  remove: [groupId: string];
}>();

const groupName = ref('');
const groupKey = ref('');
const groupColor = ref('#2563eb');

function addGroup() {
  if (props.readonly) return;
  const name = groupName.value.trim();
  if (!name) return;
  emit('add', {
    id: createId('group'),
    groupKey: groupKey.value.trim() || `GROUP_${props.groups.length + 1}`,
    name,
    color: groupColor.value,
    sequenceNo: props.groups.length + 1
  });
  groupName.value = '';
  groupKey.value = '';
}

function createId(prefix: string) {
  const suffix = typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `${prefix}-${suffix}`;
}
</script>

<template>
  <section class="workflow-card">
    <header>
      <div>
        <p class="section-kicker">人员协作</p>
        <h2>实训分组</h2>
      </div>
      <span class="count">{{ groups.length }} 组</span>
    </header>

    <p class="section-help">
      分组用于分配学员承担的阶段，不等同于业务系统中的角色或单位。
    </p>

    <ol v-if="groups.length" class="group-list">
      <li v-for="group in groups" :key="group.id">
        <span
          class="group-color"
          :style="{ background: group.color || '#64748b' }"
        />
        <div>
          <strong>{{ group.name }}</strong>
          <small>{{ group.groupKey }}</small>
        </div>
        <button
          type="button"
          class="icon-button danger"
          :disabled="readonly"
          :aria-label="`删除${group.name}`"
          @click="emit('remove', group.id)"
        >
          ×
        </button>
      </li>
    </ol>
    <p v-else class="empty-copy">先创建至少一个实训分组。</p>

    <div class="quick-add">
      <input v-model="groupName" :disabled="readonly" placeholder="分组名称，如：经办组" />
      <input v-model="groupKey" :disabled="readonly" placeholder="分组编码，如：HANDLER" />
      <input v-model="groupColor" :disabled="readonly" type="color" aria-label="分组颜色" />
      <button type="button" class="primary-small" :disabled="readonly" @click="addGroup">
        添加分组
      </button>
    </div>
  </section>
</template>

<style scoped>
.workflow-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 20px;
  box-shadow: 0 12px 35px rgb(15 23 42 / 6%);
}
header { display: flex; align-items: center; justify-content: space-between; }
h2 { margin: 2px 0 0; font-size: 20px; color: #0f172a; }
.section-kicker { margin: 0; color: #2563eb; font-size: 12px; font-weight: 800; letter-spacing: .08em; }
.section-help, .empty-copy { color: #64748b; font-size: 13px; line-height: 1.6; }
.count { padding: 5px 10px; border-radius: 999px; background: #eff6ff; color: #1d4ed8; font-size: 12px; font-weight: 700; }
.group-list { list-style: none; padding: 0; margin: 16px 0; display: grid; gap: 8px; }
.group-list li { display: grid; grid-template-columns: 10px 1fr auto; gap: 10px; align-items: center; padding: 10px 12px; border: 1px solid #e2e8f0; border-radius: 12px; }
.group-color { width: 10px; height: 34px; border-radius: 8px; }
.group-list strong, .group-list small { display: block; }
.group-list small { color: #94a3b8; margin-top: 2px; }
.icon-button { border: 0; background: transparent; font-size: 22px; cursor: pointer; }
.danger { color: #dc2626; }
.quick-add { display: grid; grid-template-columns: 1fr 1fr 44px auto; gap: 8px; }
input { min-width: 0; border: 1px solid #cbd5e1; border-radius: 10px; padding: 9px 10px; }
input[type="color"] { padding: 3px; width: 44px; }
.primary-small { border: 0; border-radius: 10px; padding: 0 14px; color: #fff; background: #2563eb; font-weight: 700; cursor: pointer; }
@media (max-width: 720px) { .quick-add { grid-template-columns: 1fr; } input[type="color"] { width: 100%; } .primary-small { min-height: 38px; } }
</style>
