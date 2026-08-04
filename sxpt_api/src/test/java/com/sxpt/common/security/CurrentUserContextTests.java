package com.sxpt.common.security;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 当前用户上下文测试。
 *
 * 业务功能：
 * 1. 验证认证链路写入的当前用户可以被后续业务层读取。
 * 2. 验证清理逻辑可靠，避免线程复用时出现身份串用。
 *
 * 关键流程：
 * 1. 测试写入、读取、必读和清理行为。
 * 2. 测试非法当前用户不会进入上下文。
 */
class CurrentUserContextTests {

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    /**
     * 校验当前用户写入后可以被业务代码读取。
     */
    @Test
    void shouldStoreCurrentUserInCurrentThread() {
        CurrentUserContext.set(new CurrentUserContext.CurrentUser("10001", "teacher001"));

        assertThat(CurrentUserContext.get()).isPresent();
        assertThat(CurrentUserContext.getRequiredUser().getUserId()).isEqualTo("10001");
        assertThat(CurrentUserContext.getRequiredUser().getUsername()).isEqualTo("teacher001");
    }

    /**
     * 校验清理后必读当前用户会返回未登录业务异常。
     */
    @Test
    void shouldRejectRequiredUserAfterClear() {
        CurrentUserContext.set(new CurrentUserContext.CurrentUser("10001", "teacher001"));

        CurrentUserContext.clear();

        assertThat(CurrentUserContext.get()).isNotPresent();
        assertThatThrownBy(CurrentUserContext::getRequiredUser)
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ApiResultCode.UNAUTHORIZED.getCode());
    }

    /**
     * 校验缺少用户 ID 的身份不会写入上下文，因为它不能支撑后续授权和审计。
     */
    @Test
    void shouldRejectCurrentUserWithoutUserId() {
        assertThatThrownBy(() -> CurrentUserContext.set(new CurrentUserContext.CurrentUser("", "teacher001")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ApiResultCode.UNAUTHORIZED.getCode());
        assertThat(CurrentUserContext.get()).isNotPresent();
    }

    @Test
    void shouldMatchUserTypeAndRoleCodesIgnoringCase() {
        CurrentUserContext.CurrentUser user = new CurrentUserContext.CurrentUser(
                "10001",
                "tenant-001",
                "teacher001",
                "张老师",
                "teacher",
                null,
                "T001",
                Arrays.asList("COURSE_OWNER", " expert "),
                Collections.emptyList(),
                Collections.emptyList());

        assertThat(user.hasAnyRole("TEACHER")).isTrue();
        assertThat(user.hasAnyRole("ADMIN", "EXPERT")).isTrue();
        assertThat(user.hasAnyRole("STUDENT")).isFalse();
        assertThat(user.hasAnyRole()).isFalse();
    }
}
