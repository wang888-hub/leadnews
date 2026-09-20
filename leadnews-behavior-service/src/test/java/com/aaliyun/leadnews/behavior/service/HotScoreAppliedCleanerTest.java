package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.HotProperties;
import com.aaliyun.leadnews.common.redis.BehaviorRedisKeys;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import java.time.Instant;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HotScoreAppliedCleanerTest {
 @Test void removesOnlyWindowsOlderThanRetention(){
  StringRedisTemplate redis=mock(StringRedisTemplate.class);
  ZSetOperations<String,String> zset=mock(ZSetOperations.class);
  when(redis.opsForZSet()).thenReturn(zset);
  long before=Instant.now().minusSeconds(7*86400L).toEpochMilli();
  new HotScoreAppliedCleaner(redis,new HotProperties()).clean();
  long after=Instant.now().minusSeconds(7*86400L).toEpochMilli();
  verify(zset).removeRangeByScore(eq(BehaviorRedisKeys.HOT_APPLIED),eq(0D),doubleThat(v->v>=before && v<=after));
 }
}
