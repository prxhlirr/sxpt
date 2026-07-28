<script setup lang="ts">
import type {
  RunBatchGroupMemberInput,
  RunBatchRoleGroup
} from '../../types/runBatch';

const props = defineProps<{
  members: RunBatchGroupMemberInput[];
  roleGroups: RunBatchRoleGroup[];
  disabled: boolean;
}>();

const emit = defineEmits<{
  change: [members: RunBatchGroupMemberInput[]];
  save: [];
}>();

function updateRow(
  index: number,
  patch: Partial<RunBatchGroupMemberInput>
) {
  emit('change', props.members.map((row, rowIndex) =>
    rowIndex === index
      ? {
          ...row,
          ...patch,
          externalAccountMapping: {
            ...row.externalAccountMapping,
            ...(patch.externalAccountMapping ?? {})
          }
        }
      : { ...row, externalAccountMapping: { ...row.externalAccountMapping } }
  ));
}

function updateMapping(index: number, key: 'accountId' | 'roleId' | 'orgId', value: string) {
  const current = props.members[index];
  if (!current) return;
  updateRow(index, {
    externalAccountMapping: {
      ...current.externalAccountMapping,
      [key]: value
    }
  });
}

function addRow() {
  const first = props.members[0];
  emit('change', [
    ...props.members.map((row) => ({
      ...row,
      externalAccountMapping: { ...row.externalAccountMapping }
    })),
    {
      learnerId: first?.learnerId ?? '',
      groupKey: props.roleGroups[0]?.groupKey ?? '',
      unitId: first?.unitId ?? '',
      externalAccountMapping: {
        accountId: '',
        roleId: '',
        orgId: ''
      }
    }
  ]);
}

function removeRow(index: number) {
  emit('change', props.members
    .filter((_, rowIndex) => rowIndex !== index)
    .map((row) => ({
      ...row,
      externalAccountMapping: { ...row.externalAccountMapping }
    })));
}
</script>

<template>
  <section class="run-card">
    <div class="card-heading">
      <div>
        <span>STEP 02</span>
        <h2>成员与多角色分组</h2>
        <p>一行代表一个组成员关系；同一学员可添加多行并加入不同角色组。</p>
      </div>
      <button type="button" :disabled="disabled" @click="addRow">添加成员关系</button>
    </div>

    <div class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>学员 ID</th>
            <th>角色组</th>
            <th>单位 ID</th>
            <th>业务账号 ID</th>
            <th>业务角色 ID</th>
            <th>业务组织 ID</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(member, index) in members" :key="index">
            <td><input :value="member.learnerId" :disabled="disabled" @input="updateRow(index, { learnerId: ($event.target as HTMLInputElement).value })" /></td>
            <td>
              <select :value="member.groupKey" :disabled="disabled" @change="updateRow(index, { groupKey: ($event.target as HTMLSelectElement).value })">
                <option v-for="group in roleGroups" :key="group.id" :value="group.groupKey">{{ group.name }}</option>
              </select>
            </td>
            <td><input :value="member.unitId" :disabled="disabled" @input="updateRow(index, { unitId: ($event.target as HTMLInputElement).value })" /></td>
            <td><input :value="member.externalAccountMapping.accountId ?? ''" :disabled="disabled" @input="updateMapping(index, 'accountId', ($event.target as HTMLInputElement).value)" /></td>
            <td><input :value="member.externalAccountMapping.roleId ?? ''" :disabled="disabled" @input="updateMapping(index, 'roleId', ($event.target as HTMLInputElement).value)" /></td>
            <td><input :value="member.externalAccountMapping.orgId ?? ''" :disabled="disabled" @input="updateMapping(index, 'orgId', ($event.target as HTMLInputElement).value)" /></td>
            <td><button type="button" class="danger" :disabled="disabled" @click="removeRow(index)">删除</button></td>
          </tr>
          <tr v-if="members.length === 0"><td colspan="7" class="empty">暂无成员关系，请先添加。</td></tr>
        </tbody>
      </table>
    </div>

    <div class="card-footer">
      <p>仅提交安全映射字段 accountId、roleId、orgId，不采集业务凭证或敏感表单值。</p>
      <button type="button" :disabled="disabled || members.length === 0" @click="emit('save')">保存成员分组</button>
    </div>
  </section>
</template>

<style scoped>
.run-card { padding: 22px; border: 1px solid #e2e8f0; border-radius: 18px; background: #fff; }
.card-heading, .card-footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.card-heading span { color: #7c3aed; font-size: 11px; font-weight: 850; letter-spacing: .14em; }
h2 { margin: 5px 0; } p { margin: 0; color: #64748b; font-size: 12px; }
.table-scroll { margin: 18px 0; overflow-x: auto; }
table { min-width: 1040px; } th, td { padding: 8px; } td input, td select { min-width: 120px; }
.danger { border-color: #fecaca; color: #b91c1c; background: #fff; }
.empty { padding: 28px; text-align: center; color: #64748b; }
.card-footer { padding-top: 15px; border-top: 1px solid #e2e8f0; }
@media (max-width: 720px) { .card-heading, .card-footer { align-items: stretch; flex-direction: column; } }
</style>
