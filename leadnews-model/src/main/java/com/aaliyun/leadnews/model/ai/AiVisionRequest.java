package com.aaliyun.leadnews.model.ai;import jakarta.validation.constraints.*;
public record AiVisionRequest(@NotBlank String prompt,@NotBlank String mimeType,byte[] bytes,String url) {}
