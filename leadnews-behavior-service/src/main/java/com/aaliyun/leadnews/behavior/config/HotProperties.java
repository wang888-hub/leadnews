package com.aaliyun.leadnews.behavior.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("leadnews.hot")
public class HotProperties {

 private int topN = 50;
 private int offlineDays = 30;

 private Duration realtimeWindow = Duration.ofSeconds(5);
 private Duration grace = Duration.ofSeconds(10);
 private Duration appliedRetention = Duration.ofDays(7);
 private boolean snapshotEnabled = true;
 private int snapshotRetentionDays = 7;
 private double minScore = 0.01;
 private double coolingFactor = 0.99;
 private double unlikePenalty = 1;
 private double uncollectPenalty = 1;

 private double viewWeight = 1;
 private double likeWeight = 3;
 private double commentWeight = 5;
 private double collectWeight = 2;

 public int getTopN() {
  return topN;
 }

 public void setTopN(int v) {
  topN = v;
 }

 public int getOfflineDays() {
  return offlineDays;
 }

 public void setOfflineDays(int v) {
  offlineDays = v;
 }

 public Duration getRealtimeWindow() {
  return realtimeWindow;
 }
 public Duration getGrace(){return grace;}
 public void setGrace(Duration v){grace=v;}
 public Duration getAppliedRetention(){return appliedRetention;}
 public void setAppliedRetention(Duration v){appliedRetention=v;}
 public boolean isSnapshotEnabled(){return snapshotEnabled;}
 public void setSnapshotEnabled(boolean v){snapshotEnabled=v;}
 public int getSnapshotRetentionDays(){return snapshotRetentionDays;}
 public void setSnapshotRetentionDays(int v){snapshotRetentionDays=v;}
 public double getMinScore(){return minScore;}
 public void setMinScore(double v){minScore=v;}
 public double getCoolingFactor(){return coolingFactor;}
 public void setCoolingFactor(double v){coolingFactor=v;}
 public double getUnlikePenalty(){return unlikePenalty;}
 public void setUnlikePenalty(double v){unlikePenalty=v;}
 public double getUncollectPenalty(){return uncollectPenalty;}
 public void setUncollectPenalty(double v){uncollectPenalty=v;}

 public void setRealtimeWindow(Duration v) {
  realtimeWindow = v;
 }

 public double getViewWeight() {
  return viewWeight;
 }

 public void setViewWeight(double v) {
  viewWeight = v;
 }

 public double getLikeWeight() {
  return likeWeight;
 }

 public void setLikeWeight(double v) {
  likeWeight = v;
 }

 public double getCommentWeight() {
  return commentWeight;
 }

 public void setCommentWeight(double v) {
  commentWeight = v;
 }

 public double getCollectWeight() {
  return collectWeight;
 }

 public void setCollectWeight(double v) {
  collectWeight = v;
 }

}
