package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.module.connector.entity.ExamQuestionScene;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.entity.PracticeTaskScene;
import com.sxpt.module.connector.mapper.ExamQuestionSceneMapper;
import com.sxpt.module.connector.mapper.PracticeTaskSceneMapper;
import com.sxpt.module.connector.service.OriginBusinessSceneService;
import com.sxpt.module.connector.vo.OriginBusinessSceneVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 原平台业务场景清单服务实现。
 *
 * 业务功能：
 * 1. 在练习模式下根据 taskId 返回老师发布任务时绑定的多个业务场景。
 * 2. 在考试模式下根据 examAttemptId 返回本次考试的题目业务场景。
 * 3. 为原平台首页或遮罩层展示任务清单提供稳定数据源。
 *
 * 关键流程：
 * 1. 判断启动上下文 sceneType。
 * 2. PRACTICE 场景按租户、任务、原平台查询 practice_task_scene。
 * 3. EXAM 场景按租户、任务、考试尝试和原平台查询 exam_question_scene。
 * 4. 转换为不包含原平台路径的 OriginBusinessSceneVO。
 */
@Service
@Profile("!test")
public class OriginBusinessSceneServiceImpl implements OriginBusinessSceneService {

    private static final String SCENE_TYPE_PRACTICE = "PRACTICE";

    private static final String SCENE_TYPE_EXAM = "EXAM";

    private static final String SCENE_STATUS_TODO = "TODO";

    private final PracticeTaskSceneMapper practiceTaskSceneMapper;

    private final ExamQuestionSceneMapper examQuestionSceneMapper;

    public OriginBusinessSceneServiceImpl(PracticeTaskSceneMapper practiceTaskSceneMapper,
                                          ExamQuestionSceneMapper examQuestionSceneMapper) {
        this.practiceTaskSceneMapper = practiceTaskSceneMapper;
        this.examQuestionSceneMapper = examQuestionSceneMapper;
    }

    /**
     * 查询本次启动上下文对应的业务场景清单。
     *
     * @param launchContext 已校验的启动上下文。
     * @return 业务场景清单。
     */
    @Override
    public List<OriginBusinessSceneVO> listScenesForLaunch(PlatformLaunchContext launchContext) {
        if (launchContext == null) {
            return Collections.emptyList();
        }
        if (SCENE_TYPE_PRACTICE.equals(launchContext.getSceneType())) {
            return listPracticeScenes(launchContext);
        }
        if (SCENE_TYPE_EXAM.equals(launchContext.getSceneType())) {
            return listExamScenes(launchContext);
        }
        return Collections.emptyList();
    }

    /**
     * 查询练习业务场景清单。
     *
     * @param launchContext 已校验的启动上下文。
     * @return 练习业务场景清单。
     */
    private List<OriginBusinessSceneVO> listPracticeScenes(PlatformLaunchContext launchContext) {
        if (!StringUtils.hasText(launchContext.getTenantId())
                || !StringUtils.hasText(launchContext.getTaskId())
                || !StringUtils.hasText(launchContext.getConnectorSystemId())) {
            return Collections.emptyList();
        }
        List<PracticeTaskScene> scenes = practiceTaskSceneMapper.selectList(new QueryWrapper<PracticeTaskScene>()
                .eq("tenant_id", launchContext.getTenantId())
                .eq("task_id", launchContext.getTaskId())
                .eq("connector_system_id", launchContext.getConnectorSystemId())
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("sort_no")
                .orderByAsc("create_time"));
        List<OriginBusinessSceneVO> result = new ArrayList<>();
        for (PracticeTaskScene scene : scenes) {
            result.add(toPracticeVO(scene));
        }
        return result;
    }

    /**
     * 查询考试题目业务场景清单。
     *
     * @param launchContext 已校验的启动上下文。
     * @return 考试题目业务场景清单。
     */
    private List<OriginBusinessSceneVO> listExamScenes(PlatformLaunchContext launchContext) {
        if (!StringUtils.hasText(launchContext.getTenantId())
                || !StringUtils.hasText(launchContext.getTaskId())
                || !StringUtils.hasText(launchContext.getExamAttemptId())
                || !StringUtils.hasText(launchContext.getConnectorSystemId())) {
            return Collections.emptyList();
        }
        List<ExamQuestionScene> scenes = examQuestionSceneMapper.selectList(new QueryWrapper<ExamQuestionScene>()
                .eq("tenant_id", launchContext.getTenantId())
                .eq("task_id", launchContext.getTaskId())
                .eq("exam_attempt_id", launchContext.getExamAttemptId())
                .eq("connector_system_id", launchContext.getConnectorSystemId())
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("sort_no")
                .orderByAsc("question_no")
                .orderByAsc("create_time"));
        List<OriginBusinessSceneVO> result = new ArrayList<>();
        for (ExamQuestionScene scene : scenes) {
            result.add(toExamVO(scene));
        }
        return result;
    }

    /**
     * 转换练习任务业务场景返回对象。
     *
     * @param scene 练习任务业务场景。
     * @return 原平台业务场景返回对象。
     */
    private OriginBusinessSceneVO toPracticeVO(PracticeTaskScene scene) {
        OriginBusinessSceneVO vo = new OriginBusinessSceneVO();
        vo.setBusinessSceneCode(scene.getBusinessSceneCode());
        vo.setBusinessSceneName(scene.getBusinessSceneName());
        vo.setSourceDataSessionId(scene.getSourceDataSessionId());
        vo.setSourceExternalBusinessId(scene.getSourceExternalBusinessId());
        vo.setStatus(SCENE_STATUS_TODO);
        vo.setSortNo(scene.getSortNo());
        vo.setRequiredFlag(scene.getRequiredFlag());
        return vo;
    }

    /**
     * 转换考试题目业务场景返回对象。
     *
     * @param scene 考试题目业务场景。
     * @return 原平台业务场景返回对象。
     */
    private OriginBusinessSceneVO toExamVO(ExamQuestionScene scene) {
        OriginBusinessSceneVO vo = new OriginBusinessSceneVO();
        vo.setBusinessSceneCode(scene.getBusinessSceneCode());
        vo.setBusinessSceneName(scene.getBusinessSceneName());
        vo.setDataSpecSnapshotJson(scene.getDataSpecSnapshotJson());
        vo.setQuestionAttemptId(scene.getQuestionAttemptId());
        vo.setQuestionNo(scene.getQuestionNo());
        vo.setStatus(SCENE_STATUS_TODO);
        vo.setSortNo(scene.getSortNo());
        vo.setRequiredFlag(scene.getRequiredFlag());
        return vo;
    }
}
