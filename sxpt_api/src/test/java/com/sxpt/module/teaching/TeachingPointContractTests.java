package com.sxpt.module.teaching;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teaching.dto.CreateTeachingPointRequest;
import com.sxpt.module.teaching.entity.TeachingPoint;
import com.sxpt.module.teaching.mapper.TeachingPointMapper;
import com.sxpt.module.teaching.vo.TeachingPointVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 教学点模块契约测试。
 *
 * 业务功能：
 * 1. 验证创建教学点请求 DTO 的必填和长度约束。
 * 2. 验证 TeachingPoint 实体、Mapper 和 VO 的对外契约稳定。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认实体表名、Mapper 泛型和 VO 字段边界。
 */
class TeachingPointContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效创建教学点请求可以通过参数校验。
     */
    @Test
    void validCreateTeachingPointRequestShouldPassValidation() {
        CreateTeachingPointRequest request = buildValidRequest();

        Set<ConstraintViolation<CreateTeachingPointRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少教学点编码时参数校验失败。
     */
    @Test
    void createTeachingPointRequestShouldRejectMissingPointCode() {
        CreateTeachingPointRequest request = buildValidRequest();
        request.setPointCode(" ");

        Set<ConstraintViolation<CreateTeachingPointRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验备案路径 JSON 超长时参数校验失败。
     */
    @Test
    void createTeachingPointRequestShouldRejectTooLongRecordPathJson() {
        CreateTeachingPointRequest request = buildValidRequest();
        request.setRecordPathJson(repeat("a", 32769));

        Set<ConstraintViolation<CreateTeachingPointRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验教学点 VO 不暴露内部软删除字段。
     */
    @Test
    void teachingPointVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> TeachingPointVO.class.getDeclaredField("deleted"));
    }

    /**
     * 校验实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void teachingPointEntityShouldMapTeachingPointTable() throws NoSuchFieldException {
        TableName tableName = TeachingPoint.class.getAnnotation(TableName.class);
        Field idField = TeachingPoint.class.getDeclaredField("id");

        assertEquals("teaching_point", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 TeachingPoint。
     */
    @Test
    void teachingPointMapperShouldBindTeachingPointEntity() {
        Type[] interfaces = TeachingPointMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(TeachingPoint.class, baseMapperType.getActualTypeArguments()[0]);
    }

    /**
     * 构造最小有效创建教学点请求。
     *
     * @return 创建教学点请求。
     */
    private CreateTeachingPointRequest buildValidRequest() {
        CreateTeachingPointRequest request = new CreateTeachingPointRequest();
        request.setTenantId("tenant_001");
        request.setConnectorSystemId("connector_001");
        request.setPointCode("TP_RECORD");
        request.setPointName("标准备案申请");
        request.setPointType("SCENARIO");
        request.setSourceCaptureSessionId("cap_001");
        request.setBusinessOverviewJson("{\"goal\":\"掌握备案\"}");
        request.setFlowFileId("file_001");
        request.setFlowFileUrl("https://files.example.com/flow.png");
        request.setRecordPathJson("[{\"segmentNo\":1}]");
        request.setRequiredExternalRoleId("role_001");
        request.setRequiredExternalRoleName("经办人");
        request.setExecutionStrategy("ROLE_SWITCH");
        request.setDataScopeJson("{\"scope\":\"training\"}");
        request.setOverlayPolicyJson("{\"mask\":true}");
        request.setDescription("备案教学点");
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
