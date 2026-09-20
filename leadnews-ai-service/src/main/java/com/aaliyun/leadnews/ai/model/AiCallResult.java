package com.aaliyun.leadnews.ai.model;
import com.aaliyun.leadnews.model.ai.AiUsage;
public record AiCallResult(String content,String requestId,long latencyMs,AiUsage usage) {}
