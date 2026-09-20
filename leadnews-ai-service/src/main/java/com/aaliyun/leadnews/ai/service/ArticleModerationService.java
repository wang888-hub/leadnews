package com.aaliyun.leadnews.ai.service;

import com.aaliyun.leadnews.ai.client.AiModelClient;
import com.aaliyun.leadnews.ai.config.*;
import com.aaliyun.leadnews.ai.model.StructuredAiCallResult;
import com.aaliyun.leadnews.ai.support.PromptCatalog;
import com.aaliyun.leadnews.common.exception.BusinessException;
import com.aaliyun.leadnews.model.ai.*;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ArticleModerationService {

 private final AiModelClient model;
 private final PromptCatalog prompts;
 private final TrustedMediaLoader media;
 private final ArticleAuditProperties p;
 private final AiProperties ai;

 public ArticleModerationService(AiModelClient m,
                                 PromptCatalog pc,
                                 TrustedMediaLoader l,
                                 ArticleAuditProperties p,
                                 AiProperties ai) {
  model = m;
  prompts = pc;
  media = l;
  this.p = p;
  this.ai = ai;
 }

 public ArticleAuditResponse audit(ArticleAuditRequest r) {
  if (r.textContent().length() > p.maxTextLength()) {
   throw new BusinessException(42210, "Article text exceeds safe automatic audit limit");
  }
  String user = render(
          prompts.load("article-audit-user"),
          Map.of(
                  "title", r.title(),
                  "labels", r.labels() == null ? "" : r.labels(),
                  "textContent", r.textContent()));
  long textBytes=user.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
  if(textBytes>p.maxRequestBytes())throw new BusinessException(42213,"Audit request text exceeds configured request size");
  StructuredAiCallResult<ArticleAuditResult> result;
  if (r.imageObjectKeys().isEmpty()) {
   result = model.structuredChatResult(
           prompts.load("article-audit-system"),
           user,
           ArticleAuditResult.class);
  } else {
   var loaded=media.load(r.imageObjectKeys());
   long binary=loaded.stream().mapToLong(x->x.bytes().length).sum();
   long estimated=textBytes+4*((binary+2)/3);
   if(estimated>p.maxRequestBytes())throw new BusinessException(42213,"Audit multimodal request exceeds configured request size");
   result = model.structuredVision(
           prompts.load("article-audit-system"),
           user,
           loaded,
           ArticleAuditResult.class);
  }
  return new ArticleAuditResponse(
          result.value(),
          ai.model(),
          result.requestId(),
          result.latencyMs(),
          result.usage());
 }

 private String render(String template, Map<String, String> values) {
  String out = template;
  for (var e : values.entrySet()) {
   out = out.replace("{" + e.getKey() + "}", e.getValue());
  }
  return out;
 }
}
