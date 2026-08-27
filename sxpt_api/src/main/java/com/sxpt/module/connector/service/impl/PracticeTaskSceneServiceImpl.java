package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.PracticeTaskScene;
import com.sxpt.module.connector.entity.TeacherRecordArtifact;
import com.sxpt.module.connector.mapper.PracticeTaskSceneMapper;
import com.sxpt.module.connector.mapper.TeacherRecordArtifactMapper;
import com.sxpt.module.connector.service.PracticeTaskSceneService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 练习任务业务场景维护服务实现。
 *
 * 业务功能：
 * 1. 承接老师发布练习任务时选择的备案产物，生成 practice_task_scene 清单。
 * 2. 用全量替换方式保证学生侧看到的任务范围和老师最后一次发布保持一致。
 *
 * 关键流程：
 * 1. 校验任务、操作人和每一条业务场景的来源样本字段。
 * 2. 软删除该任务旧清单，保留审计痕迹。
 * 3. 插入新清单，并补齐租户、任务、排序、状态和审计字段。
 */
@Service
@Profile("!test")
public class PracticeTaskSceneServiceImpl implements PracticeTaskSceneService {

    private static final String DEFAULT_STATUS = RecordStatus.ACTIVE.getValue();

    private final PracticeTaskSceneMapper practiceTaskSceneMapper;

    private final TeacherRecordArtifactMapper teacherRecordArtifactMapper;

    public PracticeTaskSceneServiceImpl(PracticeTaskSceneMapper practiceTaskSceneMapper,
                                        TeacherRecordArtifactMapper teacherRecordArtifactMapper) {
        this.practiceTaskSceneMapper = practiceTaskSceneMapper;
        this.teacherRecordArtifactMapper = teacherRecordArtifactMapper;
    }

