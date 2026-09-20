package com.aaliyun.leadnews.wemedia.audit;
import com.aaliyun.leadnews.wemedia.domain.SensitiveWord;
import com.aaliyun.leadnews.wemedia.mapper.SensitiveWordMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import java.util.*;
@Service
public class SensitiveWordDictionaryService {
 private final SensitiveWordMapper mapper;private final StringRedisTemplate redis;
 public SensitiveWordDictionaryService(SensitiveWordMapper mapper,StringRedisTemplate redis){this.mapper=mapper;this.redis=redis;}
 /** One batch is one DB version. Publication happens only after commit. */
 @Transactional public long replaceAll(Collection<String> words){
  mapper.delete(null);words.stream().map(String::trim).filter(x->!x.isBlank()).distinct().forEach(x->{var w=new SensitiveWord();w.setWord(x);w.setEnabled(true);mapper.insert(w);});
  mapper.incrementVersion();long next=mapper.version();
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){redis.convertAndSend(SensitiveWordRegistry.CHANNEL,Long.toString(next));redis.opsForValue().set("leadnews:audit:sensitive-words:version",Long.toString(next));}});
  return next;
 }
}
