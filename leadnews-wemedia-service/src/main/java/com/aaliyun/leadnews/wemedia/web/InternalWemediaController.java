package com.aaliyun.leadnews.wemedia.web;
import com.aaliyun.leadnews.feign.wemedia.WemediaAuditClient;import com.aaliyun.leadnews.model.foundation.*;import com.aaliyun.leadnews.wemedia.service.WemediaService;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/internal/wemedia/news") public class InternalWemediaController implements WemediaAuditClient {
 private final WemediaService service;public InternalWemediaController(WemediaService s){service=s;}
 @Override @GetMapping public PageResponse<WmNewsSnapshot> pending(@RequestParam(defaultValue="1")long page,@RequestParam(defaultValue="10")long size){return service.pending(page,size);}
 @Override @GetMapping("/{id}") public WmNewsSnapshot detail(@PathVariable Long id){return service.auditDetail(id);}
 @Override @PostMapping("/{id}/approve") public WmNewsSnapshot approve(@PathVariable Long id){return service.approve(id);}
 @Override @PostMapping("/{id}/reject") public WmNewsSnapshot reject(@PathVariable Long id,@RequestParam String reason){return service.reject(id,reason);}
 @Override @PostMapping("/{id}/publish-status")public void publishStatus(@PathVariable Long id,@RequestParam String status,@RequestParam Long articleId){service.publishStatus(id,status,articleId);}
}
