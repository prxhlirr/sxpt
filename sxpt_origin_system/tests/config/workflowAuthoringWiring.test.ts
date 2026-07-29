import { existsSync, readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

const source = (relativePath: string) =>
  readFileSync(new URL(`../../${relativePath}`, import.meta.url), 'utf8');

describe('workflow authoring wiring', () => {
  it('registers independent workflow and stage-record routes', () => {
    const router = source('src/router.ts');

    expect(router).toContain('/teacher/tasks/:taskId/workflow');
    expect(router).toContain(
      '/teacher/workflows/:workflowDraftId/stages/:stageId/record'
    );
    expect(router).toContain('TeacherWorkflowDesigner');
  });

  it('composes the complete workflow authoring surface', () => {
    const page = source('src/views/TeacherWorkflowDesigner.vue');
    const stageProperties = source(
      'src/components/workflow/StagePropertyPanel.vue'
    );
    for (const component of [
      'RoleGroupPanel',
      'SerialStageEditor',
      'StagePropertyPanel',
      'StageStepBindingPanel',
      'WorkflowDataTemplatePanel',
      'SubjectiveRubricEditor',
      'WorkflowValidationPanel'
    ]) {
      expect(page).toContain(component);
    }

    expect(page).toContain('保存草稿');
    expect(page).toContain('发布校验');
    expect(page).toContain('发布工作流');
    expect(page).toContain('实训分组');
    expect(stageProperties).toContain('实训分组（平台编排）');
    expect(stageProperties).toContain('业务身份');
  });

  it('ships every focused workflow editor component', () => {
    for (const component of [
      'RoleGroupPanel.vue',
      'SerialStageEditor.vue',
      'StagePropertyPanel.vue',
      'StageStepBindingPanel.vue',
      'WorkflowDataTemplatePanel.vue',
      'SubjectiveRubricEditor.vue',
      'WorkflowValidationPanel.vue'
    ]) {
      expect(
        existsSync(
          new URL(`../../src/components/workflow/${component}`, import.meta.url)
        )
      ).toBe(true);
    }
  });

  it('binds the recording page to one workflow draft and stage', () => {
    const recordPage = source('src/views/TeacherRecordDesigner.vue');

    expect(recordPage).toContain("route.params.workflowDraftId");
    expect(recordPage).toContain("route.params.stageId");
    expect(recordPage).toContain('publishRecordedStage');
    expect(recordPage).toContain('workflowApi.saveDraft');
    expect(recordPage).toContain("router.push(`/teacher/tasks/");
  });

  it('saves a dirty workflow before opening the stage recorder', () => {
    const workflowPage = source('src/views/TeacherWorkflowDesigner.vue');
    const recordHandler = workflowPage.match(
      /async function recordStage\(stageId: string\) \{([\s\S]*?)\n\}/
    )?.[1];

    expect(recordHandler).toContain(
      'if (designer.dirty.value) await designer.save();'
    );
    expect(recordHandler).toContain('router.push(');
  });

  it('keeps exam generation and grading out of the capture toolbar', () => {
    const toolbar = source('src/components/CaptureToolbar.vue');

    expect(toolbar).not.toContain('生成考试数据');
    expect(toolbar).not.toContain('考试监控');
    expect(toolbar).not.toContain('主观评分');
  });
});
