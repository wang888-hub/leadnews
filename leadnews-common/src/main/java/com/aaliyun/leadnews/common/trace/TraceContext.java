package com.aaliyun.leadnews.common.trace;

import org.slf4j.MDC;
import java.util.UUID;

public final class TraceContext {
    private TraceContext() {}
    public static String newTraceId() { return UUID.randomUUID().toString().replace("-", ""); }
    public static void setTraceId(String traceId) { MDC.put(TraceConstants.MDC_TRACE_ID, traceId); }
    public static String getTraceId() { return MDC.get(TraceConstants.MDC_TRACE_ID); }
    public static void clear() { MDC.remove(TraceConstants.MDC_TRACE_ID); }
}
