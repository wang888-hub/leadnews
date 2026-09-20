package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.HotProperties;
import com.aaliyun.leadnews.behavior.mapper.BehaviorMapper;
import com.aaliyun.leadnews.common.redis.BehaviorRedisKeys;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.*;import java.time.temporal.ChronoUnit;import java.util.*;

@Service
public class HotArticleRebuildService {
 private static final Logger log=LoggerFactory.getLogger(HotArticleRebuildService.class);
 private final BehaviorMapper db;private final StringRedisTemplate redis;private final HotProperties props;
 private final HotScoreRecoveryCalculator calculator;
 private final DefaultRedisScript<Long> begin=new DefaultRedisScript<>(),switchScript=new DefaultRedisScript<>(),abort=new DefaultRedisScript<>();
 public HotArticleRebuildService(BehaviorMapper db,StringRedisTemplate redis,HotProperties props,HotScoreRecoveryCalculator calculator){
  this.db=db;this.redis=redis;this.props=props;this.calculator=calculator;
  load(begin,"lua/hot-recovery-begin.lua");load(switchScript,"lua/hot-recovery-switch.lua");load(abort,"lua/hot-recovery-abort.lua");
 }
 private void load(DefaultRedisScript<Long>s,String path){s.setLocation(new ClassPathResource(path));s.setResultType(Long.class);}

 @Scheduled(fixedDelayString="${leadnews.hot.snapshot-interval:PT30M}")
 public void checkpoint(){
  if(!props.isSnapshotEnabled())return;
  String snapshotId=UUID.randomUUID().toString();LocalDateTime now=LocalDateTime.now();db.startHotSnapshot(snapshotId,now);
  Map<Long,Double> scores=new LinkedHashMap<>();
  Set<ZSetOperations.TypedTuple<String>> global=redis.opsForZSet().reverseRangeWithScores(BehaviorRedisKeys.HOT_GLOBAL,0,props.getTopN()-1);
  if(global!=null)for(var t:global)if(t.getValue()!=null&&t.getScore()!=null)scores.put(Long.valueOf(t.getValue()),t.getScore());
  Set<String> activeChannels=redis.opsForSet().members(BehaviorRedisKeys.HOT_ACTIVE_CHANNELS);
  if(activeChannels!=null)for(String channel:activeChannels){
   Set<ZSetOperations.TypedTuple<String>> rank=redis.opsForZSet().reverseRangeWithScores(BehaviorRedisKeys.hotChannel(Long.parseLong(channel)),0,props.getTopN()-1);
   if(rank!=null)for(var t:rank)if(t.getValue()!=null&&t.getScore()!=null)scores.putIfAbsent(Long.valueOf(t.getValue()),t.getScore());
  }
  int saved=0;
  for(var entry:scores.entrySet()){
   long article=entry.getKey();Map<Object,Object> meta=redis.opsForHash().entries(BehaviorRedisKeys.hotMeta(article));
   if(meta.get("channelId")==null)continue;Map<String,Object> stat=db.stat(article);if(stat==null)continue;
   db.insertHotSnapshot(snapshotId,article,Long.parseLong(meta.get("channelId").toString()),entry.getValue(),
    n(stat,"likeCount"),n(stat,"collectCount"),n(stat,"commentCount"),n(stat,"viewCount"),now);saved++;
  }
  db.completeHotSnapshot(snapshotId,saved);
  LocalDateTime cutoff=now.minusDays(props.getSnapshotRetentionDays());
  while(db.purgeHotSnapshots(cutoff,1000)>0){} db.purgeHotSnapshotBatches(cutoff,100);
 }

