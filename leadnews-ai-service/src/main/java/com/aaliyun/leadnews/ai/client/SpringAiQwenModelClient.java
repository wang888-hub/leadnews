package com.aaliyun.leadnews.ai.client;

import com.aaliyun.leadnews.ai.config.AiProperties;
import com.aaliyun.leadnews.ai.exception.AiExceptions;
import com.aaliyun.leadnews.ai.model.*;
import com.aaliyun.leadnews.ai.support.*;
import com.aaliyun.leadnews.model.ai.AiUsage;
import jakarta.validation.Validator;
import org.slf4j.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "leadnews.ai.runtime-api-key")
public class SpringAiQwenModelClient implements AiModelClient {

 private static final Logger log = LoggerFactory.getLogger(SpringAiQwenModelClient.class);

 private final ChatModel model;
 private final AiProperties p;
 private final AiCallExecutor calls;
 private final StructuredOutputParser parser;
 private final ImageInputValidator images;
 private final String apiKey;

 public SpringAiQwenModelClient(ChatModel model,
                                AiProperties p,
                                AiCallExecutor calls,
                                StructuredOutputParser parser,
                                ImageInputValidator images,
                                @Value("${leadnews.ai.runtime-api-key:}") String apiKey) {
  this.model = model;
  this.p = p;
  this.calls = calls;
  this.parser = parser;
  this.images = images;
  this.apiKey = apiKey;
 }

 public AiCallResult chat(String system, String user) {
  configured();
  long start = System.nanoTime();
  ChatResponse r = calls.execute(() -> model.call(prompt(system, new UserMessage(user))));
  return result(r, start, "chat", user.length(), 0);
 }

 public <T> T structuredChat(String system, String user, Class<T> type) {
  return structuredChatResult(system, user, type).value();
 }

 public <T> StructuredAiCallResult<T> structuredChatResult(String system, String user, Class<T> type) {
  BeanOutputConverter<T> c = new BeanOutputConverter<>(type);
  String instruction = system.replace("{format}", c.getFormat());
  AiCallResult r = chat(instruction, user);
  return new StructuredAiCallResult<>(
          parser.parse(r.content(), type),
          r.requestId(),
          r.latencyMs(),
          r.usage()
  );
 }

 public <T> StructuredAiCallResult<T> structuredVision(String system,
                                                       String user,
                                                       List<AiImageInput> inputs,
                                                       Class<T> type) {
  configured();
  inputs.forEach(images::validate);
  BeanOutputConverter<T> c = new BeanOutputConverter<>(type);
  String instruction = system.replace("{format}", c.getFormat());
  Media[] media = inputs.stream()
          .map(i -> i.bytes() != null
                  ? new Media(MimeTypeUtils.parseMimeType(i.mimeType()), new ByteArrayResource(i.bytes()))
                  : new Media(MimeTypeUtils.parseMimeType(i.mimeType()), i.url()))
          .toArray(Media[]::new);
  long start = System.nanoTime();
  ChatResponse response = calls.execute(() -> model.call(
          prompt(instruction, UserMessage.builder().text(user).media(media).build())
  ));
  AiCallResult r = result(response, start, "article-audit-vision", user.length(), inputs.size());
  return new StructuredAiCallResult<>(
          parser.parse(r.content(), type),
          r.requestId(),
          r.latencyMs(),
          r.usage()
  );
 }

 public AiCallResult vision(String system, String user, AiImageInput image) {
  configured();
  images.validate(image);
  Media media = image.bytes() != null
          ? new Media(MimeTypeUtils.parseMimeType(image.mimeType()), new ByteArrayResource(image.bytes()))
          : new Media(MimeTypeUtils.parseMimeType(image.mimeType()), image.url());
  long start = System.nanoTime();
  ChatResponse r = calls.execute(() -> model.call(
          prompt(system, UserMessage.builder().text(user).media(media).build())
  ));
  return result(r, start, "vision", user.length(), 1);
 }

 public Flux<String> stream(String system, String user) {
  configured();
  return Flux.defer(() -> {
   if (!calls.tryAcquire()) {
    return Flux.error(new AiExceptions.CapacityExceeded());
   }
   long start = System.nanoTime();
   AtomicLong first = new AtomicLong();
   return model.stream(prompt(system, new UserMessage(user)))
           .map(x -> x.getResult().getOutput().getText())
           .filter(x -> x != null && !x.isEmpty())
           .doOnNext(x -> first.compareAndSet(0, System.nanoTime()))
           .timeout(p.streamTimeout())
           .doOnError(e -> log.info(
                   "ai_call model={} capability=stream success=false errorCode={} promptLength={}",
                   p.model(), e.getClass().getSimpleName(), user.length()))
           .doFinally(s -> {
            log.info(
                    "ai_call model={} capability=stream success={} firstTokenMs={} totalMs={} promptLength={}",
                    p.model(),
                    s.toString(),
                    first.get() == 0 ? null : (first.get() - start) / 1_000_000,
                    (System.nanoTime() - start) / 1_000_000,
                    user.length());
            calls.release();
           });
  });
 }

 private Prompt prompt(String system, UserMessage user) {
  return new Prompt(List.of(new SystemMessage(system), user));
 }

 private AiCallResult result(ChatResponse r, long start, String capability, int promptLength, int imageCount) {
  var md = r.getMetadata();
  var usage = md.getUsage();
  AiUsage u = usage == null
          ? null
          : new AiUsage(
          toInt(usage.getPromptTokens()),
          toInt(usage.getCompletionTokens()),
          toInt(usage.getTotalTokens()));
  long latency = (System.nanoTime() - start) / 1_000_000;
  String id = md.getId();
  log.info(
          "ai_call model={} capability={} latencyMs={} success=true requestId={} promptLength={} imageCount={} totalTokens={}",
          p.model(), capability, latency, id, promptLength, imageCount,
          u == null ? null : u.totalTokens());
  return new AiCallResult(r.getResult().getOutput().getText(), id, latency, u);
 }

 private Integer toInt(Number n) {
  return n == null ? null : n.intValue();
 }

 private void configured() {
  if (apiKey == null || apiKey.isBlank()) {
   throw new AiExceptions.NotConfigured();
  }
 }
}