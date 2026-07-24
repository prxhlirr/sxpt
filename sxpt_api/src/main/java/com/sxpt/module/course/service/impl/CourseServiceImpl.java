package com.sxpt.module.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.course.entity.Course;
import com.sxpt.module.course.mapper.CourseMapper;
import com.sxpt.module.course.service.CourseService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程服务实现。
 *
 * 业务功能：
 * 1. 保存课程基础信息，形成任务发布的上层归属。
 * 2. 按租户和默认授课班级查询课程，支撑课程管理列表。
 *
 * 关键流程：
 * 1. 创建时校验最小字段，避免生成无法被任务引用的课程。
 * 2. 补齐默认版本、发布状态、通用状态和软删除字段。
 */
@Service
@Profile("!test")
public class CourseServiceImpl implements CourseService {

    private static final long DEFAULT_VERSION_NO = 1L;

    private static final String DEFAULT_COURSE_STATUS = "PUBLISHED";

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final CourseMapper courseMapper;

    public CourseServiceImpl(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    /**
     * 创建课程。
     *
     * @param course 课程实体。
     * @return 已保存的课程。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Course createCourse(Course course) {
        validateCreateFields(course);
        fillCreateDefaults(course);
        courseMapper.insert(course);
        return course;
    }

    /**
     * 查询课程。
     *
     * @param tenantId 租户 ID。
     * @param targetOrgId 默认授课班级 ID，可为空。
     * @return 课程列表。
     */
    @Override
    public List<Course> listCourses(String tenantId, String targetOrgId) {
        requireText(tenantId);
        QueryWrapper<Course> wrapper = new QueryWrapper<Course>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE);
        if (StringUtils.hasText(targetOrgId)) {
            wrapper.eq("target_org_id", targetOrgId);
        }
        return courseMapper.selectList(wrapper.orderByAsc("course_code", "version_no"));
    }

    /**
     * 校验创建课程所需的最小字段。
     *
     * @param course 课程实体。
     */
    private void validateCreateFields(Course course) {
        if (course == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(course.getId());
        requireText(course.getTenantId());
        requireText(course.getCourseCode());
        requireText(course.getCourseName());
    }

    /**
     * 校验文本字段非空。
     *
     * @param value 待校验字段值。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 补齐创建课程时的默认字段。
     *
     * @param course 课程实体。
     */
    private void fillCreateDefaults(Course course) {
        LocalDateTime now = LocalDateTime.now();
        if (course.getVersionNo() == null) {
            course.setVersionNo(DEFAULT_VERSION_NO);
        }
        if (course.getCreateTime() == null) {
            course.setCreateTime(now);
        }
        if (course.getUpdateTime() == null) {
            course.setUpdateTime(now);
        }
        if (!StringUtils.hasText(course.getCourseStatus())) {
            course.setCourseStatus(DEFAULT_COURSE_STATUS);
        }
        if (!StringUtils.hasText(course.getStatus())) {
            course.setStatus(DEFAULT_STATUS);
        }
        if (course.getDeleted() == null) {
            course.setDeleted(Boolean.FALSE);
        }
    }
}

