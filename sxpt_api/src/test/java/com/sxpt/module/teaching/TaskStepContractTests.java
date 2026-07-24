package com.sxpt.module.teaching;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teaching.dto.CreateTaskStepRequest;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.mapper.TaskStepMapper;
import com.sxpt.module.teaching.vo.TaskStepVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 教学任务步骤模块契约测试。
 *
 * 业务功能：
 * 1. 验证创建教学步骤请求 DTO 的必填和长度约束。
 * 2. 验证 TaskStep 实体、Mapper 和 VO 的对外契约稳定。
 * 3. 验证来源动作草稿到教学步骤的数据库幂等约束存在。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认实体表名、Mapper 泛型和 VO 字段边界。
 * 3. 读取 schema 文件检查唯一索引声明，避免重复发布保护被迁移脚本遗漏。
 */
class TaskStepContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效创建教学步骤请求可以通过参数校验。
     */
    @Test
    void validCreateTaskStepRequestShouldPassValidation() {
        CreateTaskStepRequest request = buildValidRequest();

        Set<ConstraintViolation<CreateTaskStepRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少步骤编码时参数校验失败。
     */
    @Test
    void createTaskStepRequestShouldRejectMissingStepCode() {
        CreateTaskStepRequest request = buildValidRequest();
        request.setStepCode(" ");

        Set<ConstraintViolation<CreateTaskStepRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验关联资源 JSON 超长时参数校验失败。
     */
    @Test
    void createTaskStepRequestShouldRejectTooLongRelatedResourceIds() {
        CreateTaskStepRequest request = buildValidRequest();
        request.setRelatedResourceIds(repeat("a", 32769));

        Set<ConstraintViolation<CreateTaskStepRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验教学步骤 VO 不暴露内部软删除字段。
     */
    @Test
    void taskStepVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> TaskStepVO.class.getDeclaredField("deleted"));
    }

    /**
     * 校验实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void taskStepEntityShouldMapTaskStepTable() throws NoSuchFieldException {
        TableName tableName = TaskStep.class.getAnnotation(TableName.class);
        Field idField = TaskStep.class.getDeclaredField("id");

        assertEquals("task_step", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 TaskStep。
     */
    @Test
    void taskStepMapperShouldBindTaskStepEntity() {
        Type[] interfaces = TaskStepMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(TaskStep.class, baseMapperType.getActualTypeArguments()[0]);
    }

    /**
     * 校验任务步骤表具备按来源动作草稿防重复发布的唯一索引。
     *
     * @throws IOException 当 schema 文件不可读时，测试应失败以暴露工程配置问题。
     */
    @Test
    void taskStepSchemaShouldPreventDuplicatePublishForSameDraft() throws IOException {
        String schema = new String(Files.readAllBytes(Paths.get("src/main/resources/db/schema/V1__mvp_schema.sql")),
                StandardCharsets.UTF_8);

        assertTrue(schema.contains("CREATE UNIQUE INDEX uk_task_step_source_action_draft"));
        assertTrue(schema.contains("ON task_step (tenant_id, source_action_draft_id)"));
        assertTrue(schema.contains("WHERE deleted = false AND source_action_draft_id IS NOT NULL"));
    }

    /**
     * 构造最小有效创建教学步骤请求。
     *
     * @return 创建教学任务步骤请求。
     */
    private CreateTaskStepRequest buildValidRequest() {
        CreateTaskStepRequest request = new CreateTaskStepRequest();
        request.setTenantId("tenant_001");
        request.setTaskId("task_001");
        request.setTeachingPointId("tp_001");
        request.setStepCode("STEP_001");
        request.setStepName("填写申请信息");
        request.setStepDescription("完成备案申请表单");
        request.setSequenceNo(1L);
        request.setSegmentNo(1L);
        request.setActorType("APPLICANT");
        request.setRequiredExternalOrgId("org_001");
        request.setRequiredExternalOrgName("申报单位");
        request.setRequiredExternalRoleId("role_001");
        request.setRequiredExternalRoleName("经办人");
        request.setSwitchStrategy("MANUAL_CONFIRM");
        request.setNextSegmentNo(2L);
        request.setSwitchConfirmRequired(Boolean.TRUE);
        request.setSwitchDecisionSource("TEACHER_PATH");
        request.setSwitchReason("申请提交后进入受理角色");
        request.setRollbackPolicy("UI_ONLY");
        request.setRelatedResourceIds("[\"res_001\"]");
        request.setGuideContent("请填写申请信息");
        request.setPracticeHint("注意必填项");
        request.setRequired(Boolean.TRUE);
        request.setAllowSkip(Boolean.FALSE);
        request.setSourceActionDraftId("draft_001");
        request.setCreateBy("teacher_001");
        return request;
    }

    /**
     * 构造指定长度字符串。
     *
     * @param value 重复字符。
     * @param count 重复次数。
     * @return 指定长度字符串。
     */
    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
