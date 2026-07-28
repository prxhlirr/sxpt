package com.sxpt.module.teachingdata;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import com.sxpt.module.teachingdata.mapper.DataPrepareJobMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据准备任务 Mapper 契约测试。
 *
 * 业务功能：
 * 1. 验证 data_prepare_job 实体与数据库表名保持一致。
 * 2. 验证 Mapper 绑定的实体类型正确，避免后续任务服务访问错误表。
 *
 * 关键流程：
 * 1. 使用反射读取实体注解，不连接数据库。
 * 2. 使用 Mapper 泛型检查 BaseMapper 绑定关系。
 */
class DataPrepareJobMapperContractTests {

    /**
     * 校验数据准备任务实体表名、主键注解和关键幂等字段。
     *
     * @throws NoSuchFieldException 当实体关键字段被误删或重命名时，测试应失败。
     */
    @Test
    void dataPrepareJobEntityShouldMapDataPrepareJobTable() throws NoSuchFieldException {
        TableName tableName = DataPrepareJob.class.getAnnotation(TableName.class);
        Field idField = DataPrepareJob.class.getDeclaredField("id");
        Field idempotencyKeyField = DataPrepareJob.class.getDeclaredField("idempotencyKey");
        Field externalRequestIdField = DataPrepareJob.class.getDeclaredField("externalRequestId");
        Field requestBatchIdField = DataPrepareJob.class.getDeclaredField("requestBatchId");
        Field retryCountField = DataPrepareJob.class.getDeclaredField("retryCount");
        Field traceIdField = DataPrepareJob.class.getDeclaredField("traceId");

        assertEquals("data_prepare_job", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
        assertEquals(String.class, idempotencyKeyField.getType());
        assertEquals(String.class, externalRequestIdField.getType());
        assertEquals(String.class, requestBatchIdField.getType());
        assertEquals(Long.class, retryCountField.getType());
        assertEquals(String.class, traceIdField.getType());
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 DataPrepareJob。
     */
    @Test
    void dataPrepareJobMapperShouldBindDataPrepareJobEntity() {
        Type[] interfaces = DataPrepareJobMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(DataPrepareJob.class, baseMapperType.getActualTypeArguments()[0]);
    }
}
