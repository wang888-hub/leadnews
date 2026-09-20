package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.HotProperties;
import com.aaliyun.leadnews.common.redis.BehaviorRedisKeys;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import java.time.Duration;
import java.time.Instant;

@Service
public class HotScoreDecayService {
 private final StringRedisTemplate redis;
 private final HotProperties props;
 private final DefaultRedisScript<Long> script;
 public HotScoreDecayService(StringRedisTemplate redis,HotProperties props){
  this.redis=redis;this.props=props;
  script=new DefaultRedisScript<>();script.setLocation(new ClassPathResource("lua/hot-decay.lua"));script.setResultType(Long.class);
 }
 @Scheduled(cron="${leadnews.hot.decay-cron:0 * * * * *}")
 public void decay(){
  String lock=BehaviorRedisKeys.HOT_DECAY_LOCK_PREFIX+(Instant.now().getEpochSecond()/60);
  if(!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lock,"1",Duration.ofMinutes(2))))return;
  apply(BehaviorRedisKeys.HOT_GLOBAL);
  Set<String> channels=redis.opsForSet().members(BehaviorRedisKeys.HOT_ACTIVE_CHANNELS);
  if(channels!=null)for(String channel:channels)apply(BehaviorRedisKeys.hotChannel(Long.parseLong(channel)));
 }
 private void apply(String key){redis.execute(script,List.of(key),String.valueOf(props.getCoolingFactor()),String.valueOf(props.getMinScore()));}
}
