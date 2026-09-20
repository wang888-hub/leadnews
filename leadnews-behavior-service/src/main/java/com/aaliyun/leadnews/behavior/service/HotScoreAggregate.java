package com.aaliyun.leadnews.behavior.service;

/** Changelog-backed value for one article's event-time window. */
public record HotScoreAggregate(Long channelId, double deltaScore) {
}
