package com.aaliyun.leadnews.wemedia.audit;
import com.aaliyun.leadnews.model.ai.*;import com.aaliyun.leadnews.wemedia.config.AuditProperties;import org.junit.jupiter.api.Test;
import java.time.Duration;import java.util.List;import static org.assertj.core.api.Assertions.assertThat;
class AuditDecisionPolicyTest {
 private final AuditDecisionPolicy p=new AuditDecisionPolicy(new AuditProperties(true,
  new AuditProperties.Ai("qwen3.8-max",.85,.90,5,20000),new AuditProperties.Kafka("aLongTopic","aVeryLongDltTopic"),
  Duration.ofSeconds(90),Duration.ofSeconds(30),Duration.ofSeconds(5),10,3));
 private ArticleAuditResult r(ModerationDecision d,double c){return new ArticleAuditResult(d,RiskLevel.LOW,"reason",c,List.of());}
 @Test void routesPassByThreshold(){assertThat(p.decide(r(ModerationDecision.PASS,.85))).isEqualTo(AuditDecisionPolicy.Outcome.APPROVE);assertThat(p.decide(r(ModerationDecision.PASS,.84))).isEqualTo(AuditDecisionPolicy.Outcome.MANUAL_REVIEW);}
 @Test void routesRejectByThreshold(){assertThat(p.decide(r(ModerationDecision.REJECT,.90))).isEqualTo(AuditDecisionPolicy.Outcome.REJECT);assertThat(p.decide(r(ModerationDecision.REJECT,.89))).isEqualTo(AuditDecisionPolicy.Outcome.MANUAL_REVIEW);}
 @Test void modelReviewAlwaysRoutesManual(){assertThat(p.decide(r(ModerationDecision.REVIEW,1))).isEqualTo(AuditDecisionPolicy.Outcome.MANUAL_REVIEW);}
}
