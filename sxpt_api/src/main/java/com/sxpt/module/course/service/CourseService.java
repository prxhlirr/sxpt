package com.sxpt.module.course.service;

import com.sxpt.module.course.entity.Course;

import java.util.List;

/**
 * 课程服务。
 *
 * 业务功能：
 * 1. 创建课程，使后续学习、练习和考试任务可以挂载到课程下。
 * 2. 支持按租户和默认授课班级查询课程。
 *
 * 关键流程：
 * 1. 创建课程时校验课程编码、名称和租户。
 * 2. 新课程默认发布，便于 MVP 后续任务发布直接引用。
 */
public interface CourseService {

    /**
     * 创建课程。
     *
     * @param course 课程实体。
     * @return 已保存的课程。
     */
    Course createCourse(Course course);

    /**
     * 查询课程。
     *
     * @param tenantId 租户 ID。
     * @param targetOrgId 默认授课班级 ID，可为空。
     * @return 课程列表。
     */
    List<Course> listCourses(String tenantId, String targetOrgId);
}
