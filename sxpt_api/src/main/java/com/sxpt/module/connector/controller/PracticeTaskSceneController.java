package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.ReplacePracticeTaskScenesByArtifactsRequest;
import com.sxpt.module.connector.entity.PracticeTaskScene;
import com.sxpt.module.connector.service.PracticeTaskSceneService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 练习任务业务场景接口。
 *
 * 业务功能：
 * 1. 提供老师发布练习任务时按备案产物生成业务场景清单的入口。
 * 2. 保证学生进入原平台后要练习的数据来自老师备案产物，而不是教学平台维护的页面路径。
 *
 * 关键流程：
 * 1. 接收 taskId 和 recordArtifactIds。
 * 2. 调用服务全量替换 practice_task_scene。
 * 3. 学生 verify launchToken 时即可按 taskId 获取本清单。
 */
@RestController
@RequestMapping("/api/v1/connector/practice-task-scenes")
@ConditionalOnProperty(name = "sxpt.connector.practice-task-scene-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class PracticeTaskSceneController {

    private final PracticeTaskSceneService practiceTaskSceneService;

    public PracticeTaskSceneController(PracticeTaskSceneService practiceTaskSceneService) {
        this.practiceTaskSceneService = practiceTaskSceneService;
    }

    /**
     * 按备案产物全量替换练习任务业务场景。
     *
     * @param request 按备案产物替换练习任务业务场景请求。
     * @return 已保存的练习任务业务场景清单。
     */
    @PostMapping("/replace-by-artifacts")
    public ApiResult<List<PracticeTaskScene>> replaceByArtifacts(
            @Valid @RequestBody ReplacePracticeTaskScenesByArtifactsRequest request) {
        return ApiResult.success(practiceTaskSceneService.replacePracticeTaskScenesByArtifacts(
                request.getTenantId(), request.getTaskId(), request.getOperatorId(), request.getRecordArtifactIds()));
    }
}
