package com.sxpt.module.evaluation.assertion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.module.connector.entity.ConnectorResource;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.mapper.ConnectorResourceMapper;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 外部 API 结果断言策略测试。
 *
 * 业务功能：验证 EXTERNAL_API_RESULT 能从正式 API 资源读取原平台地址并查询真实业务结果。
 * 关键流程：用 MockRestServiceServer 模拟原平台 GET 响应，避免测试依赖真实网络，同时证明评分策略会完成 HTTP 调用和字段抽取。
 */
class ExternalApiResultAssertionStrategyTests {

    private final ConnectorResourceMapper connectorResourceMapper = mock(ConnectorResourceMapper.class);

    private final ConnectorSystemMapper connectorSystemMapper = mock(ConnectorSystemMapper.class);

    private final RestTemplate restTemplate = new RestTemplate();

    private final ExternalApiResultAssertionStrategy strategy = new ExternalApiResultAssertionStrategy(
            connectorResourceMapper,
            connectorSystemMapper,
            new ObjectMapper(),
            restTemplate);

    /**
     * 验证外部 API 响应字段等于期望值时命中评分项。
     */
    @Test
    void evaluateShouldMatchWhenExternalApiResponseFieldEqualsExpected() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://origin.example.com/api/records/1001"))
                .andRespond(withSuccess("{\"result\":{\"status\":\"APPROVED\"}}", MediaType.APPLICATION_JSON));
        when(connectorResourceMapper.selectById("api_res_001")).thenReturn(buildApiResource("GET"));
        when(connectorSystemMapper.selectById("connector_001")).thenReturn(buildConnectorSystem());

        AssertionDecision decision = strategy.evaluate(
                buildItem(),
                buildConfig("APPROVED", "result.status"),
                Collections.emptyList());

        assertTrue(decision.isMatched());
        assertEquals("api_res_001", decision.getApiResourceId());
        server.verify();
    }

    /**
     * 验证外部 API 配置为非 GET 时不执行有副作用的调用并返回稳定未命中原因。
     */
    @Test
    void evaluateShouldRejectUnsupportedApiMethod() {
        when(connectorResourceMapper.selectById("api_res_001")).thenReturn(buildApiResource("POST"));

        AssertionDecision decision = strategy.evaluate(
                buildItem(),
                buildConfig("APPROVED", "result.status"),
                Collections.emptyList());

        assertEquals(false, decision.isMatched());
        assertEquals("UNSUPPORTED_API_METHOD", decision.getReason());
    }

    private EvaluationItem buildItem() {
        EvaluationItem item = new EvaluationItem();
        item.setRelatedApiResourceId("api_res_001");
        item.setAssertionType("EXTERNAL_API_RESULT");
        return item;
    }

    private Map<String, Object> buildConfig(String expected, String responseField) {
        Map<String, Object> config = new HashMap<>();
        config.put("expectedStatus", expected);
        config.put("responseField", responseField);
        return config;
    }

    private ConnectorResource buildApiResource(String method) {
        ConnectorResource resource = new ConnectorResource();
        resource.setId("api_res_001");
        resource.setConnectorSystemId("connector_001");
        resource.setResourceType("API");
        resource.setMetadataJson("{\"endpoint\":\"/api/records/1001\",\"method\":\"" + method + "\"}");
        resource.setDeleted(Boolean.FALSE);
        return resource;
    }

    private ConnectorSystem buildConnectorSystem() {
        ConnectorSystem system = new ConnectorSystem();
        system.setId("connector_001");
        system.setBaseUrl("https://origin.example.com");
        return system;
    }
}
