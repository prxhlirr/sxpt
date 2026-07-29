package com.sxpt.module.teachingdata.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.teachingdata.dto.CreateStudentDataLaunchRequest;
import com.sxpt.module.teachingdata.service.StudentDataLaunchService;
import com.sxpt.module.teachingdata.vo.StudentDataLaunchVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生数据启动控制器。
 *
 * 业务功能：
 * 1. 为学生端提供“使用已分配数据进入原平台”的统一入口。
 * 2. 隐藏原平台单位、角色、数据实例等敏感映射规则，避免前端拼装可信上下文。
 *
 * 关键流程：
 * 1. 学生端提交分配记录 ID。
 * 2. 服务端校验分配归属并创建 launchToken。
 * 3. 学生端根据 launchUrl 打开原平台办理页面。
 */
@RestController
@RequestMapping("/api/v1/teaching-data/student-launches")
@ConditionalOnProperty(name = "sxpt.teaching-data.admin-controller.enabled", havingValue = "true", matchIfMissing = true)
public class StudentDataLaunchController {

    private final StudentDataLaunchService studentDataLaunchService;

    public StudentDataLaunchController(StudentDataLaunchService studentDataLaunchService) {
        this.studentDataLaunchService = studentDataLaunchService;
    }

    /**
     * 创建学生进入原平台办理的一次性启动上下文。
     *
     * @param request 学生启动请求。
     * @return 启动结果。
     */
    @PostMapping("/create")
    public ApiResult<StudentDataLaunchVO> createLaunch(@RequestBody CreateStudentDataLaunchRequest request) {
        return ApiResult.success(studentDataLaunchService.createLaunch(request));
    }
}
