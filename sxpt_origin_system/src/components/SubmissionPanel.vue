<script setup lang="ts">
import { ref } from 'vue';
import type { TaskSubmission } from '../types/domain';

defineProps<{
  submitted?: TaskSubmission;
  eventCount: number;
}>();

const emit = defineEmits<{
  submit: [submission: TaskSubmission];
}>();

const businessId = ref('采购-2026-0001');
const attachmentName = ref('考试结果截图');
const description = ref('已完成采购申请并提交审批。');

function submit() {
  emit('submit', {
    businessId: businessId.value.trim(),
    attachmentName: attachmentName.value.trim(),
    description: description.value.trim(),
    submittedAt: new Date().toISOString()
  });
}

function submittedTimeText(submittedAt: string): string {
  return new Date(submittedAt).toLocaleString('zh-CN');
}
</script>

<template>
  <aside class="submission-panel">
    <div class="panel-header">
      <div>
        <p class="eyebrow">考试</p>
        <h3>成果提交</h3>
      </div>
      <strong>{{ eventCount }} 条记录</strong>
    </div>
    <p class="muted">任务目标：完成采购申请并提交业务单号。</p>
    <label>
      业务单号
      <input v-model="businessId" />
    </label>
    <label>
      附件名称
      <input v-model="attachmentName" />
    </label>
    <label>
      操作说明
      <textarea v-model="description" rows="5" />
    </label>
    <button class="primary-action" type="button" @click="submit">提交考试</button>
    <div v-if="submitted" class="evidence-box">
      <strong>已生成证据包</strong>
      <p>{{ submitted.businessId }} · {{ submitted.attachmentName }}</p>
      <small>{{ submittedTimeText(submitted.submittedAt) }}</small>
    </div>
  </aside>
</template>
