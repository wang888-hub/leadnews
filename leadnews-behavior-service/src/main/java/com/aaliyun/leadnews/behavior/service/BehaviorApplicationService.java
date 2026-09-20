package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.mapper.BehaviorMapper;
import com.aaliyun.leadnews.common.api.CommonErrorCode;
import com.aaliyun.leadnews.common.context.UserContext;
import com.aaliyun.leadnews.common.exception.BusinessException;
import com.aaliyun.leadnews.common.redis.BehaviorRedisKeys;
import com.aaliyun.leadnews.common.trace.TraceContext;
import com.aaliyun.leadnews.feign.article.ArticleInternalClient;
import com.aaliyun.leadnews.model.hot.ArticleBatchRequest;
import com.aaliyun.leadnews.model.behavior.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class BehaviorApplicationService {

 private final StringRedisTemplate redis;
 private final DefaultRedisScript<List> likeScript, unlikeScript;
 private final DefaultRedisScript<Long> viewScript, initializeCounterScript;
 private final ObjectMapper json;
 private final BehaviorEventPublisher publisher;
 private final BehaviorMapper db;
 private final ArticleInternalClient articles;
 private final com.aaliyun.leadnews.behavior.config.BehaviorProperties behaviorProperties;
 private final DefaultRedisScript<Long> initializeLikeScript;

 public BehaviorApplicationService(StringRedisTemplate r,
                                   DefaultRedisScript<List> likeScript,
                                   DefaultRedisScript<List> unlikeScript,
                                   DefaultRedisScript<Long> viewScript,
                                   DefaultRedisScript<Long> initializeCounterScript,
                                   DefaultRedisScript<Long> initializeLikeScript,
                                   ObjectMapper j,
                                   BehaviorEventPublisher p,
                                   BehaviorMapper db,
                                   ArticleInternalClient articles,
                                   com.aaliyun.leadnews.behavior.config.BehaviorProperties behaviorProperties) {
  redis = r;
  this.likeScript = likeScript;
  this.unlikeScript = unlikeScript;
  this.viewScript = viewScript;
  this.initializeCounterScript = initializeCounterScript;
  this.initializeLikeScript = initializeLikeScript;
  json = j;
  publisher = p;
  this.db = db;
  this.articles = articles;
  this.behaviorProperties=behaviorProperties;
 }

 public BehaviorResult like(long articleId) {
  return changeLike(articleId, true);
 }

 public BehaviorResult unlike(long articleId) {
  return changeLike(articleId, false);
 }

 public CollectResult collect(long articleId){return changeCollect(articleId,true);}
 public CollectResult uncollect(long articleId){return changeCollect(articleId,false);}

 private CollectResult changeCollect(long articleId,boolean collected){
  long user=requireUser();
  try{
   ensureCounter(articleId);
   ensureCollect(articleId,user);
   long channel=channelId(articleId);
   String eventId=UUID.randomUUID().toString();
   Instant occurredAt=Instant.now();
   List<?> result=redis.execute(collected?likeScript:unlikeScript,
    List.of(BehaviorRedisKeys.collectState(articleId),BehaviorRedisKeys.collectCooldown(articleId),
     BehaviorRedisKeys.articleCounter(articleId),BehaviorRedisKeys.PENDING_EVENTS,
     BehaviorRedisKeys.collectVersion(articleId,user),"collect:legacy:none",
     BehaviorRedisKeys.COLLECT_COOLDOWN_ARTICLES,BehaviorRedisKeys.collectCountVersion(articleId)),
    String.valueOf(user),eventId,String.valueOf(articleId),occurredAt.toString(),
    TraceContext.getTraceId()==null?"":TraceContext.getTraceId(),String.valueOf(channel),
    String.valueOf(occurredAt.toEpochMilli()),String.valueOf(behaviorProperties.getCollectCooldown().toMillis()),
    "COLLECT",String.valueOf(behaviorProperties.getCollectFirstHeat()),
    String.valueOf(behaviorProperties.getCollectRepeatHeat()),String.valueOf(behaviorProperties.getCollectCancelHeat()));
   if(result==null)throw unavailable();
   boolean changed=((Number)result.get(0)).longValue()==1;
   boolean resultCollected=((Number)result.get(1)).longValue()==1;
   int delta=((Number)result.get(2)).intValue();
   long count=((Number)result.get(3)).longValue();
   long version=((Number)result.get(4)).longValue();
   double heatDelta=((Number)result.get(5)).doubleValue();
   long countVersion=((Number)result.get(6)).longValue();
   if(changed)publisher.send(new BehaviorEvent(eventId,articleId,user,channel,BehaviorType.COLLECT,
    delta,occurredAt,TraceContext.getTraceId(),null,null,version,resultCollected,count,countVersion,heatDelta));
   return new CollectResult(resultCollected,count,changed);
  }catch(DataAccessException e){throw unavailable();}
 }

 private BehaviorResult changeLike(long articleId, boolean liked) {
  long user = requireUser();
  try {
   ensureCounter(articleId);
   ensureLike(articleId, user);
   long channelId = channelId(articleId);
   String eventId = UUID.randomUUID().toString();
   Instant occurredAt = Instant.now();
   List<?> result = redis.execute(
           liked ? likeScript : unlikeScript,
           List.of(
                   BehaviorRedisKeys.likeState(articleId),
                   BehaviorRedisKeys.likeCooldown(articleId),
                   BehaviorRedisKeys.articleCounter(articleId),
                   BehaviorRedisKeys.PENDING_EVENTS,
                   BehaviorRedisKeys.likeVersion(articleId, user),
                   BehaviorRedisKeys.articleLikes(articleId),
                   BehaviorRedisKeys.LIKE_COOLDOWN_ARTICLES,
                   BehaviorRedisKeys.likeCountVersion(articleId)
           ),
           user + "",
           eventId,
           articleId + "",
           occurredAt.toString(),
           TraceContext.getTraceId() == null ? "" : TraceContext.getTraceId(),
           String.valueOf(channelId),
           String.valueOf(occurredAt.toEpochMilli()),
           String.valueOf(behaviorProperties.getLikeCooldown().toMillis()),
           "LIKE",
           String.valueOf(behaviorProperties.getLikeFirstHeat()),
           String.valueOf(behaviorProperties.getLikeRepeatHeat()),
           String.valueOf(behaviorProperties.getLikeCancelHeat())
   );
   if (result == null) throw unavailable();
   boolean changed = ((Number) result.get(0)).longValue() == 1;
   boolean resultLiked = ((Number) result.get(1)).longValue() == 1;
   int delta = ((Number) result.get(2)).intValue();
   long count = ((Number) result.get(3)).longValue();
   long version = ((Number) result.get(4)).longValue();
   double heatDelta=((Number)result.get(5)).doubleValue();
   long countVersion=((Number)result.get(6)).longValue();
   if (changed) publisher.send(new BehaviorEvent(eventId, articleId, user, channelId, BehaviorType.LIKE,
           delta, occurredAt, TraceContext.getTraceId(), resultLiked, count, version,null,null,countVersion,heatDelta));
   return new BehaviorResult(
           resultLiked,
           count,
           counter(articleId, "viewCount"),
           changed
   );
  } catch (DataAccessException e) {
   throw unavailable();
  }
 }

 public BehaviorResult view(long articleId) {
  long user = requireUser();
  try {
   ensureCounter(articleId);
   BehaviorEvent event = event(articleId, user, BehaviorType.VIEW, 1);
   Long count = redis.execute(
           viewScript,
           List.of(
                   BehaviorRedisKeys.articleCounter(articleId),
                   BehaviorRedisKeys.PENDING_EVENTS
           ),
           event.eventId(),
           json.writeValueAsString(event)
   );
   if (count == null) throw unavailable();
   publisher.send(event);
   return new BehaviorResult(
           isLiked(articleId, user),
           counter(articleId, "likeCount"),
           count,
           true
   );
  } catch (DataAccessException e) {
   throw unavailable();
  } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
   throw new IllegalStateException(e);
  }
 }

 public ArticleBehaviorView detail(long articleId) {
  Long user = UserContext.getUserId();
  try {
   ensureCounter(articleId);
   if (user != null) ensureLike(articleId, user);
   return new ArticleBehaviorView(
           counter(articleId, "likeCount"),
           counter(articleId, "viewCount"),
           counter(articleId, "commentCount"),
           counter(articleId, "collectCount"),
           user != null && isLiked(articleId, user),
           true
   );
  } catch (DataAccessException e) {
   Map<String, Object> s = db.stat(articleId);
   return new ArticleBehaviorView(
           n(s, "likeCount"),
           n(s, "viewCount"),
           n(s, "commentCount"),
           n(s, "collectCount"),
           user != null && Objects.equals(db.likeStatus(articleId, user), 1),
           false
   );
  }
 }

 public void ensureCounter(long id) {
  String key = BehaviorRedisKeys.articleCounter(id);
  if (Boolean.TRUE.equals(redis.hasKey(key))
      && Boolean.TRUE.equals(redis.hasKey(BehaviorRedisKeys.likeCountVersion(id)))
      && Boolean.TRUE.equals(redis.hasKey(BehaviorRedisKeys.collectCountVersion(id)))) return;
  Map<String, Object> s = db.stat(id);
  redis.execute(
          initializeCounterScript,
          List.of(key,BehaviorRedisKeys.likeCountVersion(id),BehaviorRedisKeys.collectCountVersion(id)),
          n(s, "likeCount") + "",
          n(s, "viewCount") + "",
          n(s, "commentCount") + "",
          n(s, "collectCount") + "",
          n(s, "likeCountVersion") + "",
          n(s, "collectCountVersion") + ""
  );
 }

 private void ensureLike(long articleId, long userId) {
  Map<String, Object> relation = db.likeRelation(articleId, userId);
  int status = relation != null && ((Number) relation.get("status")).intValue() == 1 ? 1 : 0;
  long version = relation == null ? 0 : ((Number) relation.get("relationVersion")).longValue();
  redis.execute(
          initializeLikeScript,
          List.of(
                  BehaviorRedisKeys.likeState(articleId),
                  BehaviorRedisKeys.likeInitialized(articleId, userId),
                  BehaviorRedisKeys.likeVersion(articleId, userId),
                  BehaviorRedisKeys.articleLikes(articleId)
          ),
          userId + "",
          status + "",
          version + ""
  );
 }

 private void ensureCollect(long articleId,long userId){
  Map<String,Object> relation=db.collectRelation(articleId,userId);
  int status=relation!=null && ((Number)relation.get("status")).intValue()==1?1:0;
  long version=relation==null?0:((Number)relation.get("relationVersion")).longValue();
  redis.execute(initializeLikeScript,List.of(BehaviorRedisKeys.collectState(articleId),
   BehaviorRedisKeys.collectInitialized(articleId,userId),BehaviorRedisKeys.collectVersion(articleId,userId),
   "collect:legacy:none"),String.valueOf(userId),String.valueOf(status),String.valueOf(version));
 }

 private long n(Map<String, Object> m, String k) {
  return m == null || m.get(k) == null ? 0 : ((Number) m.get(k)).longValue();
 }

 private long counter(long id, String f) {
  Object v = redis.opsForHash().get(BehaviorRedisKeys.articleCounter(id), f);
  return v == null ? 0 : Long.parseLong(v.toString());
 }

 private boolean isLiked(long a, long u) {
  return "1".equals(redis.opsForHash().get(BehaviorRedisKeys.likeState(a),u+""));
 }

 private long requireUser() {
  Long u = UserContext.getUserId();
  if (u == null || !UserContext.isType("APP_USER"))
   throw new BusinessException(CommonErrorCode.AUTH_UNAUTHORIZED);
  return u;
 }

 private BehaviorEvent event(long a, long u, BehaviorType t, int d) {
  return new BehaviorEvent(
          UUID.randomUUID().toString(),
          a,
          u,
          channelId(a),
          t,
          d,
          Instant.now(),
          TraceContext.getTraceId()
  );
 }

 private long channelId(long articleId) {
  Object cached = redis.opsForHash().get(BehaviorRedisKeys.hotMeta(articleId), "channelId");
  if (cached != null) return Long.parseLong(cached.toString());
  var found = articles.batch(new ArticleBatchRequest(List.of(articleId)));
  if (found.isEmpty() || found.getFirst().channelId() == null) throw unavailable();
  long channelId = found.getFirst().channelId();
  redis.opsForHash().put(BehaviorRedisKeys.hotMeta(articleId), "channelId", String.valueOf(channelId));
  return channelId;
 }

 private BusinessException unavailable() {
  return new BusinessException(50300, "Behavior service temporarily unavailable");
 }
}
