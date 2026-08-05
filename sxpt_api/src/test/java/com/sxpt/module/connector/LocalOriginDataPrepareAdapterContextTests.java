package com.sxpt.module.connector;

import com.sxpt.module.connector.service.OriginDataPrepareAdapter;
import com.sxpt.module.connector.service.impl.LocalOriginDataPrepareAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    /**
     * 验证本地 Adapter 不返回模块无关的固定办理位置。
     *
     * 业务功能：本地联调系统没有真实原平台流程状态时，必须把当前步骤和参与方留给业务模块标准链推导。
     * 关键流程：构造任意模块造数请求，调用本地 Adapter 后确认响应只包含业务数据引用，不携带固定步骤码和参与方序号。
     */
    @Test
    void localAdapterShouldLetStandardProcessChainResolveCurrentActor() {
        LocalOriginDataPrepareAdapter adapter = new LocalOriginDataPrepareAdapter();
        OriginDataPrepareAdapter.RequestItem requestItem = new OriginDataPrepareAdapter.RequestItem();
        requestItem.setRequestItemId("batch_001:item_001");
        OriginDataPrepareAdapter.BatchCreateRequest request = new OriginDataPrepareAdapter.BatchCreateRequest();
        request.setRequestBatchId("batch_001");
        request.setModuleCode("record_apply_1785916779495");
        request.setItems(Collections.singletonList(requestItem));

        OriginDataPrepareAdapter.BatchCreateResponse response = adapter.createTeachingData(request);

        OriginDataPrepareAdapter.ResponseItem responseItem = response.getItems().get(0);
        assertNotNull(responseItem.getExternalBusinessId());
        assertNull(responseItem.getCurrentStepCode());
        assertNull(responseItem.getCurrentActorNo());
    }
}
