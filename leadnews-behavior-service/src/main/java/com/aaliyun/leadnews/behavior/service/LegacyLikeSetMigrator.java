package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.common.redis.BehaviorRedisKeys;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

/** Bounded, low-frequency migration; new writes use only the state hash. */
@Service
public class LegacyLikeSetMigrator {
 private final StringRedisTemplate redis;
 private final DefaultRedisScript<Long> script;
 public LegacyLikeSetMigrator(StringRedisTemplate redis){
  this.redis=redis;script=new DefaultRedisScript<>();
  script.setLocation(new ClassPathResource("lua/migrate-like-member.lua"));script.setResultType(Long.class);
 }
 @Scheduled(fixedDelayString="${leadnews.behavior.legacy-like-migrate-millis:600000}")
 public void migrate(){
  int moved=0;
  try(var keys=redis.scan(ScanOptions.scanOptions().match("article:like:user:*").count(100).build())){
   while(keys.hasNext() && moved<10000){
    String oldKey=keys.next();
    long article=Long.parseLong(oldKey.substring("article:like:user:".length()));
    try(var members=redis.opsForSet().scan(oldKey,ScanOptions.scanOptions().count(100).build())){
     while(members.hasNext() && moved<10000){
      redis.execute(script,List.of(oldKey,BehaviorRedisKeys.likeState(article)),members.next());
      moved++;
     }
    }
   }
  }
 }
}
