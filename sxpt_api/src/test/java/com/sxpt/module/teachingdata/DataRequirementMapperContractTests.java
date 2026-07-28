package com.sxpt.module.teachingdata;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teachingdata.entity.DataRequirement;
import com.sxpt.module.teachingdata.mapper.DataRequirementMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据需求批次 Mapper 契约测试。
 *
 * 业务功能：
 * 1. 验证 data_requirement 实体与数据库表名保持一致。
 * 2. 验证 Mapper 绑定的实体类型正确，避免后续数据准备服务访问错误表。
 *
 * 关键流程：
 * 1. 使用反射读取实体注解，不连接数据库。
 * 2. 使用 Mapper 泛型检查 BaseMapper 绑定关系。
 */
class DataRequirementMapperContractTests {

    /**
     * 校验数据需求批次实体表名、主键注解和关键约束字段。
     *
     * @throws NoSuchFieldException 当实体关键字段被误删或重命名时，测试应失败。
     */
    @Test
    void dataRequirementEntityShouldMapDataRequirementTable() throws NoSuchFieldException {
        TableName tableName = DataRequirement.class.getAnnotation(TableName.class);
        Field idField = DataRequirement.class.getDeclaredField("id");
        Field requirementCodeField = DataRequirement.class.getDeclaredField("requirementCode");
        Field sceneTypeField = DataRequirement.class.getDeclaredField("sceneType");
        Field lockVersionField = DataRequirement.class.getDeclaredField("lockVersion");

        assertEquals("data_requirement", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
        assertEquals(String.class, requirementCodeField.getType());
        assertEquals(String.class, sceneTypeField.getType());
        assertEquals(Long.class, lockVersionField.getType());
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 DataRequirement。
     */
    @Test
    void dataRequirementMapperShouldBindDataRequirementEntity() {
        Type[] interfaces = DataRequirementMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(DataRequirement.class, baseMapperType.getActualTypeArguments()[0]);
    }
}
