package com.aaliyun.leadnews.article.web;

import com.aaliyun.leadnews.common.api.ResponseResult;
import com.aaliyun.leadnews.common.context.UserContext;
import com.aaliyun.leadnews.common.trace.TraceContext;
import com.aaliyun.leadnews.feign.user.UserFeignClient;
import com.aaliyun.leadnews.model.foundation.ArticlePingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/article")
public class ArticlePingController {
    private final UserFeignClient userClient;
    public ArticlePingController(UserFeignClient userClient) { this.userClient = userClient; }
    @GetMapping("/ping")
    public ResponseResult<ArticlePingResponse> ping() {
        return ResponseResult.success(new ArticlePingResponse("leadnews-article-service", UserContext.getUserId(),
                TraceContext.getTraceId(), userClient.ping()));
    }
}
