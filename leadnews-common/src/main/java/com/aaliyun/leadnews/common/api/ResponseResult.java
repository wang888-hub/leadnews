package com.aaliyun.leadnews.common.api;

import com.aaliyun.leadnews.common.trace.TraceContext;
import java.time.Instant;

public record ResponseResult<T>(int code, String message, T data, long timestamp, String traceId) {
    public static <T> ResponseResult<T> success() { return success(null); }
    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(0, "success", data, Instant.now().toEpochMilli(), TraceContext.getTraceId());
    }
    public static <T> ResponseResult<T> error(int code, String message) {
        return new ResponseResult<>(code, message, null, Instant.now().toEpochMilli(), TraceContext.getTraceId());
    }
    public static <T> ResponseResult<T> error(ErrorCode errorCode) { return error(errorCode.code(), errorCode.message()); }
}
