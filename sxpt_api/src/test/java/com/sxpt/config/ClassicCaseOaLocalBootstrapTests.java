package com.sxpt.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OA 本地经典案例环境配对脚本的数据库行为测试。
 */
@EnabledIfSystemProperty(named = "sxpt.dev.jdbc.url", matches = ".+")
class ClassicCaseOaLocalBootstrapTests {

    @Test
    void shouldPairTest0805LearningSystemWithOaClassicCaseSource() throws Exception {
        String url = System.getProperty("sxpt.dev.jdbc.url");
        String username = System.getProperty("sxpt.dev.jdbc.username", "postgres");
        String password = System.getProperty("sxpt.dev.jdbc.password", "");

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            try {
                createTemporaryConnectorTables(connection);
                seedConnectorSystems(connection);

                ScriptUtils.executeSqlScript(
                        connection,
                        new ClassPathResource("db/dev/classic_case_oa_local_bootstrap.sql"));

                Map<String, String> environments = loadEnvironmentPair(connection);
                assertThat(environments)
                        .containsEntry(
                                "5d5bba96bdc24fc99bf70261db4ebac4",
                                "LEARNING|oa-test-0805-local")
                        .containsEntry(
                                "origin-oa-prod-system",
                                "PROD|oa-test-0805-local");
                assertTest0805DataCreateCredential(connection);
                assertClassicCaseTemplates(connection);
            } finally {
                connection.rollback();
            }
        }
    }

    private void createTemporaryConnectorTables(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TEMP TABLE connector_system ("
                    + "id varchar(64) PRIMARY KEY, tenant_id varchar(64), system_code varchar(128), "
                    + "system_name varchar(255), system_type varchar(64), environment_type varchar(32), "
                    + "environment_group_code varchar(128), base_url varchar(512), auth_type varchar(64), "
                    + "config_json jsonb, create_by varchar(64), create_time timestamp, update_by varchar(64), "
                    + "update_time timestamp, status varchar(32), deleted boolean) ON COMMIT DROP");
            statement.execute("CREATE TEMP TABLE connector_external_credential ("
                    + "id varchar(64) PRIMARY KEY, tenant_id varchar(64), connector_system_id varchar(64), "
                    + "credential_name varchar(255), api_key_prefix varchar(128), api_key_hash varchar(128), "
                    + "hash_algorithm varchar(32), create_by varchar(64), create_time timestamp, "
                    + "update_by varchar(64), update_time timestamp, status varchar(32), deleted boolean) "
                    + "ON COMMIT DROP");
            statement.execute("CREATE TEMP TABLE teaching_data_template ("
                    + "id varchar(64) PRIMARY KEY, tenant_id varchar(64), connector_system_id varchar(64), "
                    + "teaching_point_id varchar(64), template_code varchar(128), template_name varchar(256), "
                    + "scene_type varchar(32), module_code varchar(128), strategy_id varchar(64), "
                    + "init_state varchar(64), support_mode varchar(128), template_usage varchar(32), "
                    + "config_json jsonb, data_schema_json jsonb, mock_rule_json jsonb, readonly_flag boolean, "
                    + "request_schema_json jsonb, required_org_role_json jsonb, result_check_schema_json jsonb, "
                    + "sensitive_field_policy_json jsonb, create_by varchar(64), create_time timestamp, "
                    + "update_by varchar(64), update_time timestamp, status varchar(32), deleted boolean) "
                    + "ON COMMIT DROP");
            statement.execute("CREATE UNIQUE INDEX uk_test_template_code ON teaching_data_template "
                    + "(tenant_id, connector_system_id, template_code) WHERE deleted = false");
        }
    }

    private void seedConnectorSystems(Connection connection) throws Exception {
        String sql = "INSERT INTO connector_system (id, tenant_id, system_code, system_name, system_type, "
                + "environment_type, environment_group_code, base_url, auth_type, config_json, create_by, "
                + "create_time, update_by, update_time, status, deleted) "
                + "VALUES (?, 'demo-tenant', ?, ?, 'OA', ?, ?, ?, 'NONE', '{}', 'test', now(), "
                + "'test', now(), 'ACTIVE', false)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            insertConnectorSystem(
                    statement,
                    "origin-oa-demo-system",
                    "OA_DEMO",
                    "OA 协同办公系统（旧学习环境）",
                    "LEARNING",
                    "oa-demo-local",
                    "http://127.0.0.1:9527");
            insertConnectorSystem(
                    statement,
                    "5d5bba96bdc24fc99bf70261db4ebac4",
                    "test-0805",
                    "OA协同办公系统",
                    null,
                    null,
                    "http://localhost:3000/teaching-launch");
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO teaching_data_template (id, tenant_id, connector_system_id, "
                    + "template_code, template_name, scene_type, module_code, init_state, support_mode, "
                    + "template_usage, config_json, readonly_flag, create_by, create_time, update_by, "
                    + "update_time, status, deleted) VALUES ("
                    + "'normal-template', 'demo-tenant', '5d5bba96bdc24fc99bf70261db4ebac4', "
                    + "'LOCAL_TEMPLATE', '本地模板', 'PRACTICE', 'record_apply_1785916779495', "
                    + "'DRAFT', 'PRACTICE', 'NORMAL', '{\"mode\":\"demo\"}'::jsonb, false, "
                    + "'test', now(), 'test', now(), 'ACTIVE', false)");
        }
    }

    private void insertConnectorSystem(
            PreparedStatement statement,
            String id,
            String code,
            String name,
            String environmentType,
            String environmentGroupCode,
            String baseUrl) throws Exception {
        statement.setString(1, id);
        statement.setString(2, code);
        statement.setString(3, name);
        statement.setString(4, environmentType);
        statement.setString(5, environmentGroupCode);
        statement.setString(6, baseUrl);
        statement.executeUpdate();
    }

    private Map<String, String> loadEnvironmentPair(Connection connection) throws Exception {
        Map<String, String> result = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, environment_type, environment_group_code FROM connector_system "
                        + "WHERE id IN ('5d5bba96bdc24fc99bf70261db4ebac4', 'origin-oa-prod-system')");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.put(
                        resultSet.getString("id"),
                        resultSet.getString("environment_type")
                                + "|"
                                + resultSet.getString("environment_group_code"));
            }
        }
        return result;
    }

    private void assertTest0805DataCreateCredential(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT auth_type, config_json ->> 'headerName' AS header_name, "
                        + "config_json ->> 'apiKey' AS api_key, "
                        + "jsonb_exists(config_json, 'token') AS has_legacy_token "
                        + "FROM connector_system WHERE id = '5d5bba96bdc24fc99bf70261db4ebac4'");
             ResultSet resultSet = statement.executeQuery()) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("auth_type")).isEqualTo("API_KEY");
            assertThat(resultSet.getString("header_name")).isEqualTo("X-API-Key");
            assertThat(resultSet.getString("api_key")).isEqualTo("oa-data-center-incoming-token");
            assertThat(resultSet.getBoolean("has_legacy_token")).isFalse();
        }
    }

    private void assertClassicCaseTemplates(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT template_usage, template_code, init_state FROM teaching_data_template "
                        + "WHERE connector_system_id = '5d5bba96bdc24fc99bf70261db4ebac4' "
                        + "AND module_code = 'record_apply_1785916779495' "
                        + "AND template_usage IN ('CLASSIC_CASE_REPLAY', 'CLASSIC_CASE_DEMO') "
                        + "AND status = 'ACTIVE' AND deleted = false ORDER BY template_usage");
             ResultSet resultSet = statement.executeQuery()) {
            Map<String, String> templates = new HashMap<>();
            while (resultSet.next()) {
                templates.put(
                        resultSet.getString("template_usage"),
                        resultSet.getString("template_code") + "|" + resultSet.getString("init_state"));
            }
            assertThat(templates)
                    .containsEntry("CLASSIC_CASE_REPLAY", "incoming_classic_case_v1|DRAFT")
                    .containsEntry("CLASSIC_CASE_DEMO", "incoming_classic_demo_v1|DRAFT");
        }
    }
}
