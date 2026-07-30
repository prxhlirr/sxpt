package com.sxpt.module.teachingdata.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.teachingdata.dto.CreateStudentDataLaunchRequest;
import com.sxpt.module.teachingdata.dto.CreateStudentTaskLaunchRequest;
import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;
import com.sxpt.module.teachingdata.service.StudentDataLaunchService;
import com.sxpt.module.teachingdata.vo.StudentDataLaunchVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    /**
     * 按当前登录学生和教学任务创建原平台启动上下文。
     *
     * @param request 学生任务启动请求。
     * @return 启动结果。
     */
    @PostMapping("/task-launch")
    public ApiResult<StudentDataLaunchVO> createTaskLaunch(@RequestBody CreateStudentTaskLaunchRequest request) {
        return ApiResult.success(studentDataLaunchService.createLaunchForCurrentStudentTask(request));
    }

    /**
     * 查询当前登录学生在指定教学任务下已经分配的原平台数据。
     *
     * @param request 学生任务查询请求，前端只提交任务、场景和执行上下文。
     * @return 当前登录学生可见的数据分配记录。
     */
    @PostMapping("/task-allocations")
    public ApiResult<List<DataInstanceAllocation>> listCurrentStudentTaskAllocations(
            @RequestBody CreateStudentTaskLaunchRequest request) {
        return ApiResult.success(studentDataLaunchService.listCurrentStudentTaskAllocations(request));
    }

    /**
     * 为当前登录学生重新创建初始业务数据并绑定新的分配记录。
     *
     * @param request 学生任务重练请求，前端只提交任务、场景和执行上下文。
     * @return 新生成并绑定给当前学生的数据分配记录。
     */
    @PostMapping("/task-restart")
    public ApiResult<DataInstanceAllocation> restartCurrentStudentTaskData(
            @RequestBody CreateStudentTaskLaunchRequest request) {
        return ApiResult.success(studentDataLaunchService.recreateAllocationForCurrentStudentTask(request));
    }
}
