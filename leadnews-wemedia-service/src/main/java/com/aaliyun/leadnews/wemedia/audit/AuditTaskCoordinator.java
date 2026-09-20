package com.aaliyun.leadnews.wemedia.audit;

import com.aaliyun.leadnews.model.ai.*;
import com.aaliyun.leadnews.wemedia.domain.*;
import com.aaliyun.leadnews.wemedia.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class AuditTaskCoordinator {
 public record AuditExecution(WmNewsAuditRequestedEvent event,long taskId,int attemptNo){}
 private final AuditTaskMapper tasks;private final WmNewsMapper news;
 private final WmNewsAuditRecordMapper records;private final ObjectMapper json;
 public AuditTaskCoordinator(AuditTaskMapper t,WmNewsMapper n,WmNewsAuditRecordMapper r,ObjectMapper j){tasks=t;news=n;records=r;json=j;}

 /** One SQL CAS grants ownership; attemptNo is the fencing token for every later write. */
 @Transactional public Optional<AuditExecution> claim(WmNewsAuditRequestedEvent e,int maxAttempts){
  if(e==null||e.eventId()==null||e.newsId()==null)return Optional.empty();
  if(tasks.claim(e.eventId(),e.newsId(),e.auditVersion(),maxAttempts,LocalDateTime.now())!=1)return Optional.empty();
  AuditTask task=tasks.findByEventId(e.eventId());
  if(task==null)throw new IllegalStateException("Claimed audit task disappeared");
  AuditExecution x=new AuditExecution(e,task.getId(),task.getAttemptNo());
  WmNews current=news.selectById(e.newsId());
  if(current==null||!Objects.equals(current.getAuditVersion(),e.auditVersion())||current.getStatus()!=WmNewsStatus.AUDITING){markStale(x,"Article audit version or state changed");return Optional.empty();}
  return Optional.of(x);
 }

 @Transactional public boolean completeDfa(AuditExecution x,List<String> matched){
  String reason="命中本地敏感词规则";
  return complete(x,WmNewsStatus.REJECTED,"DFA_REJECTED",reason,"SUCCESS",record(x,"DFA","REJECT",null,reason,null,List.of(),matched,null,null,null,null,null));
 }
 @Transactional public boolean completeDfa(AuditExecution x,List<String> matched,long wordVersion){
  String reason="命中本地敏感词规则";var r=record(x,"DFA","REJECT",null,reason,null,List.of(),matched,null,null,null,null,null);r.setSensitiveWordVersion(wordVersion);
  return complete(x,WmNewsStatus.REJECTED,"DFA_REJECTED",reason,"SUCCESS",r);
 }
 @Transactional public boolean completeAi(AuditExecution x,ArticleAuditResponse response,AuditDecisionPolicy.Outcome outcome){
  ArticleAuditResult result=response.result();
  WmNewsStatus target=switch(outcome){case APPROVE->WmNewsStatus.APPROVED;case REJECT->WmNewsStatus.REJECTED;case MANUAL_REVIEW->WmNewsStatus.MANUAL_REVIEW;};
  String source=switch(outcome){case APPROVE->"AI_AUTO_APPROVED";case REJECT->"AI_AUTO_REJECTED";case MANUAL_REVIEW->"AI_REVIEW";};
  return complete(x,target,source,result.reason(),"SUCCESS",record(x,"AI",result.decision().name(),result.riskLevel().name(),result.reason(),result.confidence(),result.riskTags(),List.of(),response.model(),response.providerRequestId(),response.latencyMs(),null,null));
 }
 @Transactional public boolean completeAi(AuditExecution x,ArticleAuditResponse response,AuditDecisionPolicy.Outcome outcome,long wordVersion){
  ArticleAuditResult result=response.result();WmNewsStatus target=switch(outcome){case APPROVE->WmNewsStatus.APPROVED;case REJECT->WmNewsStatus.REJECTED;case MANUAL_REVIEW->WmNewsStatus.MANUAL_REVIEW;};
  String source=switch(outcome){case APPROVE->"AI_AUTO_APPROVED";case REJECT->"AI_AUTO_REJECTED";case MANUAL_REVIEW->"AI_REVIEW";};
  var r=record(x,"AI",result.decision().name(),result.riskLevel().name(),result.reason(),result.confidence(),result.riskTags(),List.of(),response.model(),response.providerRequestId(),response.latencyMs(),null,null);r.setSensitiveWordVersion(wordVersion);
  return complete(x,target,source,result.reason(),"SUCCESS",r);
 }
 @Transactional public boolean completeManual(AuditExecution x,String code,String reason){
  return complete(x,WmNewsStatus.MANUAL_REVIEW,"AI_REVIEW",reason,"SUCCESS",record(x,"AI","REVIEW",null,reason,null,List.of(),List.of(),null,null,null,code,null));
 }
 @Transactional public boolean failAttempt(AuditExecution x,String code,String reason,int maxAttempts,Duration retryBackoff){
  if(x.attemptNo()<maxAttempts)return tasks.retryAttempt(x.taskId(),x.attemptNo(),LocalDateTime.now().plus(retryBackoff),safe(code+": "+reason))==1;
  return exhaust(x,code,reason);
 }
 @Transactional public boolean deferWithoutAttempt(AuditExecution x,String code,Duration delay){return tasks.deferWithoutAttempt(x.taskId(),x.attemptNo(),LocalDateTime.now().plus(delay),safe(code))==1;}
 public long manualBacklog(){return news.selectCount(new LambdaQueryWrapper<WmNews>().eq(WmNews::getStatus,WmNewsStatus.MANUAL_REVIEW));}

 @Transactional public int recover(LocalDateTime stale,int maxAttempts,int limit){
  List<AuditTask> zombies=tasks.selectList(new LambdaQueryWrapper<AuditTask>().eq(AuditTask::getStatus,"RUNNING").lt(AuditTask::getStartedAt,stale).orderByAsc(AuditTask::getStartedAt).last("LIMIT "+limit));
  int changed=0;
  for(AuditTask task:zombies){
   var event=new WmNewsAuditRequestedEvent(task.getEventId(),task.getNewsId(),task.getAuditVersion(),Instant.now(),null);
   var x=new AuditExecution(event,task.getId(),task.getAttemptNo());
   if(task.getAttemptNo()>=maxAttempts){if(exhaust(x,"AUTO_AUDIT_EXHAUSTED","自动审核执行超时且已达到最大尝试次数"))changed++;}
   else if(tasks.retryAttempt(task.getId(),task.getAttemptNo(),LocalDateTime.now(),"RUNNING_TIMEOUT")==1)changed++;
  }
  return changed;
 }

 public List<AuditTask> dispatchable(int limit,int maxAttempts){return tasks.selectList(new LambdaQueryWrapper<AuditTask>()
  .eq(AuditTask::getStatus,"PENDING").eq(AuditTask::getDispatchStatus,"PENDING").lt(AuditTask::getAttemptNo,maxAttempts)
  .and(w->w.isNull(AuditTask::getNextDispatchTime).or().le(AuditTask::getNextDispatchTime,LocalDateTime.now())).orderByAsc(AuditTask::getId).last("LIMIT "+limit));}
 @Transactional public void markSent(Long id){tasks.update(null,new LambdaUpdateWrapper<AuditTask>().eq(AuditTask::getId,id).eq(AuditTask::getStatus,"PENDING").eq(AuditTask::getDispatchStatus,"PENDING").set(AuditTask::getDispatchStatus,"SENT").set(AuditTask::getLastDispatchedAt,LocalDateTime.now()));}
 @Transactional public void markSendFailure(Long id,String error,LocalDateTime next){tasks.update(null,new LambdaUpdateWrapper<AuditTask>().eq(AuditTask::getId,id).eq(AuditTask::getStatus,"PENDING").eq(AuditTask::getDispatchStatus,"PENDING").setSql("dispatch_retry_count = dispatch_retry_count + 1").set(AuditTask::getLastError,safe(error)).set(AuditTask::getNextDispatchTime,next));}
 public WmNews news(Long id){return news.selectById(id);}
 public List<WmNewsAuditRecord> trail(Long id){return records.selectList(new LambdaQueryWrapper<WmNewsAuditRecord>().eq(WmNewsAuditRecord::getNewsId,id).orderByAsc(WmNewsAuditRecord::getAuditVersion).orderByAsc(WmNewsAuditRecord::getCreatedTime));}
 @Transactional public void manualRecord(Long newsId,long version,String decision,String reason,Long reviewerId){var e=new WmNewsAuditRequestedEvent("manual",newsId,version,Instant.now(),null);try{records.insert(record(new AuditExecution(e,0,0),"MANUAL",decision,null,reason,null,List.of(),List.of(),null,null,null,null,reviewerId));}catch(DuplicateKeyException ignored){}}

 private boolean exhaust(AuditExecution x,String code,String reason){return complete(x,WmNewsStatus.MANUAL_REVIEW,"AUTO_AUDIT_EXHAUSTED",reason,"FAILED",record(x,"AI","REVIEW",null,reason,null,List.of(),List.of(),null,null,null,code,null));}
 private boolean complete(AuditExecution x,WmNewsStatus target,String source,String reason,String taskStatus,WmNewsAuditRecord record){
  WmNews current=news.selectById(x.event().newsId());
  if(current==null||!Objects.equals(current.getAuditVersion(),x.event().auditVersion())||current.getStatus()!=WmNewsStatus.AUDITING){markStale(x,"Stale audit result ignored");return false;}
  int taskChanged=tasks.update(null,new LambdaUpdateWrapper<AuditTask>().eq(AuditTask::getId,x.taskId()).eq(AuditTask::getStatus,"RUNNING").eq(AuditTask::getAttemptNo,x.attemptNo()).set(AuditTask::getStatus,taskStatus).set(AuditTask::getFinishedAt,LocalDateTime.now()).set(AuditTask::getLastError,"FAILED".equals(taskStatus)?safe(reason):null));
  if(taskChanged!=1)return false;
  int articleChanged=news.update(null,new LambdaUpdateWrapper<WmNews>().eq(WmNews::getId,x.event().newsId()).eq(WmNews::getAuditVersion,x.event().auditVersion()).eq(WmNews::getStatus,WmNewsStatus.AUDITING).set(WmNews::getStatus,target).set(WmNews::getAuditSource,source).set(WmNews::getReason,reason));
  if(articleChanged!=1)throw new IllegalStateException("Article changed while completing audit");
  records.insert(record);return true;
 }
 private void markStale(AuditExecution x,String error){tasks.update(null,new LambdaUpdateWrapper<AuditTask>().eq(AuditTask::getId,x.taskId()).eq(AuditTask::getStatus,"RUNNING").eq(AuditTask::getAttemptNo,x.attemptNo()).set(AuditTask::getStatus,"STALE").set(AuditTask::getFinishedAt,LocalDateTime.now()).set(AuditTask::getLastError,safe(error)));}
 private WmNewsAuditRecord record(AuditExecution x,String stage,String decision,String risk,String reason,Double confidence,List<String> tags,List<String> matched,String model,String requestId,Long latency,String error,Long reviewer){WmNewsAuditRecord r=new WmNewsAuditRecord();r.setNewsId(x.event().newsId());r.setAuditVersion(x.event().auditVersion());r.setAttemptNo(x.attemptNo());r.setAuditStage(stage);r.setDecision(decision);r.setRiskLevel(risk);r.setReason(safe(reason));r.setConfidence(confidence);r.setRiskTags(write(tags));r.setMatchedWords(write(matched));r.setModel(model);r.setProviderRequestId(requestId);r.setLatencyMs(latency);r.setErrorCode(error);r.setReviewerId(reviewer);if(reviewer!=null)r.setReviewedTime(LocalDateTime.now());return r;}
 private String write(Object o){try{return json.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException("Cannot serialize audit record",e);}}
 private String safe(String s){if(s==null||s.isBlank())return "审核服务不可用，已转人工";return s.length()>500?s.substring(0,500):s;}
}
