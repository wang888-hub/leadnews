package com.aaliyun.leadnews.model.search;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleSearchDocument(Long articleId, long articleVersion, boolean deleted, String title, String content, String summary,
                                    Long authorId, String authorName, Long channelId, String channelName,
                                    List<String> labels, LocalDateTime publishTime, String staticUrl,
                                    LocalDateTime createdTime) {}
