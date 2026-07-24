package com.sxpt.common.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等控制注解。
 *
 * 业务功能：
 * 1. 标记必须携带 Idempotency-Key 的关键写接口。
 * 2. 将幂等控制从业务代码中抽离，避免每个接口重复实现。
 *
 * 关键流程：
 * 1. 拦截器识别被注解的方法。
 * 2. 使用 Redis 记录幂等键。
 * 3. 重复提交时直接拒绝。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等键有效期秒数。
     *
     * @return Redis 记录过期时间。
     */
    long expireSeconds() default 300L;
}
