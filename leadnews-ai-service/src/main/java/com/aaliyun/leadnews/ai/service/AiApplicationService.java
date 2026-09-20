package com.aaliyun.leadnews.ai.service;

import com.aaliyun.leadnews.ai.client.AiModelClient;
import com.aaliyun.leadnews.ai.config.AiProperties;
import com.aaliyun.leadnews.ai.model.AiImageInput;
import com.aaliyun.leadnews.ai.support.PromptCatalog;
import com.aaliyun.leadnews.model.ai.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.net.URI;

@Service
public class AiApplicationService {

 private final AiModelClient client;
 private final PromptCatalog prompts;
 private final AiProperties p;

 public AiApplicationService(AiModelClient client, PromptCatalog prompts, AiProperties p) {
  this.client = client;
  this.prompts = prompts;
  this.p = p;
 }

 public AiChatResponse chat(AiChatRequest request) {
  var r = client.chat(prompts.load("chat-system"), request.prompt());
  return new AiChatResponse(p.model(), r.content(), r.requestId(), r.latencyMs(), r.usage());
 }

 public StructuredModerationResult structured(AiChatRequest request) {
  return client.structuredChat(
          prompts.load("structured-test"),
          request.prompt(),
          StructuredModerationResult.class);
 }

 public AiChatResponse vision(AiVisionRequest request) {
  URI uri = request.url() == null || request.url().isBlank()
          ? null
          : URI.create(request.url());
  var r = client.vision(
          prompts.load("chat-system"),
          prompts.load("vision-test") + "\n" + request.prompt(),
          new AiImageInput(request.mimeType(), request.bytes(), uri));
  return new AiChatResponse(p.model(), r.content(), r.requestId(), r.latencyMs(), r.usage());
 }

 public Flux<String> stream(AiChatRequest request) {
  return client.stream(prompts.load("chat-system"), request.prompt());
 }
}