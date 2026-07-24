package com.sxpt.module.capture;

import com.sxpt.module.capture.service.TeacherFilingOrchestrationService;
import com.sxpt.module.capture.vo.TeacherFilingOrchestrationResultVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 老师备案编排服务契约测试。
 *
 * 业务功能：
 * 1. 锁定老师备案编排服务的最小入口，避免后续实现时把入口参数拆散到多个临时方法。
 * 2. 明确编排服务只返回阶段结果，不直接暴露采集、教学和评分实体。
 *
 * 关键流程：
 * 1. 使用反射检查接口方法名、参数和返回类型。
 * 2. 不加载 Spring 上下文，避免契约测试依赖数据库或 Mapper。
 */
class TeacherFilingOrchestrationServiceContractTests {

    /**
     * 校验准备教学资产的方法签名保持稳定。
     *
     * @throws NoSuchMethodException 当方法被误删或参数被误改时，测试应失败。
     */
    @Test
    void prepareTeachingAssetsMethodShouldKeepStableContract() throws NoSuchMethodException {
        Method method = TeacherFilingOrchestrationService.class.getMethod(
                "prepareTeachingAssets",
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class);

        assertEquals(TeacherFilingOrchestrationResultVO.class, method.getReturnType());
    }
}
