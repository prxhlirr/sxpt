package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.ExamQuestionSceneItemRequest;
import com.sxpt.module.connector.dto.ReplaceExamQuestionScenesRequest;
import com.sxpt.module.connector.entity.ExamQuestionScene;
import com.sxpt.module.connector.service.ExamQuestionSceneService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 考试题目业务场景接口。
 *
 * 业务功能：
 * 1. 提供考试发布或组卷后固化题目业务场景的入口。
 * 2. 保证学生考试造数可以绑定到具体 questionAttemptId。
 *
 * 关键流程：
 * 1. 接收 examAttemptId 和题目业务场景清单。
 * 2. 转换为 ExamQuestionScene 实体后调用服务全量替换。
 * 3. 原平台 verify launchToken 时按考试尝试读取清单。
 */
@RestController
@RequestMapping("/api/v1/connector/exam-question-scenes")
@ConditionalOnProperty(name = "sxpt.connector.exam-question-scene-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class ExamQuestionSceneController {

    private final ExamQuestionSceneService examQuestionSceneService;

    public ExamQuestionSceneController(ExamQuestionSceneService examQuestionSceneService) {
        this.examQuestionSceneService = examQuestionSceneService;
    }

    /**
     * 全量替换考试题目业务场景清单。
     *
     * @param request 替换考试题目业务场景请求。
     * @return 已保存的考试题目业务场景清单。
     */
    @PostMapping("/replace")
    public ApiResult<List<ExamQuestionScene>> replace(@Valid @RequestBody ReplaceExamQuestionScenesRequest request) {
        return ApiResult.success(examQuestionSceneService.replaceExamQuestionScenes(
                request.getTenantId(),
                request.getTaskId(),
                request.getExecutionId(),
                request.getExamAttemptId(),
                request.getOperatorId(),
                toScenes(request.getScenes())));
    }

    /**
     * 将请求明细转换为考试题目业务场景实体。
     *
     * @param items 请求明细。
     * @return 考试题目业务场景实体列表。
     */
    private List<ExamQuestionScene> toScenes(List<ExamQuestionSceneItemRequest> items) {
        List<ExamQuestionScene> scenes = new ArrayList<>();
        for (ExamQuestionSceneItemRequest item : items) {
            ExamQuestionScene scene = new ExamQuestionScene();
            scene.setQuestionAttemptId(item.getQuestionAttemptId());
            scene.setQuestionNo(item.getQuestionNo());
            scene.setConnectorSystemId(item.getConnectorSystemId());
            scene.setBusinessSceneCode(item.getBusinessSceneCode());
            scene.setBusinessSceneName(item.getBusinessSceneName());
            scene.setDataSpecSnapshotJson(item.getDataSpecSnapshotJson());
            scene.setScoreRuleSnapshotJson(item.getScoreRuleSnapshotJson());
            scene.setSortNo(item.getSortNo());
            scene.setRequiredFlag(item.getRequiredFlag());
            scenes.add(scene);
        }
        return scenes;
    }
}
