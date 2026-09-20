package com.aaliyun.leadnews.ai.controller;

import com.aaliyun.leadnews.ai.service.AiApplicationService;
import com.aaliyun.leadnews.feign.ai.AiInternalClient;
import com.aaliyun.leadnews.model.ai.*;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/internal/ai")
public class AiInternalController implements AiInternalClient {

 private final AiApplicationService service;
 private final com.aaliyun.leadnews.ai.service.ArticleModerationService moderation;
 private final com.aaliyun.leadnews.ai.service.AiWritingService writing;

 public AiInternalController(AiApplicationService service,
                             com.aaliyun.leadnews.ai.service.ArticleModerationService moderation,
                             com.aaliyun.leadnews.ai.service.AiWritingService writing) {
  this.service = service;
  this.moderation = moderation;
  this.writing = writing;
 }

 public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
  return service.chat(request);
 }

 public StructuredModerationResult structured(@Valid @RequestBody AiChatRequest request) {
  return service.structured(request);
 }

 public AiChatResponse vision(@Valid @RequestBody AiVisionRequest request) {
  return service.vision(request);
 }

 @PostMapping(value = {"/stream", "/test/stream"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
 public Flux<String> stream(@Valid @RequestBody AiChatRequest request) {
  return service.stream(request);
 }

 @PostMapping("/test/chat")
 public AiChatResponse testChat(@Valid @RequestBody AiChatRequest request) {
  return chat(request);
 }

 @PostMapping("/test/structured")
 public StructuredModerationResult testStructured(@Valid @RequestBody AiChatRequest request) {
  return structured(request);
 }

 @PostMapping("/test/vision")
 public AiChatResponse testVision(@Valid @RequestBody AiVisionRequest request) {
  return vision(request);
 }

 @PostMapping("/article-audit")
 public ArticleAuditResponse auditArticle(@Valid @RequestBody ArticleAuditRequest request) {
  return moderation.audit(request);
 }

 @PostMapping("/article-summary")
 public ArticleSummaryResponse summarize(@Valid @RequestBody ArticleSummaryRequest request) {
  return writing.summarize(request);
 }

 @PostMapping(value = "/article-continuation", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
 public Flux<String> continueArticle(@Valid @RequestBody ArticleContinuationRequest request) {
  return writing.continueArticle(request);
 }
}