package com.aaliyun.leadnews.model.foundation;

public record PublishResponse(Long articleId, String status, String staticObjectKey,
                              String staticUrl, boolean alreadyPublished) {}
