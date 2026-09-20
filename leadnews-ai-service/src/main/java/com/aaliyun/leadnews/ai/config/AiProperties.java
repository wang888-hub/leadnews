package com.aaliyun.leadnews.ai.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties("leadnews.ai")
public record AiProperties(
        @NotBlank String model,
        @DecimalMin("0.0") @DecimalMax("2.0") double temperature,
        @Min(1) int maxTokens,
        @NotNull Duration timeout,
        @NotNull Duration streamTimeout,
        @Min(0) @Max(2) int maxRetries,
        @NotNull Duration retryBackoff,
        @Min(1) int maxConcurrentCalls,
        @Min(1) @Max(4) int maxImages,
        @Min(1024) long maxImageBytes,
        @NotNull List<String> allowedImageHosts) {
}