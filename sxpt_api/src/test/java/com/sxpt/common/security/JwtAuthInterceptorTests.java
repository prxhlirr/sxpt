package com.sxpt.common.security;

import org.apache.shiro.mgt.SecurityManager;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class JwtAuthInterceptorTests {

    @Test
    void shouldAllowCorsPreflightWithoutBearerToken() {
        SecurityManager securityManager = mock(SecurityManager.class);
        JwtAuthInterceptor interceptor = new JwtAuthInterceptor(securityManager);
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/connector/system/list");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(allowed);
        verifyNoInteractions(securityManager);
    }
}
