package com.aaliyun.leadnews.feign.ai;

import com.aaliyun.leadnews.model.ai.*;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "leadnews-ai-service",
        contextId = "aiAuditInternalClient",
        path = "/internal/ai"
)
public interface AiAuditInternalClient {
 @PostMapping("/article-audit")
 ArticleAuditResponse auditArticle(@Valid @RequestBody ArticleAuditRequest request);
}