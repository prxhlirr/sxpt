package com.sxpt.module.capture;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.capture.entity.CaptureActionDraft;
import com.sxpt.module.capture.mapper.CaptureActionDraftMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 页面动作草稿模块契约测试。
 *
 * 业务功能：
 * 1. 验证 CaptureActionDraft 实体与 capture_action_draft 表名保持一致。
 * 2. 验证 Mapper 绑定实体类型正确，避免草稿误写入事件表。
 * 3. 验证来源事件到草稿的数据库幂等约束存在，避免并发生成重复草稿。
 *
 * 关键流程：
 * 1. 使用反射读取实体注解，不连接数据库。
 * 2. 使用 Mapper 泛型检查 BaseMapper 绑定关系。
 * 3. 读取 schema 文件检查唯一索引声明，避免迁移脚本遗漏关键业务约束。
 */
class CaptureActionDraftMapperContractTests {

    /**
     * 校验实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void captureActionDraftEntityShouldMapCaptureActionDraftTable() throws NoSuchFieldException {
        TableName tableName = CaptureActionDraft.class.getAnnotation(TableName.class);
        Field idField = CaptureActionDraft.class.getDeclaredField("id");

        assertEquals("capture_action_draft", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 CaptureActionDraft。
     */
    @Test
    void captureActionDraftMapperShouldBindCaptureActionDraftEntity() {
        Type[] interfaces = CaptureActionDraftMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(CaptureActionDraft.class, baseMapperType.getActualTypeArguments()[0]);
    }

    /**
     * 校验草稿表具备按来源事件防重复生成的唯一索引。
     *
     * @throws IOException 当 schema 文件不可读时，测试应失败以暴露工程配置问题。
     */
    @Test
    void captureActionDraftSchemaShouldPreventDuplicateDraftsForSameEvent() throws IOException {
        String schema = new String(Files.readAllBytes(Paths.get("src/main/resources/db/schema/V1__mvp_schema.sql")),
                StandardCharsets.UTF_8);

        assertTrue(schema.contains("CREATE UNIQUE INDEX uk_action_draft_session_event"));
        assertTrue(schema.contains("ON capture_action_draft (tenant_id, capture_session_id, event_id)"));
        assertTrue(schema.contains("WHERE deleted = false AND event_id IS NOT NULL"));
    }
}
