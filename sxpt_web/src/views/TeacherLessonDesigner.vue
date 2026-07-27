<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  completionMethodLabel,
  completionMethodOptions
} from '../engine/displayLabels';
import { purchaseLessonFlow } from '../mock/lessonFlows';

const lesson = ref({
  ...purchaseLessonFlow,
  nodes: purchaseLessonFlow.nodes.map((node) => ({
    ...node,
    binding: node.binding ? { ...node.binding, rect: { ...node.binding.rect } } : undefined
  }))
});

const selectedNodeId = ref(lesson.value.nodes[0]?.id ?? '');
const selectedNode = computed(() =>
  lesson.value.nodes.find((node) => node.id === selectedNodeId.value)
);
</script>

<template>
  <section class="page">
    <p class="eyebrow">教师编排</p>
    <h2>教案流程编排</h2>
    <p class="lede">采购申请实训教案草稿。</p>
    <div class="designer-grid">
      <ol class="flow-list">
        <li
          v-for="(node, index) in lesson.nodes"
          :key="node.id"
          :class="{ active: selectedNodeId === node.id }"
          @click="selectedNodeId = node.id"
        >
          <span class="step-index">{{ index + 1 }}</span>
          <div>
            <strong>{{ node.name }}</strong>
            <p>{{ completionMethodLabel(node.completionMethod) }} · {{ node.score }} 分 · {{ node.required ? '必做' : '选做' }}</p>
          </div>
        </li>
      </ol>
      <form v-if="selectedNode" class="editor-panel">
        <label>
          节点名称
          <input v-model="selectedNode.name" />
        </label>
        <label>
          节点分值
          <input v-model.number="selectedNode.score" type="number" min="0" />
        </label>
        <label>
          完成方式
          <select v-model="selectedNode.completionMethod">
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
          <input v-model="selectedNode.required" type="checkbox" />
          设为必做节点
        </label>
        <label>
          教学说明
          <textarea v-model="selectedNode.teachingText" rows="6" />
        </label>
      </form>
    </div>
  </section>
</template>
