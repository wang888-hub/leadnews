package com.aaliyun.leadnews.ai;
import com.aaliyun.leadnews.ai.config.AiProperties;import java.time.Duration;import java.util.List;
public final class TestFixtures {private TestFixtures(){} public static AiProperties properties(Duration timeout,int retries,int concurrency){return new AiProperties("qwen3.8-max",.3,1024,timeout,Duration.ofSeconds(2),retries,Duration.ZERO,concurrency,1,1024*1024,List.of("localhost"));}}
