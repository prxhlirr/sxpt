package com.sxpt.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 开发环境模拟身份初始化测试。
 */
@ExtendWith(MockitoExtension.class)
class DevDemoIdentityInitializerTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateTeacherAndStudentWhenTheyDoNotExist() {
        when(jdbcTemplate.queryForObject(
                anyString(), eq(new Object[]{DevDemoIdentityInitializer.DEMO_TEACHER_ID}), eq(Integer.class)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(
                anyString(), eq(new Object[]{DevDemoIdentityInitializer.DEMO_STUDENT_ID}), eq(Integer.class)))
                .thenReturn(0);

        new DevDemoIdentityInitializer(jdbcTemplate).run(null);

        verify(jdbcTemplate).update(
                anyString(),
                eq(DevDemoIdentityInitializer.DEMO_TEACHER_ID),
                eq(DevDemoIdentityInitializer.DEMO_TENANT_ID),
                eq("demo-teacher"),
                eq("模拟教师"),
                eq("TEACHER"));
        verify(jdbcTemplate).update(
                anyString(),
                eq(DevDemoIdentityInitializer.DEMO_STUDENT_ID),
                eq(DevDemoIdentityInitializer.DEMO_TENANT_ID),
                eq("demo-student"),
                eq("模拟学生"),
                eq("STUDENT"));
    }

    @Test
    void shouldNotOverwriteExistingDemoUsers() {
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.<Object[]>any(),
                eq(Integer.class))).thenReturn(1);

        new DevDemoIdentityInitializer(jdbcTemplate).run(null);

        verify(jdbcTemplate, org.mockito.Mockito.never()).update(
                anyString(),
                org.mockito.ArgumentMatchers.<Object>any(),
                org.mockito.ArgumentMatchers.<Object>any(),
                org.mockito.ArgumentMatchers.<Object>any(),
                org.mockito.ArgumentMatchers.<Object>any(),
                org.mockito.ArgumentMatchers.<Object>any());
    }
}
