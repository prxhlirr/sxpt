package com.sxpt.controller;

import com.sxpt.common.api.ApiResult;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基础设施健康检查接口。
 *
 * 业务功能：
 * 1. 验证 dev/prod 环境下 PostgreSQL 与 Redis 是否可用。
 * 2. 将基础设施连通性与普通应用存活检查拆开，便于定位故障边界。
 *
 * 关键流程：
 * 1. 数据库检查执行轻量 SQL。
 * 2. Redis 检查执行 ping。
 * 3. 测试 profile 下不注册本 Controller，避免单元测试被外部服务阻断。
 */
@Profile("!test")
@RestController
@RequestMapping("/api/v1/system")
public class InfrastructureHealthController {

    private final JdbcTemplate jdbcTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    public InfrastructureHealthController(JdbcTemplate jdbcTemplate, StringRedisTemplate stringRedisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 查询数据库连通状态。
     *
     * @return 数据库健康信息。
     */
    @GetMapping("/db-health")
    public ApiResult<Map<String, Object>> dbHealth() {
        Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        Map<String, Object> healthInfo = new LinkedHashMap<String, Object>();
        healthInfo.put("database", "PostgreSQL");
        healthInfo.put("status", Integer.valueOf(1).equals(value) ? "UP" : "DOWN");
        return ApiResult.success(healthInfo);
    }

    /**
     * 查询 Redis 连通状态。
     *
     * @return Redis 健康信息。
     */
    @GetMapping("/redis-health")
    public ApiResult<Map<String, Object>> redisHealth() {
        String pong = stringRedisTemplate.getConnectionFactory().getConnection().ping();
        Map<String, Object> healthInfo = new LinkedHashMap<String, Object>();
        healthInfo.put("redis", "Redis");
        healthInfo.put("status", "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN");
        return ApiResult.success(healthInfo);
    }
}
