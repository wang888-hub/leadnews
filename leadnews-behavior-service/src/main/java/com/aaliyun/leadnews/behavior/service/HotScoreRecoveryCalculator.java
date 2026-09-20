package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.BehaviorProperties;
import com.aaliyun.leadnews.behavior.config.HotProperties;
import org.springframework.stereotype.Component;

/** Approximate disaster recovery only; the real-time path always consumes BehaviorEvent.heatDelta. */
@Component
public class HotScoreRecoveryCalculator {
 private final BehaviorProperties behavior;
 private final HotProperties hot;
 public HotScoreRecoveryCalculator(BehaviorProperties behavior,HotProperties hot){this.behavior=behavior;this.hot=hot;}
 public double estimatedDelta(long like,long collect,long comment,long view){
  return signed(like,behavior.getLikeFirstHeat(),-behavior.getLikeCancelHeat())
   +signed(collect,behavior.getCollectFirstHeat(),-behavior.getCollectCancelHeat())
   +Math.max(0,comment)*hot.getCommentWeight()+Math.max(0,view)*hot.getViewWeight();
 }
 private double signed(long delta,double positive,double negative){return delta>=0?delta*positive:-delta*negative;}
 public double recover(double snapshotScore,double estimatedDelta,double elapsedMinutes){
  double elapsed=Math.max(0,elapsedMinutes);
  return snapshotScore*Math.pow(hot.getCoolingFactor(),elapsed)
   +estimatedDelta*Math.pow(hot.getCoolingFactor(),elapsed/2D);
 }
}
