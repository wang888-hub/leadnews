package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.mapper.BehaviorMapper;
import com.aaliyun.leadnews.model.behavior.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class BehaviorEventConsumer {

 private final BehaviorMapper db;
 private final ObjectMapper json;

 public BehaviorEventConsumer(BehaviorMapper db, ObjectMapper json) {
  this.db = db;
  this.json = json;
 }

 @KafkaListener(
         topics = "${leadnews.behavior.topic:leadnews.behavior.events}",
         groupId = "${spring.kafka.consumer.group-id:leadnews-behavior-persistence}"
 )
 @Transactional
 public void consume(BehaviorEvent e) {
  if (e == null || e.eventId() == null || e.articleId() == null || e.behaviorType() == null) {
   throw new IllegalArgumentException("Invalid behavior event");
  }

  if ((e.behaviorType() == BehaviorType.LIKE || e.behaviorType() == BehaviorType.UNLIKE)
          && (e.userId() == null || e.liked() == null || e.relationVersion() == null
              || e.relationVersion() <= 0 || e.likeCount() == null || e.likeCount() < 0
              || e.countVersion()==null || e.countVersion()<=0)) {
   throw new IllegalArgumentException("Invalid versioned like event");
  }
  if ((e.behaviorType()==BehaviorType.COLLECT || e.behaviorType()==BehaviorType.UNCOLLECT)
       && (e.userId()==null || e.collected()==null || e.relationVersion()==null
           || e.relationVersion()<=0 || e.collectCount()==null || e.collectCount()<0
           || e.countVersion()==null || e.countVersion()<=0))
   throw new IllegalArgumentException("Invalid versioned collect event");
  if (db.claimConsumed("leadnews-behavior-persistence", e.eventId(), e.behaviorType().name(), e.articleId()) == 0) {
   return;
  }

  db.insertEvent(e);

  switch (e.behaviorType()) {
   case LIKE, UNLIKE -> {
    db.ensureStat(e.articleId());
    Map<String,Object> current = db.likeRelation(e.articleId(), e.userId());
    long version = current == null ? 0 : ((Number)current.get("relationVersion")).longValue();
    if (e.relationVersion() > version) {
     int after = e.liked() ? 1 : 0;
     db.saveVersionedLike(e.articleId(), e.userId(), after, e.relationVersion());
    }
    db.saveLikeCountVersioned(e.articleId(),e.likeCount(),e.countVersion());
   }
   case COLLECT, UNCOLLECT -> {
    db.ensureStat(e.articleId());
    Map<String,Object> current=db.collectRelation(e.articleId(),e.userId());
    long version=current==null?0:((Number)current.get("relationVersion")).longValue();
    if(e.relationVersion()>version){
     int after=e.collected()?1:0;
     db.saveVersionedCollect(e.articleId(),e.userId(),after,e.relationVersion());
    }
    db.saveCollectCountVersioned(e.articleId(),e.collectCount(),e.countVersion());
   }
   case VIEW -> {
    /* durable event log; ViewBatchPersistenceService aggregates it */
   }
   default -> throw new IllegalArgumentException(
           "Unsupported stage-6 behavior: " + e.behaviorType()
   );
  }
 }

 @KafkaListener(
         topics = "${leadnews.behavior.dlt-topic:leadnews.behavior.events.DLT}",
         groupId = "leadnews-behavior-dlt-audit"
 )
 public void dlt(BehaviorEvent e) {
  try {
   db.failed(
           e == null ? null : e.eventId(),
           e == null ? null : e.articleId(),
           json.writeValueAsString(e),
           "Consumer retries exhausted"
   );
  } catch (Exception ex) {
   throw new IllegalStateException("Cannot persist DLT event", ex);
  }
 }
}
