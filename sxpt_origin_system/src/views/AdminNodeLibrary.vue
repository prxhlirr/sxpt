<script setup lang="ts">
import { ref } from 'vue';
import {
  completionMethodOptions,
  nodeCodeLabel,
  nodeTypeLabel
} from '../engine/displayLabels';
import { nodeDefs as initialNodeDefs } from '../mock/nodeDefs';

const nodes = ref(initialNodeDefs.map((node) => ({ ...node })));
</script>

<template>
  <section class="page">
    <p class="eyebrow">管理配置</p>
    <h2>标准节点库</h2>
    <p class="lede">采购申请模块节点字典。</p>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>节点编码</th>
            <th>名称</th>
            <th>类型</th>
            <th>默认分</th>
            <th>完成方式</th>
            <th>业务校验</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="node in nodes" :key="node.id">
            <td>{{ nodeCodeLabel(node.code) }}</td>
            <td><input v-model="node.name" /></td>
            <td>{{ nodeTypeLabel(node.type) }}</td>
            <td><input v-model.number="node.defaultScore" type="number" min="0" /></td>
            <td>
              <select v-model="node.defaultCompletionMethod">
                <option
                  v-for="option in completionMethodOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
            </td>
            <td>
              <label class="inline-check">
                <input v-model="node.needBusinessCheck" type="checkbox" />
                需要
              </label>
            </td>
            <td>
              <label class="inline-check">
                <input v-model="node.enabled" type="checkbox" />
                启用
              </label>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
