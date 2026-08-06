package com.sxpt.module.connector;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.support.BusinessSsoLaunchUrlBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 业务平台单点登录启动地址构造测试。
 *
 * 这些测试保护启动参数编码、原查询参数保留和 fragment 位置，避免令牌或 redirect
 * 被拼到 fragment 中而无法被业务平台服务端读取。
 */
class BusinessSsoLaunchUrlBuilderTests {

    private final BusinessSsoLaunchUrlBuilder builder = new BusinessSsoLaunchUrlBuilder();

    @Test
    void shouldAppendEncodedLaunchParametersBeforeFragment() {
        String result = builder.build(
                "https://oa.example.com/sso?client=sxpt#login",
                "tenant one",
                "ctx_+/=",
                "https://oa.example.com/purchase/apply?draft=new");

        assertEquals(
                "https://oa.example.com/sso?client=sxpt"
                        + "&tenantId=tenant+one"
                        + "&launchToken=ctx_%2B%2F%3D"
                        + "&redirect=https%3A%2F%2Foa.example.com%2Fpurchase%2Fapply%3Fdraft%3Dnew"
                        + "#login",
                result);
    }

    @Test
    void shouldUseQuestionMarkWhenSsoEntryHasNoQuery() {
        String result = builder.build(
                "https://oa.example.com/sso",
                "tenant-1",
                "ctx_token",
                "/purchase/apply");

        assertEquals(
                "https://oa.example.com/sso?tenantId=tenant-1"
                        + "&launchToken=ctx_token"
                        + "&redirect=%2Fpurchase%2Fapply",
                result);
    }

    @Test
    void shouldRejectMissingLaunchValues() {
        assertThrows(BusinessException.class,
                () -> builder.build("", "tenant-1", "ctx_token", "/purchase/apply"));
        assertThrows(BusinessException.class,
                () -> builder.build("https://oa.example.com/sso", " ", "ctx_token", "/purchase/apply"));
        assertThrows(BusinessException.class,
                () -> builder.build("https://oa.example.com/sso", "tenant-1", "", "/purchase/apply"));
        assertThrows(BusinessException.class,
                () -> builder.build("https://oa.example.com/sso", "tenant-1", "ctx_token", null));
    }
}
