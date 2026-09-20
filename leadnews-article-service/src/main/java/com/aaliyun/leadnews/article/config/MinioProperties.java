package com.aaliyun.leadnews.article.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("leadnews.minio")
public record MinioProperties(String endpoint,String publicEndpoint,String accessKey,String secretKey,String bucket) {}
