package com.sxpt.common.security;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.service.ExternalConnectorCredentialService;
import org.apache.shiro.mgt.SecurityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthInterceptorTests {

    @AfterEach
    void clearExternalConnectorContext() {
        ExternalConnectorContext.clear();
    }

    @Test
    void shouldAllowCorsPreflightWithoutBearerToken() {
        SecurityManager securityManager = mock(SecurityManager.class);
        AuthenticatedUserContextService contextService = mock(AuthenticatedUserContextService.class);
        JwtAuthInterceptor interceptor = new JwtAuthInterceptor(securityManager, contextService);
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/connector/system/list");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(allowed);
        verifyNoInteractions(securityManager);
    }

    @Test
    void connectorKeyInterceptorShouldRejectMissingKey() {
        ExternalConnectorCredentialService credentialService =
                mock(ExternalConnectorCredentialService.class);
        ConnectorKeyAuthInterceptor interceptor =
                new ConnectorKeyAuthInterceptor(credentialService);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/v1/connector/classic-cases/upsert");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request,
                        new MockHttpServletResponse(), new Object()));

        assertEquals(ApiResultCode.UNAUTHORIZED.getCode(), exception.getCode());
        assertFalse(ExternalConnectorContext.get().isPresent());
        verifyNoInteractions(credentialService);
    }

    @Test
    void connectorKeyInterceptorShouldDelegateSharedBusinessModuleRequestToJwt() {
        ExternalConnectorCredentialService credentialService =
                mock(ExternalConnectorCredentialService.class);
        ConnectorKeyAuthInterceptor interceptor =
                new ConnectorKeyAuthInterceptor(credentialService);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/connector/business-modules");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        assertFalse(ExternalConnectorContext.get().isPresent());
        verifyNoInteractions(credentialService);
    }

    @Test
    void connectorKeyInterceptorShouldEstablishConnectorContext() {
        ExternalConnectorCredentialService credentialService =
                mock(ExternalConnectorCredentialService.class);
        ExternalConnectorCredentialService.AuthenticatedExternalConnector connector =
                mock(ExternalConnectorCredentialService.AuthenticatedExternalConnector.class);
        when(credentialService.authenticate("local-key")).thenReturn(connector);
        ConnectorKeyAuthInterceptor interceptor =
                new ConnectorKeyAuthInterceptor(credentialService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Connector-Key", "local-key");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        verify(credentialService).authenticate("local-key");
        assertEquals(connector, ExternalConnectorContext.require());
    }
}
