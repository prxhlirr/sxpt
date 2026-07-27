<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const props = defineProps<{
  lessonId: string;
  current: 'editor' | 'exam' | 'groups' | 'data' | 'publish';
}>();

const route = useRoute();
const router = useRouter();

const steps = [
  { key: 'editor', label: '教案编排', caption: '流程与录制', routeName: 'lesson-editor' },
  { key: 'exam', label: '考试设置', caption: '规则与评分', routeName: 'exam-setup' },
  { key: 'groups', label: '分组设置', caption: '角色与成员', routeName: 'group-setup' },
  { key: 'data', label: '数据生成', caption: '正式与备用', routeName: 'exam-data' },
  { key: 'publish', label: '发布任务', caption: '检查与下发', routeName: 'publish-center' }
] as const;

const activeIndex = computed(() => steps.findIndex((step) => step.key === props.current));

function navigate(routeName: string) {
  if (route.name === routeName) return;
  void router.push({ name: routeName, params: { lessonId: props.lessonId } });
}
</script>

<template>
  <nav class="workflow-stepper" aria-label="教案考试业务链">
    <button
      v-for="(step, index) in steps"
      :key="step.key"
      type="button"
      class="workflow-step"
      :class="{
        active: step.key === current,
        completed: index < activeIndex
      }"
      :aria-current="step.key === current ? 'step' : undefined"
      @click="navigate(step.routeName)"
    >
      <span class="workflow-step__number">{{ index + 1 }}</span>
      <span class="workflow-step__copy">
        <strong>{{ step.label }}</strong>
        <small>{{ step.caption }}</small>
      </span>
      <span v-if="index < steps.length - 1" class="workflow-step__line" />
    </button>
  </nav>
</template>

<style src="./exam-pages.css"></style>

<style scoped>
.workflow-stepper {
  display: grid;
  grid-template-columns: repeat(5, minmax(130px, 1fr));
  margin-bottom: 20px;
  overflow-x: auto;
  border: 1px solid #e6e9f0;
  border-radius: 14px;
  background: #fff;
  padding: 14px 16px;
  box-shadow: 0 8px 24px rgb(30 36 80 / 4%);
}

.workflow-step {
  position: relative;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  min-width: 130px;
  min-height: 52px;
  align-items: center;
  gap: 9px;
  border: 0;
  padding: 4px 22px 4px 4px;
  background: transparent;
  text-align: left;
}

.workflow-step:hover {
  border-color: transparent;
  background: #faf9ff;
  box-shadow: none;
  transform: none;
}

.workflow-step__number {
  z-index: 1;
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: 1px solid #dfe3ec;
  border-radius: 50%;
  background: #fff;
  color: #8791a3;
  font-size: 12px;
  font-weight: 900;
}

.workflow-step__copy {
  z-index: 1;
  display: grid;
  gap: 3px;
}

.workflow-step__copy strong {
  color: #546176;
  font-size: 12px;
}

.workflow-step__copy small {
  color: #9aa3b2;
  font-size: 9px;
}

.workflow-step__line {
  position: absolute;
  z-index: 0;
  top: 26px;
  right: -8px;
  width: 28px;
  height: 1px;
  background: #e3e6ed;
}

.workflow-step.active .workflow-step__number {
  border-color: #6d5dfc;
  background: #6d5dfc;
  color: #fff;
  box-shadow: 0 5px 13px rgb(109 93 252 / 25%);
}

.workflow-step.active .workflow-step__copy strong {
  color: #5748d8;
}

.workflow-step.completed .workflow-step__number {
  border-color: #bde8d8;
  background: #e9f9f3;
  color: #0c8c64;
}

.workflow-step.completed .workflow-step__number::before {
  content: "✓";
}

.workflow-step.completed .workflow-step__number {
  font-size: 0;
}

.workflow-step.completed .workflow-step__number::before {
  font-size: 13px;
}

@media (max-width: 900px) {
  .workflow-stepper {
    grid-template-columns: repeat(5, 150px);
  }
}
</style>
