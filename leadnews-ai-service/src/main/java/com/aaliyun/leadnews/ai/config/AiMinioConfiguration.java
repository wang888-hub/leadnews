package com.aaliyun.leadnews.ai.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.*;

@Configuration
public class AiMinioConfiguration {

    @Bean
    MinioClient aiMinioClient(AiMinioProperties p) {
        return MinioClient.builder()
                .endpoint(p.endpoint())
                .credentials(p.accessKey(), p.secretKey())
                .build();
    }
}