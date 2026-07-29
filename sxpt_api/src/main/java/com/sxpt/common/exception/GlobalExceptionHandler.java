package com.sxpt.common.exception;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.api.ApiResultCode;
import org.apache.shiro.authc.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * 业务功能：
 * 1. 将参数异常、业务异常、系统异常统一转换为标准响应结构。
 * 2. 避免 Controller 重复编写 try-catch，保证错误格式一致。
 *
 * 关键流程：
 * 1. 参数校验失败返回参数错误码。
 * 2. 业务异常返回业务自身错误码。
 * 3. 未预期异常记录完整日志，对外返回系统异常。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理请求体参数校验异常。
     *
     * @param exception Spring 参数校验异常。
     * @return 统一失败响应。
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ApiResult.failure(ApiResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理查询参数校验异常。
     *
     * @param exception 查询参数校验异常。
     * @return 统一失败响应。
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        return ApiResult.failure(ApiResultCode.PARAM_ERROR.getCode(), exception.getMessage());
    }

    /**
     * 处理 JWT 缺失、无效或过期等认证异常。
     *
     * @param exception Shiro 认证异常。
     * @return 401 统一失败响应。
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException.class)
    public ApiResult<Void> handleAuthenticationException(AuthenticationException exception) {
        return ApiResult.failure(ApiResultCode.UNAUTHORIZED.getCode(), ApiResultCode.UNAUTHORIZED.getMessage());
    }

    /**
     * 处理业务异常。
     *
     * @param exception 业务异常。
     * @return 统一失败响应。
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException exception) {
        return ApiResult.failure(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理数据库约束异常。
     *
     * @param exception 数据完整性异常。
     * @return 统一失败响应。
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiResult<Void> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        LOGGER.warn("数据约束异常", exception);
        return ApiResult.failure(ApiResultCode.PARAM_ERROR.getCode(), "数据保存失败，请检查必填字段、唯一编码或关联数据是否有效");
    }

    /**
     * 处理未预期系统异常。
     *
     * @param exception 系统异常。
     * @return 统一失败响应。
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception) {
        LOGGER.error("系统异常", exception);
        return ApiResult.failure(ApiResultCode.SYSTEM_ERROR.getCode(), ApiResultCode.SYSTEM_ERROR.getMessage());
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ":" + fieldError.getDefaultMessage();
    }
}
