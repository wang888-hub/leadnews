package com.aaliyun.leadnews.behavior.service;
import com.aaliyun.leadnews.behavior.config.BehaviorProperties;import com.aaliyun.leadnews.common.redis.BehaviorRedisKeys;import com.aaliyun.leadnews.model.behavior.*;import com.fasterxml.jackson.databind.ObjectMapper;import org.slf4j.*;import org.springframework.data.redis.core.StringRedisTemplate;import org.springframework.kafka.core.KafkaTemplate;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Service;import java.util.*;
@Service public class BehaviorEventPublisher {private static final Logger log=LoggerFactory.getLogger(BehaviorEventPublisher.class);private final KafkaTemplate<String,BehaviorEvent> kafka;private final StringRedisTemplate redis;private final ObjectMapper json;private final BehaviorProperties p;public BehaviorEventPublisher(KafkaTemplate<String,BehaviorEvent>k,StringRedisTemplate r,ObjectMapper j,BehaviorProperties p){kafka=k;redis=r;json=j;this.p=p;}
 public void send(BehaviorEvent e){
  try {
   Object payload=redis.opsForHash().get(BehaviorRedisKeys.PENDING_EVENTS,e.eventId());
   if(payload==null)return;
   BehaviorEvent original=json.readValue(payload.toString(),BehaviorEvent.class);
   if(!e.eventId().equals(original.eventId()))throw new IllegalStateException("Pending event id mismatch");
   String key=original.behaviorType()==BehaviorType.VIEW?original.articleId().toString():original.articleId()+":"+original.userId();
   kafka.send(p.getTopic(),key,original).whenComplete((ok,error)->{
    if(error==null){try{redis.opsForHash().delete(BehaviorRedisKeys.PENDING_EVENTS,original.eventId());}catch(Exception x){log.warn("Kafka ACK succeeded; pending delete deferred eventId={}",original.eventId(),x);}}
    else log.warn("Behavior event remains pending eventId={} articleId={}",original.eventId(),original.articleId(),error);
   });
  }catch(Exception ex){log.warn("Behavior event remains pending eventId={}",e.eventId(),ex);}
 }
 @Scheduled(fixedDelayString="${leadnews.behavior.pending-retry-millis:5000}") public void resendPending(){Map<Object,Object> entries=redis.opsForHash().entries(BehaviorRedisKeys.PENDING_EVENTS);entries.values().stream().limit(100).forEach(v->{try{send(json.readValue(v.toString(),BehaviorEvent.class));}catch(Exception e){log.error("Invalid pending behavior event retained",e);}});}
}
