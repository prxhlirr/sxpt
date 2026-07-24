package com.sxpt.common.exception;

import com.sxpt.common.api.ApiResultCode;

/**
 * 业务异常。
 *
 * 业务功能：
 * 1. 承载可预期的业务失败，避免使用 RuntimeException 表达正常业务分支。
 * 2. 绑定统一错误码，保证接口响应可被前端和调用方稳定识别。
 *
 * 关键流程：
 * 1. Service 层发现业务规则不满足时抛出本异常。
 * 2. 全局异常处理器捕获后转换为统一响应结构。
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ApiResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
