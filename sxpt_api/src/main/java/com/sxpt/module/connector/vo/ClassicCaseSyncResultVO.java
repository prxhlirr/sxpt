package com.sxpt.module.connector.vo;

/** OA 推送/停用经典案例后的稳定同步结果。 */
public class ClassicCaseSyncResultVO {

    private String classicCaseId;
    private String caseCode;
    private String caseVersionId;
    private String syncStatus;

    public String getClassicCaseId() {
        return classicCaseId;
    }

    public void setClassicCaseId(String classicCaseId) {
        this.classicCaseId = classicCaseId;
    }

    public String getCaseCode() {
        return caseCode;
    }

    public void setCaseCode(String caseCode) {
        this.caseCode = caseCode;
    }

    public String getCaseVersionId() {
        return caseVersionId;
    }

    public void setCaseVersionId(String caseVersionId) {
        this.caseVersionId = caseVersionId;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }
}
