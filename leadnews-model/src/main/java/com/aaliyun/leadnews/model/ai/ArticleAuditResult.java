package com.aaliyun.leadnews.model.ai;
import jakarta.validation.constraints.*;import java.util.List;
public record ArticleAuditResult(@NotNull ModerationDecision decision,@NotNull RiskLevel riskLevel,@NotBlank String reason,@DecimalMin("0.0") @DecimalMax("1.0") double confidence,@NotNull List<@NotBlank String> riskTags) {}
