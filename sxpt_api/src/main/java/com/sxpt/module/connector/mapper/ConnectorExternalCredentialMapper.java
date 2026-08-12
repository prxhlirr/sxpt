package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.ConnectorExternalCredential;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方原平台系统级凭证数据访问接口。
 *
 * 业务功能：
 * 1. 提供 API Key 哈希反查凭证的基础数据访问能力。
 * 2. 支持后台生成、禁用、审计第三方调用凭证。
 *
 * 关键流程：
 * 1. Service 层负责密钥生成、哈希和边界校验。
 * 2. Mapper 只负责持久化，避免把认证规则散落到 SQL 访问层。
 */
@Mapper
public interface ConnectorExternalCredentialMapper extends BaseMapper<ConnectorExternalCredential> {
}
