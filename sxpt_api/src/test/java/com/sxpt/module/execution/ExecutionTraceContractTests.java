package com.sxpt.module.execution;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.execution.dto.ReportExecutionTraceRequest;
import com.sxpt.module.execution.entity.ExecutionTrace;
import com.sxpt.module.execution.mapper.ExecutionTraceMapper;
import com.sxpt.module.execution.vo.ExecutionTraceVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 学生执行轨迹契约测试。
 *
 * 业务功能：
 * 1. 验证执行轨迹 DTO、VO、实体和 Mapper 的接口契约稳定。
 * 2. 防止大 JSON 证据和软删除字段泄露给 SDK 或教师端。
 * 3. 验证客户端轨迹 ID 的数据库级幂等约束存在，避免并发重复上报。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认实体表名、主键和 VO 字段边界。
 * 3. 读取 schema 文件检查唯一索引声明，避免迁移脚本遗漏关键业务约束。
 */
class ExecutionTraceContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效执行轨迹上报请求可以通过参数校验。
     */
    @Test
    void validReportExecutionTraceRequestShouldPassValidation() {
        Set<ConstraintViolation<ReportExecutionTraceRequest>> violations =
                validator.validate(buildValidRequest());

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少客户端轨迹 ID 时参数校验失败，避免幂等能力失效。
     */
    @Test
    void reportExecutionTraceRequestShouldRejectMissingClientTraceId() {
        ReportExecutionTraceRequest request = buildValidRequest();
        request.setClientTraceId(" ");

        Set<ConstraintViolation<ReportExecutionTraceRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验证据 JSON 超长时参数校验失败，避免保存完整页面或完整响应。
     */
    @Test
    void reportExecutionTraceRequestShouldRejectTooLongEvidenceJson() {
        ReportExecutionTraceRequest request = buildValidRequest();
        request.setEvidenceJson(repeat("a", 32769));

        Set<ConstraintViolation<ReportExecutionTraceRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验执行轨迹 VO 不暴露内部和大 JSON 字段。
     */
    @Test
    void executionTraceVoShouldHideInternalFields() {
        assertThrows(NoSuchFieldException.class, () -> ExecutionTraceVO.class.getDeclaredField("deleted"));
        assertThrows(NoSuchFieldException.class, () -> ExecutionTraceVO.class.getDeclaredField("inputDataJson"));
        assertThrows(NoSuchFieldException.class, () -> ExecutionTraceVO.class.getDeclaredField("evidenceJson"));
    }

    /**
     * 校验实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void executionTraceEntityShouldMapExecutionTraceTable() throws NoSuchFieldException {
        TableName tableName = ExecutionTrace.class.getAnnotation(TableName.class);
        Field idField = ExecutionTrace.class.getDeclaredField("id");

        assertEquals("execution_trace", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 ExecutionTrace。
     */
    @Test
    void executionTraceMapperShouldBindExecutionTraceEntity() {
        Type[] interfaces = ExecutionTraceMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(ExecutionTrace.class, baseMapperType.getActualTypeArguments()[0]);
    }

    /**
     * 校验执行轨迹表具备按客户端轨迹 ID 防重复上报的唯一索引。
     *
     * @throws IOException 当 schema 文件不可读时，测试应失败以暴露工程配置问题。
     */
    @Test
    void executionTraceSchemaShouldPreventDuplicateClientTrace() throws IOException {
        String schema = new String(Files.readAllBytes(Paths.get("src/main/resources/db/schema/V1__mvp_schema.sql")),
                StandardCharsets.UTF_8);

        assertTrue(schema.contains("CREATE UNIQUE INDEX uk_execution_trace_client_trace"));
        assertTrue(schema.contains("ON execution_trace (tenant_id, execution_id, client_trace_id)"));
        assertTrue(schema.contains("WHERE deleted = false AND client_trace_id IS NOT NULL"));
    }

    /**
     * 构造最小有效执行轨迹上报请求。
     *
     * @return SDK 执行轨迹上报请求。
     */
    private ReportExecutionTraceRequest buildValidRequest() {
        ReportExecutionTraceRequest request = new ReportExecutionTraceRequest();
        request.setTenantId("tenant_001");
        request.setExecutionId("execution_001");
        request.setSdkSessionId("sdk_001");
        request.setClientTraceId("client_trace_001");
        request.setTaskStepId("step_001");
        request.setTeachingPointId("tp_001");
        request.setResourceId("res_001");
        request.setTraceType("CLICK");
        request.setTraceTime(LocalDateTime.now());
        request.setSequenceNo(1L);
        request.setInputDataJson("{\"masked\":true}");
        request.setEvidenceJson("{\"resource\":\"res_001\"}");
        request.setSuccess(Boolean.TRUE);
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
