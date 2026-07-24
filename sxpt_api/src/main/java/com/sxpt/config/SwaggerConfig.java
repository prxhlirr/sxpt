package com.sxpt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger 接口文档配置。
 *
 * 业务功能：
 * 1. 生成后端接口文档，降低前后端联调成本。
 * 2. 仅扫描 com.sxpt.controller 包，避免内部类被误暴露。
 *
 * 关键流程：
 * 1. Spring 启动时创建 Docket。
 * 2. Swagger UI 根据 Docket 扫描 Controller 并生成接口描述。
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    /**
     * 创建 Swagger 文档配置。
     *
     * @return Swagger Docket。
     */
    @Bean
    public Docket apiDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.sxpt.controller"))
                .paths(PathSelectors.ant("/api/v1/**"))
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("sxpt_api 接口文档")
                .description("后端项目工程规范约束下的 API 文档")
                .version("1.0.0")
                .build();
    }
}
