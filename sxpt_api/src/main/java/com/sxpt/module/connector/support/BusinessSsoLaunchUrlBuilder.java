package com.sxpt.module.connector.support;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds the short-lived SSO entry URL used to open a configured business module.
 */
@Component
public class BusinessSsoLaunchUrlBuilder {

    public String build(String ssoEntryUrl,
                        String tenantId,
                        String launchToken,
                        String redirectUrl) {
        requireText(ssoEntryUrl);
        requireText(tenantId);
        requireText(launchToken);
        requireText(redirectUrl);

        int fragmentIndex = ssoEntryUrl.indexOf('#');
        String entryWithoutFragment = fragmentIndex >= 0
                ? ssoEntryUrl.substring(0, fragmentIndex)
                : ssoEntryUrl;
        String fragment = fragmentIndex >= 0
                ? ssoEntryUrl.substring(fragmentIndex)
                : "";

        String separator;
        if (entryWithoutFragment.endsWith("?") || entryWithoutFragment.endsWith("&")) {
            separator = "";
        } else {
            separator = entryWithoutFragment.contains("?") ? "&" : "?";
        }

        return entryWithoutFragment
                + separator
                + "tenantId=" + encode(tenantId)
                + "&launchToken=" + encode(launchToken)
                + "&redirect=" + encode(redirectUrl)
                + fragment;
    }

    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException exception) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
    }
}
