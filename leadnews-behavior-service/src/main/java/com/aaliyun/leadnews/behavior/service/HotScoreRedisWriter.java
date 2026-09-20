package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.HotProperties;
import com.aaliyun.leadnews.common.redis.BehaviorRedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HotScoreRedisWriter {
 private static final Logger log=LoggerFactory.getLogger(HotScoreRedisWriter.class);
 private final StringRedisTemplate redis;
 private final HotProperties props;
 private final DefaultRedisScript<Long> script;
 public HotScoreRedisWriter(StringRedisTemplate redis,HotProperties props){
  this.redis=redis;this.props=props;
  script=new DefaultRedisScript<>();script.setLocation(new ClassPathResource("lua/hot-increment.lua"));script.setResultType(Long.class);
 }
 public boolean apply(HotScoreWindowResult result){
  try {
   Long applied=redis.execute(script,List.of(BehaviorRedisKeys.HOT_GLOBAL,
    BehaviorRedisKeys.hotChannel(result.channelId()),BehaviorRedisKeys.HOT_ACTIVE_CHANNELS,
    BehaviorRedisKeys.HOT_APPLIED,BehaviorRedisKeys.HOT_RECOVERY_FLAG),result.windowEventId(),String.valueOf(result.articleId()),
    String.valueOf(result.deltaScore()),String.valueOf(result.windowEnd()),
    String.valueOf(props.getMinScore()),String.valueOf(props.getTopN()),String.valueOf(result.channelId()));
   if(applied==null)throw new IllegalStateException("Hot increment script returned no result");
   return applied==1;
  }catch(RuntimeException e){log.error("Hot Redis increment failed windowEventId={}",result.windowEventId(),e);throw e;}
 }
}