 /** Approximate recovery of an empty rank from the latest complete snapshot. */
 public int rebuild(){
  String recoveryId=UUID.randomUUID().toString();
  Long acquired=redis.execute(begin,List.of(BehaviorRedisKeys.HOT_GLOBAL,BehaviorRedisKeys.HOT_RECOVERY_FLAG),recoveryId,"600000");
  if(!Long.valueOf(1).equals(acquired))return 0;
  try{
   String snapshotId=db.latestHotSnapshotId();if(snapshotId==null){abort(recoveryId);return 0;}
   Set<Long> channels=new LinkedHashSet<>();int restored=0;long after=0;
   while(true){
    List<Map<String,Object>> page=db.hotSnapshotPage(snapshotId,after,200);if(page.isEmpty())break;
    for(Map<String,Object> row:page){
     after=n(row,"id");long article=n(row,"articleId"),channel=n(row,"channelId");Map<String,Object> current=db.stat(article);if(current==null)continue;
     LocalDateTime snapshotTime=(LocalDateTime)row.get("snapshotTime");
     double minutes=Math.max(0,ChronoUnit.MILLIS.between(snapshotTime,LocalDateTime.now())/60000D);
     double estimated=calculator.estimatedDelta(n(current,"likeCount")-n(row,"likeCount"),n(current,"collectCount")-n(row,"collectCount"),
      n(current,"commentCount")-n(row,"commentCount"),n(current,"viewCount")-n(row,"viewCount"));
     double score=calculator.recover(d(row,"hotScore"),estimated,minutes);if(score<props.getMinScore())continue;
     redis.opsForZSet().add(BehaviorRedisKeys.hotRecoveryTempGlobal(recoveryId),String.valueOf(article),score);
     redis.opsForZSet().add(BehaviorRedisKeys.hotRecoveryTempChannel(recoveryId,channel),String.valueOf(article),score);
     redis.expire(BehaviorRedisKeys.hotRecoveryTempGlobal(recoveryId),Duration.ofMinutes(15));
     redis.expire(BehaviorRedisKeys.hotRecoveryTempChannel(recoveryId,channel),Duration.ofMinutes(15));
     redis.opsForHash().put(BehaviorRedisKeys.hotMeta(article),"channelId",String.valueOf(channel));channels.add(channel);restored++;
    }
    if(page.size()<200)break;
   }
   Set<String> active=redis.opsForSet().members(BehaviorRedisKeys.HOT_ACTIVE_CHANNELS);
   if(active!=null)for(String channel:active)channels.add(Long.parseLong(channel));
   List<String> keys=new ArrayList<>();keys.add(BehaviorRedisKeys.HOT_RECOVERY_FLAG);
   triplet(keys,BehaviorRedisKeys.HOT_GLOBAL,BehaviorRedisKeys.hotRecoveryTempGlobal(recoveryId),BehaviorRedisKeys.hotRecoveryDeltaGlobal(recoveryId));
   for(long channel:channels)triplet(keys,BehaviorRedisKeys.hotChannel(channel),BehaviorRedisKeys.hotRecoveryTempChannel(recoveryId,channel),BehaviorRedisKeys.hotRecoveryDeltaChannel(recoveryId,channel));
   Long switched=redis.execute(switchScript,keys,recoveryId,String.valueOf(props.getTopN()),String.valueOf(props.getMinScore()));
   if(!Long.valueOf(1).equals(switched))throw new IllegalStateException("Hot recovery switch failed");
   if(!channels.isEmpty())redis.opsForSet().add(BehaviorRedisKeys.HOT_ACTIVE_CHANNELS,channels.stream().map(String::valueOf).toArray(String[]::new));
   return restored;
  }catch(RuntimeException e){abort(recoveryId);log.error("Hot snapshot recovery failed recoveryId={}",recoveryId,e);throw e;}
 }
 private void abort(String id){redis.execute(abort,List.of(BehaviorRedisKeys.HOT_RECOVERY_FLAG),id);}
 private void triplet(List<String>k,String official,String temp,String delta){k.add(official);k.add(temp);k.add(delta);}
 private long n(Map<?,?>m,String k){return m.get(k)==null?0:((Number)m.get(k)).longValue();}
 private double d(Map<?,?>m,String k){return ((Number)m.get(k)).doubleValue();}
 @EventListener(ApplicationReadyEvent.class)public void restoreOnStart(){rebuild();}
 @Scheduled(fixedDelayString="${leadnews.hot.restore-check-interval-ms:300000}")public void restoreIfMissing(){rebuild();}
}
