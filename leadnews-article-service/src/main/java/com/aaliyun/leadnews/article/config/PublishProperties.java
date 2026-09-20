package com.aaliyun.leadnews.article.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("leadnews.publish")
public record PublishProperties(int maxRetries, Duration timeout, int cacheMaxAgeSeconds) {}
