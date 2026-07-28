package com.sxpt.module.teachingdata;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;
import com.sxpt.module.teachingdata.mapper.DataRequirementItemMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据需求明细 Mapper 契约测试。
 *
 * 业务功能：
 * 1. 验证 data_requirement_item 实体与数据库表名保持一致。
 * 2. 验证 Mapper 绑定的实体类型正确，避免后续逐条数据准备服务访问错误表。
 *
 * 关键流程：
 * 1. 使用反射读取实体注解，不连接数据库。
 * 2. 使用 Mapper 泛型检查 BaseMapper 绑定关系。
 */
class DataRequirementItemMapperContractTests {

    /**
     * 校验数据需求明细实体表名、主键注解和关键约束字段。
     *
     * @throws NoSuchFieldException 当实体关键字段被误删或重命名时，测试应失败。
     */
    @Test
    void dataRequirementItemEntityShouldMapDataRequirementItemTable() throws NoSuchFieldException {
        TableName tableName = DataRequirementItem.class.getAnnotation(TableName.class);
        Field idField = DataRequirementItem.class.getDeclaredField("id");
        Field requestBatchIdField = DataRequirementItem.class.getDeclaredField("requestBatchId");
        Field requiredExternalOrgIdField = DataRequirementItem.class.getDeclaredField("requiredExternalOrgId");
        Field requiredExternalRoleIdField = DataRequirementItem.class.getDeclaredField("requiredExternalRoleId");
        Field validationStatusField = DataRequirementItem.class.getDeclaredField("validationStatus");

        assertEquals("data_requirement_item", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
        assertEquals(String.class, requestBatchIdField.getType());
        assertEquals(String.class, requiredExternalOrgIdField.getType());
        assertEquals(String.class, requiredExternalRoleIdField.getType());
        assertEquals(String.class, validationStatusField.getType());
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 DataRequirementItem。
     */
    @Test
    void dataRequirementItemMapperShouldBindDataRequirementItemEntity() {
        Type[] interfaces = DataRequirementItemMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(DataRequirementItem.class, baseMapperType.getActualTypeArguments()[0]);
    }
}
