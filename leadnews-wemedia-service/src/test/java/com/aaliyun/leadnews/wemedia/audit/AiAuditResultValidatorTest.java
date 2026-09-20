package com.aaliyun.leadnews.wemedia.audit;
import com.aaliyun.leadnews.model.ai.*;import org.junit.jupiter.api.Test;import java.util.List;import static org.assertj.core.api.Assertions.*;
class AiAuditResultValidatorTest {
 private final AiAuditResultValidator v=new AiAuditResultValidator();
 @Test void rejectsConfidenceOutsideRange(){assertThatThrownBy(()->v.validate(new ArticleAuditResult(ModerationDecision.PASS,RiskLevel.LOW,"ok",1.7,List.of()))).isInstanceOf(AiAuditExceptions.InvalidResponse.class);assertThatThrownBy(()->v.validate(new ArticleAuditResult(ModerationDecision.PASS,RiskLevel.LOW,"ok",-.2,List.of()))).isInstanceOf(AiAuditExceptions.InvalidResponse.class);}
 @Test void rejectsContradictoryCombinationsAndBlankReason(){assertThatThrownBy(()->v.validate(new ArticleAuditResult(ModerationDecision.PASS,RiskLevel.HIGH,"ok",.9,List.of()))).isInstanceOf(AiAuditExceptions.InvalidResponse.class);assertThatThrownBy(()->v.validate(new ArticleAuditResult(ModerationDecision.REJECT,RiskLevel.HIGH," ",.9,List.of()))).isInstanceOf(AiAuditExceptions.InvalidResponse.class);}
 @Test void acceptsCoherentSignal(){assertThatCode(()->v.validate(new ArticleAuditResult(ModerationDecision.REVIEW,RiskLevel.MEDIUM,"人工复核",.7,List.of("ambiguous")))).doesNotThrowAnyException();}
}
