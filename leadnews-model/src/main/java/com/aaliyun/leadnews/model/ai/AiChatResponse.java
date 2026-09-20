package com.aaliyun.leadnews.model.ai;
public record AiChatResponse(String model,String content,String requestId,long latencyMs,AiUsage usage) {}
