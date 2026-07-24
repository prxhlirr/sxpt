package com.sxpt.module.evaluation;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.entity.EvaluationRule;
import com.sxpt.module.evaluation.service.EvaluationConfigService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 评分配置接口测试。
 *
 * 业务功能：
 * 1. 验证评价规则和评分项创建接口遵循统一响应结构。
 * 2. 验证配置查询接口可用，且不返回内部软删除字段。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.evaluation.config-controller.enabled=true")
class EvaluationConfigControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private EvaluationConfigService evaluationConfigService;

    /**
     * 校验创建评价规则成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createRuleShouldReturnEvaluationRuleVo() throws Exception {
        EvaluationRule saved = buildSavedRule();
        when(evaluationConfigService.createEvaluationRule(any(EvaluationRule.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/evaluation/config/rules/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"ruleCode\":\"RULE_RECORD\",\"ruleName\":\"备案评分规则\",\"taskId\":\"task_001\",\"teachingPointId\":\"tp_001\",\"totalScore\":100.00,\"description\":\"备案评分规则说明\",\"createBy\":\"teacher_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("rule_001")))
                .andExpect(jsonPath("$.result.ruleCode", is("RULE_RECORD")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<EvaluationRule> captor = ArgumentCaptor.forClass(EvaluationRule.class);
        verify(evaluationConfigService).createEvaluationRule(captor.capture());
        EvaluationRule requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("RULE_RECORD", requestEntity.getRuleCode());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("100.00"), requestEntity.getTotalScore());
    }

    /**
     * 校验规则总分为 0 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createRuleShouldRejectZeroTotalScore() throws Exception {
        mockMvc.perform(post("/api/v1/evaluation/config/rules/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"ruleCode\":\"RULE_RECORD\",\"ruleName\":\"备案评分规则\",\"totalScore\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验查询评价规则成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listRulesShouldReturnEvaluationRuleVos() throws Exception {
        EvaluationRule saved = buildSavedRule();
        when(evaluationConfigService.listRules("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/evaluation/config/rules")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("taskId", "task_001")
                        .param("teachingPointId", "tp_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("rule_001")))
                .andExpect(jsonPath("$.result[0].ruleCode", is("RULE_RECORD")));

        verify(evaluationConfigService).listRules("tenant_001", "task_001", "tp_001");
    }

    /**
     * 校验创建评分项成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createItemShouldReturnEvaluationItemVo() throws Exception {
        EvaluationItem saved = buildSavedItem();
        when(evaluationConfigService.createEvaluationItem(any(EvaluationItem.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/evaluation/config/items/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"evaluationRuleId\":\"rule_001\",\"teachingPointId\":\"tp_001\",\"itemCode\":\"ITEM_SUBMIT\",\"itemName\":\"提交申请\",\"itemType\":\"KEY_ACTION\",\"relatedResourceId\":\"res_001\",\"relatedTaskStepId\":\"step_001\",\"score\":10.00,\"required\":true,\"assertionType\":\"TRACE_EXISTS\",\"assertionConfigJson\":\"{\\\"eventType\\\":\\\"CLICK\\\"}\",\"failPolicy\":\"NO_SCORE\",\"createBy\":\"teacher_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("item_001")))
                .andExpect(jsonPath("$.result.itemCode", is("ITEM_SUBMIT")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<EvaluationItem> captor = ArgumentCaptor.forClass(EvaluationItem.class);
        verify(evaluationConfigService).createEvaluationItem(captor.capture());
        EvaluationItem requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("rule_001", requestEntity.getEvaluationRuleId());
        org.junit.jupiter.api.Assertions.assertEquals("TRACE_EXISTS", requestEntity.getAssertionType());
    }

    /**
     * 校验缺少断言配置时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createItemShouldRejectMissingAssertionConfig() throws Exception {
        mockMvc.perform(post("/api/v1/evaluation/config/items/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"evaluationRuleId\":\"rule_001\",\"itemCode\":\"ITEM_SUBMIT\",\"itemName\":\"提交申请\",\"itemType\":\"KEY_ACTION\",\"score\":10.00,\"assertionType\":\"TRACE_EXISTS\",\"failPolicy\":\"NO_SCORE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验查询评分项成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listItemsShouldReturnEvaluationItemVos() throws Exception {
        EvaluationItem saved = buildSavedItem();
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/evaluation/config/items")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("evaluationRuleId", "rule_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("item_001")))
                .andExpect(jsonPath("$.result[0].itemCode", is("ITEM_SUBMIT")));

        verify(evaluationConfigService).listItemsByRule("tenant_001", "rule_001");
    }

    /**
     * 构造 Service 返回的已保存评价规则。
     *
     * @return 评价规则实体。
     */
    private EvaluationRule buildSavedRule() {
        EvaluationRule rule = new EvaluationRule();
        rule.setId("rule_001");
        rule.setTenantId("tenant_001");
        rule.setRuleCode("RULE_RECORD");
        rule.setRuleName("备案评分规则");
        rule.setVersionNo(1L);
        rule.setTaskId("task_001");
        rule.setTeachingPointId("tp_001");
        rule.setTotalScore(new BigDecimal("100.00"));
        rule.setDescription("备案评分规则说明");
        rule.setStatus("ACTIVE");
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        return rule;
    }

    /**
     * 构造 Service 返回的已保存评分项。
     *
     * @return 评分项实体。
     */
    private EvaluationItem buildSavedItem() {
        EvaluationItem item = new EvaluationItem();
        item.setId("item_001");
        item.setTenantId("tenant_001");
        item.setEvaluationRuleId("rule_001");
        item.setTeachingPointId("tp_001");
        item.setItemCode("ITEM_SUBMIT");
        item.setItemName("提交申请");
        item.setItemType("KEY_ACTION");
        item.setRelatedResourceId("res_001");
        item.setRelatedTaskStepId("step_001");
        item.setScore(new BigDecimal("10.00"));
        item.setRequired(Boolean.TRUE);
        item.setAssertionType("TRACE_EXISTS");
        item.setAssertionConfigJson("{\"eventType\":\"CLICK\"}");
        item.setFailPolicy("NO_SCORE");
        item.setStatus("ACTIVE");
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        return item;
    }

    /**
     * 构造通过 JWT 拦截器所需的认证请求头。
     *
     * @return Bearer Token 请求头值。
     */
    private String bearerToken() {
        return "Bearer " + jwtService.generateToken("admin_001", "admin");
    }
}
