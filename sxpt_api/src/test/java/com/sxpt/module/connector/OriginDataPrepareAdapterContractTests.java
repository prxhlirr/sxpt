package com.sxpt.module.connector;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.entity.PlatformCapability;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.connector.mapper.PlatformCapabilityMapper;
import com.sxpt.module.connector.service.OriginDataPrepareAdapter;
import com.sxpt.module.connector.service.impl.HttpOriginDataPrepareAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 原平台数据准备适配器契约测试。
 *
 * 业务功能：
 * 1. 验证 Adapter 固定暴露创建、查询、校验、锁定和归档五类能力。
 * 2. 验证批量创建请求和响应可以按 requestItemId 逐条对账。
 *
 * 关键流程：
 * 1. 使用反射检查接口方法签名，避免后续实现绕开统一 Adapter 契约。
 * 2. 使用纯对象测试检查请求响应字段可读写，不依赖 Spring 和数据库。
 */
class OriginDataPrepareAdapterContractTests {

    private static final String OA_CREATE_CONTRACT =
            "{\"contract\":\"TEACHING_DATA_BATCH_CREATE_V1\","
                    + "\"businessModuleCode\":\"doc_incoming\","
                    + "\"templateCode\":\"incoming_pending_reg_v1\","
                    + "\"initState\":\"PENDING_REG\","
                    + "\"defaultPoolKey\":\"incoming-default\","
                    + "\"preferPoolKey\":true,\"forwardBizParams\":false}";

    /**
     * 校验 Adapter 暴露五个核心动作。
     *
     * @throws NoSuchMethodException 当接口方法被误删或改名时测试失败。
     */
    @Test
    void adapterShouldExposeCoreOriginDataActions() throws NoSuchMethodException {
        Method createMethod = OriginDataPrepareAdapter.class.getMethod(
                "createTeachingData", OriginDataPrepareAdapter.BatchCreateRequest.class);
        Method queryMethod = OriginDataPrepareAdapter.class.getMethod(
                "queryTeachingData", OriginDataPrepareAdapter.QueryRequest.class);
        Method validateMethod = OriginDataPrepareAdapter.class.getMethod(
                "validateTeachingData", OriginDataPrepareAdapter.ValidationRequest.class);
        Method lockMethod = OriginDataPrepareAdapter.class.getMethod(
                "lockTeachingData", OriginDataPrepareAdapter.LockRequest.class);
        Method archiveMethod = OriginDataPrepareAdapter.class.getMethod(
                "archiveTeachingData", OriginDataPrepareAdapter.ArchiveRequest.class);

        assertEquals(OriginDataPrepareAdapter.BatchCreateResponse.class, createMethod.getReturnType());
        assertEquals(OriginDataPrepareAdapter.QueryResponse.class, queryMethod.getReturnType());
        assertEquals(OriginDataPrepareAdapter.ValidationResponse.class, validateMethod.getReturnType());
        assertEquals(OriginDataPrepareAdapter.LockResponse.class, lockMethod.getReturnType());
        assertEquals(OriginDataPrepareAdapter.ArchiveResponse.class, archiveMethod.getReturnType());
    }

    /**
     * 校验批量创建请求和响应可以表达逐条请求与逐条返回。
     */
    @Test
    void batchCreateContractShouldCarryRequestAndResponseItems() {
        OriginDataPrepareAdapter.RequestItem requestItem = new OriginDataPrepareAdapter.RequestItem();
        requestItem.setRequestItemId("batch_001:item_001");
        requestItem.setStudentId("student_001");
        requestItem.setRequiredExternalOrgId("org_001");
        requestItem.setRequiredExternalRoleId("role_001");

        OriginDataPrepareAdapter.BatchCreateRequest request = new OriginDataPrepareAdapter.BatchCreateRequest();
        request.setTenantId("tenant_001");
        request.setConnectorSystemId("connector_001");
        request.setModuleCode("BUSINESS_APPLY");
        request.setTemplateId("template_001");
        request.setInitState("DRAFT");
        request.setSceneType("PRACTICE");
        request.setRequestBatchId("batch_001");
        request.setIdempotencyKey("prepare:tenant_001:batch_001");
        request.setTraceId("trace_001");
        request.setItems(Collections.singletonList(requestItem));

        OriginDataPrepareAdapter.ResponseItem responseItem = new OriginDataPrepareAdapter.ResponseItem();
        responseItem.setRequestItemId("batch_001:item_001");
        responseItem.setExternalBusinessId("biz_001");
        responseItem.setExternalBusinessNo("NO-001");
        responseItem.setExternalStatus("DRAFT");
        responseItem.setTargetUrl("/origin/business/biz_001");

        OriginDataPrepareAdapter.BatchCreateResponse response = new OriginDataPrepareAdapter.BatchCreateResponse();
        response.setExternalRequestId("origin_req_001");
        response.setRequestBatchId("batch_001");
        response.setAdapterStatus("SUCCESS");
        response.setItems(Collections.singletonList(responseItem));

        assertEquals("batch_001:item_001", request.getItems().get(0).getRequestItemId());
        assertEquals("student_001", request.getItems().get(0).getStudentId());
        assertEquals("template_001", request.getTemplateId());
        assertEquals("trace_001", request.getTraceId());
        assertEquals("origin_req_001", response.getExternalRequestId());
        assertEquals("biz_001", response.getItems().get(0).getExternalBusinessId());
        assertNotNull(response.getItems().get(0).getTargetUrl());
    }

