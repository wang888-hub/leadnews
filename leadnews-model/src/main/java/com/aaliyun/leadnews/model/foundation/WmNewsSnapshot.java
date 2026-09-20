package com.aaliyun.leadnews.model.foundation;

import java.time.LocalDateTime;
import java.util.List;
import com.aaliyun.leadnews.model.ai.AuditTrailItem;

public record WmNewsSnapshot(Long id, Long userId, String authorName, String title,
                             List<ArticleContentItemDTO> content, Integer layout, Long channelId,
                             String labels, List<String> coverImages, String status,
                             LocalDateTime submittedTime, LocalDateTime publishTime, String reason,
                             Long auditVersion,String auditSource,List<AuditTrailItem> auditTrail) {}
