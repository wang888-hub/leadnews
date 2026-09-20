package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.common.redis.BehaviorRedisKeys;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class ReactionCooldownCleaner {
 private final StringRedisTemplate redis;
 private final DefaultRedisScript<Long> script;
 public ReactionCooldownCleaner(StringRedisTemplate redis){
  this.redis=redis;script=new DefaultRedisScript<>();
  script.setLocation(new ClassPathResource("lua/reaction-cooldown-clean.lua"));script.setResultType(Long.class);
 }
 @Scheduled(fixedDelayString="${leadnews.behavior.cooldown-clean-millis:60000}")
 public void clean(){
  long now=Instant.now().toEpochMilli();
  cleanKind(BehaviorRedisKeys.LIKE_COOLDOWN_ARTICLES,true,now);
  cleanKind(BehaviorRedisKeys.COLLECT_COOLDOWN_ARTICLES,false,now);
 }
 private void cleanKind(String index,boolean like,long now){
  var due=redis.opsForZSet().rangeByScore(index,Double.NEGATIVE_INFINITY,now,0,100);
  if(due==null)return;
  for(String raw:due){
   long article=Long.parseLong(raw);
   redis.execute(script,List.of(like?BehaviorRedisKeys.likeState(article):BehaviorRedisKeys.collectState(article),
    like?BehaviorRedisKeys.likeCooldown(article):BehaviorRedisKeys.collectCooldown(article),index),
    raw,String.valueOf(now),"100");
  }
 }
}
