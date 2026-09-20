package com.aaliyun.leadnews.wemedia.config;import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("leadnews.ai.continuation") public record ContinuationProperties(String aiBaseUrl,int minTargetLength,int maxTargetLength,int maxInputLength,int maxInstructionLength){}
