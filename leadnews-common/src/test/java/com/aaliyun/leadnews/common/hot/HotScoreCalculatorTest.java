package com.aaliyun.leadnews.common.hot;
import org.junit.jupiter.api.Test;import java.time.Instant;import static org.assertj.core.api.Assertions.*;
class HotScoreCalculatorTest {private final HotScoreCalculator c=new HotScoreCalculator(1,5,8,10,.35,2);
 @Test void appliesWeightsAndDeterministicDecay(){Instant now=Instant.parse("2026-09-07T12:00:00Z");double score=c.calculate(100,10,2,1,now.minusSeconds(3600),now);assertThat(score).isCloseTo(177/Math.pow(3,.35),within(1e-9));}
 @Test void newerArticleWinsWhenBehaviorIsEqual(){Instant now=Instant.parse("2026-09-07T12:00:00Z");assertThat(c.calculate(10,1,0,0,now.minusSeconds(3600),now)).isGreaterThan(c.calculate(10,1,0,0,now.minusSeconds(86400),now));}
 @Test void negativeCountersCannotReduceScore(){Instant now=Instant.parse("2026-09-07T12:00:00Z");assertThat(c.calculate(-1,-2,-3,-4,now,now)).isPositive();}
}
