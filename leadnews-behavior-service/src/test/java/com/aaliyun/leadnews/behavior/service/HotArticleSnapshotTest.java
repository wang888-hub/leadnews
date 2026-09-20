package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.*;
import com.aaliyun.leadnews.behavior.mapper.BehaviorMapper;
import com.aaliyun.leadnews.common.redis.BehaviorRedisKeys;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.*;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HotArticleSnapshotTest {
 @Test void snapshotStoresScoreAndAllBusinessCounts(){
  BehaviorMapper db=mock(BehaviorMapper.class);StringRedisTemplate redis=mock(StringRedisTemplate.class);
  ZSetOperations<String,String> zset=mock(ZSetOperations.class);SetOperations<String,String> sets=mock(SetOperations.class);
  HashOperations<String,Object,Object> hashes=mock(HashOperations.class);
  when(redis.opsForZSet()).thenReturn(zset);when(redis.opsForSet()).thenReturn(sets);when(redis.opsForHash()).thenReturn(hashes);
  when(zset.reverseRangeWithScores(BehaviorRedisKeys.HOT_GLOBAL,0,49)).thenReturn(Set.of(new DefaultTypedTuple<>("19",12.5)));
  when(sets.members(BehaviorRedisKeys.HOT_ACTIVE_CHANNELS)).thenReturn(Set.of());
  when(hashes.entries(BehaviorRedisKeys.hotMeta(19))).thenReturn(Map.of("channelId","3"));
  when(db.stat(19)).thenReturn(Map.of("likeCount",7L,"collectCount",5L,"commentCount",2L,"viewCount",100L));
  when(db.purgeHotSnapshots(any(),eq(1000))).thenReturn(0);
  HotProperties hot=new HotProperties();
  new HotArticleRebuildService(db,redis,hot,new HotScoreRecoveryCalculator(new BehaviorProperties(),hot)).checkpoint();
  verify(db).insertHotSnapshot(anyString(),eq(19L),eq(3L),eq(12.5),eq(7L),eq(5L),eq(2L),eq(100L),any());
  verify(db).completeHotSnapshot(anyString(),eq(1));
 }
}
