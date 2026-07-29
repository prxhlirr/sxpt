package com.sxpt.module.teachingdata;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teachingdata.entity.CollaborationSegmentAllocation;
import com.sxpt.module.teachingdata.mapper.CollaborationSegmentAllocationMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 协作片段分配 Mapper 契约测试。
 *
 * 业务功能：
 * 1. 验证 collaboration_segment_allocation 实体与数据库表名保持一致。
 * 2. 验证 Mapper 绑定的实体类型正确，避免后续协作片段服务访问错误表。
 *
 * 关键流程：
 * 1. 使用反射读取实体注解，不连接数据库。
 * 2. 使用 Mapper 泛型检查 BaseMapper 绑定关系。
 */
class CollaborationSegmentAllocationMapperContractTests {

    /**
     * 校验协作片段分配实体表名、主键注解和关键分配字段。
     *
     * @throws NoSuchFieldException 当实体关键字段被误删或重命名时，测试应失败。
     */
    @Test
    void collaborationSegmentAllocationEntityShouldMapCollaborationSegmentAllocationTable() throws NoSuchFieldException {
        TableName tableName = CollaborationSegmentAllocation.class.getAnnotation(TableName.class);
        Field idField = CollaborationSegmentAllocation.class.getDeclaredField("id");
        Field collaborationUnitIdField = CollaborationSegmentAllocation.class.getDeclaredField("collaborationUnitId");
        Field studentIdField = CollaborationSegmentAllocation.class.getDeclaredField("studentId");
        Field segmentNoField = CollaborationSegmentAllocation.class.getDeclaredField("segmentNo");
        Field requiredExternalOrgIdField = CollaborationSegmentAllocation.class.getDeclaredField("requiredExternalOrgId");
        Field requiredExternalRoleIdField = CollaborationSegmentAllocation.class.getDeclaredField("requiredExternalRoleId");
        Field scorePointSnapshotJsonField = CollaborationSegmentAllocation.class.getDeclaredField("scorePointSnapshotJson");

        assertEquals("collaboration_segment_allocation", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
        assertEquals(String.class, collaborationUnitIdField.getType());
        assertEquals(String.class, studentIdField.getType());
        assertEquals(Long.class, segmentNoField.getType());
        assertEquals(String.class, requiredExternalOrgIdField.getType());
        assertEquals(String.class, requiredExternalRoleIdField.getType());
        assertEquals(String.class, scorePointSnapshotJsonField.getType());
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 CollaborationSegmentAllocation。
     */
    @Test
    void collaborationSegmentAllocationMapperShouldBindCollaborationSegmentAllocationEntity() {
        Type[] interfaces = CollaborationSegmentAllocationMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(CollaborationSegmentAllocation.class, baseMapperType.getActualTypeArguments()[0]);
    }
}
