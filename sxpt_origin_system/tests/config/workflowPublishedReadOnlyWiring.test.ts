import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

const workflowDesignerSource = readFileSync(
  new URL('../../src/views/TeacherWorkflowDesigner.vue', import.meta.url),
  'utf8'
);
const recorderSource = readFileSync(
  new URL('../../src/views/TeacherRecordDesigner.vue', import.meta.url),
  'utf8'
);

describe('published workflow read-only wiring', () => {
  it('passes read-only state to every mutating workflow editor', () => {
    expect(workflowDesignerSource).toContain(
      "const isReadOnly = computed(() => designer.isReadOnly.value)"
    );
    for (const component of [
      'RoleGroupPanel',
      'WorkflowDataTemplatePanel',
      'SerialStageEditor',
      'StagePropertyPanel',
      'StageStepBindingPanel',
      'SubjectiveRubricEditor'
    ]) {
      expect(workflowDesignerSource).toMatch(
        new RegExp(`<${component}[\\s\\S]*?:readonly="isReadOnly"`)
      );
    }
    expect(workflowDesignerSource).toContain('@click="continueEditing"');
  });

  it('blocks direct recording-page mutation for a published workflow version', () => {
    expect(recorderSource).toContain('const isWorkflowReadOnly = computed(');
    expect(recorderSource).toContain('workflow-read-only-shield');
    expect(recorderSource).toContain(
      ':inert="isWorkflowReadOnly ? true : undefined"'
    );
  });
});
