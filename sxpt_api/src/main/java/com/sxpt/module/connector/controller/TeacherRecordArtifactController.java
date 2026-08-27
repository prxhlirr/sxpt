package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.CreateTeacherRecordArtifactRequest;
import com.sxpt.module.connector.entity.TeacherRecordArtifact;
import com.sxpt.module.connector.service.TeacherRecordArtifactService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 老师备案产物接口。
 *
 * 业务功能：
 * 1. 提供老师完成原平台备案后沉淀教学资产的入口。
 * 2. 后续练习任务发布只引用备案产物，不维护原平台模块路径。
 *
 * 关键流程：
 * 1. 接收老师备案 DataSession ID。
 * 2. 调用服务校验该 DataSession 属于老师 RECORD 场景。
 * 3. 返回已创建或已存在的备案产物。
 */
@RestController
@RequestMapping("/api/v1/connector/teacher-record-artifacts")
@ConditionalOnProperty(name = "sxpt.connector.teacher-record-artifact-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class TeacherRecordArtifactController {

    private final TeacherRecordArtifactService teacherRecordArtifactService;

    public TeacherRecordArtifactController(TeacherRecordArtifactService teacherRecordArtifactService) {
        this.teacherRecordArtifactService = teacherRecordArtifactService;
    }

    /**
     * 根据老师备案 DataSession 创建备案产物。
     *
     * @param request 创建老师备案产物请求。
     * @return 已创建或已存在的老师备案产物。
     */
    @PostMapping("/create-from-data-session")
    public ApiResult<TeacherRecordArtifact> createFromDataSession(
            @Valid @RequestBody CreateTeacherRecordArtifactRequest request) {
        return ApiResult.success(teacherRecordArtifactService.createFromDataSession(
                request.getTenantId(), request.getDataSessionId(), request.getOperatorId()));
    }
}
