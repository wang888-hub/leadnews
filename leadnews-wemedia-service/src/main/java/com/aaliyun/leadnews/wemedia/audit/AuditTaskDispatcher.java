package com.aaliyun.leadnews.wemedia.audit;
import com.aaliyun.leadnews.common.trace.TraceContext;
import com.aaliyun.leadnews.model.ai.WmNewsAuditRequestedEvent;
import com.aaliyun.leadnews.wemedia.config.AuditProperties;
import com.aaliyun.leadnews.wemedia.domain.AuditTask;
import org.slf4j.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.*;
@Service public class AuditTaskDispatcher {
 private static final Logger log=LoggerFactory.getLogger(AuditTaskDispatcher.class);
 private final AuditTaskCoordinator coordinator;private final KafkaTemplate<String,WmNewsAuditRequestedEvent> kafka;private final AuditProperties p;
 public AuditTaskDispatcher(AuditTaskCoordinator c,KafkaTemplate<String,WmNewsAuditRequestedEvent> k,AuditProperties p){coordinator=c;kafka=k;this.p=p;}
 @Scheduled(fixedDelayString="${audit.dispatch-interval-ms:1000}") public void dispatch(){
  if(!p.enabled())return;
  for(AuditTask t:coordinator.dispatchable(p.dispatchBatchSize(),p.maxAttempts())){
   var event=new WmNewsAuditRequestedEvent(t.getEventId(),t.getNewsId(),t.getAuditVersion(),Instant.now(),TraceContext.getTraceId());
   kafka.send(p.kafka().topic(),String.valueOf(t.getNewsId()),event).whenComplete((r,e)->{
    if(e==null)coordinator.markSent(t.getId());
    else{coordinator.markSendFailure(t.getId(),e.getClass().getSimpleName(),LocalDateTime.now().plusSeconds(5));log.warn("audit_dispatch success=false taskId={} errorCode={}",t.getId(),e.getClass().getSimpleName());}
   });
  }
 }
 @Scheduled(fixedDelayString="${audit.recovery-scan-interval:PT30S}") public void recover(){
  coordinator.recover(LocalDateTime.now().minus(p.runningTimeout()),p.maxAttempts(),p.dispatchBatchSize());
 }
}
