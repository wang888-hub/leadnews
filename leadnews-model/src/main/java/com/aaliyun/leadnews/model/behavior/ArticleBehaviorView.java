package com.aaliyun.leadnews.model.behavior;

public record ArticleBehaviorView(long likeCount, long viewCount, long commentCount,
                                  long collectCount, boolean liked, boolean realtime) {}
