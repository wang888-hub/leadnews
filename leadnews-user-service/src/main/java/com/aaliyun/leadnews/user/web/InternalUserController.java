package com.aaliyun.leadnews.user.web;

import com.aaliyun.leadnews.common.api.CommonErrorCode;
import com.aaliyun.leadnews.common.context.UserContext;
import com.aaliyun.leadnews.common.exception.BusinessException;
import com.aaliyun.leadnews.common.trace.TraceConstants;
import com.aaliyun.leadnews.common.trace.TraceContext;
import com.aaliyun.leadnews.feign.user.UserFeignClient;
import com.aaliyun.leadnews.model.foundation.PingResponse;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController implements UserFeignClient {
    private final HttpServletRequest request;

    public InternalUserController(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    @GetMapping("/ping")
    public PingResponse ping() {
        if (!"true".equals(request.getHeader(TraceConstants.INTERNAL_REQUEST_HEADER))) {
            throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
        }
        return new PingResponse("leadnews-user-service", "pong", UserContext.getUserId(), TraceContext.getTraceId());
    }
}
