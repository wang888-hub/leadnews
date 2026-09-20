package com.aaliyun.leadnews.ai.config;
import org.springframework.boot.SpringApplication;import org.springframework.boot.env.EnvironmentPostProcessor;import org.springframework.core.Ordered;import org.springframework.core.env.*;import java.util.Map;
public class MissingKeyEnvironmentPostProcessor implements EnvironmentPostProcessor,Ordered {
 private static final String EXCLUDES=String.join(",",
  "com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatAutoConfiguration",
  "com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAgentAutoConfiguration",
  "com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeImageAutoConfiguration",
  "com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeVideoAutoConfiguration",
  "com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAudioTranscriptionAutoConfiguration",
  "com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAudioSpeechAutoConfiguration",
  "com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeEmbeddingAutoConfiguration",
  "com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeRerankAutoConfiguration");
 public void postProcessEnvironment(ConfigurableEnvironment environment,SpringApplication application){
  String key=System.getenv("API-KEY");
  if(key==null||key.isBlank()){
   environment.getPropertySources().addFirst(new MapPropertySource("leadnewsAiMissingKey",Map.of("spring.autoconfigure.exclude",EXCLUDES)));
  }else{
   environment.getPropertySources().addFirst(new MapPropertySource("leadnewsAiExactApiKey",Map.of("spring.ai.dashscope.api-key",key,"leadnews.ai.runtime-api-key",key)));
  }
 }
 public int getOrder(){return Ordered.HIGHEST_PRECEDENCE;}
}
