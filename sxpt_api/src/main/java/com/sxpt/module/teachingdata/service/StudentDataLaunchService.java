package com.sxpt.module.teachingdata.service;

import com.sxpt.module.teachingdata.dto.CreateStudentDataLaunchRequest;
import com.sxpt.module.teachingdata.dto.CreateStudentTaskLaunchRequest;
import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;
import com.sxpt.module.teachingdata.vo.StudentDataLaunchVO;

import java.util.List;

/**
 * 学生数据启动服务。
 *
 * 业务功能：
 * 1. 将学生已领取的数据分配记录转换为原平台启动上下文。
 * 2. 固化“学生只能进入分配给自己的那条数据”的服务端校验边界。
 *
 * 关键流程：
 * 1. 校验请求和分配记录归属。
 * 2. 基于分配记录中的原平台系统、业务数据、办理步骤、单位和角色生成 launchToken。
 * 3. 返回可直接跳转的 launchUrl 给学生端。
 */
public interface StudentDataLaunchService {

    /**
     * 创建学生进入原平台办理的一次性启动上下文。
     *
     * @param request 学生启动请求。
     * @return 启动结果，包含一次性 token、跳转地址和分配记录快照。
     */
    StudentDataLaunchVO createLaunch(CreateStudentDataLaunchRequest request);

    /**
     * 查询当前登录学生在指定教学任务下已经分配的原平台数据。
     *
     * @param request 学生任务查询请求，前端不允许提交学生身份。
     * @return 当前登录学生可见的数据分配记录。
     */
    List<DataInstanceAllocation> listCurrentStudentTaskAllocations(CreateStudentTaskLaunchRequest request);

    /**
     * 为当前登录学生重新创建一条初始业务数据并完成新的分配绑定。
     *
     * @param request 学生任务重练请求，前端不允许提交学生身份、单位或角色。
     * @return 新生成并绑定给当前学生的数据分配记录。
     */
    DataInstanceAllocation recreateAllocationForCurrentStudentTask(CreateStudentTaskLaunchRequest request);

    /**
     * 按当前登录学生和教学任务创建原平台启动上下文。
     *
     * @param request 学生任务启动请求，前端只允许提交任务、场景和执行上下文。
     * @return 启动结果，包含一次性 token、跳转地址和分配记录快照。
     */
    StudentDataLaunchVO createLaunchForCurrentStudentTask(CreateStudentTaskLaunchRequest request);
}
