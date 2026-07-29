package com.sxpt.module.connector.service;

/**
 * 业务模块办理链快照生成服务。
 *
 * 业务功能：
 * 1. 在原平台未回传完整真实办理链时，根据教学平台维护的标准步骤和步骤参与方生成实例链路快照。
 * 2. 同时解析出数据实例默认进入的当前步骤、当前参与方、原平台单位和原平台角色。
 * 3. 为数据准备结果回填、学生数据分配和 launchToken 身份定位提供统一快照生成入口。
 *
 * 关键流程：
 * 1. 数据准备任务拿到原平台造数结果后，优先使用原平台返回的真实链路。
 * 2. 如果没有真实链路，则调用本服务读取启用的标准步骤和启用参与方。
 * 3. 本服务按当前步骤、当前参与方优先级生成快照和默认进入身份。
 */
public interface BusinessModuleProcessSnapshotService {

    /**
     * 根据业务模块标准链生成数据实例办理链快照。
     *
     * @param tenantId 租户 ID。
     * @param businessModuleId 业务模块 ID。
     * @param preferredStepCode 原平台返回的当前步骤编码，可为空。
     * @param preferredActorNo 原平台返回的当前参与方序号，可为空。
     * @return 办理链快照生成结果。
     */
    SnapshotResult generateStandardSnapshot(String tenantId,
                                            String businessModuleId,
                                            String preferredStepCode,
                                            Integer preferredActorNo);

    /**
     * 办理链快照生成结果。
     *
     * 业务功能：
     * 1. 保存可直接写入 process_chain_snapshot_json 的 JSON 字符串。
     * 2. 保存学生默认进入原平台时需要使用的步骤、参与方、单位和角色。
     *
     * 关键流程：
     * 1. 数据准备明细回填时写入 snapshotJson、currentStepCode、currentActorNo。
     * 2. 学生分配时从这些字段和快照中确定 launchToken 的原平台身份。
     */
    class SnapshotResult {

        private String snapshotJson;

        private String currentStepCode;

        private String currentStepName;

        private Integer currentActorNo;

        private String currentActorRelation;

        private String currentOrgId;

        private String currentOrgName;

        private String currentRoleId;

        private String currentRoleName;

        public String getSnapshotJson() {
            return snapshotJson;
        }

        public void setSnapshotJson(String snapshotJson) {
            this.snapshotJson = snapshotJson;
        }

        public String getCurrentStepCode() {
            return currentStepCode;
        }

        public void setCurrentStepCode(String currentStepCode) {
            this.currentStepCode = currentStepCode;
        }

        public String getCurrentStepName() {
            return currentStepName;
        }

        public void setCurrentStepName(String currentStepName) {
            this.currentStepName = currentStepName;
        }

        public Integer getCurrentActorNo() {
            return currentActorNo;
        }

        public void setCurrentActorNo(Integer currentActorNo) {
            this.currentActorNo = currentActorNo;
        }

        public String getCurrentActorRelation() {
            return currentActorRelation;
        }

        public void setCurrentActorRelation(String currentActorRelation) {
            this.currentActorRelation = currentActorRelation;
        }

        public String getCurrentOrgId() {
            return currentOrgId;
        }

        public void setCurrentOrgId(String currentOrgId) {
            this.currentOrgId = currentOrgId;
        }

        public String getCurrentOrgName() {
            return currentOrgName;
        }

        public void setCurrentOrgName(String currentOrgName) {
            this.currentOrgName = currentOrgName;
        }

        public String getCurrentRoleId() {
            return currentRoleId;
        }

        public void setCurrentRoleId(String currentRoleId) {
            this.currentRoleId = currentRoleId;
        }

        public String getCurrentRoleName() {
            return currentRoleName;
        }

        public void setCurrentRoleName(String currentRoleName) {
            this.currentRoleName = currentRoleName;
        }
    }
}
