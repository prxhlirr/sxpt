package com.sxpt.module.capture;

import com.sxpt.module.capture.service.CaptureResourcePromoteService;
import com.sxpt.module.connector.entity.ConnectorResource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 采集资源快照晋升服务契约测试。
 *
 * 业务功能：
 * 1. 验证晋升服务接口的最小方法签名稳定，避免后续实现偏离阶段任务边界。
 * 2. 明确阶段 2 的入口是单个快照晋升，不在接口层提前引入批量和草稿绑定复杂度。
 *
 * 关键流程：
 * 1. 使用反射检查接口方法名、参数和返回类型。
 * 2. 不加载 Spring 上下文，保持契约测试轻量稳定。
 */
class CaptureResourcePromoteServiceContractTests {

    /**
     * 校验单快照晋升方法签名。
     *
     * @throws NoSuchMethodException 当方法被误删或参数被误改时，测试应失败。
     */
    @Test
    void promoteSnapshotMethodShouldKeepStableContract() throws NoSuchMethodException {
        Method method = CaptureResourcePromoteService.class.getMethod(
                "promoteSnapshot", String.class, String.class, String.class);

        assertEquals(ConnectorResource.class, method.getReturnType());
    }
}
