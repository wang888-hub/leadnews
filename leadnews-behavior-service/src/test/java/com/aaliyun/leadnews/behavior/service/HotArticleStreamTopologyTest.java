package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.*;
import com.aaliyun.leadnews.model.behavior.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HotArticleStreamTopologyTest {
 private static final Instant START=Instant.parse("2026-09-17T12:00:00Z");
 private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
 private BehaviorEvent event(String id,long article,BehaviorType type,int delta,double seconds){
  Double heat=(type==BehaviorType.LIKE || type==BehaviorType.UNLIKE)?(delta>0?3D:-1D):null;
  return new BehaviorEvent(id,article,1L,3L,type,delta,START.plusMillis((long)(seconds*1000)),"test",
   null,null,null,null,null,null,heat);
 }
 private void pipe(TestInputTopic<String,String> input,BehaviorEvent event,Instant kafkaTime)throws Exception{
  input.pipeInput(String.valueOf(event.articleId()),json.writeValueAsString(event),kafkaTime);
 }
 private TopologyTestDriver driver(HotScoreRedisWriter writer,HotArticleStreamTopology topology){
  StreamsBuilder builder=new StreamsBuilder();
  topology.hotScoreStream(builder,json,new BehaviorProperties(),new HotProperties(),writer,new HotScoreDeltaCalculator());
  Properties properties=new Properties();
  properties.put(StreamsConfig.APPLICATION_ID_CONFIG,"hot-score-test-v2");
  properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,"dummy:9092");
  return new TopologyTestDriver(builder.build(),properties);
 }
 @Test void aggregatesOneFinalDeltaPerArticleAndKeepsArticlesSeparate()throws Exception{
  HotScoreRedisWriter writer=mock(HotScoreRedisWriter.class);
  try(var driver=driver(writer,new HotArticleStreamTopology())){
   var in=driver.createInputTopic("leadnews.behavior.events",new StringSerializer(),new StringSerializer());
   pipe(in,event("view",1001,BehaviorType.VIEW,1,1),START.plusSeconds(1));
   pipe(in,event("like",1001,BehaviorType.LIKE,1,2),START.plusSeconds(2));
   pipe(in,event("comment",1001,BehaviorType.COMMENT,1,3),START.plusSeconds(3));
   pipe(in,event("other",1002,BehaviorType.VIEW,1,4),START.plusSeconds(4));
   verifyNoInteractions(writer);
   assertThat(driver.<String,HotScoreAggregate>getWindowStore(HotArticleStreamTopology.STORE).fetch("1001",START.toEpochMilli())).isEqualTo(new HotScoreAggregate(3L,9D));
   pipe(in,event("advance",9999,BehaviorType.VIEW,1,16),START.plusSeconds(16));
   verify(writer,times(1)).apply(HotScoreWindowResult.of(1001,new HotScoreAggregate(3L,9D),START.toEpochMilli(),START.plusSeconds(5).toEpochMilli()));
   verify(writer,times(1)).apply(HotScoreWindowResult.of(1002,new HotScoreAggregate(3L,1D),START.toEpochMilli(),START.plusSeconds(5).toEpochMilli()));
  }
 }
 @Test void eventTimeControlsGraceAndTooLateEventIsDropped()throws Exception{
  HotScoreRedisWriter writer=mock(HotScoreRedisWriter.class);
  try(var driver=driver(writer,new HotArticleStreamTopology())){
   var in=driver.createInputTopic("leadnews.behavior.events",new StringSerializer(),new StringSerializer());
   pipe(in,event("first",1001,BehaviorType.VIEW,1,1),START.plusSeconds(1));
   pipe(in,event("move",1002,BehaviorType.VIEW,1,8),START.plusSeconds(8));
   pipe(in,event("delayed",1001,BehaviorType.LIKE,1,4),START.plusSeconds(20));
   assertThat(driver.<String,HotScoreAggregate>getWindowStore(HotArticleStreamTopology.STORE).fetch("1001",START.toEpochMilli())).isEqualTo(new HotScoreAggregate(3L,4D));
   pipe(in,event("close",1002,BehaviorType.VIEW,1,16),START.plusSeconds(16));
   verify(writer,times(1)).apply(HotScoreWindowResult.of(1001,new HotScoreAggregate(3L,4D),START.toEpochMilli(),START.plusSeconds(5).toEpochMilli()));
   pipe(in,event("too-late",1001,BehaviorType.COMMENT,1,3),START.plusSeconds(21));
   verify(writer,times(1)).apply(HotScoreWindowResult.of(1001,new HotScoreAggregate(3L,4D),START.toEpochMilli(),START.plusSeconds(5).toEpochMilli()));
  }
 }
 @Test void toggleHasNegativeFeedback(){
  HotProperties p=new HotProperties();
  assertThat(new HotScoreDeltaCalculator().calculate(event("like",1,BehaviorType.LIKE,1,1),p)).isEqualTo(3D);
  assertThat(new HotScoreDeltaCalculator().calculate(event("unlike",1,BehaviorType.LIKE,-1,2),p)).isEqualTo(-1D);
 }
 @Test void streamsUsesEventHeatDeltaNotBehaviorTypeGuess(){
  var p=new HotProperties();
  var custom=new BehaviorEvent("repeat",1L,2L,3L,BehaviorType.LIKE,1,START,"t",
   true,1L,1L,1D);
  assertThat(new HotScoreDeltaCalculator().calculate(custom,p)).isEqualTo(1D);
 }
 @Test void windowEventIdIsDeterministic(){
  var aggregate=new HotScoreAggregate(3L,9D);
  var first=HotScoreWindowResult.of(1001,aggregate,START.toEpochMilli(),START.plusSeconds(5).toEpochMilli());
  assertThat(first.windowEventId()).isEqualTo("1001:"+START.toEpochMilli()+":"+START.plusSeconds(5).toEpochMilli());
  assertThat(HotScoreWindowResult.of(1001,aggregate,START.toEpochMilli(),START.plusSeconds(5).toEpochMilli()))
   .isEqualTo(first);
 }
 @Test void aggregateSerdeRoundTripsForChangelogRecovery(){
  var serde=new JsonSerde<>(HotScoreAggregate.class,json);
  var expected=new HotScoreAggregate(3L,9D);
  byte[] bytes=serde.serializer().serialize("hot-score-window-v3",expected);
  assertThat(serde.deserializer().deserialize("hot-score-window-v3",bytes)).isEqualTo(expected);
 }
}
