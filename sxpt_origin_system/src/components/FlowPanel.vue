<script setup lang="ts">
import { computed } from 'vue';
import { calculateScore, getCurrentNode } from '../engine/flowState';
import type { LessonFlow, StudentSession } from '../types/domain';

const props = defineProps<{
  lessonFlow: LessonFlow;
  session: StudentSession;
  showTeachingText: boolean;
  showCompletionButton?: boolean;
}>();

const emit = defineEmits<{
  complete: [];
  select: [nodeId: string];
}>();

const currentNode = computed(() => getCurrentNode(props.lessonFlow, props.session));
const score = computed(() => calculateScore(props.lessonFlow, props.session));

function statusText(nodeId: string) {
  const status = props.session.nodeResults[nodeId];
  const map = {
    pending: '待解锁',
    available: '当前可做',
    clicked: '已点击',
    success: '已完成',
    failed: '未通过',
    manual_pending: '待确认'
  };

  return map[status] ?? status;
}
</script>

<template>
  <aside class="flow-panel">
    <div class="panel-header">
      <div>
        <p class="eyebrow">流程</p>
        <h3>{{ lessonFlow.title }}</h3>
      </div>
      <strong>{{ score }} 分</strong>
    </div>
    <ol class="flow-list compact">
      <li
        v-for="(node, index) in lessonFlow.nodes"
        :key="node.id"
        :class="session.nodeResults[node.id]"
        @click="emit('select', node.id)"
      >
        <span class="step-index">{{ index + 1 }}</span>
        <div>
          <strong>{{ node.name }}</strong>
          <p>{{ statusText(node.id) }} · {{ node.score }} 分</p>
          <small v-if="showTeachingText">{{ node.teachingText }}</small>
        </div>
      </li>
    </ol>
    <button
      v-if="showCompletionButton"
      class="primary-action"
      :disabled="!currentNode"
      @click="emit('complete')"
    >
      我已完成当前节点
    </button>
  </aside>
</template>