    /**
     * 全量替换练习任务业务场景清单。
     *
     * @param tenantId 租户 ID。
     * @param taskId 练习任务 ID。
     * @param operatorId 操作人 ID。
     * @param scenes 本次发布后的业务场景清单。
     * @return 已保存的业务场景清单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PracticeTaskScene> replacePracticeTaskScenes(String tenantId,
                                                             String taskId,
                                                             String operatorId,
                                                             List<PracticeTaskScene> scenes) {
        validateReplaceFields(tenantId, taskId, operatorId, scenes);
        softDeleteExistingScenes(tenantId, taskId, operatorId);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < scenes.size(); i++) {
            PracticeTaskScene scene = scenes.get(i);
            fillCreateFields(scene, tenantId, taskId, operatorId, now, i + 1);
            practiceTaskSceneMapper.insert(scene);
        }
        return scenes;
    }

    /**
     * 根据老师备案产物 ID 全量替换练习任务业务场景清单。
     *
     * @param tenantId 租户 ID。
     * @param taskId 练习任务 ID。
     * @param operatorId 操作人 ID。
     * @param recordArtifactIds 老师备案产物 ID 列表。
     * @return 已保存的业务场景清单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PracticeTaskScene> replacePracticeTaskScenesByArtifacts(String tenantId,
                                                                        String taskId,
                                                                        String operatorId,
                                                                        List<String> recordArtifactIds) {
        validateArtifactReplaceFields(tenantId, taskId, operatorId, recordArtifactIds);
        List<PracticeTaskScene> scenes = new ArrayList<>();
        for (String recordArtifactId : recordArtifactIds) {
            TeacherRecordArtifact artifact = loadReadyArtifact(tenantId, recordArtifactId);
            scenes.add(toPracticeTaskScene(artifact));
        }
        return replacePracticeTaskScenes(tenantId, taskId, operatorId, scenes);
    }

    /**
     * 校验替换清单的最小业务字段。
     *
     * @param tenantId 租户 ID。
     * @param taskId 练习任务 ID。
     * @param operatorId 操作人 ID。
     * @param scenes 本次发布后的业务场景清单。
     */
    private void validateReplaceFields(String tenantId,
                                       String taskId,
                                       String operatorId,
                                       List<PracticeTaskScene> scenes) {
        requireText(tenantId);
        requireText(taskId);
        requireText(operatorId);
        if (CollectionUtils.isEmpty(scenes)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        for (PracticeTaskScene scene : scenes) {
            validateScene(scene);
        }
    }

    /**
     * 校验单条业务场景的来源样本字段。
     *
     * @param scene 练习任务业务场景。
     */
    private void validateScene(PracticeTaskScene scene) {
        if (scene == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(scene.getConnectorSystemId());
        requireText(scene.getRecordArtifactId());
        requireText(scene.getBusinessSceneCode());
        requireText(scene.getSourceDataSessionId());
        requireText(scene.getSourceExternalBusinessId());
    }

    /**
     * 校验按备案产物替换清单的最小字段。
     *
     * @param tenantId 租户 ID。
     * @param taskId 练习任务 ID。
     * @param operatorId 操作人 ID。
     * @param recordArtifactIds 老师备案产物 ID 列表。
     */
    private void validateArtifactReplaceFields(String tenantId,
                                               String taskId,
                                               String operatorId,
                                               List<String> recordArtifactIds) {
        requireText(tenantId);
        requireText(taskId);
        requireText(operatorId);
        if (CollectionUtils.isEmpty(recordArtifactIds)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        for (String recordArtifactId : recordArtifactIds) {
            requireText(recordArtifactId);
        }
    }

    /**
     * 加载可发布的老师备案产物。
     *
     * @param tenantId 租户 ID。
     * @param recordArtifactId 老师备案产物 ID。
     * @return 老师备案产物。
     */
    private TeacherRecordArtifact loadReadyArtifact(String tenantId, String recordArtifactId) {
        TeacherRecordArtifact artifact = teacherRecordArtifactMapper.selectOne(new QueryWrapper<TeacherRecordArtifact>()
                .eq("tenant_id", tenantId)
                .eq("id", recordArtifactId)
                .eq("artifact_status", "READY")
                .eq("deleted", Boolean.FALSE));
        if (artifact == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return artifact;
    }

    /**
     * 将老师备案产物转换为练习任务业务场景。
     *
     * @param artifact 老师备案产物。
     * @return 练习任务业务场景。
     */
    private PracticeTaskScene toPracticeTaskScene(TeacherRecordArtifact artifact) {
        PracticeTaskScene scene = new PracticeTaskScene();
        scene.setConnectorSystemId(artifact.getConnectorSystemId());
        scene.setRecordArtifactId(artifact.getId());
        scene.setBusinessSceneCode(artifact.getBusinessSceneCode());
        scene.setBusinessSceneName(artifact.getBusinessSceneName());
        scene.setSourceDataSessionId(artifact.getSourceDataSessionId());
        scene.setSourceExternalBusinessId(artifact.getSourceExternalBusinessId());
        scene.setScoreRuleSnapshotJson(artifact.getScoreRuleSnapshotJson());
        scene.setRequiredFlag(Boolean.TRUE);
        return scene;
    }

    /**
     * 软删除任务旧清单。
     *
     * @param tenantId 租户 ID。
     * @param taskId 练习任务 ID。
     * @param operatorId 操作人 ID。
     */
    private void softDeleteExistingScenes(String tenantId, String taskId, String operatorId) {
        PracticeTaskScene deletedScene = new PracticeTaskScene();
        deletedScene.setDeleted(Boolean.TRUE);
        deletedScene.setUpdateBy(operatorId);
        deletedScene.setUpdateTime(LocalDateTime.now());
        practiceTaskSceneMapper.update(deletedScene, new UpdateWrapper<PracticeTaskScene>()
                .eq("tenant_id", tenantId)
                .eq("task_id", taskId)
                .eq("deleted", Boolean.FALSE));
    }

    /**
     * 补齐创建清单时的默认字段。
     *
     * @param scene 练习任务业务场景。
     * @param tenantId 租户 ID。
     * @param taskId 练习任务 ID。
     * @param operatorId 操作人 ID。
     * @param now 当前时间。
     * @param defaultSortNo 默认排序号。
     */
    private void fillCreateFields(PracticeTaskScene scene,
                                  String tenantId,
                                  String taskId,
                                  String operatorId,
                                  LocalDateTime now,
                                  int defaultSortNo) {
        scene.setId(generateId());
        scene.setTenantId(tenantId);
        scene.setTaskId(taskId);
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
