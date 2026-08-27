package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.ExamQuestionScene;
import com.sxpt.module.connector.mapper.ExamQuestionSceneMapper;
import com.sxpt.module.connector.service.ExamQuestionSceneService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 考试题目业务场景维护服务实现。
 *
 * 业务功能：
 * 1. 保存考试题目与原平台业务场景编码、造数规格之间的绑定关系。
 * 2. 支撑考试中“一道题对应一个原平台数据”的注册和评分追溯。
 *
 * 关键流程：
 * 1. 校验考试尝试、操作人和每道题的 questionAttemptId、businessSceneCode。
 * 2. 软删除同一次考试尝试的旧题目场景，避免重新组卷后的旧题混入。
 * 3. 插入新题目场景，补齐排序、必做标记、状态和审计字段。
 */
@Service
@Profile("!test")
public class ExamQuestionSceneServiceImpl implements ExamQuestionSceneService {

    private static final String DEFAULT_STATUS = RecordStatus.ACTIVE.getValue();

    private final ExamQuestionSceneMapper examQuestionSceneMapper;

    public ExamQuestionSceneServiceImpl(ExamQuestionSceneMapper examQuestionSceneMapper) {
        this.examQuestionSceneMapper = examQuestionSceneMapper;
    }

    /**
     * 全量替换考试题目业务场景清单。
     *
     * @param tenantId 租户 ID。
     * @param taskId 考试任务 ID。
     * @param executionId 任务执行 ID。
     * @param examAttemptId 考试尝试 ID。
     * @param operatorId 操作人 ID。
     * @param scenes 本次考试题目业务场景清单。
     * @return 已保存的考试题目业务场景清单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ExamQuestionScene> replaceExamQuestionScenes(String tenantId,
                                                             String taskId,
                                                             String executionId,
                                                             String examAttemptId,
                                                             String operatorId,
                                                             List<ExamQuestionScene> scenes) {
        validateReplaceFields(tenantId, taskId, examAttemptId, operatorId, scenes);
        softDeleteExistingScenes(tenantId, taskId, examAttemptId, operatorId);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < scenes.size(); i++) {
            ExamQuestionScene scene = scenes.get(i);
            fillCreateFields(scene, tenantId, taskId, executionId, examAttemptId, operatorId, now, i + 1);
            examQuestionSceneMapper.insert(scene);
        }
        return scenes;
    }

    /**
     * 校验替换清单的最小业务字段。
     *
     * @param tenantId 租户 ID。
     * @param taskId 考试任务 ID。
     * @param examAttemptId 考试尝试 ID。
     * @param operatorId 操作人 ID。
     * @param scenes 本次考试题目业务场景清单。
     */
    private void validateReplaceFields(String tenantId,
                                       String taskId,
                                       String examAttemptId,
                                       String operatorId,
                                       List<ExamQuestionScene> scenes) {
        requireText(tenantId);
        requireText(taskId);
        requireText(examAttemptId);
        requireText(operatorId);
        if (CollectionUtils.isEmpty(scenes)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        for (ExamQuestionScene scene : scenes) {
            validateScene(scene);
        }
    }

    /**
     * 校验单道题的业务场景字段。
     *
     * @param scene 考试题目业务场景。
     */
    private void validateScene(ExamQuestionScene scene) {
        if (scene == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(scene.getQuestionAttemptId());
        requireText(scene.getConnectorSystemId());
        requireText(scene.getBusinessSceneCode());
    }

    /**
     * 软删除同一次考试尝试下的旧题目业务场景。
     *
     * @param tenantId 租户 ID。
     * @param taskId 考试任务 ID。
     * @param examAttemptId 考试尝试 ID。
     * @param operatorId 操作人 ID。
     */
    private void softDeleteExistingScenes(String tenantId,
                                          String taskId,
                                          String examAttemptId,
                                          String operatorId) {
        ExamQuestionScene deletedScene = new ExamQuestionScene();
        deletedScene.setDeleted(Boolean.TRUE);
        deletedScene.setUpdateBy(operatorId);
        deletedScene.setUpdateTime(LocalDateTime.now());
        examQuestionSceneMapper.update(deletedScene, new UpdateWrapper<ExamQuestionScene>()
                .eq("tenant_id", tenantId)
                .eq("task_id", taskId)
                .eq("exam_attempt_id", examAttemptId)
                .eq("deleted", Boolean.FALSE));
    }

    /**
     * 补齐创建清单时的默认字段。
     *
     * @param scene 考试题目业务场景。
     * @param tenantId 租户 ID。
     * @param taskId 考试任务 ID。
     * @param executionId 任务执行 ID。
     * @param examAttemptId 考试尝试 ID。
     * @param operatorId 操作人 ID。
     * @param now 当前时间。
     * @param defaultSortNo 默认排序号。
     */
    private void fillCreateFields(ExamQuestionScene scene,
                                  String tenantId,
                                  String taskId,
                                  String executionId,
                                  String examAttemptId,
                                  String operatorId,
                                  LocalDateTime now,
                                  int defaultSortNo) {
        scene.setId(generateId());
        scene.setTenantId(tenantId);
        scene.setTaskId(taskId);
        scene.setExecutionId(executionId);
        scene.setExamAttemptId(examAttemptId);
        if (scene.getSortNo() == null) {
            scene.setSortNo(defaultSortNo);
        }
        if (scene.getRequiredFlag() == null) {
            scene.setRequiredFlag(Boolean.TRUE);
        }
        scene.setCreateBy(operatorId);
        scene.setCreateTime(now);
        scene.setUpdateBy(operatorId);
        scene.setUpdateTime(now);
        if (!StringUtils.hasText(scene.getStatus())) {
            scene.setStatus(DEFAULT_STATUS);
        }
        scene.setDeleted(Boolean.FALSE);
    }

    /**
     * 校验文本字段非空。
     *
     * @param value 待校验字段值。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 生成应用层主键。
     *
     * @return 32 位无横线字符串 ID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