    /**
     * 校验查询、校验、锁定和归档共享外部业务数据定位字段。
     */
    @Test
    void businessDataReferenceShouldBeSharedByReadAndLifecycleRequests() {
        OriginDataPrepareAdapter.ValidationRequest validationRequest = new OriginDataPrepareAdapter.ValidationRequest();
        validationRequest.setTenantId("tenant_001");
        validationRequest.setConnectorSystemId("connector_001");
        validationRequest.setModuleCode("BUSINESS_APPLY");
        validationRequest.setExternalBusinessId("biz_001");
        validationRequest.setRequiredExternalOrgId("org_001");
        validationRequest.setRequiredExternalRoleId("role_001");

        OriginDataPrepareAdapter.ValidationResponse validationResponse = new OriginDataPrepareAdapter.ValidationResponse();
        validationResponse.setPassed(true);
        validationResponse.setValidationStatus("PASSED");

        OriginDataPrepareAdapter.LockResponse lockResponse = new OriginDataPrepareAdapter.LockResponse();
        lockResponse.setLocked(true);
        lockResponse.setExternalStatus("LOCKED");

        OriginDataPrepareAdapter.ArchiveResponse archiveResponse = new OriginDataPrepareAdapter.ArchiveResponse();
        archiveResponse.setArchived(true);
        archiveResponse.setResultJson("{\"archived\":true}");

        assertEquals("tenant_001", validationRequest.getTenantId());
        assertEquals("biz_001", validationRequest.getExternalBusinessId());
        assertEquals("org_001", validationRequest.getRequiredExternalOrgId());
        assertEquals("PASSED", validationResponse.getValidationStatus());
        assertEquals("LOCKED", lockResponse.getExternalStatus());
        assertEquals("{\"archived\":true}", archiveResponse.getResultJson());
    }

