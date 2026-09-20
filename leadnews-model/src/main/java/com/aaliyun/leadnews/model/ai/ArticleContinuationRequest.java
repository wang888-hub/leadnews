package com.aaliyun.leadnews.model.ai;
import jakarta.validation.constraints.*;
public record ArticleContinuationRequest(@NotBlank @Size(max=128)String title,@NotBlank String currentContent,@NotBlank String instruction,@Positive int targetLength){}
