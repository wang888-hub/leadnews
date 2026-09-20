package com.aaliyun.leadnews.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.aaliyun.leadnews.ai.config.AiBusinessProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@EnableConfigurationProperties(AiBusinessProperties.class)
@ConfigurationPropertiesScan
public class AiServiceApplication {
    public static void main(String[] args) {

        SpringApplication.run(AiServiceApplication.class, args);
    }
}
