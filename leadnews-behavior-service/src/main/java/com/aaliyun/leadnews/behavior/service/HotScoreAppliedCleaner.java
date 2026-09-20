package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.HotProperties;
import com.aaliyun.leadnews.common.redis.BehaviorRedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class HotScoreAppliedCleaner {
 private final StringRedisTemplate redis;
 private final HotProperties props;
 public HotScoreAppliedCleaner(StringRedisTemplate redis,HotProperties props){this.redis=redis;this.props=props;}
 @Scheduled(cron="${leadnews.hot.applied-cleanup-cron:0 15 * * * *}")
 public void clean(){
  long cutoff=Instant.now().minus(props.getAppliedRetention()).toEpochMilli();
  redis.opsForZSet().removeRangeByScore(BehaviorRedisKeys.HOT_APPLIED,0,cutoff);
 }
}
