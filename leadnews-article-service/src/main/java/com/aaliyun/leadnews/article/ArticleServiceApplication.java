package com.aaliyun.leadnews.article;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.mybatis.spring.annotation.MapperScan;
import com.aaliyun.leadnews.feign.user.UserFeignClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.aaliyun.leadnews.article.config.AiSummaryProperties;
import com.aaliyun.leadnews.article.config.SearchSyncProperties;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.aaliyun.leadnews.feign")
@MapperScan("com.aaliyun.leadnews.article.mapper")
@EnableScheduling
@EnableConfigurationProperties({AiSummaryProperties.class,SearchSyncProperties.class})
public class ArticleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArticleServiceApplication.class, args);
    }
}
