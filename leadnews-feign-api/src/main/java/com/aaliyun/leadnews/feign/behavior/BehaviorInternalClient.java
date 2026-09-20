package com.aaliyun.leadnews.feign.behavior;

import com.aaliyun.leadnews.model.behavior.ArticleBehaviorView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "leadnews-behavior-service", contextId = "behaviorInternalClient")
public interface BehaviorInternalClient {
    @GetMapping("/internal/behavior/articles/{articleId}")
    ArticleBehaviorView articleBehavior(@PathVariable("articleId") Long articleId);
}
