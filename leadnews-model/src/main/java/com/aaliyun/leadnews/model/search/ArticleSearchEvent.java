package com.aaliyun.leadnews.model.search;

import java.time.Instant;

public record ArticleSearchEvent(String eventId, Long articleId, long articleVersion, EventType eventType,
                                 Instant occurredAt, String traceId) {
    public enum EventType { UPSERT, DELETE }
}
