package com.aaliyun.leadnews.feign.ai;import com.aaliyun.leadnews.model.ai.*;import jakarta.validation.Valid;import org.springframework.cloud.openfeign.FeignClient;import org.springframework.web.bind.annotation.*;
@FeignClient(name="leadnews-ai-service",contextId="aiCapabilityInternalClient",path="/internal/ai") public interface AiInternalClient {
 @PostMapping("/chat") AiChatResponse chat(@Valid @RequestBody AiChatRequest request);
 @PostMapping("/structured") StructuredModerationResult structured(@Valid @RequestBody AiChatRequest request);
 @PostMapping("/vision") AiChatResponse vision(@Valid @RequestBody AiVisionRequest request);
 @PostMapping("/article-summary") ArticleSummaryResponse summarize(@Valid @RequestBody ArticleSummaryRequest request);
}
