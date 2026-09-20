package com.aaliyun.leadnews.common.api;

public enum CommonErrorCode implements ErrorCode {
    AUTH_UNAUTHORIZED(40100, "Authentication required or token invalid"),
    AUTH_FORBIDDEN(40300, "Access denied"),
    VALIDATION_FAILED(40000, "Request validation failed"),
    VALIDATION(40000, "Request validation failed"),
    FEIGN_CALL_FAILED(50200, "Internal service call failed"),
    RATE_LIMITED(42900, "请求过于频繁，请稍后重试"),
    HOT_PARAM_RATE_LIMITED(42901, "热点文章访问过于频繁，请稍后重试"),
    SYSTEM_ERROR(50000, "Internal server error");

    private final int code;
    private final String message;

    CommonErrorCode(int code, String message) { this.code = code; this.message = message; }
    public int code() { return code; }
    public String message() { return message; }
}
