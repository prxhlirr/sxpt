package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.ConnectorExternalCredential;
import com.sxpt.module.connector.entity.ConnectorSystem;

/**
 * 第三方原平台系统级凭证服务。
 *
 * 业务功能：
 * 1. 为教学平台中的原平台正式环境生成外部调用 API Key。
 * 2. 在第三方请求进入时校验 API Key，并返回其绑定的正式环境身份。
 *
 * 关键流程：
 * 1. 生成凭证时只返回一次明文，数据库只保存哈希。
 * 2. 认证成功后返回凭证、正式环境平台和同环境组学习环境，供外部接口做边界校验。
 */
public interface ExternalConnectorCredentialService {

    GeneratedCredential generateApiKey(String connectorSystemId, String operator);

    ConnectorExternalCredential getApiKeyByConnectorSystemId(String connectorSystemId);

    ConnectorExternalCredential disableApiKey(String credentialId, String operator);

    AuthenticatedExternalConnector authenticate(String apiKey);

    class GeneratedCredential {

        private final ConnectorExternalCredential credential;

        private final String apiKey;

        public GeneratedCredential(ConnectorExternalCredential credential, String apiKey) {
            this.credential = credential;
            this.apiKey = apiKey;
        }

        public ConnectorExternalCredential getCredential() {
            return credential;
        }

        public String getApiKey() {
            return apiKey;
        }
    }

    class AuthenticatedExternalConnector {

        private final ConnectorExternalCredential credential;

        private final ConnectorSystem sourceSystem;

        private final ConnectorSystem learningSystem;

        public AuthenticatedExternalConnector(ConnectorExternalCredential credential,
                                              ConnectorSystem sourceSystem,
                                              ConnectorSystem learningSystem) {
            this.credential = credential;
            this.sourceSystem = sourceSystem;
            this.learningSystem = learningSystem;
        }

        public ConnectorExternalCredential getCredential() {
            return credential;
        }

        public ConnectorSystem getSourceSystem() {
            return sourceSystem;
        }

        public ConnectorSystem getLearningSystem() {
            return learningSystem;
        }
    }
}
