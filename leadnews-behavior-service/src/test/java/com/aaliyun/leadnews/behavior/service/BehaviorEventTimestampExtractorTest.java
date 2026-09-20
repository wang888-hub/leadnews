package com.aaliyun.leadnews.behavior.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BehaviorEventTimestampExtractorTest {
 @Test void usesOccurredAtEvenWhenKafkaTimestampIsLater(){
  String payload="{\"eventId\":\"e\",\"articleId\":1,\"userId\":2,\"behaviorType\":\"VIEW\",\"delta\":1,\"occurredAt\":\"2026-09-17T12:00:04Z\"}";
  ConsumerRecord<Object,Object> record=mock(ConsumerRecord.class);
  when(record.value()).thenReturn(payload);
  when(record.timestamp()).thenReturn(Instant.parse("2026-09-17T12:00:20Z").toEpochMilli());
  assertThat(new BehaviorEventTimestampExtractor(new ObjectMapper().findAndRegisterModules()).extract(record,0))
   .isEqualTo(Instant.parse("2026-09-17T12:00:04Z").toEpochMilli());
 }
 @Test void malformedPayloadFallsBackToKafkaTimestamp(){
  ConsumerRecord<Object,Object> record=mock(ConsumerRecord.class);
  when(record.value()).thenReturn("invalid");
  when(record.timestamp()).thenReturn(12345L);
  assertThat(new BehaviorEventTimestampExtractor(new ObjectMapper().findAndRegisterModules()).extract(record,0)).isEqualTo(12345L);
 }
}
