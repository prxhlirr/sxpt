package com.sxpt.module.teachingdata;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teachingdata.entity.CollaborationUnit;
import com.sxpt.module.teachingdata.mapper.CollaborationUnitMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 协作单元 Mapper 契约测试。
 *
 * 业务功能：
 * 1. 验证 collaboration_unit 实体与数据库表名保持一致。
 * 2. 验证 Mapper 绑定的实体类型正确，避免后续协作重置服务访问错误表。
 *
 * 关键流程：
 * 1. 使用反射读取实体注解，不连接数据库。
 * 2. 使用 Mapper 泛型检查 BaseMapper 绑定关系。
 */
class CollaborationUnitMapperContractTests {

    /**
     * 校验协作单元实体表名、主键注解和关键聚合字段。
     *
     * @throws NoSuchFieldException 当实体关键字段被误删或重命名时，测试应失败。
     */
    @Test
    void collaborationUnitEntityShouldMapCollaborationUnitTable() throws NoSuchFieldException {
        TableName tableName = CollaborationUnit.class.getAnnotation(TableName.class);
        Field idField = CollaborationUnit.class.getDeclaredField("id");
        Field dataInstanceIdField = CollaborationUnit.class.getDeclaredField("dataInstanceId");
        Field externalBusinessIdField = CollaborationUnit.class.getDeclaredField("externalBusinessId");
        Field resetFromUnitIdField = CollaborationUnit.class.getDeclaredField("resetFromUnitId");
        Field lockVersionField = CollaborationUnit.class.getDeclaredField("lockVersion");

        assertEquals("collaboration_unit", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
        assertEquals(String.class, dataInstanceIdField.getType());
        assertEquals(String.class, externalBusinessIdField.getType());
        assertEquals(String.class, resetFromUnitIdField.getType());
        assertEquals(Long.class, lockVersionField.getType());
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 CollaborationUnit。
     */
    @Test
    void collaborationUnitMapperShouldBindCollaborationUnitEntity() {
        Type[] interfaces = CollaborationUnitMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(CollaborationUnit.class, baseMapperType.getActualTypeArguments()[0]);
    }
}
