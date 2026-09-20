package com.aaliyun.leadnews.model.ai;
import jakarta.validation.constraints.*;
public record ArticleSummaryRequest(@NotNull Long articleId,@Positive long summaryVersion,@NotBlank @Size(max=128)String title,@Size(max=255)String labels,@NotBlank String textContent){}
