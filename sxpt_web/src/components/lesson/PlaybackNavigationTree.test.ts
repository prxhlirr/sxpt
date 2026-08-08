import { createSSRApp } from 'vue';
import { renderToString } from '@vue/server-renderer';
import { describe, expect, it } from 'vitest';
import PlaybackNavigationTree from './PlaybackNavigationTree.vue';

describe('PlaybackNavigationTree', () => {
  it('renders nodes only beneath the active teaching point', async () => {
    const stages = [
      {
        id: 'stage-a',
        name: '教学点 A',
        recordedSteps: [{ id: 'a-1' }]
      },
      {
        id: 'stage-b',
        name: '教学点 B',
        recordedSteps: [{ id: 'b-1' }, { id: 'b-2' }]
      }
    ];
    const steps = [
      {
        stage: stages[0],
        step: { id: 'a-1', title: 'A 节点' },
        stageIndex: 0,
        stepIndex: 0
      },
      {
        stage: stages[1],
        step: { id: 'b-1', title: 'B 节点一' },
        stageIndex: 1,
        stepIndex: 0
      },
      {
        stage: stages[1],
        step: { id: 'b-2', title: 'B 节点二' },
        stageIndex: 1,
        stepIndex: 1
      }
    ];

    const html = await renderToString(
      createSSRApp(PlaybackNavigationTree, {
        teachingPoints: stages,
        steps,
        currentStageId: 'stage-b',
        currentIndex: 2,
        showStageIntroduction: false,
        disabled: false
      })
    );

    expect(html).toContain('教学点 A');
    expect(html).toContain('教学点 B');
    expect(html).not.toContain('A 节点');
    expect(html).toContain('B 节点一');
    expect(html).toContain('B 节点二');
    expect(html.match(/playback-tree-nodes/g)).toHaveLength(1);
  });
});
