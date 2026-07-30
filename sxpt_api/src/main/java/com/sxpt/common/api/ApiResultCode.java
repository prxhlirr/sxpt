package com.sxpt.common.api;

/**
 * 统一响应码枚举。
 *
 * 业务功能：
 * 1. 集中维护基础响应码，避免业务代码散落魔法字符串。
 * 2. 为后续模块级错误码扩展提供统一格式。
 *
 * 关键流程：
 * 1. Controller 和异常处理器只引用枚举。
 * 2. 前端根据 code 识别业务结果。
 */
public enum ApiResultCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    DATA_NOT_FOUND(404, "数据不存在"),
    DATA_PREPARE_CONFIG_INCOMPLETE(422, "数据准备配置不完整"),
    STATE_NOT_ALLOWED(500, "状态不允许操作"),
    IDEMPOTENCY_KEY_REQUIRED(400, "缺少幂等请求头"),
    IDEMPOTENCY_CONFLICT(409, "重复请求"),
    UNAUTHORIZED(401, "用户未登录"),
    FORBIDDEN(403, "用户无权限"),
    SYSTEM_ERROR(500, "系统异常");

    private final Integer code;

    private final String message;

    ApiResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
