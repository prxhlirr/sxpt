<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  completionMethodLabel,
  versionLabel
} from '../engine/displayLabels';
import { purchaseFlowTemplate } from '../mock/flowTemplates';

const template = ref({
  ...purchaseFlowTemplate,
  nodes: purchaseFlowTemplate.nodes.map((node) => ({ ...node }))
});

const totalScore = computed(() =>
  template.value.nodes.reduce((total, node) => total + node.score, 0)
);
</script>

<template>
  <section class="page">
    <p class="eyebrow">管理配置</p>
    <h2>模块标准流程库</h2>
    <p class="lede">采购申请标准流程模板。</p>
    <div class="summary-bar">
      <span>{{ template.moduleName }}</span>
      <span>{{ template.title }}</span>
      <span>{{ versionLabel(template.version) }}</span>
      <strong>{{ totalScore }} 分</strong>
    </div>
    <ol class="flow-list">
      <li v-for="(node, index) in template.nodes" :key="node.id">
        <span class="step-index">{{ index + 1 }}</span>
        <div>
          <strong>{{ node.name }}</strong>
          <p>{{ node.required ? '必做节点' : '选做节点' }} · {{ completionMethodLabel(node.completionMethod) }} · {{ node.score }} 分</p>
        </div>
        <label class="inline-check">
          <input v-model="node.required" type="checkbox" />
          必做
        </label>
        <input v-model.number="node.score" type="number" min="0" />
      </li>
    </ol>
  </section>
</template>
