package com.aaliyun.leadnews.behavior.web;

import com.aaliyun.leadnews.behavior.service.BehaviorApplicationService;
import com.aaliyun.leadnews.common.api.ResponseResult;
import com.aaliyun.leadnews.model.behavior.BehaviorResult;
import com.aaliyun.leadnews.model.behavior.CollectResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/behavior/articles/{articleId}")
public class BehaviorController {

    private final BehaviorApplicationService service;

    public BehaviorController(BehaviorApplicationService s) {
        service = s;
    }

    @PostMapping("/like")
    public ResponseResult<BehaviorResult> like(@PathVariable long articleId) {
        return ResponseResult.success(service.like(articleId));
    }

    @DeleteMapping("/like")
    public ResponseResult<BehaviorResult> unlike(@PathVariable long articleId) {
        return ResponseResult.success(service.unlike(articleId));
    }

    @PostMapping("/view")
    public ResponseResult<BehaviorResult> view(@PathVariable long articleId) {
        return ResponseResult.success(service.view(articleId));
    }

    @PostMapping("/collect")
    public ResponseResult<CollectResult> collect(@PathVariable long articleId){
        return ResponseResult.success(service.collect(articleId));
    }

    @DeleteMapping("/collect")
    public ResponseResult<CollectResult> uncollect(@PathVariable long articleId){
        return ResponseResult.success(service.uncollect(articleId));
    }
}
