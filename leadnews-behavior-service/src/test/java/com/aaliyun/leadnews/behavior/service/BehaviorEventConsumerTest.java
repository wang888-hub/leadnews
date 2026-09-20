package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.mapper.BehaviorMapper;
import com.aaliyun.leadnews.model.behavior.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.Map;
import static org.mockito.Mockito.*;

class BehaviorEventConsumerTest {
 private BehaviorMapper db;
 private BehaviorEventConsumer consumer;
 @BeforeEach void setup(){db=mock(BehaviorMapper.class);consumer=new BehaviorEventConsumer(db,new ObjectMapper());}
 private BehaviorEvent like(String id,boolean liked,long version){return new BehaviorEvent(id,1L,2L,3L,BehaviorType.LIKE,liked?1:-1,Instant.now(),"t",liked,liked?1L:0L,version,null,null,version,liked?2D:-1D);}
 @Test void firstLikeChangesRelationAndOverwritesFinalCount(){BehaviorEvent e=like("first",true,1);when(db.claimConsumed(anyString(),eq("first"),anyString(),eq(1L))).thenReturn(1);when(db.likeRelation(1,2)).thenReturn(null);consumer.consume(e);verify(db).saveVersionedLike(1,2,1,1);verify(db).saveLikeCountVersioned(1,1,1);verify(db,never()).changeLikeStat(anyLong(),anyInt());}
 @Test void unlikeOverwritesFinalCount(){BehaviorEvent e=like("second",false,2);when(db.claimConsumed(anyString(),eq("second"),anyString(),eq(1L))).thenReturn(1);when(db.likeRelation(1,2)).thenReturn(Map.of("status",1,"relationVersion",1L));consumer.consume(e);verify(db).saveVersionedLike(1,2,0,2);verify(db).saveLikeCountVersioned(1,0,2);verify(db,never()).changeLikeStat(anyLong(),anyInt());}
 @Test void duplicateEventDoesNotUpdateStats(){BehaviorEvent e=like("same",true,1);consumer.consume(e);verify(db,never()).saveVersionedLike(anyLong(),anyLong(),anyInt(),anyLong());verify(db,never()).changeLikeStat(anyLong(),anyInt());}
 @Test void olderRelationCannotOverwriteNewerAndCountUsesIndependentCas(){BehaviorEvent e=like("old",true,1);when(db.claimConsumed(anyString(),eq("old"),anyString(),eq(1L))).thenReturn(1);when(db.likeRelation(1,2)).thenReturn(Map.of("status",0,"relationVersion",2L));consumer.consume(e);verify(db,never()).saveVersionedLike(anyLong(),anyLong(),anyInt(),anyLong());verify(db).saveLikeCountVersioned(1,1,1);}
 @Test void businessFailureEscapesForTransactionRollback(){BehaviorEvent e=like("fail",true,1);when(db.claimConsumed(anyString(),eq("fail"),anyString(),eq(1L))).thenReturn(1);when(db.likeRelation(1,2)).thenReturn(null);doThrow(new IllegalStateException("db failed")).when(db).saveVersionedLike(1,2,1,1);Assertions.assertThrows(IllegalStateException.class,()->consumer.consume(e));}
 @Test void unsupportedBehaviorThrowsForRetryAndDlt(){BehaviorEvent e=new BehaviorEvent("collect",1L,2L,BehaviorType.COLLECT,1,Instant.now(),"t");when(db.claimConsumed(anyString(),anyString(),anyString(),anyLong())).thenReturn(1);Assertions.assertThrows(IllegalArgumentException.class,()->consumer.consume(e));}
 @Test void collectUsesRelationVersionAndIgnoresHeatDeltaForMysqlCount(){
  BehaviorEvent e=new BehaviorEvent("collect-new",1L,2L,3L,BehaviorType.COLLECT,1,Instant.now(),"t",
   null,null,1L,true,1L,1L,5D);
  when(db.claimConsumed(anyString(),eq("collect-new"),anyString(),eq(1L))).thenReturn(1);
  when(db.collectRelation(1,2)).thenReturn(null);
  consumer.consume(e);
  verify(db).saveVersionedCollect(1,2,1,1);
  verify(db).saveCollectCountVersioned(1,1,1);
  verify(db,never()).changeCollectStat(anyLong(),anyInt());
  verify(db,never()).addStat(anyLong(),anyLong(),anyLong());
 }
}
