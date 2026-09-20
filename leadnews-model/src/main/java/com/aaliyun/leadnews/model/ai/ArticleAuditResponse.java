package com.aaliyun.leadnews.model.ai;
public record ArticleAuditResponse(ArticleAuditResult result,String model,String providerRequestId,long latencyMs,AiUsage usage) {}
