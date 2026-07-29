package com.sxpt.module.connector;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.mapper.TeachingDataTemplateMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 教学业务数据模板模块契约测试。
 *
 * 业务功能：
 * 1. 验证 TeachingDataTemplate 实体与 teaching_data_template 表名保持一致。
 * 2. 验证 Mapper 绑定实体类型正确，避免后续模板能力写错表。
 *
 * 关键流程：
 * 1. 使用反射读取实体注解，不连接数据库。
 * 2. 使用 Mapper 泛型检查 BaseMapper 绑定关系。
 */
class TeachingDataTemplateMapperContractTests {

    /**
     * 校验实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void teachingDataTemplateEntityShouldMapTeachingDataTemplateTable() throws NoSuchFieldException {
        TableName tableName = TeachingDataTemplate.class.getAnnotation(TableName.class);
        Field idField = TeachingDataTemplate.class.getDeclaredField("id");
        Field moduleCodeField = TeachingDataTemplate.class.getDeclaredField("moduleCode");
        Field strategyIdField = TeachingDataTemplate.class.getDeclaredField("strategyId");
        Field requestSchemaJsonField = TeachingDataTemplate.class.getDeclaredField("requestSchemaJson");
        Field requiredOrgRoleJsonField = TeachingDataTemplate.class.getDeclaredField("requiredOrgRoleJson");
        Field resultCheckSchemaJsonField = TeachingDataTemplate.class.getDeclaredField("resultCheckSchemaJson");
        Field sensitiveFieldPolicyJsonField = TeachingDataTemplate.class.getDeclaredField("sensitiveFieldPolicyJson");

        assertEquals("teaching_data_template", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
        assertEquals(String.class, moduleCodeField.getType());
        assertEquals(String.class, strategyIdField.getType());
        assertEquals(String.class, requestSchemaJsonField.getType());
        assertEquals(String.class, requiredOrgRoleJsonField.getType());
        assertEquals(String.class, resultCheckSchemaJsonField.getType());
        assertEquals(String.class, sensitiveFieldPolicyJsonField.getType());
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 TeachingDataTemplate。
     */
    @Test
    void teachingDataTemplateMapperShouldBindTeachingDataTemplateEntity() {
        Type[] interfaces = TeachingDataTemplateMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(TeachingDataTemplate.class, baseMapperType.getActualTypeArguments()[0]);
    }
}
