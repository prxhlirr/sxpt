import { apiRequest } from './http';
import type {
  ExecutionTrace,
  IsoDateTime,
  RuntimeContext,
  TaskExecution
} from './contracts';

export const executionApi = {
  start(request: {
    tenantId: string;
    taskId: string;
    connectorSystemId: string;
    executionMode: string;
    sdkMode: string;
    executionIdentityJson?: string;
  }) {
    return apiRequest<TaskExecution>({
      method: 'POST',
      url: '/student/task-executions/start',
      data: request
    });
  },

  submit(tenantId: string, executionId: string) {
    return apiRequest<TaskExecution>({
      method: 'POST',
      url: '/student/task-executions/submit',
      data: { tenantId, executionId }
    });
  },

  list(tenantId: string, studentId: string, taskId: string) {
    return apiRequest<TaskExecution[]>({
      method: 'GET',
      url: '/student/task-executions',
      params: { tenantId, studentId, taskId }
    });
  },

  context(
    mode: 'LEARNING' | 'PRACTICE' | 'EXAM',
    tenantId: string,
    executionId: string,
    taskId: string
  ) {
    return apiRequest<RuntimeContext>({
      method: 'GET',
      url: `/sdk/${mode.toLowerCase()}/context`,
      params: { tenantId, executionId, taskId }
    });
  },

  reportTrace(request: {
    tenantId: string;
    executionId: string;
    sdkSessionId?: string;
    clientTraceId: string;
    taskStepId?: string;
    teachingPointId?: string;
    resourceId?: string;
    traceType: string;
    traceTime: IsoDateTime;
    sequenceNo: number;
    retryCount?: number;
    inputDataJson?: string;
    outputDataJson?: string;
    beforeStateJson?: string;
    afterStateJson?: string;
    evidenceJson?: string;
    success?: boolean;
    errorMessage?: string;
  }) {
    return apiRequest<ExecutionTrace>({
      method: 'POST',
      url: '/execution/traces/report',
      data: request
    });
  }
};
