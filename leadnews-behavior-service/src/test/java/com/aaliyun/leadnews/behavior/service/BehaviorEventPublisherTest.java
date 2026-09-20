package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.BehaviorProperties;
import com.aaliyun.leadnews.common.redis.BehaviorRedisKeys;
import com.aaliyun.leadnews.model.behavior.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;

class BehaviorEventPublisherTest {
 @Test void resendKeepsOriginalPayloadAndDeletesOnlyAfterAck() throws Exception {
  KafkaTemplate<String,BehaviorEvent> kafka=mock(KafkaTemplate.class);
  StringRedisTemplate redis=mock(StringRedisTemplate.class);
  HashOperations<String,Object,Object> hash=mock(HashOperations.class);
  when(redis.opsForHash()).thenReturn(hash);
  ObjectMapper json=new ObjectMapper().findAndRegisterModules();
  BehaviorEvent original=new BehaviorEvent("fixed-id",3L,4L,7L,BehaviorType.LIKE,1,
    Instant.parse("2026-09-17T12:00:00Z"),"trace",true,8L,5L,2D);
  String payload=json.writeValueAsString(original);
  when(hash.get(BehaviorRedisKeys.PENDING_EVENTS,"fixed-id")).thenReturn(payload);
  when(hash.entries(BehaviorRedisKeys.PENDING_EVENTS)).thenReturn(Map.of("fixed-id",payload));
  CompletableFuture<SendResult<String,BehaviorEvent>> future=new CompletableFuture<>();
  when(kafka.send("leadnews.behavior.events","3:4",original)).thenReturn(future);
  BehaviorEventPublisher publisher=new BehaviorEventPublisher(kafka,redis,json,new BehaviorProperties());
  publisher.resendPending();
  org.junit.jupiter.api.Assertions.assertEquals(2D,json.readValue(payload,BehaviorEvent.class).heatDelta());
  verify(hash,never()).delete(anyString(),any());
  future.complete(null);
  verify(hash).delete(BehaviorRedisKeys.PENDING_EVENTS,"fixed-id");
 }
 @Test void failedKafkaSendRetainsPending() throws Exception {
  KafkaTemplate<String,BehaviorEvent> kafka=mock(KafkaTemplate.class);
  StringRedisTemplate redis=mock(StringRedisTemplate.class);
  HashOperations<String,Object,Object> hash=mock(HashOperations.class);
  when(redis.opsForHash()).thenReturn(hash);
  ObjectMapper json=new ObjectMapper().findAndRegisterModules();
  BehaviorEvent event=new BehaviorEvent("fixed-id",3L,4L,7L,BehaviorType.LIKE,1,
    Instant.parse("2026-09-17T12:00:00Z"),"trace",true,8L,5L,1D);
  when(hash.get(BehaviorRedisKeys.PENDING_EVENTS,"fixed-id")).thenReturn(json.writeValueAsString(event));
  CompletableFuture<SendResult<String,BehaviorEvent>> failed=new CompletableFuture<>();
  failed.completeExceptionally(new IllegalStateException("Kafka unavailable"));
  when(kafka.send("leadnews.behavior.events","3:4",event)).thenReturn(failed);
  new BehaviorEventPublisher(kafka,redis,json,new BehaviorProperties()).send(event);
  verify(hash,never()).delete(anyString(),any());
 }
}
