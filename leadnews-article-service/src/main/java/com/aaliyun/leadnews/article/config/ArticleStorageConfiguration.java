package com.aaliyun.leadnews.article.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

@Configuration
@EnableConfigurationProperties({MinioProperties.class, PublishProperties.class})
public class ArticleStorageConfiguration {

 @Bean
 MinioClient minioClient(MinioProperties p) {
  return MinioClient.builder()
          .endpoint(p.endpoint())
          .credentials(p.accessKey(), p.secretKey())
          .build();
 }
}