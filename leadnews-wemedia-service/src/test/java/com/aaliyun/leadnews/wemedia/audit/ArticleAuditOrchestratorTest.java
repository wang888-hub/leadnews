package com.aaliyun.leadnews.wemedia.audit;

import com.aaliyun.leadnews.feign.ai.AiAuditInternalClient;
import com.aaliyun.leadnews.model.ai.*;
import com.aaliyun.leadnews.wemedia.config.AuditProperties;
import com.aaliyun.leadnews.wemedia.config.AuditResilienceProperties;
import com.aaliyun.leadnews.wemedia.domain.WmNews;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;

class ArticleAuditOrchestratorTest {

 AuditTaskCoordinator c = mock(AuditTaskCoordinator.class);
 SensitiveWordMatcher d = mock(SensitiveWordMatcher.class); SensitiveWordRegistry words=mock(SensitiveWordRegistry.class);
 AuditRequestFactory f = mock(AuditRequestFactory.class);
 AiAuditInternalClient ai = mock(AiAuditInternalClient.class);
 AuditDecisionPolicy policy = mock(AuditDecisionPolicy.class);
 AuditPublicationService pub = mock(AuditPublicationService.class);
 AuditMetrics metrics = mock(AuditMetrics.class);
 AiAuditResultValidator validator=mock(AiAuditResultValidator.class); AuditAiGuard guard=mock(AuditAiGuard.class);

 ArticleAuditOrchestrator o;

 WmNewsAuditRequestedEvent e =
         new WmNewsAuditRequestedEvent("event", 1L, 1, Instant.now(), "trace");

 ArticleAuditRequest req = new ArticleAuditRequest(
         1L, 1, "title", "label", "text", List.of()
 );
 AuditTaskCoordinator.AuditExecution execution =
         new AuditTaskCoordinator.AuditExecution(e, 9L, 1);

 @BeforeEach
 void setup() {
  var p = new AuditProperties(
          true,
          new AuditProperties.Ai("qwen3.8-max", .85, .9, 5, 20000),
          new AuditProperties.Kafka(
                  "leadnews.wemedia.audit",
                  "leadnews.wemedia.audit.DLT"
          ),
          Duration.ofSeconds(90),
          Duration.ofSeconds(30),
          Duration.ofSeconds(5),
          10,
          3
  );
  var resilience=new AuditResilienceProperties(4,Duration.ofMillis(50),5,Duration.ofSeconds(30),1,500,Duration.ofSeconds(2),Duration.ofSeconds(30),2);
  o = new ArticleAuditOrchestrator(c, words, f, ai, policy, pub, p, metrics,validator,guard,resilience);

  WmNews n = new WmNews();
  n.setId(1L);
  n.setTitle("title");
  n.setLabels("label");

  when(c.claim(e, 3)).thenReturn(Optional.of(execution));
  when(c.news(1L)).thenReturn(n);
  when(f.create(n)).thenReturn(req);
  when(words.snapshot()).thenReturn(new SensitiveWordRegistry.Snapshot(7,d));
  when(d.match(anyString())).thenReturn(SensitiveWordMatchResult.none());
  when(guard.call(any())).thenAnswer(x->((java.util.function.Supplier<?>)x.getArgument(0)).get());
 }

 @Test
 void dfaHitNeverCallsAi() {
  when(d.match(anyString()))
          .thenReturn(new SensitiveWordMatchResult(true, List.of("风险词")));

  o.process(e);

  verify(ai, never()).auditArticle(any());
  verify(c).completeDfa(execution, List.of("风险词"),7);
 }

 @Test
 void dfaMissCallsAiAndPassReusesPublication() {
  var result = new ArticleAuditResult(
          ModerationDecision.PASS, RiskLevel.LOW, "ok", .95, List.of()
  );
  var response = new ArticleAuditResponse(
          result, "qwen3.8-max", "request", 10, null
  );

  when(ai.auditArticle(req)).thenReturn(response);
  when(policy.decide(result)).thenReturn(AuditDecisionPolicy.Outcome.APPROVE);
  when(c.completeAi(execution, response, AuditDecisionPolicy.Outcome.APPROVE,7))
          .thenReturn(true);

  o.process(e);

  verify(ai).auditArticle(req);
  verify(pub).publish(1L);
 }

 @Test
 void providerOrParseOrImageFailureRoutesManual() {
  when(ai.auditArticle(req))
          .thenThrow(new RuntimeException("provider unavailable"));

  o.process(e);

  verify(c).failAttempt(eq(execution), anyString(), anyString(), eq(3), eq(Duration.ofSeconds(5)));
  verify(pub, never()).publish(any());
 }

 @Test
 void duplicateEventDoesNotCallAiTwice() {
  when(c.claim(e, 3)).thenReturn(Optional.empty());

  o.process(e);

  verifyNoInteractions(ai);
 }
}
