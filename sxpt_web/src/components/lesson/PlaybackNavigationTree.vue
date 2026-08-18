<script setup lang="ts">
import { computed } from 'vue';
import { listStageStepEntries } from '../../utils/playbackPresentation';

interface NavigationStage {
  id: string;
  name: string;
  recordedSteps: Array<{ id: string }>;
}

interface NavigationStep {
  stage: { id: string };
  step: { id: string; title: string };
  stepIndex: number;
}

const props = defineProps<{
  teachingPoints: NavigationStage[];
  steps: NavigationStep[];
  currentStageId?: string;
  currentIndex: number;
  showStageIntroduction: boolean;
  disabled: boolean;
}>();

const emit = defineEmits<{
  selectStage: [stageId: string];
  selectStep: [globalIndex: number];
}>();

const currentStageStepEntries = computed(() =>
  listStageStepEntries(props.steps, props.currentStageId)
);
</script>

<template>
  <section class="playback-navigation-tree">
    <article
      v-for="(stage, index) in teachingPoints"
      :key="stage.id"
      class="playback-tree-item"
      :class="{ active: stage.id === currentStageId }"
    >
      <button
        class="playback-tree-stage"
        type="button"
        :disabled="disabled"
        @click="emit('selectStage', stage.id)"
      >
        <span>{{ index + 1 }}</span>
        <strong>{{ stage.name }}</strong>
        <small>{{ stage.recordedSteps.length }} 个节点</small>
      </button>

      <div v-if="stage.id === currentStageId" class="playback-tree-nodes">
        <button
          v-for="entry in currentStageStepEntries"
          :key="entry.item.step.id"
          type="button"
          :disabled="disabled"
          :class="{
            active:
              entry.globalIndex === currentIndex && !showStageIntroduction
          }"
          @click="emit('selectStep', entry.globalIndex)"
        >
          {{ entry.item.stepIndex + 1 }}. {{ entry.item.step.title }}
        </button>
      </div>
    </article>
  </section>
</template>

<style scoped>
.playback-navigation-tree {
  display: grid;
  min-height: 0;
  gap: 9px;
  overflow-y: auto;
  padding: 14px;
}

.playback-tree-item {
  overflow: hidden;
  border: 1px solid #e5e9f0;
  border-radius: 14px;
  background: #fff;
}

.playback-tree-item.active {
  border-color: #786bd4;
  background: #f7f5ff;
}

.playback-tree-stage {
  display: grid;
  width: 100%;
  min-height: 64px;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  border: 0;
  padding: 10px 12px;
  color: #556177;
  text-align: left;
  background: transparent;
  cursor: pointer;
}

.playback-tree-stage:disabled,
.playback-tree-nodes button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.playback-tree-stage span {
  display: grid;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  place-items: center;
  color: #fff;
  font-size: 14px;
  background: #8a93a4;
}

.playback-tree-item.active .playback-tree-stage span {
  background: #6b5dd3;
}

.playback-tree-stage strong {
  min-width: 0;
  overflow: hidden;
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playback-tree-stage small {
  color: #9199a8;
  font-size: 14px;
}

.playback-tree-nodes {
  display: grid;
  gap: 6px;
  padding: 0 10px 11px 52px;
}

.playback-tree-nodes button {
  overflow: hidden;
  border: 0;
  min-height: 42px;
  border-radius: 9px;
  padding: 9px 11px;
  color: #68758a;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #fff;
  font-size: 15px;
  cursor: pointer;
}

.playback-tree-nodes button:hover,
.playback-tree-nodes button.active {
  color: #5546b9;
  background: #ebe7ff;
}
</style>
