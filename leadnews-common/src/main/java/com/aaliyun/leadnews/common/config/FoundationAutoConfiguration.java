package com.aaliyun.leadnews.common.config;

import com.aaliyun.leadnews.common.trace.TraceConstants;
import com.aaliyun.leadnews.common.trace.TraceContext;
import com.aaliyun.leadnews.common.context.UserContext;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class FoundationAutoConfiguration {
    @Bean @ConditionalOnClass(RequestInterceptor.class)
    RequestInterceptor traceFeignRequestInterceptor() {
        return template -> {
            String traceId = TraceContext.getTraceId();
            if (traceId != null) template.header(TraceConstants.TRACE_ID_HEADER, traceId);
            Long userId = UserContext.getUserId();
            if (userId != null) template.header(TraceConstants.USER_ID_HEADER, userId.toString());
            String userType = UserContext.getUserType();
            if (userType != null) template.header(TraceConstants.USER_TYPE_HEADER, userType);
            template.header(TraceConstants.INTERNAL_REQUEST_HEADER, "true");
        };
    }
}
