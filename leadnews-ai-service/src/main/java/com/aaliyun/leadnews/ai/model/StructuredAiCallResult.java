package com.aaliyun.leadnews.ai.model;
import com.aaliyun.leadnews.model.ai.AiUsage;
public record StructuredAiCallResult<T>(T value,String requestId,long latencyMs,AiUsage usage) {}
