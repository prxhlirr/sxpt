import { createRouter, createWebHistory } from 'vue-router';
import AdminNodeLibrary from './views/AdminNodeLibrary.vue';
import AdminFlowTemplate from './views/AdminFlowTemplate.vue';
import TeacherLessonDesigner from './views/TeacherLessonDesigner.vue';
import TeacherRecordDesigner from './views/TeacherRecordDesigner.vue';
import TeacherWorkflowDesigner from './views/TeacherWorkflowDesigner.vue';
import TeacherExamBatchWizard from './views/TeacherExamBatchWizard.vue';
import TeacherExamDataOverview from './views/TeacherExamDataOverview.vue';
import StudentRunner from './views/StudentRunner.vue';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: TeacherRecordDesigner },
    { path: '/admin/nodes', component: AdminNodeLibrary },
    { path: '/admin/templates', component: AdminFlowTemplate },
    { path: '/teacher/lesson', component: TeacherLessonDesigner },
    { path: '/teacher/record', component: TeacherRecordDesigner },
    {
      path: '/teacher/tasks/:taskId/workflow',
      component: TeacherWorkflowDesigner
    },
    {
      path: '/teacher/workflows/:workflowDraftId/stages/:stageId/record',
      component: TeacherRecordDesigner
    },
    {
      path: '/teacher/exams/new',
      component: TeacherExamBatchWizard
    },
    {
      path: '/teacher/exams/:batchId/data',
      component: TeacherExamDataOverview
    },
    {
      path: '/teacher/practices/new',
      component: TeacherExamBatchWizard
    },
    {
      path: '/teacher/practices/:batchId/data',
      component: TeacherExamDataOverview
    },
    { path: '/learn', component: StudentRunner },
    { path: '/student/runner', component: StudentRunner }
  ]
});
