<script setup lang="ts">
import { completionMethodOptions } from '../engine/displayLabels';
import type { CompletionMethod, LessonNode } from '../types/domain';

const props = defineProps<{
  node: LessonNode;
}>();

const emit = defineEmits<{
  'update:node': [node: LessonNode];
}>();

function updateNode(patch: Partial<LessonNode>) {
  emit('update:node', {
    ...props.node,
    ...patch
  });
}

function updateCompletionMethod(value: string) {
  updateNode({ completionMethod: value as CompletionMethod });
}
</script>

<template>
  <form class="editor-panel">
    <label>
      节点名称
      <input :value="node.name" @input="updateNode({ name: ($event.target as HTMLInputElement).value })" />
    </label>
    <label>
      节点分值
      <input
        :value="node.score"
        type="number"
        min="0"
        @input="updateNode({ score: Number(($event.target as HTMLInputElement).value) })"
      />
    </label>
    <label>
      完成方式
      <select :value="node.completionMethod" @change="updateCompletionMethod(($event.target as HTMLSelectElement).value)">
        <option
          v-for="option in completionMethodOptions"
          :key="option.value"
          :value="option.value"
        >
          {{ option.label }}
        </option>
      </select>
    </label>
    <label class="inline-check">
      <input
        :checked="node.required"
        type="checkbox"
        @change="updateNode({ required: ($event.target as HTMLInputElement).checked })"
      />
      设为必做节点
    </label>
    <label>
      教学说明
      <textarea
        :value="node.teachingText"
        rows="5"
        @input="updateNode({ teachingText: ($event.target as HTMLTextAreaElement).value })"
      />
    </label>
  </form>
</template>
