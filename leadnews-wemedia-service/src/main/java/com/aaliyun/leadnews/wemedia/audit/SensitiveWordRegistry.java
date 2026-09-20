package com.aaliyun.leadnews.wemedia.audit;
import com.aaliyun.leadnews.wemedia.mapper.SensitiveWordMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.*;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.*;

/** Immutable DFA snapshots; readers never observe a tree while it is being built. */
@Component
public class SensitiveWordRegistry implements MessageListener {
 public static final String CHANNEL="leadnews:audit:sensitive-words:changed";
 private static final Logger log=LoggerFactory.getLogger(SensitiveWordRegistry.class);
 private final SensitiveWordMapper mapper;
 private final AtomicReference<SensitiveWordMatcher> matcher=new AtomicReference<>(new SensitiveWordMatcher(java.util.List.of()));
 private final AtomicLong version=new AtomicLong();
 public SensitiveWordRegistry(SensitiveWordMapper mapper){this.mapper=mapper;}
 @PostConstruct public void initialize(){reloadIfNewer(mapper.version());}
 public Snapshot snapshot(){return new Snapshot(version.get(),matcher.get());}
 @Scheduled(fixedDelayString="${audit.sensitive-words.version-check-interval:PT30S}")
 public void reconcile(){reloadIfNewer(mapper.version());}
 @Override public void onMessage(Message message,byte[] pattern){try{reloadIfNewer(Long.parseLong(message.toString()));}catch(RuntimeException e){log.warn("Sensitive word notification ignored; periodic reconciliation remains active",e);}}
 synchronized void reloadIfNewer(long remote){if(remote<=version.get())return;var next=new SensitiveWordMatcher(mapper.enabledWords());matcher.set(next);version.set(remote);log.info("Sensitive word DFA switched to version={}",remote);}
 public record Snapshot(long version,SensitiveWordMatcher matcher){}
}
