package com.sxpt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * sxpt_api 服务启动入口。
 *
 * 关键流程：
 * 1. 由 Spring Boot 加载 Web、配置、数据源、Redis 等基础能力。
 * 2. 扫描 com.sxpt 包下的 Controller、Service、Mapper 和配置类。
 * 3. 作为后端 API 服务的唯一启动入口，避免多入口导致环境行为不一致。
 */
@SpringBootApplication
@ComponentScan("com.sxpt")
public class SxptApiApplication {

    /**
     * 启动后端服务。
     *
     * @param args JVM 启动参数，保留给不同环境注入 profile 或运行参数。
     */
    public static void main(String[] args) {
        SpringApplication.run(SxptApiApplication.class, args);
    }
}
