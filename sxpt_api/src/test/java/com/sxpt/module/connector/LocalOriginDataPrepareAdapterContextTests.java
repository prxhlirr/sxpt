package com.sxpt.module.connector;

import com.sxpt.module.connector.service.OriginDataPrepareAdapter;
import com.sxpt.module.connector.service.impl.LocalOriginDataPrepareAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地原平台数据准备适配器上下文测试。
 *
 * 业务功能：
 * 1. 验证本地开发 Adapter 能作为 OriginDataPrepareAdapter Bean 注册。
 * 2. 防止数据准备编排服务启动时因为缺少原平台适配器而失败。
 *
 * 关键流程：
 * 1. 构造轻量 Spring 上下文。
 * 2. 注册 LocalOriginDataPrepareAdapter。
 * 3. 按接口类型获取 Bean 并校验实现类型。
 */
class LocalOriginDataPrepareAdapterContextTests {

    /**
     * 验证本地 Adapter 可以按接口类型被 Spring 解析。
     */
    @Test
    void localAdapterShouldBeResolvableAsOriginDataPrepareAdapter() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(LocalOriginDataPrepareAdapter.class);
            context.refresh();

            OriginDataPrepareAdapter adapter = context.getBean(OriginDataPrepareAdapter.class);

            assertNotNull(adapter);
            assertTrue(adapter instanceof LocalOriginDataPrepareAdapter);
        }
    }
}
