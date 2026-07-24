package com.sxpt.module.capture;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.capture.entity.CaptureResourceSnapshot;
import com.sxpt.module.capture.mapper.CaptureResourceSnapshotMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 采集资源快照模块契约测试。
 *
 * 业务功能：
 * 1. 验证 CaptureResourceSnapshot 实体与 capture_resource_snapshot 表名保持一致。
 * 2. 验证 Mapper 绑定实体类型正确，避免快照误写入事件表。
 *
 * 关键流程：
 * 1. 使用反射读取实体注解，不连接数据库。
 * 2. 使用 Mapper 泛型检查 BaseMapper 绑定关系。
 */
class CaptureResourceSnapshotMapperContractTests {

    /**
     * 校验实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void captureResourceSnapshotEntityShouldMapCaptureResourceSnapshotTable() throws NoSuchFieldException {
        TableName tableName = CaptureResourceSnapshot.class.getAnnotation(TableName.class);
        Field idField = CaptureResourceSnapshot.class.getDeclaredField("id");

        assertEquals("capture_resource_snapshot", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 CaptureResourceSnapshot。
     */
    @Test
    void captureResourceSnapshotMapperShouldBindCaptureResourceSnapshotEntity() {
        Type[] interfaces = CaptureResourceSnapshotMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(CaptureResourceSnapshot.class, baseMapperType.getActualTypeArguments()[0]);
    }
}
