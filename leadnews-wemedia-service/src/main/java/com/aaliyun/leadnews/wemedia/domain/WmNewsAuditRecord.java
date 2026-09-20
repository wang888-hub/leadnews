package com.aaliyun.leadnews.wemedia.domain;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("wm_news_audit_record") public class WmNewsAuditRecord {
 @TableId(type=IdType.AUTO)private Long id;
 private Long newsId;private Long auditVersion;private Integer attemptNo;private Long sensitiveWordVersion;private String auditStage;private String decision;
 private String riskLevel;private String reason;private Double confidence;private String riskTags;private String matchedWords;
 private String model;private String providerRequestId;private Long latencyMs;private String errorCode;private Long reviewerId;
 private LocalDateTime reviewedTime;private LocalDateTime createdTime;
 public void setNewsId(Long v){newsId=v;}public Long getNewsId(){return newsId;}
 public void setAuditVersion(Long v){auditVersion=v;}public Long getAuditVersion(){return auditVersion;}
 public void setAttemptNo(Integer v){attemptNo=v;}public Integer getAttemptNo(){return attemptNo;}
 public void setSensitiveWordVersion(Long v){sensitiveWordVersion=v;}public Long getSensitiveWordVersion(){return sensitiveWordVersion;}
 public void setAuditStage(String v){auditStage=v;}public String getAuditStage(){return auditStage;}
 public void setDecision(String v){decision=v;}public String getDecision(){return decision;}
 public void setRiskLevel(String v){riskLevel=v;}public String getRiskLevel(){return riskLevel;}
 public void setReason(String v){reason=v;}public String getReason(){return reason;}
 public void setConfidence(Double v){confidence=v;}public Double getConfidence(){return confidence;}
 public void setRiskTags(String v){riskTags=v;}public String getRiskTags(){return riskTags;}
 public void setMatchedWords(String v){matchedWords=v;}public String getMatchedWords(){return matchedWords;}
 public void setModel(String v){model=v;}public String getModel(){return model;}
 public void setProviderRequestId(String v){providerRequestId=v;}public void setLatencyMs(Long v){latencyMs=v;}
 public void setErrorCode(String v){errorCode=v;}public String getErrorCode(){return errorCode;}
 public void setReviewerId(Long v){reviewerId=v;}public Long getReviewerId(){return reviewerId;}
 public void setReviewedTime(LocalDateTime v){reviewedTime=v;}public LocalDateTime getCreatedTime(){return createdTime;}
}
