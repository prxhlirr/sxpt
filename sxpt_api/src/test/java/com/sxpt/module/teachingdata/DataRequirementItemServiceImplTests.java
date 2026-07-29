package com.sxpt.module.teachingdata;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RequirementItemStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.ValidationStatus;
import com.sxpt.module.teachingdata.mapper.DataRequirementItemMapper;
import com.sxpt.module.teachingdata.service.DataRequirementItemService;
import com.sxpt.module.teachingdata.service.impl.DataRequirementItemServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据需求明细服务测试。
 *
 * 业务功能：
 * 1. 验证创建明细时会补齐状态、校验状态、审计时间和软删除默认值。
 * 2. 验证单位和角色是创建明细的硬约束，避免后续进入原平台时才暴露不可操作问题。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class DataRequirementItemServiceImplTests {

    private final DataRequirementItemMapper mapper = mock(DataRequirementItemMapper.class);

    private final DataRequirementItemService service = new DataRequirementItemServiceImpl(mapper);

    /**
     * 验证创建数据需求明细时写入 Mapper 并补齐默认值。
     */
    @Test
    void createDataRequirementItemShouldInsertAndFillDefaults() {
        DataRequirementItem item = buildValidItem();

        DataRequirementItem saved = service.createDataRequirementItem(item);

        assertSame(item, saved);
        assertEquals(RequirementItemStatus.CREATED.getValue(), saved.getItemStatus());
        assertEquals(ValidationStatus.NOT_CHECKED.getValue(), saved.getValidationStatus());
        assertEquals(RecordStatus.ACTIVE.getValue(), saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 验证缺少所需原平台角色时拒绝创建，避免生成无法被正确角色操作的数据。
     */
    @Test
    void createDataRequirementItemShouldRejectMissingRequiredRole() {
        DataRequirementItem item = buildValidItem();
        item.setRequiredExternalRoleId(" ");

        assertThrows(BusinessException.class, () -> service.createDataRequirementItem(item));
        verify(mapper, times(0)).insert(item);
    }

    /**
     * 验证按需求批次查询明细时返回 Mapper 结果。
     */
    @Test
    void listByRequirementShouldReturnMapperResult() {
        DataRequirementItem item = buildValidItem();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(item));

        List<DataRequirementItem> result = service.listByRequirement("tenant_001", "req_001");

        assertEquals(1, result.size());
        assertSame(item, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 验证按学生和场景查询明细时返回 Mapper 结果。
     */
    @Test
    void listByStudentAndSceneShouldReturnMapperResult() {
        DataRequirementItem item = buildValidItem();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(item));

        List<DataRequirementItem> result = service.listByStudentAndScene(
                "tenant_001", "task_001", "PRACTICE", "student_001");

        assertEquals(1, result.size());
        assertSame(item, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造最小有效数据需求明细。
     *
     * @return 数据需求明细实体。
     */
    private DataRequirementItem buildValidItem() {
        DataRequirementItem item = new DataRequirementItem();
        item.setId("item_001");
        item.setTenantId("tenant_001");
        item.setRequirementId("req_001");
        item.setRequestBatchId("batch_001");
        item.setRequestItemId("batch_001:item_001");
        item.setConnectorSystemId("connector_001");
        item.setModuleCode("BUSINESS_APPLY");
        item.setSceneType("PRACTICE");
        item.setTaskId("task_001");
        item.setStudentId("student_001");
        item.setActorType("STUDENT");
        item.setOwnerExternalOrgId("org_owner_001");
        item.setRequiredExternalOrgId("org_operator_001");
        item.setRequiredExternalRoleId("role_operator_001");
        return item;
    }
}
