package com.aaliyun.leadnews.wemedia.audit;

import com.aaliyun.leadnews.feign.ai.AiAuditInternalClient;
import com.aaliyun.leadnews.model.ai.*;
import com.aaliyun.leadnews.wemedia.config.AuditProperties;
import com.aaliyun.leadnews.wemedia.config.AuditResilienceProperties;
import com.aaliyun.leadnews.wemedia.domain.WmNews;
import org.slf4j.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ArticleAuditOrchestrator {

 private static final Logger log = LoggerFactory.getLogger(ArticleAuditOrchestrator.class);

 private final AuditTaskCoordinator coordinator;
 private final SensitiveWordRegistry words;
 private final AuditRequestFactory requests;
 private final AiAuditInternalClient ai;
 private final AuditDecisionPolicy policy;
 private final AuditPublicationService publication;
 private final AuditProperties p;
 private final AuditMetrics metrics;
 private final AiAuditResultValidator validator;
 private final AuditAiGuard guard;
 private final AuditResilienceProperties resilience;

 public ArticleAuditOrchestrator(AuditTaskCoordinator c,
                                 SensitiveWordRegistry words,
                                 AuditRequestFactory r,
                                 AiAuditInternalClient a,
                                 AuditDecisionPolicy po,
                                 AuditPublicationService pub,
                                 AuditProperties p,
                                 AuditMetrics metrics,AiAuditResultValidator validator,
                                 AuditAiGuard guard,AuditResilienceProperties resilience) {
  coordinator = c;
  this.words = words;
  requests = r;
  ai = a;
  policy = po;
  publication = pub;
  this.p = p;
  this.metrics = metrics;
  this.validator=validator;this.guard=guard;this.resilience=resilience;
 }

 @KafkaListener(
         topics = "${audit.kafka.topic:leadnews.wemedia.audit}",
         groupId = "leadnews-wemedia-audit-v1"
 )
 public void consume(WmNewsAuditRequestedEvent event) {
  process(event);
 }

 public void process(WmNewsAuditRequestedEvent event) {
  var claimed = coordinator.claim(event, p.maxAttempts());
  if (claimed.isEmpty()) {
   return;
  }
  AuditTaskCoordinator.AuditExecution execution = claimed.get();

  WmNews news = coordinator.news(event.newsId());

  try {
   ArticleAuditRequest request = requests.create(news);

   String all = news.getTitle()
           + "\n"
           + (news.getLabels() == null ? "" : news.getLabels())
           + "\n"
           + request.textContent();

   SensitiveWordRegistry.Snapshot wordSnapshot=words.snapshot();
   SensitiveWordMatchResult match = wordSnapshot.matcher().match(all);
   if (match.matched()) {
    coordinator.completeDfa(execution, match.matchedWords(),wordSnapshot.version());
    metrics.increment("audit.dfa.reject");
    return;
   }

   if (request.textContent().length() > p.ai().maxTextLength()) {
    coordinator.completeManual(execution, "TEXT_LIMIT", "正文超过自动审核安全上限");
    metrics.increment("audit.ai.review");
    return;
   }

   ArticleAuditResponse response = guard.call(()->ai.auditArticle(request));
   validator.validate(response.result());
   metrics.aiLatency(response.latencyMs());

   AuditDecisionPolicy.Outcome outcome = policy.decide(response.result());
   boolean applied = coordinator.completeAi(execution, response, outcome,wordSnapshot.version());

   metrics.increment(switch (outcome) {
    case APPROVE -> "audit.ai.pass";
    case REJECT -> "audit.ai.reject";
    case MANUAL_REVIEW -> "audit.ai.review";
   });

   if (applied && outcome == AuditDecisionPolicy.Outcome.APPROVE) {
    publication.publish(event.newsId());
   }
  } catch (AiAuditExceptions.CapacityBusy e) {
   coordinator.deferWithoutAttempt(execution,"AI_CAPACITY_BUSY",resilience.capacityRetryBackoff());
   metrics.increment("audit.ai.capacity.busy");
  } catch (AiAuditExceptions.CircuitOpen e) {
   if(isPriority(news)&&coordinator.manualBacklog()<resilience.manualReviewBacklogLimit())
    coordinator.completeManual(execution,"AI_CIRCUIT_OPEN","AI 暂不可用，高优先级稿件转人工审核");
   else coordinator.deferWithoutAttempt(execution,"AI_CIRCUIT_OPEN",resilience.circuitRetryBackoff());
   metrics.increment("audit.ai.circuit.open");
  } catch (AiAuditExceptions.InvalidResponse e) {
   if(execution.attemptNo()<resilience.invalidResponseMaxAttempts())
    coordinator.failAttempt(execution,"INVALID_AI_RESPONSE",e.getMessage(),p.maxAttempts(),p.taskRetryBackoff());
   else coordinator.completeManual(execution,"INVALID_AI_RESPONSE","AI 审核结果不满足业务约束");
   metrics.increment("audit.ai.invalid");
  } catch (RuntimeException e) {
   coordinator.failAttempt(execution, safeCode(e), "自动审核基础设施暂时不可用", p.maxAttempts(), p.taskRetryBackoff());
   metrics.increment("audit.ai.failure");
   log.warn(
           "article_audit success=false newsId={} auditVersion={} errorCode={}",
           event.newsId(),
           event.auditVersion(),
           safeCode(e)
   );
  }
 }

 private boolean isPriority(WmNews news){String labels=news.getLabels();return labels!=null&&(labels.toLowerCase().contains("high-risk")||labels.contains("高风险")||labels.contains("紧急"));}

 private String safeCode(Throwable e) {
  return e.getClass().getSimpleName().replaceAll("[^A-Za-z0-9_]", "");
 }
}
