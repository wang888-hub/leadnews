package com.aaliyun.leadnews.behavior.service;

import com.aaliyun.leadnews.behavior.config.HotProperties;
import com.aaliyun.leadnews.model.behavior.BehaviorEvent;
import com.aaliyun.leadnews.model.behavior.BehaviorType;
import org.springframework.stereotype.Component;

/** Converts immutable behavior events into configurable hot-score increments. */
@Component
public class HotScoreDeltaCalculator {
 public double calculate(BehaviorEvent e,HotProperties p){
  BehaviorType type=e.behaviorType();
  return switch(type){
   case VIEW -> e.delta()>0?p.getViewWeight()*e.delta():0;
   case LIKE, UNLIKE -> requiredHeatDelta(e);
   case COMMENT -> e.delta()>0?p.getCommentWeight()*e.delta():0;
   case COLLECT, UNCOLLECT -> requiredHeatDelta(e);
  };
 }
 private double requiredHeatDelta(BehaviorEvent e){
  if(e.heatDelta()==null || !Double.isFinite(e.heatDelta()))
   throw new IllegalArgumentException("Missing heatDelta for reaction event " + e.eventId());
  return e.heatDelta();
 }
}
