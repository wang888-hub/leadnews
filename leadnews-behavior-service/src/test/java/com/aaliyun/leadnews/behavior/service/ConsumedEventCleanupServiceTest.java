package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.BehaviorProperties;
import com.aaliyun.leadnews.behavior.mapper.BehaviorMapper;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ConsumedEventCleanupServiceTest {
 @Test void usesRetentionCutoffAndBoundedBatches() {
  BehaviorMapper db=mock(BehaviorMapper.class);
  BehaviorProperties p=new BehaviorProperties();
  p.setConsumedEventRetentionDays(30);
  when(db.purgeConsumed(any(LocalDateTime.class),eq(500))).thenReturn(500,10);
  LocalDateTime before=LocalDateTime.now().minusDays(30);
  new ConsumedEventCleanupService(db,p).cleanup();
  var capture=org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
  verify(db,times(2)).purgeConsumed(capture.capture(),eq(500));
  assertTrue(capture.getValue().isAfter(before.minusSeconds(5)));
  assertTrue(capture.getValue().isBefore(LocalDateTime.now().minusDays(30).plusSeconds(5)));
 }
}
