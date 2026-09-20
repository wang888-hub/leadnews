package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.BehaviorProperties;
import com.aaliyun.leadnews.behavior.mapper.BehaviorMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class ConsumedEventCleanupService {
 private final BehaviorMapper db;
 private final BehaviorProperties properties;
 public ConsumedEventCleanupService(BehaviorMapper db, BehaviorProperties properties) {
  this.db=db; this.properties=properties;
 }
 @Scheduled(cron="${leadnews.behavior.consumed-event-cleanup-cron:0 0 3 * * *}")
 public void cleanup() {
  LocalDateTime cutoff=LocalDateTime.now().minusDays(properties.getConsumedEventRetentionDays());
  for (int batch=0; batch<10; batch++) {
   if (db.purgeConsumed(cutoff,500)<500) break;
  }
 }
}
