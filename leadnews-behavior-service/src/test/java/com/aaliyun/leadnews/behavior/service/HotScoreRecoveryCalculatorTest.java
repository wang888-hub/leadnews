package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.BehaviorProperties;
import com.aaliyun.leadnews.behavior.config.HotProperties;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HotScoreRecoveryCalculatorTest {
 private final HotScoreRecoveryCalculator calculator=new HotScoreRecoveryCalculator(new BehaviorProperties(),new HotProperties());
 @Test void estimatesPositiveCountDeltasWithFirstInteractionWeights(){
  assertThat(calculator.estimatedDelta(10,2,4,200)).isEqualTo(10*2+2*5+4*5+200);
 }
 @Test void negativeLikeAndCollectUseCancellationWeights(){
  assertThat(calculator.estimatedDelta(-5,-3,0,0)).isEqualTo(-5-6);
 }
 @Test void decaysSnapshotForFullIntervalAndEstimatedDeltaForMidpoint(){
  double actual=calculator.recover(500,100,30);
  assertThat(actual).isCloseTo(500*Math.pow(.99,30)+100*Math.pow(.99,15),
   org.assertj.core.data.Offset.offset(1e-9));
 }
}
