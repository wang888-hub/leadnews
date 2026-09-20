package com.aaliyun.leadnews.model.search;

import java.time.Instant;

public record ArticleSearchEvent(String eventId, Long articleId, EventType eventType,
                                 Instant occurredAt, String traceId) {
    public enum EventType { UPSERT, DELETE }
}
