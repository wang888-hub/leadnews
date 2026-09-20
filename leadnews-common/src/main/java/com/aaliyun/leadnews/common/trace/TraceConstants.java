package com.aaliyun.leadnews.common.trace;

public final class TraceConstants {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_TYPE_HEADER = "X-User-Type";
    public static final String INTERNAL_REQUEST_HEADER = "X-Internal-Request";
    public static final String MDC_TRACE_ID = "traceId";
    private TraceConstants() {}
}
