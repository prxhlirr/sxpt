package com.sxpt.module.course.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 创建任务教学点关联请求。
 *
 * 业务功能：
 * 1. 承载教师将教学点加入学习、练习或考试任务时提交的参数。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发基础参数校验。
 * 2. Controller 将请求转换为 TaskTeachingPoint 实体后交给 Service 写入。
 */
public class CreateTaskTeachingPointRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "任务 ID 不能为空")
    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @NotBlank(message = "教学点 ID 不能为空")
    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    private Boolean requiredFlag;

    @NotNull(message = "排序号不能为空")
    private Long sequenceNo;

    @Size(max = 64, message = "创建人 ID 长度不能超过 64")
    private String createBy;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTeachingPointId() {
        return teachingPointId;
    }

    public void setTeachingPointId(String teachingPointId) {
        this.teachingPointId = teachingPointId;
    }

    public Boolean getRequiredFlag() {
        return requiredFlag;
    }

    public void setRequiredFlag(Boolean requiredFlag) {
        this.requiredFlag = requiredFlag;
    }

    public Long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
}
