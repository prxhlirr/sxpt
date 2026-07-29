package com.sxpt.module.teachingdata.service;

import com.sxpt.module.teachingdata.dto.CreateStudentDataLaunchRequest;
import com.sxpt.module.teachingdata.vo.StudentDataLaunchVO;

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
}
