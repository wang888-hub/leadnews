package com.aaliyun.leadnews.ai.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties("leadnews.ai.audit")
public record ArticleAuditProperties(
        @Min(1) int maxImages,
        @Min(1) int maxTextLength,
        @NotBlank String bucket,
        @NotNull List<@NotBlank String> trustedPrefixes,
        @Min(1024) long maxSourceImageBytes,
        @Min(1024) int maxAuditImageBytes,
        @Min(1024) long maxTotalImageBytes,
        @Min(1024) long maxRequestBytes,
        @Min(64) int maxEdge,
        @DecimalMin("0.1") @DecimalMax("1.0") double jpegQuality) {
}
