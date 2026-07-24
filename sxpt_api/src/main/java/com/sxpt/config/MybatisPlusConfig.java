package com.sxpt.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 基础配置。
 *
 * 业务功能：
 * 1. 统一 Mapper 扫描路径，避免各模块重复配置。
 * 2. 注册分页插件，保证分页查询使用框架能力而不是手写分页逻辑。
 *
 * 关键流程：
 * 1. 各业务 Mapper 使用 @Mapper 标记，避免在无数据源测试环境中强制创建 Mapper Bean。
 * 2. 分页查询通过 MybatisPlusInterceptor 处理数据库分页。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 创建分页插件。
     *
     * @return MyBatis Plus 插件集合。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
