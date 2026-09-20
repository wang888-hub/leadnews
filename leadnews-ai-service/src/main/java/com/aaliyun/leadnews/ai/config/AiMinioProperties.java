package com.aaliyun.leadnews.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("leadnews.ai.minio")
public record AiMinioProperties(String endpoint, String accessKey, String secretKey) {
}