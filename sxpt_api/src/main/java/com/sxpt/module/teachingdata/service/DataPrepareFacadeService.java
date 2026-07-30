package com.sxpt.module.teachingdata.service;

import com.sxpt.module.teachingdata.entity.DataPrepareJob;

/**
 * 数据准备门面服务。
 *
 * 业务功能：
 * 1. 提供数据准备模块内部的统一触发入口。
 * 2. 将需求明细生成、准备任务创建和编排执行串成一个稳定流程，避免 Controller、练习重置、考试绑定各自拼接任务。
 *
 * 关键流程：
 * 1. 调用方传入已有数据需求批次和参与者约束。
 * 2. 服务先生成 DataRequirementItem，再创建 DataPrepareJob。
 * 3. 服务调用 DataPrepareOrchestrationService 执行原平台数据创建并返回最终任务状态。
 */
public interface DataPrepareFacadeService {

    /**
     * 生成需求明细并立即触发原平台数据准备。
     *
     * @param request 数据准备触发请求。
     * @return 执行后的数据准备任务。
     */
    DataPrepareJob prepareAndExecute(PrepareAndExecuteRequest request);

    /**
     * 人工重试失败的数据准备任务。
     *
     * 业务功能：为后台证据链页面提供受控补偿入口。当前只允许全失败任务直接重试；
     * 部分失败且已有成功数据时需要失败项级补偿，避免重复创建已成功数据。
     *
     * @param request 失败任务重试请求。
     * @return 重试执行后的数据准备任务。
     */
    DataPrepareJob retryFailedJob(RetryFailedJobRequest request);

    /**
     * 数据准备触发请求。
     *
     * 业务功能：
     * 1. 承载需求明细生成请求和准备任务触发信息。
     * 2. 允许调用方显式指定 jobId 和幂等键，方便发布、按需、重试等不同入口统一审计。
     */
    class PrepareAndExecuteRequest {

        private String jobId;

        private String jobType;

        private String triggerType;

        private String idempotencyKey;

        private String traceId;

        private String requestJson;

        private DataRequirementGenerationService.GenerateRequest generateRequest;

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public String getJobType() {
            return jobType;
        }

        public void setJobType(String jobType) {
            this.jobType = jobType;
        }

        public String getTriggerType() {
            return triggerType;
        }

        public void setTriggerType(String triggerType) {
            this.triggerType = triggerType;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public String getRequestJson() {
            return requestJson;
        }

        public void setRequestJson(String requestJson) {
            this.requestJson = requestJson;
        }

        public DataRequirementGenerationService.GenerateRequest getGenerateRequest() {
            return generateRequest;
        }

        public void setGenerateRequest(DataRequirementGenerationService.GenerateRequest generateRequest) {
            this.generateRequest = generateRequest;
        }
    }

    /**
     * 失败任务重试请求。
     *
     * 业务功能：承载租户、任务和操作人信息，保证人工重试具备租户隔离和审计来源。
     */
    class RetryFailedJobRequest {

        private String tenantId;

        private String jobId;

        private String updateBy;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public String getUpdateBy() {
            return updateBy;
        }

        public void setUpdateBy(String updateBy) {
            this.updateBy = updateBy;
        }
    }
}
