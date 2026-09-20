package com.aaliyun.leadnews.model.ai;
import java.time.Instant;
public record ArticlePublishedEvent(String eventId,Long articleId,long summaryVersion,Instant publishedAt,Instant occurredAt,String traceId){}
