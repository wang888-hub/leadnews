package com.aaliyun.leadnews.feign.user;

import com.aaliyun.leadnews.model.foundation.PingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "leadnews-user-service", path = "/internal/users")
public interface UserFeignClient {
    @GetMapping("/ping")
    PingResponse ping();
}
