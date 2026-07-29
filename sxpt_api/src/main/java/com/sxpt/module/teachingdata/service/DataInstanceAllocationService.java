package com.sxpt.module.teachingdata.service;

import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;

import java.util.List;

/**
 * 数据实例分配服务。
 *
 * 业务功能：
 * 1. 创建学生与教学数据实例的分配记录，固化领取时的单位、角色和业务身份。
 * 2. 查询学生在练习或考试场景下已领取的数据，为重置、进入原平台和审计追踪提供入口。
 *
 * 关键流程：
 * 1. 数据池选出可用 TeachingDataInstance 后，调用本服务写入 DataInstanceAllocation。
 * 2. 分配记录保存 ownerUserId、requiredExternalOrgId、requiredExternalRoleId 和 actorType。
 * 3. 后续 launchToken 构造和遮罩 SDK 角色切换基于分配记录读取原平台上下文。
 */
public interface DataInstanceAllocationService {

    /**
     * 创建数据实例分配记录。
     *
     * @param allocation 数据实例分配实体。
     * @return 已保存的数据实例分配记录。
     */
    DataInstanceAllocation createDataInstanceAllocation(DataInstanceAllocation allocation);

    /**
     * 从指定数据池领取一个可用数据实例，并写入分配记录。
     *
     * @param request 数据领取请求。
     * @return 已创建的分配记录。
     */
    DataInstanceAllocation acquireReadyInstance(AcquireReadyInstanceRequest request);

    /**
     * 查询指定数据需求批次下的分配记录。
     *
     * @param tenantId 租户 ID。
     * @param requirementId 数据需求批次 ID。
     * @return 当前批次下所有有效分配记录，按领取时间倒序排列。
     */
    List<DataInstanceAllocation> listByRequirement(String tenantId, String requirementId);

    /**
     * 查询指定学生在任务和场景下的数据分配记录。
     *
     * @param tenantId 租户 ID。
     * @param taskId 教学任务 ID。
     * @param allocationScene 分配场景。
     * @param ownerUserId 领取数据的用户 ID。
     * @return 数据实例分配记录列表。
     */
    List<DataInstanceAllocation> listByOwnerAndScene(String tenantId,
                                                     String taskId,
                                                     String allocationScene,
                                                     String ownerUserId);

    /**
     * 查询指定数据实例的有效分配记录。
     *
     * @param tenantId 租户 ID。
     * @param dataInstanceId 教学数据实例 ID。
     * @return 数据实例分配记录列表。
     */
    List<DataInstanceAllocation> listByDataInstance(String tenantId, String dataInstanceId);

    /**
     * 数据实例领取请求。
     *
     * 业务功能：承载学生或考试题目从数据池领取实例时需要固化的上下文。
     * 关键流程：服务先校验数据池和可用实例，再创建分配记录并推进实例状态。
     */
    class AcquireReadyInstanceRequest {

        private String tenantId;

        private String poolId;

        private String ownerUserId;

        private String taskId;

        private String allocationScene;

        private String attemptId;

        private String questionAttemptId;

        private String createBy;

        private String updateBy;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getPoolId() {
            return poolId;
        }

        public void setPoolId(String poolId) {
            this.poolId = poolId;
        }

        public String getOwnerUserId() {
            return ownerUserId;
        }

        public void setOwnerUserId(String ownerUserId) {
            this.ownerUserId = ownerUserId;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getAllocationScene() {
            return allocationScene;
        }

        public void setAllocationScene(String allocationScene) {
            this.allocationScene = allocationScene;
        }

        public String getAttemptId() {
            return attemptId;
        }

        public void setAttemptId(String attemptId) {
            this.attemptId = attemptId;
        }

        public String getQuestionAttemptId() {
            return questionAttemptId;
        }

        public void setQuestionAttemptId(String questionAttemptId) {
            this.questionAttemptId = questionAttemptId;
        }

        public String getCreateBy() {
            return createBy;
        }

        public void setCreateBy(String createBy) {
            this.createBy = createBy;
        }

        public String getUpdateBy() {
            return updateBy;
        }

        public void setUpdateBy(String updateBy) {
            this.updateBy = updateBy;
        }
    }
}
