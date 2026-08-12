package com.sxpt.module.connector.vo;

import java.util.List;

/**
 * 经典案例批量生成返回对象。
 *
 * 业务功能：
 * 1. 返回一批学生 demo 数据生成后的使用记录集合。
 * 2. 每条 usage 都包含 teachingDataInstanceId，后续可按单个学生进入原平台学习环境。
 */
public class ClassicCaseBatchGenerateVO {

    private String requestBatchId;

    private Integer totalCount;

    private Integer successCount;

    private Integer failedCount;

    private List<ClassicCaseUsageVO> usages;

    public String getRequestBatchId() {
        return requestBatchId;
    }

    public void setRequestBatchId(String requestBatchId) {
        this.requestBatchId = requestBatchId;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public List<ClassicCaseUsageVO> getUsages() {
        return usages;
    }

    public void setUsages(List<ClassicCaseUsageVO> usages) {
        this.usages = usages;
    }
}
