package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.BehaviorProperties;
import com.aaliyun.leadnews.behavior.config.HotProperties;
import com.aaliyun.leadnews.model.behavior.BehaviorEvent;
import com.aaliyun.leadnews.model.behavior.BehaviorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@EnableKafkaStreams
public class HotArticleStreamTopology {
 // V3 is deliberate: V2 changelog values were Double and cannot be decoded as HotScoreAggregate.
 public static final String STORE="hot-score-window-v3";
 private final AtomicLong malformedEvents=new AtomicLong();
 public long malformedEventCount(){return malformedEvents.get();}

 @Bean
 KStream<String,String> hotScoreStream(StreamsBuilder builder,ObjectMapper json,
                                      BehaviorProperties behavior,HotProperties hot,HotScoreRedisWriter writer,
                                      HotScoreDeltaCalculator calculator){
  KStream<String,String> source=builder.stream(behavior.getTopic(),
   Consumed.with(Serdes.String(),Serdes.String())
    .withTimestampExtractor(new BehaviorEventTimestampExtractor(json)));

  JsonSerde<HotScoreAggregate> aggregateSerde=new JsonSerde<>(HotScoreAggregate.class,json);
  source.map((key,value)->{
    try {
     BehaviorEvent event=json.readValue(value,BehaviorEvent.class);
     if(event.articleId()==null || event.channelId()==null || event.behaviorType()==null){
      malformedEvents.incrementAndGet();
      return org.apache.kafka.streams.KeyValue.<String,HotScoreAggregate>pair(null,null);
     }
     return org.apache.kafka.streams.KeyValue.pair(String.valueOf(event.articleId()),
      new HotScoreAggregate(event.channelId(),calculator.calculate(event,hot)));
    }catch(Exception ex){malformedEvents.incrementAndGet();return org.apache.kafka.streams.KeyValue.<String,HotScoreAggregate>pair(null,null);}
   })
   .filter((articleId,delta)->articleId!=null && delta!=null && delta.deltaScore()!=0D)
   .groupByKey(Grouped.with(Serdes.String(),aggregateSerde))
   .windowedBy(TimeWindows.ofSizeAndGrace(hot.getRealtimeWindow(),hot.getGrace()))
   .aggregate(()->new HotScoreAggregate(null,0D),(articleId,next,current)->{
     if(current.channelId()!=null && !current.channelId().equals(next.channelId()))
      throw new IllegalStateException("Article channel changed within hot window articleId="+articleId);
     return new HotScoreAggregate(next.channelId(),current.deltaScore()+next.deltaScore());
    },Materialized.<String,HotScoreAggregate,org.apache.kafka.streams.state.WindowStore<org.apache.kafka.common.utils.Bytes,byte[]>>as(STORE)
     .withKeySerde(Serdes.String()).withValueSerde(aggregateSerde))
   .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.maxRecords(10000).shutDownWhenFull()))
   .toStream()
   .foreach((windowed,score)->writer.apply(HotScoreWindowResult.of(Long.parseLong(windowed.key()),score,
     windowed.window().start(),windowed.window().end())));
  return source;
 }

}
