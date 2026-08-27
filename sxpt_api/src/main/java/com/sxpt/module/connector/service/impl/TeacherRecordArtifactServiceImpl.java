package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.OriginDataSession;
import com.sxpt.module.connector.entity.TeacherRecordArtifact;
import com.sxpt.module.connector.mapper.OriginDataSessionMapper;
import com.sxpt.module.connector.mapper.TeacherRecordArtifactMapper;
import com.sxpt.module.connector.service.TeacherRecordArtifactService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 老师备案产物服务实现。
 *
 * 业务功能：
 * 1. 将老师备案 DataSession 转换为 teacher_record_artifact。
 * 2. 固化业务场景、来源原平台业务数据和评分依据，支撑学生练习复制数据。
 *
 * 关键流程：
 * 1. 校验 dataSessionId 对应的数据存在且属于老师备案场景。
 * 2. 按 sourceDataSessionId 幂等复用已有产物。
 * 3. 首次沉淀时复制 DataSession 中的业务语义和快照字段，生成 READY 产物。
 */
@Service
@Profile("!test")
public class TeacherRecordArtifactServiceImpl implements TeacherRecordArtifactService {

    private static final String ACTOR_TYPE_TEACHER = "TEACHER";

    private static final String SCENE_TYPE_RECORD = "RECORD";

    private static final String ARTIFACT_STATUS_READY = "READY";

    private static final String DEFAULT_STATUS = RecordStatus.ACTIVE.getValue();

    private final TeacherRecordArtifactMapper teacherRecordArtifactMapper;

    private final OriginDataSessionMapper originDataSessionMapper;

    public TeacherRecordArtifactServiceImpl(TeacherRecordArtifactMapper teacherRecordArtifactMapper,
                                            OriginDataSessionMapper originDataSessionMapper) {
        this.teacherRecordArtifactMapper = teacherRecordArtifactMapper;
        this.originDataSessionMapper = originDataSessionMapper;
    }

    /**
     * 根据老师备案 DataSession 创建备案产物。
     *
     * @param tenantId 租户 ID。
     * @param dataSessionId 老师备案 DataSession ID。
     * @param operatorId 操作人 ID。
     * @return 已创建或已存在的老师备案产物。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeacherRecordArtifact createFromDataSession(String tenantId, String dataSessionId, String operatorId) {
        requireText(tenantId);
        requireText(dataSessionId);
        requireText(operatorId);
        TeacherRecordArtifact existing = findExistingArtifact(tenantId, dataSessionId);
        if (existing != null) {
            return existing;
        }
        OriginDataSession dataSession = loadRecordDataSession(tenantId, dataSessionId);
        TeacherRecordArtifact artifact = buildArtifact(dataSession, operatorId);
        teacherRecordArtifactMapper.insert(artifact);
        return artifact;
    }

    /**
     * 查询是否已经由同一个 DataSession 沉淀过备案产物。
     *
     * @param tenantId 租户 ID。
     * @param dataSessionId 老师备案 DataSession ID。
     * @return 已存在产物，不存在时返回 null。
     */
    private TeacherRecordArtifact findExistingArtifact(String tenantId, String dataSessionId) {
        return teacherRecordArtifactMapper.selectOne(new QueryWrapper<TeacherRecordArtifact>()
                .eq("tenant_id", tenantId)
                .eq("source_data_session_id", dataSessionId)
                .eq("deleted", Boolean.FALSE));
    }

    /**
     * 加载老师备案 DataSession 并校验场景边界。
     *
     * @param tenantId 租户 ID。
     * @param dataSessionId 老师备案 DataSession ID。
     * @return 老师备案 DataSession。
     */
    private OriginDataSession loadRecordDataSession(String tenantId, String dataSessionId) {
        OriginDataSession dataSession = originDataSessionMapper.selectOne(new QueryWrapper<OriginDataSession>()
                .eq("tenant_id", tenantId)
                .eq("id", dataSessionId)
                .eq("deleted", Boolean.FALSE));
        if (dataSession == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        if (!ACTOR_TYPE_TEACHER.equals(dataSession.getActorType())
                || !SCENE_TYPE_RECORD.equals(dataSession.getSceneType())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        requireText(dataSession.getBusinessSceneCode());
        requireText(dataSession.getExternalBusinessId());
        return dataSession;
    }

    /**
     * 根据 DataSession 构造备案产物。
     *
     * @param dataSession 老师备案 DataSession。
     * @param operatorId 操作人 ID。
     * @return 待保存备案产物。
     */
    private TeacherRecordArtifact buildArtifact(OriginDataSession dataSession, String operatorId) {
        LocalDateTime now = LocalDateTime.now();
        TeacherRecordArtifact artifact = new TeacherRecordArtifact();
        artifact.setId(generateId());
        artifact.setTenantId(dataSession.getTenantId());
        artifact.setTeacherUserId(dataSession.getUserId());
        artifact.setConnectorSystemId(dataSession.getConnectorSystemId());
        artifact.setCaptureSessionId(dataSession.getCaptureSessionId());
        artifact.setSourceDataSessionId(dataSession.getId());
        artifact.setSourceDataInstanceId(dataSession.getDataInstanceId());
        artifact.setBusinessSceneCode(dataSession.getBusinessSceneCode());
        artifact.setBusinessSceneName(dataSession.getBusinessSceneName());
        artifact.setSourceExternalBusinessId(dataSession.getExternalBusinessId());
        artifact.setSourceExternalBusinessNo(dataSession.getExternalBusinessNo());
        artifact.setTargetStatus(dataSession.getExternalStatus());
        artifact.setScoreRuleSnapshotJson(dataSession.getDataSpecSnapshotJson());
        artifact.setOriginContextSnapshotJson(dataSession.getOriginPayloadSnapshotJson());
        artifact.setArtifactStatus(ARTIFACT_STATUS_READY);
        artifact.setCreateBy(operatorId);
        artifact.setCreateTime(now);
        artifact.setUpdateBy(operatorId);
        artifact.setUpdateTime(now);
        artifact.setStatus(DEFAULT_STATUS);
        artifact.setDeleted(Boolean.FALSE);
        return artifact;
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
