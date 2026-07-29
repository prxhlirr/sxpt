package com.sxpt.module.connector;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.PlatformCapability;
import com.sxpt.module.connector.mapper.PlatformCapabilityMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 原平台能力注册模块契约测试。
 *
 * 业务功能：
 * 1. 验证 platform_capability 实体和数据库表名保持一致。
 * 2. 验证 Mapper 绑定的实体类型正确，避免后续策略校验读错表。
 *
 * 关键流程：
 * 1. 通过反射读取实体注解，不依赖数据库连接。
 * 2. 通过泛型检查确认 Mapper 继承 MyBatis Plus 标准 Mapper。
 */
class PlatformCapabilityMapperContractTests {

    /**
     * 校验实体表名和主键注解，避免能力注册校验落到错误数据表。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void platformCapabilityEntityShouldMapPlatformCapabilityTable() throws NoSuchFieldException {
        TableName tableName = PlatformCapability.class.getAnnotation(TableName.class);
        Field idField = PlatformCapability.class.getDeclaredField("id");

        assertEquals("platform_capability", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 绑定 PlatformCapability，保证 Service 层后续按能力注册表查询。
     */
    @Test
    void platformCapabilityMapperShouldBindPlatformCapabilityEntity() {
        Type[] interfaces = PlatformCapabilityMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(PlatformCapability.class, baseMapperType.getActualTypeArguments()[0]);
    }
}
