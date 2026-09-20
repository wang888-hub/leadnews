package com.aaliyun.leadnews.behavior;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.aaliyun.leadnews.behavior.mapper")
@EnableFeignClients(basePackages="com.aaliyun.leadnews.feign")
public class BehaviorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BehaviorServiceApplication.class, args);
    }
}
