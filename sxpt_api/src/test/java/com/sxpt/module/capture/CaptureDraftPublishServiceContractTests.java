package com.sxpt.module.capture;

import com.sxpt.module.capture.service.CaptureDraftPublishService;
import com.sxpt.module.teaching.entity.TaskStep;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 动作草稿发布教学步骤服务契约测试。
 *
 * 业务功能：
 * 1. 验证草稿发布服务接口的最小方法签名稳定。
 * 2. 明确阶段 3 的入口只负责草稿到 task_step 的发布，不提前混入评分项生成。
 *
 * 关键流程：
 * 1. 使用反射检查接口方法名、参数和返回类型。
 * 2. 不加载 Spring 上下文，避免契约测试引入数据库或 Mapper 依赖。
 */
class CaptureDraftPublishServiceContractTests {

    /**
     * 校验发布草稿到任务步骤的方法签名。
     *
     * @throws NoSuchMethodException 当方法被误删或参数被误改时，测试应失败。
     */
    @Test
    void publishDraftToTaskStepMethodShouldKeepStableContract() throws NoSuchMethodException {
        Method method = CaptureDraftPublishService.class.getMethod(
                "publishDraftToTaskStep", String.class, String.class, String.class, String.class, String.class);

        assertEquals(TaskStep.class, method.getReturnType());
    }
}
