package com.sxpt.common.idempotent;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * 幂等请求拦截器。
 *
 * 业务功能：
 * 1. 对标记 @Idempotent 的接口强制校验 Idempotency-Key。
 * 2. 使用 Redis SETNX 防止关键写操作重复提交。
 *
 * 关键流程：
 * 1. 非 HandlerMethod 或未标注注解的方法直接放行。
 * 2. 缺少 Idempotency-Key 返回参数类业务错误。
 * 3. Redis 写入失败说明重复提交，返回幂等冲突错误。
 */
public class IdempotentInterceptor implements HandlerInterceptor {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private static final String KEY_PREFIX = "sxpt:idempotency:";

    private final StringRedisTemplate stringRedisTemplate;

    public IdempotentInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Idempotent idempotent = handlerMethod.getMethodAnnotation(Idempotent.class);
        if (idempotent == null) {
            return true;
        }
        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException(ApiResultCode.IDEMPOTENCY_KEY_REQUIRED);
        }
        String redisKey = KEY_PREFIX + request.getRequestURI() + ":" + idempotencyKey;
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSING", idempotent.expireSeconds(), TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(success)) {
            throw new BusinessException(ApiResultCode.IDEMPOTENCY_CONFLICT);
        }
        return true;
    }
}
