package com.aaliyun.leadnews.model.ai;
public record ArticleSummaryResponse(String summary,String model,String providerRequestId,long latencyMs,AiUsage usage){}
