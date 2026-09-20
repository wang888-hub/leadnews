package com.aaliyun.leadnews.ai.support;

import com.aaliyun.leadnews.ai.config.AiProperties;
import com.aaliyun.leadnews.ai.exception.AiExceptions;
import org.springframework.stereotype.Component;
import java.net.*;import java.time.Duration;import java.util.concurrent.*;import java.util.function.Supplier;

@Component
public class AiCallExecutor implements AutoCloseable {
 private final AiProperties properties; private final Semaphore semaphore; private final ExecutorService executor=Executors.newVirtualThreadPerTaskExecutor();
 public AiCallExecutor(AiProperties properties){this.properties=properties;this.semaphore=new Semaphore(properties.maxConcurrentCalls());}
 public <T> T execute(Supplier<T> operation){
  if(!semaphore.tryAcquire()) throw new AiExceptions.CapacityExceeded();
  try {
   for(int attempt=0;;attempt++){
    Future<T> future=executor.submit(operation::get);
    try{return future.get(properties.timeout().toMillis(),TimeUnit.MILLISECONDS);}
    catch(TimeoutException e){future.cancel(true);if(attempt<properties.maxRetries()){pause(properties.retryBackoff());continue;}throw new AiExceptions.Timeout();}
    catch(InterruptedException e){Thread.currentThread().interrupt();throw new AiExceptions.ProviderUnavailable();}
    catch(ExecutionException e){Throwable cause=e.getCause();if(attempt<properties.maxRetries()&&isTransient(cause)){pause(properties.retryBackoff());continue;}throw map(cause);}
   }
  } finally {semaphore.release();}
 }
 public boolean tryAcquire(){return semaphore.tryAcquire();} public void release(){semaphore.release();}
 static boolean isTransient(Throwable error){
  if(error instanceof SocketTimeoutException||error instanceof ConnectException) return true;
  String m=String.valueOf(error.getMessage()).toLowerCase();
  return m.contains("429")||m.contains("too many requests")||m.matches(".*\\b5\\d\\d\\b.*");
 }
 private RuntimeException map(Throwable cause){if(cause instanceof RuntimeException r&&r instanceof com.aaliyun.leadnews.common.exception.BusinessException)return r;return new AiExceptions.ProviderUnavailable();}
 private void pause(Duration duration){try{Thread.sleep(duration);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AiExceptions.ProviderUnavailable();}}
 @Override public void close(){executor.close();}
}
