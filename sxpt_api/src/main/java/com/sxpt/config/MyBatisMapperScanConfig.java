package com.sxpt.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * MyBatis Mapper 扫描配置。
 *
 * 业务功能：
 * 1. 在非 test 环境注册业务 Mapper，保障真实运行环境可以访问数据库。
 * 2. 在 test 环境跳过真实 Mapper，避免无数据源的 MockMvc 测试创建 Mapper 失败。
 *
 * 关键流程：
 * 1. dev/prod 等环境启动时扫描继承 BaseMapper 的接口。
 * 2. test profile 依赖 MockBean 或纯单元测试验证业务逻辑，不注册真实 Mapper。
 */
@Configuration
@Profile("!test")
@MapperScan(basePackages = "com.sxpt.module", markerInterface = BaseMapper.class)
public class MyBatisMapperScanConfig {
}
