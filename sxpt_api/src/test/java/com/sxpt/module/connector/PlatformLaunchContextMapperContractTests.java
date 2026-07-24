package com.sxpt.module.connector;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.mapper.PlatformLaunchContextMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 原平台启动上下文模块契约测试。
 *
 * 业务功能：
 * 1. 验证 PlatformLaunchContext 实体与 platform_launch_context 表名保持一致。
 * 2. 验证 Mapper 绑定实体类型正确，避免后续 launchToken 能力写错表。
 *
 * 关键流程：
 * 1. 使用反射读取实体注解，不连接数据库。
 * 2. 使用 Mapper 泛型检查 BaseMapper 绑定关系。
 */
class PlatformLaunchContextMapperContractTests {

    /**
     * 校验实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void platformLaunchContextEntityShouldMapPlatformLaunchContextTable() throws NoSuchFieldException {
        TableName tableName = PlatformLaunchContext.class.getAnnotation(TableName.class);
        Field idField = PlatformLaunchContext.class.getDeclaredField("id");

        assertEquals("platform_launch_context", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 PlatformLaunchContext。
     */
    @Test
    void platformLaunchContextMapperShouldBindPlatformLaunchContextEntity() {
        Type[] interfaces = PlatformLaunchContextMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(PlatformLaunchContext.class, baseMapperType.getActualTypeArguments()[0]);
    }
}
