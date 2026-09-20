package com.aaliyun.leadnews.user.web;

import com.aaliyun.leadnews.common.api.ResponseResult;
import com.aaliyun.leadnews.common.context.UserContext;
import com.aaliyun.leadnews.common.trace.TraceContext;
import com.aaliyun.leadnews.model.foundation.PingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserPingController {
    @GetMapping("/ping")
    public ResponseResult<PingResponse> ping() {
        return ResponseResult.success(new PingResponse("leadnews-user-service", "pong", UserContext.getUserId(), TraceContext.getTraceId()));
    }
}
