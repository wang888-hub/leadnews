package com.aaliyun.leadnews.wemedia.service;

import com.aaliyun.leadnews.common.exception.BusinessException;
import com.aaliyun.leadnews.common.trace.TraceConstants;
import com.aaliyun.leadnews.model.ai.ArticleContinuationRequest;
import com.aaliyun.leadnews.wemedia.config.ContinuationProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.*;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;

@Service
public class ArticleContinuationService {

 private final WemediaService news;
 private final ContinuationProperties p;
 private final WebClient client;
 private final MeterRegistry metrics;

 public ArticleContinuationService(WemediaService n,
                                   ContinuationProperties p,
                                   WebClient.Builder b,
                                   MeterRegistry m) {
  news = n;
  this.p = p;
  client = b.baseUrl(p.aiBaseUrl()).build();
  metrics = m;
 }

 public Flux<ServerSentEvent<Map<String, Object>>> stream(
         Long id,
         String instruction,
         int targetLength,
         String currentContent) {

  String title = news.continuationTitle(id);
  validate(instruction, targetLength, currentContent);

  String requestId = UUID.randomUUID().toString();
  long start = System.nanoTime();
  AtomicBoolean first = new AtomicBoolean();
  AtomicInteger chunks = new AtomicInteger();

  ArticleContinuationRequest req = new ArticleContinuationRequest(
          title, currentContent, instruction, targetLength
  );

  Flux<String> upstream = client.post()
          .uri("/internal/ai/article-continuation")
          .header(TraceConstants.INTERNAL_REQUEST_HEADER, "true")
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.TEXT_EVENT_STREAM)
          .bodyValue(req)
          .retrieve()
          .bodyToFlux(String.class);

  Flux<ServerSentEvent<Map<String, Object>>> body = upstream
          .filter(s -> s != null && !s.isEmpty())
          .map(s -> {
           if (first.compareAndSet(false, true)) {
            metrics.timer("ai.continuation.first-token")
                    .record(Duration.ofNanos(System.nanoTime() - start));
           }
           chunks.incrementAndGet();
           return event("chunk", Map.of("text", plain(s)));
          });

  return Flux.concat(
                  Flux.just(event("meta", Map.of(
                          "requestId", requestId,
                          "model", "qwen3.8-max"
                  ))),
                  body,
                  Flux.defer(() -> Flux.just(event("done", Map.of(
                          "finishReason", "STOP",
                          "chunkCount", chunks.get()
                  ))))
          )
          .onErrorResume(e -> Flux.just(event("error", Map.of(
                  "code", 50320,
                  "message", "AI continuation is temporarily unavailable"
          ))));
 }

 private void validate(String i, int target, String content) {
  if (i == null || i.isBlank()
          || content == null || content.isBlank()
          || i.length() > p.maxInstructionLength()
          || content.length() > p.maxInputLength()
          || target < p.minTargetLength()
          || target > p.maxTargetLength()) {
   throw new BusinessException(40000, "Continuation input exceeds limits");
  }
 }

 private String plain(String s) {
  return s.replace("<script", "&lt;script")
          .replace("</script>", "&lt;/script&gt;");
 }

 private ServerSentEvent<Map<String, Object>> event(
         String name,
         Map<String, Object> data) {
  return ServerSentEvent.<Map<String, Object>>builder()
          .event(name)
          .data(data)
          .build();
 }
}