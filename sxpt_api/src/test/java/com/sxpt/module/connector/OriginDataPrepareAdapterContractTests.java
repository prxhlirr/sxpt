package com.sxpt.module.connector;

import com.sxpt.module.connector.service.OriginDataPrepareAdapter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        request.setSceneType("PRACTICE");
        request.setRequestBatchId("batch_001");
        request.setIdempotencyKey("prepare:tenant_001:batch_001");
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
}
