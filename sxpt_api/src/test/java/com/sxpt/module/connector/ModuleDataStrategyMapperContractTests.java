package com.sxpt.module.connector;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.ModuleDataStrategy;
import com.sxpt.module.connector.mapper.ModuleDataStrategyMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 原平台业务模块数据准备策略 Mapper 契约测试。
 *
 * 业务功能：
 * 1. 验证 module_data_strategy 实体与数据库表名保持一致。
 * 2. 验证 Mapper 绑定的实体类型正确，避免策略管理服务访问错误表。
 *
 * 关键流程：
 * 1. 使用反射读取实体注解和关键字段，不连接真实数据库。
 * 2. 使用 Mapper 泛型检查 BaseMapper 绑定关系。
 */
class ModuleDataStrategyMapperContractTests {

    /**
     * 校验数据策略实体表名、主键注解和关键约束字段。
     *
     * @throws NoSuchFieldException 当实体关键字段被误删或重命名时，测试应失败。
     */
    @Test
    void moduleDataStrategyEntityShouldMapModuleDataStrategyTable() throws NoSuchFieldException {
        TableName tableName = ModuleDataStrategy.class.getAnnotation(TableName.class);
        Field idField = ModuleDataStrategy.class.getDeclaredField("id");
        Field businessModuleIdField = ModuleDataStrategy.class.getDeclaredField("businessModuleId");
        Field sceneTypeField = ModuleDataStrategy.class.getDeclaredField("sceneType");
        Field strategyVersionField = ModuleDataStrategy.class.getDeclaredField("strategyVersion");
        Field lockVersionField = ModuleDataStrategy.class.getDeclaredField("lockVersion");

        assertEquals("module_data_strategy", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
        assertEquals(String.class, businessModuleIdField.getType());
        assertEquals(String.class, sceneTypeField.getType());
        assertEquals(Long.class, strategyVersionField.getType());
        assertEquals(Long.class, lockVersionField.getType());
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 ModuleDataStrategy。
     */
    @Test
    void moduleDataStrategyMapperShouldBindModuleDataStrategyEntity() {
        Type[] interfaces = ModuleDataStrategyMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(ModuleDataStrategy.class, baseMapperType.getActualTypeArguments()[0]);
    }
}
