package com.sxpt.module.connector;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.mapper.BusinessModuleMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 原平台业务模块 Mapper 契约测试。
 *
 * 业务功能：
 * 1. 验证 business_module 实体与数据库表名保持一致。
 * 2. 验证 Mapper 绑定的实体类型正确，避免后续业务模块管理服务访问错误表。
 *
 * 关键流程：
 * 1. 使用反射读取实体注解，不连接数据库。
 * 2. 使用 Mapper 泛型检查 BaseMapper 绑定关系。
 */
class BusinessModuleMapperContractTests {

    /**
     * 校验业务模块实体表名、主键注解和关键约束字段。
     *
     * @throws NoSuchFieldException 当实体关键字段被误删或重命名时，测试应失败。
     */
    @Test
    void businessModuleEntityShouldMapBusinessModuleTable() throws NoSuchFieldException {
        TableName tableName = BusinessModule.class.getAnnotation(TableName.class);
        Field idField = BusinessModule.class.getDeclaredField("id");
        Field connectorSystemIdField = BusinessModule.class.getDeclaredField("connectorSystemId");
        Field moduleCodeField = BusinessModule.class.getDeclaredField("moduleCode");
        Field lockVersionField = BusinessModule.class.getDeclaredField("lockVersion");

        assertEquals("business_module", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
        assertEquals(String.class, connectorSystemIdField.getType());
        assertEquals(String.class, moduleCodeField.getType());
        assertEquals(Long.class, lockVersionField.getType());
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 BusinessModule。
     */
    @Test
    void businessModuleMapperShouldBindBusinessModuleEntity() {
        Type[] interfaces = BusinessModuleMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(BusinessModule.class, baseMapperType.getActualTypeArguments()[0]);
    }
}
