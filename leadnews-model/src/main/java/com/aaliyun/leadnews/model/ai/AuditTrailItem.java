package com.aaliyun.leadnews.model.ai;
import java.time.LocalDateTime;import java.util.List;
public record AuditTrailItem(long auditVersion,String auditStage,String decision,String riskLevel,String reason,Double confidence,List<String> riskTags,String model,String errorCode,Long reviewerId,LocalDateTime createdTime) {}