    /**
     * 校验内部批次会被映射为 OA 标准收文造数协议，且不会发送学生姓名等非契约字段。
     */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void httpAdapterShouldMapOaStandardBatchCreateContract() {
        ConnectorSystemMapper systemMapper = mock(ConnectorSystemMapper.class);
        PlatformCapabilityMapper capabilityMapper = mock(PlatformCapabilityMapper.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        ConnectorSystem system = new ConnectorSystem();
        system.setId("origin-oa");
        system.setTenantId("tenant-1");
        system.setBaseUrl("http://127.0.0.1:9527");
        system.setAuthType("BEARER");
        system.setConfigJson("{\"token\":\"oa-token\"}");

        PlatformCapability capability = new PlatformCapability();
        capability.setEndpointUrl("/openapi/teaching-data/batch-create");
        capability.setMethod("POST");
        capability.setRequestSchemaJson(OA_CREATE_CONTRACT);

        when(systemMapper.selectOne(any(QueryWrapper.class))).thenReturn(system);
        when(capabilityMapper.selectOne(any(QueryWrapper.class))).thenReturn(capability);
        when(restTemplate.exchange(
                eq("http://127.0.0.1:9527/openapi/teaching-data/batch-create"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(standardSuccessResponse(), HttpStatus.OK));

        HttpOriginDataPrepareAdapter adapter = new HttpOriginDataPrepareAdapter(
                systemMapper, capabilityMapper, restTemplate);
        OriginDataPrepareAdapter.BatchCreateResponse response =
                adapter.createTeachingData(createStandardRequest());

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("http://127.0.0.1:9527/openapi/teaching-data/batch-create"),
                eq(HttpMethod.POST), entityCaptor.capture(), eq(String.class));
        HttpEntity entity = entityCaptor.getValue();
        Map<String, Object> body = new ObjectMapper().convertValue(entity.getBody(), Map.class);
        assertNotNull(body);
        assertEquals("doc_incoming", body.get("businessModuleCode"));
        assertEquals("incoming_pending_reg_v1", body.get("templateCode"));
        assertEquals("PENDING_REG", body.get("initState"));
        assertEquals("PRACTICE", body.get("sceneType"));
        Map<String, Object> participant =
                (Map<String, Object>) ((java.util.List) body.get("participants")).get(0);
        assertEquals("item-1", participant.get("participantId"));
        assertEquals("student-1", participant.get("ownerUserId"));
        assertEquals("incoming-default", participant.get("poolKey"));
        assertFalse(participant.containsKey("externalOrgId"));
        assertFalse(participant.containsKey("studentName"));
        assertFalse(body.containsKey("tenantId"));
        assertEquals(Collections.emptyMap(), body.get("bizParams"));
        assertEquals("Bearer oa-token", entity.getHeaders().getFirst("Authorization"));
        assertEquals("trace-1", entity.getHeaders().getFirst("X-Trace-Id"));
        assertEquals("idem-1", entity.getHeaders().getFirst("X-Idempotency-Key"));
        assertEquals("oa-batch-1", response.getExternalRequestId());
        assertEquals("oa-data-1", response.getItems().get(0).getExternalBusinessId());
        assertEquals("PENDING_REG", response.getItems().get(0).getExternalStatus());
        assertEquals("/workspace/incoming/detail/oa-data-1", response.getItems().get(0).getTargetUrl());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void httpAdapterShouldKeepAuthoringOrganizationWhenQuestionPoolIsAbsent() {
        ConnectorSystemMapper systemMapper = mock(ConnectorSystemMapper.class);
        PlatformCapabilityMapper capabilityMapper = mock(PlatformCapabilityMapper.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        ConnectorSystem system = new ConnectorSystem();
        system.setId("origin-oa");
        system.setTenantId("tenant-1");
        system.setBaseUrl("http://127.0.0.1:9527");
        system.setAuthType("BEARER");
        system.setConfigJson("{\"token\":\"oa-token\"}");

        PlatformCapability capability = new PlatformCapability();
        capability.setEndpointUrl("/openapi/teaching-data/batch-create");
        capability.setMethod("POST");
        capability.setRequestSchemaJson(OA_CREATE_CONTRACT);

        when(systemMapper.selectOne(any(QueryWrapper.class))).thenReturn(system);
        when(capabilityMapper.selectOne(any(QueryWrapper.class))).thenReturn(capability);
        when(restTemplate.exchange(
                eq("http://127.0.0.1:9527/openapi/teaching-data/batch-create"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(standardSuccessResponse(), HttpStatus.OK));

        OriginDataPrepareAdapter.BatchCreateRequest request = createStandardRequest();
        request.getItems().get(0).setQuestionId(null);
        HttpOriginDataPrepareAdapter adapter = new HttpOriginDataPrepareAdapter(
                systemMapper, capabilityMapper, restTemplate);
        adapter.createTeachingData(request);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("http://127.0.0.1:9527/openapi/teaching-data/batch-create"),
                eq(HttpMethod.POST), entityCaptor.capture(), eq(String.class));
        Map<String, Object> body = new ObjectMapper().convertValue(
                entityCaptor.getValue().getBody(), Map.class);
        Map<String, Object> participant =
                (Map<String, Object>) ((java.util.List) body.get("participants")).get(0);
        assertEquals("teaching-org-1", participant.get("externalOrgId"));
        assertFalse(participant.containsKey("poolKey"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void httpAdapterShouldCorrelateLongInternalRequestItemIdThroughOaParticipantId() {
        ConnectorSystemMapper systemMapper = mock(ConnectorSystemMapper.class);
        PlatformCapabilityMapper capabilityMapper = mock(PlatformCapabilityMapper.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        ConnectorSystem system = new ConnectorSystem();
        system.setId("origin-oa");
        system.setTenantId("tenant-1");
        system.setBaseUrl("http://127.0.0.1:9527");
        system.setAuthType("BEARER");
        system.setConfigJson("{\"token\":\"oa-token\"}");

        PlatformCapability capability = new PlatformCapability();
        capability.setEndpointUrl("/openapi/teaching-data/batch-create");
        capability.setMethod("POST");
        capability.setRequestSchemaJson(OA_CREATE_CONTRACT);

        when(systemMapper.selectOne(any(QueryWrapper.class))).thenReturn(system);
        when(capabilityMapper.selectOne(any(QueryWrapper.class))).thenReturn(capability);
        String[] outboundParticipantId = new String[1];
        when(restTemplate.exchange(
                eq("http://127.0.0.1:9527/openapi/teaching-data/batch-create"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    HttpEntity entity = invocation.getArgument(2);
                    Map<String, Object> body = new ObjectMapper().convertValue(entity.getBody(), Map.class);
                    Map<String, Object> participant =
                            (Map<String, Object>) ((java.util.List) body.get("participants")).get(0);
                    outboundParticipantId[0] = (String) participant.get("participantId");
                    return new ResponseEntity<>(
                            standardSuccessResponse(outboundParticipantId[0]), HttpStatus.OK);
                });

        String internalRequestItemId = "authoring_0123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789";
        OriginDataPrepareAdapter.BatchCreateRequest request = createStandardRequest();
        request.getItems().get(0).setRequestItemId(internalRequestItemId);
        HttpOriginDataPrepareAdapter adapter = new HttpOriginDataPrepareAdapter(
                systemMapper, capabilityMapper, restTemplate);

        OriginDataPrepareAdapter.BatchCreateResponse response = adapter.createTeachingData(request);

        assertNotNull(outboundParticipantId[0]);
        assertTrue(outboundParticipantId[0].length() <= 64);
        assertNotEquals(internalRequestItemId, outboundParticipantId[0]);
        assertEquals(internalRequestItemId, response.getItems().get(0).getRequestItemId());
    }

    private OriginDataPrepareAdapter.BatchCreateRequest createStandardRequest() {
        OriginDataPrepareAdapter.RequestItem item = new OriginDataPrepareAdapter.RequestItem();
        item.setRequestItemId("item-1");
        item.setStudentId("student-1");
        item.setRequiredExternalOrgId("teaching-org-1");
        item.setQuestionId("internal-question-17");

        OriginDataPrepareAdapter.BatchCreateRequest request = new OriginDataPrepareAdapter.BatchCreateRequest();
        request.setTenantId("tenant-1");
        request.setConnectorSystemId("origin-oa");
        request.setModuleCode("internal-record-module");
        request.setTemplateId("template-1");
        request.setInitState("DRAFT");
        request.setSceneType("PRACTICE");
        request.setRequestBatchId("batch-1");
        request.setTraceId("trace-1");
        request.setIdempotencyKey("idem-1");
        request.setRequestJson("{\"templateCode\":\"internal-record-template\","
                + "\"initState\":\"DRAFT\",\"traceId\":\"trace-1\","
                + "\"bizParams\":{\"restartAttemptId\":\"restart-1\"}}");
        request.setItems(Collections.singletonList(item));
        return request;
    }

    private String standardSuccessResponse() {
        return standardSuccessResponse("item-1");
    }

    private String standardSuccessResponse(String participantId) {
        return "{\"success\":true,\"code\":200,\"message\":\"success\",\"result\":{"
                + "\"originBatchId\":\"oa-batch-1\",\"status\":\"SUCCESS\","
                + "\"totalCount\":1,\"successCount\":1,\"failedCount\":0,\"items\":[{"
                + "\"participantId\":\"" + participantId + "\",\"ownerUserId\":\"student-1\","
                + "\"externalDataId\":\"oa-data-1\",\"externalBizNo\":\"收文〔2026〕1号\","
                + "\"externalStatus\":\"PENDING_REG\","
                + "\"entryUrl\":\"/workspace/incoming/detail/oa-data-1\","
                + "\"externalOrgId\":\"oa-org-1\",\"rawData\":{\"title\":\"测试收文\"}}]}}";
    }
}
