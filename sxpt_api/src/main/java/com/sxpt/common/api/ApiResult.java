package com.sxpt.common.api;

/**
 * 统一接口响应对象。
 *
 * 业务功能：
 * 1. 固定接口响应结构，降低前后端联调成本。
 * 2. 统一承载业务状态码、提示信息、响应数据和服务端时间。
 *
 * 关键流程：
 * 1. 成功请求通过 success 方法创建响应。
 * 2. 业务失败通过 failure 方法创建响应。
 * 3. timestamp 由服务端统一生成毫秒时间戳，匹配 MVP 接口文档约束。
 *
 * @param <T> 响应数据类型。
 */
public class ApiResult<T> {

    private Boolean success;

    private Integer code;

    private String message;

    private T result;

    private Long timestamp;

    public ApiResult() {
    }

    private ApiResult(Boolean success, Integer code, String message, T result) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.result = result;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构建成功响应。
     *
     * @param result 响应数据。
     * @param <T> 响应数据类型。
     * @return 统一成功响应。
     */
    public static <T> ApiResult<T> success(T result) {
        return new ApiResult<T>(true, ApiResultCode.SUCCESS.getCode(), ApiResultCode.SUCCESS.getMessage(), result);
    }

    /**
     * 构建失败响应。
     *
     * @param code 错误码。
     * @param message 错误信息。
     * @param <T> 响应数据类型。
     * @return 统一失败响应。
     */
    public static <T> ApiResult<T> failure(Integer code, String message) {
        return new ApiResult<T>(false, code, message, null);
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
