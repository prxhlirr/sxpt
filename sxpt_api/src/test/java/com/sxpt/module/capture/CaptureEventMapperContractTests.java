package com.sxpt.module.capture;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.capture.entity.CaptureEvent;
import com.sxpt.module.capture.mapper.CaptureEventMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SDK 采集事件模块契约测试。
 *
 * 业务功能：
 * 1. 验证 CaptureEvent 实体与 capture_event 表名保持一致。
 * 2. 验证 Mapper 绑定实体类型正确，避免事件误写入其它采集表。
 *
 * 关键流程：
 * 1. 使用反射读取实体注解，不连接数据库。
 * 2. 使用 Mapper 泛型检查 BaseMapper 绑定关系。
 */
class CaptureEventMapperContractTests {

    /**
     * 校验实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void captureEventEntityShouldMapCaptureEventTable() throws NoSuchFieldException {
        TableName tableName = CaptureEvent.class.getAnnotation(TableName.class);
        Field idField = CaptureEvent.class.getDeclaredField("id");

        assertEquals("capture_event", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 CaptureEvent。
     */
    @Test
    void captureEventMapperShouldBindCaptureEventEntity() {
        Type[] interfaces = CaptureEventMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(CaptureEvent.class, baseMapperType.getActualTypeArguments()[0]);
    }
}
