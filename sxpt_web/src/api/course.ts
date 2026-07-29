import { apiRequest } from './http';
import type {
  Course,
  IsoDateTime,
  TaskTeachingPoint,
  TeachingTask
} from './contracts';

export interface CreateCourseRequest {
  tenantId: string;
  courseCode: string;
  courseName: string;
  targetOrgId?: string;
  startTime?: IsoDateTime;
  endTime?: IsoDateTime;
  description?: string;
  createBy?: string;
}

export interface CreateTeachingTaskRequest {
  tenantId: string;
  courseId: string;
  publishOrgId: string;
  taskCode: string;
  taskName: string;
  taskType: string;
  taskGoal: string;
  taskDescription?: string;
  startTime?: IsoDateTime;
  endTime?: IsoDateTime;
  timeLimitMinutes?: number;
  overlayPolicyJson?: string;
  createBy?: string;
}

export const courseApi = {
  createCourse(request: CreateCourseRequest) {
    return apiRequest<Course>({
      method: 'POST',
      url: '/courses/create',
      data: request
    });
  },

  listCourses(tenantId: string, targetOrgId?: string) {
    return apiRequest<Course[]>({
      method: 'GET',
      url: '/courses',
      params: { tenantId, targetOrgId }
    });
  },

  createTask(request: CreateTeachingTaskRequest) {
    return apiRequest<TeachingTask>({
      method: 'POST',
      url: '/tasks/create',
      data: request
    });
  },

  listTasks(tenantId: string, courseId?: string, publishOrgId?: string) {
    return apiRequest<TeachingTask[]>({
      method: 'GET',
      url: '/tasks',
      params: { tenantId, courseId, publishOrgId }
    });
  },

  createTaskTeachingPoint(request: {
    tenantId: string;
    taskId: string;
    teachingPointId: string;
    requiredFlag: boolean;
    sequenceNo: number;
    createBy?: string;
  }) {
    return apiRequest<TaskTeachingPoint>({
      method: 'POST',
      url: '/tasks/teaching-points/create',
      data: request
    });
  },

  listTaskTeachingPoints(tenantId: string, taskId: string) {
    return apiRequest<TaskTeachingPoint[]>({
      method: 'GET',
      url: '/tasks/teaching-points',
      params: { tenantId, taskId }
    });
  },

  publishTaskTeachingPoint(
    tenantId: string,
    taskId: string,
    teachingPointId: string,
    evaluationRuleId: string
  ) {
    return apiRequest<TeachingTask>({
      method: 'POST',
      url: '/tasks/teaching-points/publish',
      params: { tenantId, taskId, teachingPointId, evaluationRuleId }
    });
  }
};
