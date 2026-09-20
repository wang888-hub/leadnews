package com.aaliyun.leadnews.ai.service;

import com.aaliyun.leadnews.ai.client.AiModelClient;
import com.aaliyun.leadnews.ai.config.*;
import com.aaliyun.leadnews.ai.support.PromptCatalog;
import com.aaliyun.leadnews.common.exception.BusinessException;
import com.aaliyun.leadnews.model.ai.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

@Service
public class AiWritingService {

 private final AiModelClient model;
 private final PromptCatalog prompts;
 private final AiProperties ai;
 private final AiBusinessProperties p;
 private final MeterRegistry metrics;

 public AiWritingService(AiModelClient m,
                         PromptCatalog pc,
                         AiProperties a,
                         AiBusinessProperties p,
                         MeterRegistry metrics) {
  model = m;
  prompts = pc;
  ai = a;
  this.p = p;
  this.metrics = metrics;
 }

 public ArticleSummaryResponse summarize(ArticleSummaryRequest r) {
  if (r.textContent().length() > p.summary().maxInputLength()) {
   throw new BusinessException(40000, "Summary input exceeds limit");
  }
  int min = effectiveSummaryMin(r.textContent(), p.summary().minLength());
  String user = render(
          prompts.load("article-summary-user"),
          Map.of(
                  "title", r.title(),
                  "labels", r.labels() == null ? "" : r.labels(),
                  "content", r.textContent(),
                  "minLength", min,
                  "maxLength", p.summary().maxLength()));
  var result = model.chat(prompts.load("article-summary-system"), user);
  String s = clean(result.content());
  if (s.length() < min || s.length() > p.summary().maxLength()) {
   throw new BusinessException(50210, "AI summary output is invalid");
  }
  return new ArticleSummaryResponse(s, ai.model(), result.requestId(), result.latencyMs(), result.usage());
 }

 public Flux<String> continueArticle(ArticleContinuationRequest r) {
  var c = p.continuation();
  if (r.currentContent().length() > c.maxInputLength()
          || r.instruction().length() > c.maxInstructionLength()
          || r.targetLength() < c.minTargetLength()
          || r.targetLength() > c.maxTargetLength()) {
   throw new BusinessException(40000, "Continuation input exceeds limits");
  }
  String user = render(
          prompts.load("article-continuation-user"),
          Map.of(
                  "title", r.title(),
                  "content", r.currentContent(),
                  "instruction", r.instruction(),
                  "targetLength", r.targetLength()));
  long start = System.nanoTime();
  metrics.counter("ai.continuation.requests").increment();
  return model.stream(prompts.load("article-continuation-system"), user)
          .doOnCancel(() -> metrics.counter("ai.continuation.cancelled").increment())
          .doOnError(e -> metrics.counter("ai.continuation.failed").increment())
          .doFinally(x -> metrics.timer("ai.continuation.total-latency")
                  .record(Duration.ofNanos(System.nanoTime() - start)));
 }

 private String clean(String s) {
  if (s == null) {
   return "";
  }
  String v = s.trim();
  for (String x : new String[]{"好的，以下是摘要：", "以下是摘要：", "摘要："}) {
   if (v.startsWith(x)) {
    v = v.substring(x.length()).trim();
   }
  }
  return v;
 }

 public static int effectiveSummaryMin(String content, int configuredMin) {
  int codePoints = content.codePointCount(0, content.length());
  return Math.min(configuredMin, Math.max(10, codePoints / 2));
 }

 private String render(String t, Map<String, Object> v) {
  for (var e : v.entrySet()) {
   t = t.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
  }
  return t;
 }
}