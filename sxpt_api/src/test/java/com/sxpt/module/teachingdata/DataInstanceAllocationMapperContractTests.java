package com.sxpt.module.teachingdata;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;
import com.sxpt.module.teachingdata.mapper.DataInstanceAllocationMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据实例分配 Mapper 契约测试。
 *
 * 业务功能：
 * 1. 验证 data_instance_allocation 实体与数据库表名保持一致。
 * 2. 验证 Mapper 绑定的实体类型正确，避免后续领取服务访问错误表。
 *
 * 关键流程：
 * 1. 使用反射读取实体注解，不连接数据库。
 * 2. 使用 Mapper 泛型检查 BaseMapper 绑定关系。
 */
class DataInstanceAllocationMapperContractTests {

    /**
     * 校验数据实例分配实体表名、主键注解和关键绑定字段。
     *
     * @throws NoSuchFieldException 当实体关键字段被误删或重命名时，测试应失败。
     */
    @Test
    void dataInstanceAllocationEntityShouldMapDataInstanceAllocationTable() throws NoSuchFieldException {
        TableName tableName = DataInstanceAllocation.class.getAnnotation(TableName.class);
        Field idField = DataInstanceAllocation.class.getDeclaredField("id");
        Field dataInstanceIdField = DataInstanceAllocation.class.getDeclaredField("dataInstanceId");
        Field ownerUserIdField = DataInstanceAllocation.class.getDeclaredField("ownerUserId");
        Field requiredExternalOrgIdField = DataInstanceAllocation.class.getDeclaredField("requiredExternalOrgId");
        Field requiredExternalRoleIdField = DataInstanceAllocation.class.getDeclaredField("requiredExternalRoleId");
        Field collaborationUnitIdField = DataInstanceAllocation.class.getDeclaredField("collaborationUnitId");

        assertEquals("data_instance_allocation", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
        assertEquals(String.class, dataInstanceIdField.getType());
        assertEquals(String.class, ownerUserIdField.getType());
        assertEquals(String.class, requiredExternalOrgIdField.getType());
        assertEquals(String.class, requiredExternalRoleIdField.getType());
        assertEquals(String.class, collaborationUnitIdField.getType());
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 DataInstanceAllocation。
     */
    @Test
    void dataInstanceAllocationMapperShouldBindDataInstanceAllocationEntity() {
        Type[] interfaces = DataInstanceAllocationMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(DataInstanceAllocation.class, baseMapperType.getActualTypeArguments()[0]);
    }
}
