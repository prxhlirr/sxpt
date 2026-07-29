package com.sxpt.module.connector.service.impl;

import com.sxpt.module.connector.service.OriginDataPrepareAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 本地开发原平台数据准备适配器。
 *
 * 业务功能：
 * 1. 在真实原平台 HTTP Adapter 尚未接入前，为页面联调提供可启动、可观察的契约实现。
 * 2. 按 requestItemId 逐条返回原平台业务数据引用，帮助验证教学平台侧批次、明细、任务和实例流转。
 *
 * 关键流程：
 * 1. 数据准备编排服务调用 createTeachingData。
 * 2. 本适配器为每个请求项生成外部业务 ID、业务编号、初始状态和目标地址。
 * 3. 后续接入真实原平台时，以新的 OriginDataPrepareAdapter Bean 替换本适配器。
 */
@Service
@ConditionalOnProperty(name = "sxpt.origin.local-adapter.enabled", havingValue = "true", matchIfMissing = true)
public class LocalOriginDataPrepareAdapter implements OriginDataPrepareAdapter {

    private static final String SUCCESS_STATUS = "SUCCESS";

    private static final String DEFAULT_STEP_CODE = "PURCHASE_CREATE";

    private static final int DEFAULT_ACTOR_NO = 1;

    /**
     * 批量创建教学用原平台业务数据。
     *
     * @param request 批量创建请求。
     * @return 批量创建响应。
     */
    @Override
    public BatchCreateResponse createTeachingData(BatchCreateRequest request) {
        BatchCreateResponse response = new BatchCreateResponse();
        response.setExternalRequestId("local-" + shortId());
        response.setRequestBatchId(request.getRequestBatchId());
        response.setAdapterStatus(SUCCESS_STATUS);
        response.setResultJson("{\"adapter\":\"local\",\"status\":\"SUCCESS\"}");
        List<ResponseItem> items = new ArrayList<>();
        if (request.getItems() != null) {
            for (RequestItem requestItem : request.getItems()) {
                items.add(buildResponseItem(request, requestItem));
            }
        }
        response.setItems(items);
        return response;
    }

    /**
     * 查询原平台业务数据摘要。
     *
     * @param request 查询请求。
     * @return 查询响应。
     */
    @Override
    public QueryResponse queryTeachingData(QueryRequest request) {
        QueryResponse response = new QueryResponse();
        response.setTenantId(request.getTenantId());
        response.setConnectorSystemId(request.getConnectorSystemId());
        response.setModuleCode(request.getModuleCode());
        response.setExternalBusinessId(request.getExternalBusinessId());
        response.setExternalStatus("DRAFT");
        response.setTargetUrl("/local-origin/business/" + request.getExternalBusinessId());
        response.setMetadataJson("{\"adapter\":\"local\"}");
        return response;
    }

    /**
     * 校验原平台业务数据是否满足教学约束。
     *
     * @param request 校验请求。
     * @return 校验响应。
     */
    @Override
    public ValidationResponse validateTeachingData(ValidationRequest request) {
        ValidationResponse response = new ValidationResponse();
        response.setPassed(true);
        response.setValidationStatus("PASSED");
        response.setValidationResultJson("{\"adapter\":\"local\",\"passed\":true}");
        return response;
    }

    /**
     * 锁定原平台业务数据。
     *
     * @param request 锁定请求。
     * @return 锁定响应。
     */
    @Override
    public LockResponse lockTeachingData(LockRequest request) {
        LockResponse response = new LockResponse();
        response.setLocked(true);
        response.setExternalStatus("LOCKED");
        response.setResultJson("{\"adapter\":\"local\",\"locked\":true}");
        return response;
    }

    /**
     * 归档原平台业务数据。
     *
     * @param request 归档请求。
     * @return 归档响应。
     */
    @Override
    public ArchiveResponse archiveTeachingData(ArchiveRequest request) {
        ArchiveResponse response = new ArchiveResponse();
        response.setArchived(true);
        response.setResultJson("{\"adapter\":\"local\",\"archived\":true}");
        return response;
    }

    /**
     * 构造逐条创建响应。
     *
     * @param request 批量创建请求。
     * @param requestItem 逐条请求项。
     * @return 逐条创建响应。
     */
    private ResponseItem buildResponseItem(BatchCreateRequest request, RequestItem requestItem) {
        String businessId = "biz-" + shortId();
        ResponseItem item = new ResponseItem();
        item.setRequestItemId(requestItem.getRequestItemId());
        item.setExternalBusinessId(businessId);
        item.setExternalBusinessNo("NO-" + businessId);
        item.setExternalBusinessName("本地联调业务数据-" + requestItem.getRequestItemId());
        item.setExternalStatus("DRAFT");
        item.setTargetUrl("/local-origin/" + request.getModuleCode() + "/" + businessId);
        item.setCurrentStepCode(DEFAULT_STEP_CODE);
        item.setCurrentActorNo(DEFAULT_ACTOR_NO);
        item.setItemStatus(SUCCESS_STATUS);
        return item;
    }

    /**
     * 生成短业务标识。
     *
     * @return 短 UUID。
     */
    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
