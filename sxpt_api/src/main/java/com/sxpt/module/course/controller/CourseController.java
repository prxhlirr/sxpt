package com.sxpt.module.course.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.course.dto.CreateCourseRequest;
import com.sxpt.module.course.entity.Course;
import com.sxpt.module.course.service.CourseService;
import com.sxpt.module.course.vo.CourseVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 课程接口。
 *
 * 业务功能：
 * 1. 提供课程创建入口，保存课程编码、名称、默认班级和时间范围。
 * 2. 提供课程查询入口，为后续任务发布提供课程清单。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation，提前拦截缺少课程编码或名称的无效课程。
 * 2. 将 DTO 转换为 Course 实体并生成应用层主键。
 * 3. 调用 Service 写入课程，再转换为 VO 返回前端。
 */
@RestController
@RequestMapping("/api/v1/courses")
@ConditionalOnProperty(name = "sxpt.course.controller.enabled", havingValue = "true", matchIfMissing = true)
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * 创建课程。
     *
     * @param request 创建课程请求。
     * @return 已保存的课程。
     */
    @PostMapping("/create")
    public ApiResult<CourseVO> create(@Valid @RequestBody CreateCourseRequest request) {
        return ApiResult.success(toVO(courseService.createCourse(toEntity(request))));
    }

    /**
     * 查询课程。
     *
     * @param tenantId 租户 ID。
     * @param targetOrgId 默认授课班级 ID，可为空。
     * @return 课程列表。
     */
    @GetMapping
    public ApiResult<List<CourseVO>> list(@RequestParam String tenantId,
                                          @RequestParam(required = false) String targetOrgId) {
        return ApiResult.success(toVOList(courseService.listCourses(tenantId, targetOrgId)));
    }

    /**
     * 将创建请求转换为实体。
     *
     * @param request 创建课程请求。
     * @return 课程实体。
     */
    private Course toEntity(CreateCourseRequest request) {
        Course course = new Course();
        course.setId(generateId());
        course.setTenantId(request.getTenantId());
        course.setCourseCode(request.getCourseCode());
        course.setCourseName(request.getCourseName());
        course.setTargetOrgId(request.getTargetOrgId());
        course.setStartTime(request.getStartTime());
        course.setEndTime(request.getEndTime());
        course.setDescription(request.getDescription());
        course.setCreateBy(request.getCreateBy());
        course.setUpdateBy(request.getCreateBy());
        return course;
    }

    /**
     * 将课程实体列表转换为 VO 列表。
     *
     * @param courses 课程实体列表。
     * @return 课程 VO 列表。
     */
    private List<CourseVO> toVOList(List<Course> courses) {
        List<CourseVO> result = new ArrayList<>();
        for (Course course : courses) {
            result.add(toVO(course));
        }
        return result;
    }

    /**
     * 将课程实体转换为 VO。
     *
     * @param course 课程实体。
     * @return 课程 VO。
     */
    private CourseVO toVO(Course course) {
        CourseVO vo = new CourseVO();
        vo.setId(course.getId());
        vo.setTenantId(course.getTenantId());
        vo.setCourseCode(course.getCourseCode());
        vo.setCourseName(course.getCourseName());
        vo.setVersionNo(course.getVersionNo());
        vo.setTargetOrgId(course.getTargetOrgId());
        vo.setStartTime(course.getStartTime());
        vo.setEndTime(course.getEndTime());
        vo.setDescription(course.getDescription());
        vo.setCourseStatus(course.getCourseStatus());
        vo.setStatus(course.getStatus());
        vo.setCreateTime(course.getCreateTime());
        vo.setUpdateTime(course.getUpdateTime());
        return vo;
    }

    /**
     * 生成应用层主键。
     *
     * @return 32 位无横线字符串 ID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
