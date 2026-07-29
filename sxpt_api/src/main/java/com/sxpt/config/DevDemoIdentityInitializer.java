package com.sxpt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 开发环境模拟身份初始化器。
 *
 * 业务功能：
 * 1. 在正式用户、角色和登录逻辑接入前，为前端固定使用的模拟教师和学生创建最小用户镜像。
 * 2. 保证 task_execution、practice_attempt 等带用户外键的业务表可以引用模拟身份。
 *
 * 关键流程：
 * 1. 仅在 dev profile 且 sxpt.demo-identity.enabled 未关闭时运行。
 * 2. 按固定用户 ID 幂等检查，已经存在的用户不会被覆盖。
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "sxpt.demo-identity.enabled", havingValue = "true", matchIfMissing = true)
public class DevDemoIdentityInitializer implements ApplicationRunner {

    static final String DEMO_TENANT_ID = "demo-tenant";

    static final String DEMO_TEACHER_ID = "demo-teacher";

    static final String DEMO_STUDENT_ID = "demo-student";

    private static final Logger LOGGER = LoggerFactory.getLogger(DevDemoIdentityInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DevDemoIdentityInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureDemoUser(DEMO_TEACHER_ID, "demo-teacher", "模拟教师", "TEACHER");
        ensureDemoUser(DEMO_STUDENT_ID, "demo-student", "模拟学生", "STUDENT");
    }

    /**
     * 创建不存在的模拟用户。
     *
     * @param id 固定用户 ID。
     * @param username 模拟账号。
     * @param realName 展示名称。
     * @param userType 用户类型。
     */
    private void ensureDemoUser(String id, String username, String realName, String userType) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(1) FROM teach_user WHERE id = ?",
                new Object[]{id},
                Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO teach_user "
                        + "(id, tenant_id, username, real_name, user_type, source_type, "
                        + "create_by, create_time, update_by, update_time, status, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, 'MOCK', 'dev-bootstrap', now(), "
                        + "'dev-bootstrap', now(), 'ACTIVE', false)",
                id, DEMO_TENANT_ID, username, realName, userType);
        LOGGER.info("Initialized development demo identity: {}", id);
    }
}
