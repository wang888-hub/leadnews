package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.HotProperties;
import com.aaliyun.leadnews.common.redis.BehaviorRedisKeys;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HotScoreRedisWriterTest {
 @Test void appliesOnceAndReportsDuplicate(){
  StringRedisTemplate redis=mock(StringRedisTemplate.class);
  when(redis.execute(any(DefaultRedisScript.class),anyList(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(1L,0L);
  var writer=new HotScoreRedisWriter(redis,new HotProperties());
  var window=new HotScoreWindowResult("19:0:5000",19,1,0,5000,10);
  assertTrue(writer.apply(window));
  assertFalse(writer.apply(window));
  verify(redis,times(2)).execute(any(DefaultRedisScript.class),anyList(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString());
 }
 @Test void redisFailureIsVisibleAndCannotAffectMysqlConsumer(){
  StringRedisTemplate redis=mock(StringRedisTemplate.class);
  when(redis.execute(any(DefaultRedisScript.class),anyList(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString())).thenThrow(new IllegalStateException("Redis down"));
  HotScoreRedisWriter writer=new HotScoreRedisWriter(redis,new HotProperties());
  assertThrows(IllegalStateException.class,()->writer.apply(new HotScoreWindowResult("19:0:5000",19,1,0,5000,3)));
 }
}
