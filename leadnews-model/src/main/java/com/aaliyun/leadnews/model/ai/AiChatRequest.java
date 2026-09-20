package com.aaliyun.leadnews.model.ai;import jakarta.validation.constraints.*;
public record AiChatRequest(@NotBlank @Size(max=20000) String prompt) {}
