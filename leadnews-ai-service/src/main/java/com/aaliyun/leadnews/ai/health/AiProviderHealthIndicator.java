package com.aaliyun.leadnews.ai.health;
import com.aaliyun.leadnews.ai.config.AiProperties;import org.springframework.beans.factory.annotation.Value;import org.springframework.boot.actuate.health.*;import org.springframework.stereotype.Component;
@Component("aiProvider") public class AiProviderHealthIndicator implements HealthIndicator {
 private final String apiKey;private final AiProperties p;public AiProviderHealthIndicator(@Value("${leadnews.ai.runtime-api-key:}")String apiKey,AiProperties p){this.apiKey=apiKey;this.p=p;}
 public Health health(){return (apiKey==null||apiKey.isBlank()?Health.status(new Status("NOT_CONFIGURED")):Health.up()).withDetail("model",p.model()).withDetail("configured",apiKey!=null&&!apiKey.isBlank()).build();}
}
