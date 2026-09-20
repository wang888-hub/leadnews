package com.aaliyun.leadnews.model.behavior;

import java.time.Instant;

public record BehaviorEvent(String eventId, Long articleId, Long userId, Long channelId,
                            BehaviorType behaviorType, int delta,
                            Instant occurredAt, String traceId,
                            Boolean liked, Long likeCount, Long relationVersion,
                            Boolean collected, Long collectCount, Long countVersion, Double heatDelta) {
    public BehaviorEvent(String eventId, Long articleId, Long userId, BehaviorType behaviorType,
                         int delta, Instant occurredAt, String traceId) {
        this(eventId, articleId, userId, null, behaviorType, delta, occurredAt, traceId, null, null, null, null, null, null, null);
    }
    public BehaviorEvent(String eventId, Long articleId, Long userId, Long channelId,
                         BehaviorType behaviorType, int delta, Instant occurredAt, String traceId) {
        this(eventId, articleId, userId, channelId, behaviorType, delta, occurredAt, traceId, null, null, null, null, null, null, null);
    }
    public BehaviorEvent(String eventId, Long articleId, Long userId, BehaviorType behaviorType,
                         int delta, Instant occurredAt, String traceId,
                         Boolean liked, Long likeCount, Long relationVersion) {
        this(eventId, articleId, userId, null, behaviorType, delta, occurredAt, traceId, liked, likeCount, relationVersion, null, null, null, null);
    }
    public BehaviorEvent(String eventId, Long articleId, Long userId, Long channelId,
                         BehaviorType behaviorType, int delta, Instant occurredAt, String traceId,
                         Boolean liked, Long likeCount, Long relationVersion, Double heatDelta) {
        this(eventId, articleId, userId, channelId, behaviorType, delta, occurredAt, traceId,
                liked, likeCount, relationVersion, null, null, null, heatDelta);
    }
}
