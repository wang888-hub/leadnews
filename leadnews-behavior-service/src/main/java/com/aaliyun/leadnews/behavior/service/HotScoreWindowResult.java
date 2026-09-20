package com.aaliyun.leadnews.behavior.service;

public record HotScoreWindowResult(String windowEventId, long articleId, long channelId,
                                   long windowStart, long windowEnd, double deltaScore) {
    public static HotScoreWindowResult of(long articleId, HotScoreAggregate aggregate,
                                          long windowStart, long windowEnd) {
        return new HotScoreWindowResult(articleId + ":" + windowStart + ":" + windowEnd,
                articleId, aggregate.channelId(), windowStart, windowEnd, aggregate.deltaScore());
    }
}
