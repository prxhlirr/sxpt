<script setup lang="ts">
import { reactive, watch } from 'vue';
import type { SwitchSegmentInput } from '../engine/captureOrchestrator';

const props = defineProps<{
  currentSegmentNo: number;
  busy?: boolean;
}>();

const emit = defineEmits<{
  submit: [input: SwitchSegmentInput];
  cancel: [];
}>();

const form = reactive<SwitchSegmentInput>({
  nextSegmentNo: props.currentSegmentNo + 1,
  actorType: 'APPROVER',
  requiredExternalOrgId: 'org-finance',
  requiredExternalOrgName: '财务部门',
  requiredExternalRoleId: 'role-approver',
  requiredExternalRoleName: '财务审批人',
  targetUrl: '/business-approval-demo.html',
  switchReason: '申请提交后进入审批环节'
});

watch(
  () => props.currentSegmentNo,
  (segmentNo) => {
    form.nextSegmentNo = segmentNo + 1;
  }
);

function submit() {
  emit('submit', { ...form });
}
</script>

<template>
  <section class="floating-glass segment-switch-panel">
    <div class="panel-header">
      <div>
        <p class="eyebrow">角色切换</p>
        <h3>保存本段并开始下一段</h3>
      </div>
      <button type="button" class="secondary-action" @click="emit('cancel')">关闭</button>
    </div>
    <div class="segment-switch-grid">
      <label>下一段号<input v-model.number="form.nextSegmentNo" type="number" min="2" /></label>
      <label>参与方类型<input v-model="form.actorType" /></label>
      <label>单位 ID<input v-model="form.requiredExternalOrgId" /></label>
      <label>单位名称<input v-model="form.requiredExternalOrgName" /></label>
      <label>角色 ID<input v-model="form.requiredExternalRoleId" /></label>
      <label>角色名称<input v-model="form.requiredExternalRoleName" /></label>
      <label class="wide-field">目标地址<input v-model="form.targetUrl" /></label>
      <label class="wide-field">切换原因<textarea v-model="form.switchReason" rows="2" /></label>
    </div>
    <div class="panel-submit-actions">
      <button type="button" class="secondary-action" @click="emit('cancel')">取消</button>
      <button type="button" :disabled="busy" @click="submit">{{ busy ? '切换中...' : '保存并进入下一段' }}</button>
    </div>
  </section>
</template>
