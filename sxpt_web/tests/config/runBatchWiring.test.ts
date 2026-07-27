import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

const source = (relativePath: string) =>
  readFileSync(new URL(`../../${relativePath}`, import.meta.url), 'utf8');

describe('teacher run batch wiring', () => {
  it('registers reusable exam and practice wizard and overview routes', () => {
    const router = source('src/router.ts');

    expect(router).toContain('/teacher/exams/new');
    expect(router).toContain('/teacher/exams/:batchId/data');
    expect(router).toContain('/teacher/practices/new');
    expect(router).toContain('/teacher/practices/:batchId/data');
    expect(router).toContain('TeacherExamBatchWizard');
    expect(router).toContain('TeacherExamDataOverview');
  });

  it('adds task-aware creation entries to the workflow designer', () => {
    const workflowDesigner = source(
      'src/views/TeacherWorkflowDesigner.vue'
    );

    expect(workflowDesigner).toContain('创建考试批次');
    expect(workflowDesigner).toContain('创建练习轮次');
    expect(workflowDesigner).toContain(
      'query: { taskId: taskId.value }'
    );
  });

  it('adds workflow-designer and batch-creation entries to the record designer', () => {
    const recordDesigner = source(
      'src/views/TeacherRecordDesigner.vue'
    );

    expect(recordDesigner).toContain('工作流编排');
    expect(recordDesigner).toContain('创建考试批次');
    expect(recordDesigner).toContain('创建练习轮次');
    expect(recordDesigner).toContain('effectiveTaskId');
    expect(recordDesigner).toContain(
      'taskId: effectiveTaskId.value'
    );
  });

  it('uses only the configured authenticated run-batch API', () => {
    const wizardPage = source('src/views/TeacherExamBatchWizard.vue');
    const overviewPage = source('src/views/TeacherExamDataOverview.vue');
    const pages = wizardPage + overviewPage;

    expect(wizardPage).toContain('createRunBatchApiFromConfig');
    expect(overviewPage).toContain('createRunBatchApiFromConfig');
    expect(pages).toContain('createAuthenticatedApiClient');
    expect(pages).not.toContain('createMockRunBatchApi');
    expect(pages).not.toContain('localStorage');
    expect(pages).not.toContain('StudentRunner');
  });

  it('wires the five-step immutable wizard contract', () => {
    const wizardPage = source('src/views/TeacherExamBatchWizard.vue');
    const groupAssignment = source(
      'src/components/run-batch/GroupAssignmentStep.vue'
    );
    const dataGeneration = source(
      'src/components/run-batch/DataGenerationStep.vue'
    );
    const preflight = source(
      'src/components/run-batch/BatchPreflightStep.vue'
    );

    for (const component of [
      'FlowVersionStep',
      'GroupAssignmentStep',
      'SubjectiveRubricStep',
      'DataGenerationStep',
      'BatchPreflightStep'
    ]) {
      expect(wizardPage).toContain(component);
    }
    expect(groupAssignment).toContain('externalAccountMapping');
    expect(groupAssignment).not.toMatch(/\bgroupA\b|\bgroupB\b/);
    expect(dataGeneration).toContain('requestedCount');
    expect(dataGeneration).toContain('readyCount');
    expect(dataGeneration).toContain('failedCount');
    expect(dataGeneration).toContain('jobStatus');
    expect(preflight).toContain('issue.unitId');
    expect(wizardPage).toContain('wizard.setMembers(');
    expect(wizardPage).toContain('wizard.setUnitPlans(');
    expect(wizardPage).toContain('wizard.saveUnitPlans()');
    expect(wizardPage).toContain('wizard.canPublish.value');
  });

  it('restores a created batch and keeps the data-adjustment loop connected', () => {
    const wizardPage = source('src/views/TeacherExamBatchWizard.vue');
    const overviewPage = source(
      'src/views/TeacherExamDataOverview.vue'
    );

    expect(wizardPage).toContain('route.query.batchId');
    expect(wizardPage).toContain('wizard.loadBatch(');
    expect(wizardPage).toContain('batchId: created.id');
    expect(wizardPage).toContain('查看/调整数据池');
    expect(overviewPage).toContain('返回批次配置');
    expect(overviewPage).toContain('query: { batchId: current.id }');
    expect(overviewPage).toContain('watch(batchId');
    expect(overviewPage).toContain('Promise.all([');
    expect(overviewPage).toContain(
      ':items="pageLoading ? [] : (pageResult?.items ?? [])"'
    );
  });

  it('keeps editable row identity and invalid JSON drafts stable', () => {
    const groupAssignment = source(
      'src/components/run-batch/GroupAssignmentStep.vue'
    );
    const dataGeneration = source(
      'src/components/run-batch/DataGenerationStep.vue'
    );

    expect(groupAssignment).toContain(':key="index"');
    expect(dataGeneration).toContain(':key="index"');
    expect(dataGeneration).toContain('parameterSignatures');
  });

  it('shows honest data state, gate-controlled actions and immutable detail', () => {
    const overviewPage = source(
      'src/views/TeacherExamDataOverview.vue'
    );
    const dataTable = source(
      'src/components/run-batch/RunDataTable.vue'
    );
    const drawer = source(
      'src/components/run-batch/RunDataItemDrawer.vue'
    );

    expect(dataTable).toContain('businessReferenceMasked');
    expect(dataTable).not.toContain('businessReferenceRaw');
    expect(dataTable).toContain('generationJobId');
    expect(dataTable).toContain('平台暂不可可靠判定');
    expect(dataTable).toContain("item.status === 'READY'");
    expect(dataTable).toContain("item.kind === 'SPARE'");
    expect(dataTable).toContain(
      "item.status === 'GENERATION_FAILED'"
    );
    expect(dataTable).toContain("item.status === 'IN_USE'");
    expect(dataTable).toContain('办理中');
    expect(drawer).toContain('generationParameters');
    expect(drawer).toContain('auditHistory');
    expect(overviewPage).toContain('pool.disable(');
    expect(overviewPage).toContain('pool.promote(');
    expect(overviewPage).toContain('pool.replace(');
    expect(overviewPage).toContain('pool.retry(');
    const confirmationHandler = overviewPage.match(
      /async function confirmAction\(\) \{([\s\S]*?)\n\}/
    )?.[1];
    expect(confirmationHandler).not.toContain('pool.select(result.id)');
  });
});
