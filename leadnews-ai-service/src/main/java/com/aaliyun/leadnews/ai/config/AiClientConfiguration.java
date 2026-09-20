package com.aaliyun.leadnews.ai.config;

import com.aaliyun.leadnews.ai.client.AiModelClient;
import com.aaliyun.leadnews.ai.exception.AiExceptions;
import com.aaliyun.leadnews.ai.model.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import reactor.core.publisher.Flux;

@Configuration
public class AiClientConfiguration {

 @Bean
 @ConditionalOnMissingBean(AiModelClient.class)
 AiModelClient notConfiguredAiModelClient() {
  return new AiModelClient() {

   private <T> T unavailable() {
    throw new AiExceptions.NotConfigured();
   }

   public AiCallResult chat(String s, String u) {
    return unavailable();
   }

   public <T> T structuredChat(String s, String u, Class<T> t) {
    return unavailable();
   }

   public <T> StructuredAiCallResult<T> structuredChatResult(String s, String u, Class<T> t) {
    return unavailable();
   }

   public <T> StructuredAiCallResult<T> structuredVision(String s,
                                                         String u,
                                                         java.util.List<AiImageInput> i,
                                                         Class<T> t) {
    return unavailable();
   }

   public AiCallResult vision(String s, String u, AiImageInput i) {
    return unavailable();
   }

   public Flux<String> stream(String s, String u) {
    return Flux.error(new AiExceptions.NotConfigured());
   }
  };
 }
}