package com.aaliyun.leadnews.model.ai;
import jakarta.validation.constraints.*;import java.util.List;
public record ArticleAuditRequest(@NotNull Long newsId,@Min(1) long auditVersion,@NotBlank @Size(max=128) String title,@Size(max=255) String labels,@NotBlank String textContent,@NotNull List<@NotBlank String> imageObjectKeys) {}
