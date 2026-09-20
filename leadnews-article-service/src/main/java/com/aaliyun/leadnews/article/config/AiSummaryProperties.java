package com.aaliyun.leadnews.article.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("leadnews.ai.summary")
public record AiSummaryProperties(
        boolean enabled,
        int minLength,
        int maxLength,
        int maxRetries,
        Duration runningTimeout,
        long dispatchIntervalMs,
        int backfillPageSize) {
}