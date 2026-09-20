package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.model.behavior.BehaviorEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

public class BehaviorEventTimestampExtractor implements TimestampExtractor {
 private final ObjectMapper json;
 public BehaviorEventTimestampExtractor(ObjectMapper json){this.json=json;}
 @Override public long extract(ConsumerRecord<Object,Object> record,long partitionTime){
  try {
   BehaviorEvent e=json.readValue(String.valueOf(record.value()),BehaviorEvent.class);
   if(e.occurredAt()!=null && e.occurredAt().toEpochMilli()>0)return e.occurredAt().toEpochMilli();
  }catch(Exception ignored){/* invalid event uses Kafka record timestamp */}
  return record.timestamp()>=0?record.timestamp():partitionTime;
 }
}
