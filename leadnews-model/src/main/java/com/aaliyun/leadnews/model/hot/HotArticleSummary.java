package com.aaliyun.leadnews.model.hot;
import java.time.LocalDateTime;
public record HotArticleSummary(Long articleId,Long channelId,String title,String summary,String staticUrl,LocalDateTime publishTime) {}
