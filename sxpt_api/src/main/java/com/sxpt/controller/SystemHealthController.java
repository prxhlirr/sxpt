package com.sxpt.controller;

import com.sxpt.common.api.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统健康检查接口。
 *
 * 业务功能：
 * 1. 提供最小可用的 GET 查询接口，用于验证服务启动状态。
 * 2. 返回统一响应结构，确保项目骨架符合接口规范。
 *
 * 关键流程：
 * 1. 调用方请求 /api/v1/system/health。
 * 2. 服务端返回应用名称和运行状态。
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    /**
     * 查询服务健康状态。
     *
     * @return 统一响应结构包装后的健康状态。
     */
    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new LinkedHashMap<String, Object>();
        healthInfo.put("application", "sxpt_api");
        healthInfo.put("status", "UP");
        return ApiResult.success(healthInfo);
    }
}
