package com.aaliyun.leadnews.wemedia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.aaliyun.leadnews.wemedia.config.ContinuationProperties;

@SpringBootApplication
@MapperScan("com.aaliyun.leadnews.wemedia.mapper")
@EnableScheduling
@EnableFeignClients(basePackages="com.aaliyun.leadnews.feign")
@EnableConfigurationProperties(ContinuationProperties.class)
public class WemediaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WemediaServiceApplication.class, args);
    }
}
