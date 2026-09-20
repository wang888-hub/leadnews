package com.aaliyun.leadnews.model.ai;import java.time.Instant;public record WmNewsAuditRequestedEvent(String eventId,Long newsId,long auditVersion,Instant occurredAt,String traceId) {}
