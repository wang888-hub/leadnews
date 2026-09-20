package com.aaliyun.leadnews.common.web;

import com.aaliyun.leadnews.common.context.UserContext;
import com.aaliyun.leadnews.common.trace.TraceConstants;
import com.aaliyun.leadnews.common.trace.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class RequestContextInterceptor implements HandlerInterceptor {
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(TraceConstants.TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) traceId = TraceContext.newTraceId();
        TraceContext.setTraceId(traceId);
        response.setHeader(TraceConstants.TRACE_ID_HEADER, traceId);
        String userId = request.getHeader(TraceConstants.USER_ID_HEADER);
        if (userId != null && userId.matches("\\d+")) UserContext.setUserId(Long.valueOf(userId));
        String userType = request.getHeader(TraceConstants.USER_TYPE_HEADER);
        if (userType != null) UserContext.setUserType(userType);
        UserContext.setInternal("true".equals(request.getHeader(TraceConstants.INTERNAL_REQUEST_HEADER)));
        return true;
    }
    @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear(); TraceContext.clear();
    }
}
