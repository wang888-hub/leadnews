package com.aaliyun.leadnews.feign.wemedia;

import com.aaliyun.leadnews.model.foundation.PageResponse;
import com.aaliyun.leadnews.model.foundation.WmNewsSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="leadnews-wemedia-service", path="/internal/wemedia/news")
public interface WemediaAuditClient {
    @GetMapping PageResponse<WmNewsSnapshot> pending(@RequestParam(defaultValue="1") long page, @RequestParam(defaultValue="10") long size);
    @GetMapping("/{id}") WmNewsSnapshot detail(@PathVariable Long id);
    @PostMapping("/{id}/approve") WmNewsSnapshot approve(@PathVariable Long id);
    @PostMapping("/{id}/reject") WmNewsSnapshot reject(@PathVariable Long id, @RequestParam String reason);
    @PostMapping("/{id}/publish-status") void publishStatus(@PathVariable Long id,@RequestParam String status,@RequestParam Long articleId);
}
