package com.aaliyun.leadnews.wemedia.config;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
@Validated @ConfigurationProperties("audit.resilience")
public record AuditResilienceProperties(@Min(1) int maxConcurrent,@NotNull Duration acquireTimeout,
 @Min(1) int failureThreshold,@NotNull Duration openDuration,@Min(1) int halfOpenPermits,
 @Min(1) int manualReviewBacklogLimit,@NotNull Duration capacityRetryBackoff,@NotNull Duration circuitRetryBackoff,
 @Min(1) int invalidResponseMaxAttempts){ }
