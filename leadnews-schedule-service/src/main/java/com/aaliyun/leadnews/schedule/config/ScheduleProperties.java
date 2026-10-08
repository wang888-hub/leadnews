package com.aaliyun.leadnews.schedule.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("leadnews.schedule")
public record ScheduleProperties(
        @NotNull Duration scanInterval,
        @NotNull Duration reconcileInterval,
        @NotNull Duration retryDelay,
        @NotNull Duration runningTimeout,
        @Min(1) int maxRetries,
        @Min(1) int scanBatchSize,
        @Min(1) int workerCoreSize,
        @Min(1) int workerMaxSize,
        @Min(1) int workerQueueCapacity) {
}
