package com.aaliyun.leadnews.wemedia.audit;
import com.aaliyun.leadnews.wemedia.config.AuditResilienceProperties;
import org.springframework.stereotype.Component;
import java.time.*;import java.util.concurrent.*;import java.util.function.Supplier;
/** Small in-process bulkhead/circuit breaker; Kafka/AuditTask remains the durable queue. */
@Component
public class AuditAiGuard {
 enum State{CLOSED,OPEN,HALF_OPEN}
 private final AuditResilienceProperties p; private final Semaphore permits; private int failures; private Instant openedAt; private int probes;
 public AuditAiGuard(AuditResilienceProperties p){this.p=p;permits=new Semaphore(p.maxConcurrent());}
 public <T>T call(Supplier<T> call){
  enterCircuit(); boolean acquired=false;
  try{acquired=permits.tryAcquire(p.acquireTimeout().toMillis(),TimeUnit.MILLISECONDS);if(!acquired)throw new AiAuditExceptions.CapacityBusy();
   T value=call.get();success();return value;
  }catch(InterruptedException e){Thread.currentThread().interrupt();throw new AiAuditExceptions.CapacityBusy();}
  catch(AiAuditExceptions.CapacityBusy|AiAuditExceptions.InvalidResponse e){throw e;}
  catch(RuntimeException e){String m=String.valueOf(e.getMessage()).toLowerCase();if(m.contains("50312")||m.contains("capacity exceeded"))throw new AiAuditExceptions.CapacityBusy();if(m.contains("50210")||m.contains("structured output is invalid"))throw new AiAuditExceptions.InvalidResponse("Provider returned invalid structured output");failure();throw e;} finally{if(acquired)permits.release();exitProbe();}
 }
 synchronized State state(){if(openedAt==null)return State.CLOSED;if(Instant.now().isBefore(openedAt.plus(p.openDuration())))return State.OPEN;return State.HALF_OPEN;}
 private synchronized void enterCircuit(){State s=state();if(s==State.OPEN)throw new AiAuditExceptions.CircuitOpen();if(s==State.HALF_OPEN){if(probes>=p.halfOpenPermits())throw new AiAuditExceptions.CircuitOpen();probes++;}}
 private synchronized void success(){failures=0;openedAt=null;probes=0;}
 private synchronized void failure(){if(state()==State.HALF_OPEN||++failures>=p.failureThreshold()){openedAt=Instant.now();probes=0;}}
 private synchronized void exitProbe(){if(openedAt!=null&&probes>0)probes--;}
}
